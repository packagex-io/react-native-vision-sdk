package io.packagex.visionsdk.ocr.ml.process.sl

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.asadullah.androidsecurity.AES
import com.asadullah.androidsecurity.RSA
import com.asadullah.androidsecurity.decodeHex
import com.asadullah.androidsecurity.enums.Efficiency
import com.asadullah.handyutils.SDKHelper
import com.asadullah.handyutils.capitalizeWords
import com.asadullah.handyutils.chunked
import com.asadullah.handyutils.findWithObjectOrNull
import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.isNetworkAvailable
import com.asadullah.handyutils.isNullOrEmptyOrBlank
import com.asadullah.handyutils.toReadableDuration
import com.scottyab.rootbeer.RootBeer
import io.packagex.visionsdk.ApiManager
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.ocr.courier.Courier
import io.packagex.visionsdk.ocr.courier.LabelsExtraction
import io.packagex.visionsdk.ocr.courier.RegexResult
import io.packagex.visionsdk.ocr.courier.allCouriers
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.PlatformType
import io.packagex.visionsdk.ocr.ml.core.onnx.VisionOrtSession
import io.packagex.visionsdk.ocr.ml.dto.ExchangeInfo
import io.packagex.visionsdk.ocr.ml.dto.MLResults
import io.packagex.visionsdk.ocr.ml.dto.PredictionResult
import io.packagex.visionsdk.ocr.ml.enums.ExecutionProvider
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.ocr.security.SecurityWorker
import io.packagex.visionsdk.preferences.VisionSDKSettings
import io.packagex.visionsdk.service.response.ConnectResponse
import io.packagex.visionsdk.utils.DownloadUtils
import io.packagex.visionsdk.utils.TAG
import io.packagex.visionsdk.utils.WORKER_PARAM_MODEL_CLASS
import io.packagex.visionsdk.utils.WORKER_PARAM_MODEL_SIZE
import io.packagex.visionsdk.utils.getAndroidDeviceId
import io.packagex.visionsdk.utils.toSafeString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Timer
import javax.crypto.SecretKey
import kotlin.concurrent.timerTask
import kotlin.time.measureTime

internal abstract class SLModel(context: Context, private val locationProcessor: LocationProcessor) {

    private val mas = "ThjgMuEL3D4yURfF9Q8a2debs5P6KzcS"

    private val longLivePalestine: SecretKey by lazy {
        val aes = AES()
        val longLivePalestine = aes.getSecretKey("LongLivePalestine")
        if (longLivePalestine == null) {
            aes.generateAndStoreSecretKey("LongLivePalestine")
            aes.getSecretKey("LongLivePalestine")!!
        } else {
            longLivePalestine
        }
    }

    private val mainDir by lazy { File(context.filesDir, "VisionSDKModels").also { it.mkdirs() } }

    private val apiManager by lazy { ApiManager() }

    private var invalidateForeverDays = 5L

    private var invalidateTimeInMillis = 300_000L // 5 minutes
    private var freeMemoryTimer: Timer? = null

    private var pingTimeInMillis = 1_800_000L // 30 minutes
    private var pingTimer: Timer? = null

    protected abstract fun getModelClass(): ModelClass

    protected abstract fun getModelSize(): ModelSize

    protected abstract suspend fun predict(
        ortEnvironment: OrtEnvironment,
        ortSession: OrtSession,
        locationProcessor: LocationProcessor,
        bitmap: Bitmap,
        barcodes: List<String>
    ): PredictionResult

    protected abstract fun cleanUp()

    protected open fun loadRequiredData(context: Context) {}

    private fun checkForRootedDevice(context: Context) {
        if (RootBeer(context).isRooted) {
            permanentlyDeleteAllModels()
            throw VisionSDKException.RootDeviceDetected
        }
    }

    @SuppressLint("HardwareIds")
    private suspend fun makeConnectCall(
        context: Context,
        apiKey: String?,
        token: String?,
        platformType: PlatformType,
    ): ConnectResponse {

        checkForRootedDevice(context)

        makeTelemetryCall(context, platformType)

        val modelUsage = VisionSDKSettings.getOnDeviceModelUsages().firstOrNull { it.modelClass == getModelClass() && it.modelSize == getModelSize() }

        val connectResponse = apiManager.connectCallSync(
            apiKey = apiKey,
            token = token,
            sdkId = VisionSDK.getInstance().environment.sdkId,
            deviceId = context.getAndroidDeviceId(),
            platformType = platformType,
            modelToRequest = ApiManager.ModelToRequest(
                modelClass = getModelClass(),
                modelSize = getModelSize(),
                getDownloadLink = isModelAlreadyDownloaded().not(),
                usageCounter = modelUsage?.usageCount ?: 0,
                usageDuration = modelUsage?.usageDuration ?: 0L
            ),
        ) ?: throw VisionSDKException.UnknownException(RuntimeException("Something went terribly wrong. Sorry!"))

        if (connectResponse.status != 200) {
            val errorMessageBuilder = StringBuilder()
            errorMessageBuilder.append("API responded with")
                .append('\n')
                .append("Status: ${connectResponse.status}")
                .append('\n')
                .append("Code: ${connectResponse.code}")
                .append('\n')
                .append("Message: ${connectResponse.message}")

            val errorToThrow = when (connectResponse.code) {
                "sdk.platform.invalid" -> VisionSDKException.SdkInvalidPlatformException(errorMessageBuilder.toString())
                "sdk.disabled" -> VisionSDKException.SdkDisabledException(errorMessageBuilder.toString())
                "sdk.model.invalid" -> VisionSDKException.SdkInvalidModelException(errorMessageBuilder.toString())
                "sdk.model.version.invalid" -> VisionSDKException.SdkInvalidModelVersionException(errorMessageBuilder.toString())
                "sdk.in_cloud.disabled" -> VisionSDKException.SdkCloudDisabledException(errorMessageBuilder.toString())
                "sdk.on_device.disabled" -> VisionSDKException.SdkOnDeviceDisabledException(errorMessageBuilder.toString())
                "sdk.processing.disabled" -> VisionSDKException.SdkProcessingDisabledException(errorMessageBuilder.toString())
                else -> VisionSDKException.UnknownException(RuntimeException(errorMessageBuilder.toString()))
            }

            errorToThrow.printStackTrace()
            permanentlyDeleteGivenModel()

            throw errorToThrow
        }

        VisionSDKSettings.clearOnDeviceModelUsages()

        if (isUserAllowedToConfigure(connectResponse).not()) {
            Log.d(TAG, "User not allowed to configure model. Deleting the model.")
            permanentlyDeleteGivenModel()
            throw VisionSDKException.UserRestrictedException
        }

        connectResponse.data?.u.ifNeitherNullNorEmptyNorBlank {
            VisionSDKSettings.setLicenseCheckDateTime(
                LocalDateTime.parse(
                    it.substringBefore("."),
                    DateTimeFormatter.ISO_DATE_TIME
                )
            )
        } ?: throw VisionSDKException.VitalInformationMissingException

        connectResponse.data?.rq?.i.ifNeitherNullNorEmptyNorBlank { modelId ->
            connectResponse.data?.rq?.mv?.i.ifNeitherNullNorEmptyNorBlank { modelVersionId ->
                VisionSDKSettings.setModelIdAndModelVersionId(
                    modelClass = getModelClass(),
                    modelSize = getModelSize(),
                    modelId = modelId,
                    modelVersionId = modelVersionId
                )
            } ?: VisionSDKException.VitalInformationMissingException
        } ?: VisionSDKException.VitalInformationMissingException

        connectResponse.data?.c?.let { pingTimeInMinutes ->
            pingTimeInMillis = pingTimeInMinutes * 3600L // converting minutes into millis
        }

        connectResponse.data?.i?.let { maxInactivityTimeInMinutes ->
            invalidateTimeInMillis = maxInactivityTimeInMinutes * 3600L // converting minutes into millis
        }

        connectResponse.data?.o?.let { maxOfflineTimeInMinutes ->
            invalidateForeverDays = maxOfflineTimeInMinutes / 1440L // 1440 minutes in a day
        }

        return connectResponse
    }

    private suspend fun makeTelemetryCall(context: Context, platformType: PlatformType) {
        val telemetryDataToPost = VisionSDKSettings.getTelemetryData()
        telemetryDataToPost.chunked(20).forEach {
            try {
                apiManager.internalTelemetryCallSync(
                    sdkId = VisionSDK.getInstance().environment.sdkId,
                    deviceId = context.getAndroidDeviceId(),
                    platformType = platformType,
                    telemetryDataList = it,
                )
                VisionSDKSettings.removeTelemetryData(it)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
    }

    private fun isUserAllowedToConfigure(connectResponse: ConnectResponse): Boolean {
        return connectResponse.data?.b?.e == true && connectResponse.data.b.d == true && (
                with(connectResponse.data.b.le) {
                    if (this == null) return@with true
                    val expiryDate = LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME)
                    return LocalDateTime.now().isBefore(expiryDate)
                }
                )
    }

    private fun isConfigured() = VisionOrtSession.isConfigured()

    suspend fun configure(
        context: Context,
        apiKey: String? = null,
        token: String? = null,
        platformType: PlatformType = PlatformType.Native,
        executionProvider: ExecutionProvider = ExecutionProvider.NNAPI,
        progressListener: ((Float) -> Unit)? = null
    ) {

        checkForRootedDevice(context)

        if (isConfigured()) return

        if (context.isNetworkAvailable().not()) {
            proceedWithNoInternet(
                context = context,
                apiKey = apiKey,
                token = token,
                platformType = platformType,
                executionProvider = executionProvider
            ) { loadingProgress ->
                progressListener?.invoke(loadingProgress)
            }
            return
        }

        val connectResponse = makeConnectCall(
            context = context,
            apiKey = apiKey,
            token = token,
            platformType = platformType
        )

        val modelVersion = connectResponse.data?.rq?.mv?.v

        val isModelDownloadedWithCorrectVersionFromAPI = isModelWithVersionAlreadyDownloaded(
            versionDirName = modelVersion ?: throw VisionSDKException.VitalInformationMissingException
        )

        val isModelAlreadyDownloaded = isModelAlreadyDownloaded()

        if (isModelAlreadyDownloaded && isModelDownloadedWithCorrectVersionFromAPI) {
            loadModel(
                context = context,
                apiKey = apiKey,
                token = token,
                platformType = platformType,
                versionDirName = modelVersion,
                executionProvider = executionProvider
            ) { loadingProgress ->
                progressListener?.invoke(loadingProgress)
            }
            return
        }

        val previousModelVersion = if (isModelAlreadyDownloaded) {
            loadModel(
                context = context,
                apiKey = apiKey,
                token = token,
                platformType = platformType,
                executionProvider = executionProvider
            ) { loadingProgress ->
                progressListener?.invoke(loadingProgress)
            }
            getModelFile(getModelClass(), getModelSize())
        } else {
            null
        }

        // If model was downloaded but it was not the correct version, then request
        // correct version download link.
//        if (isModelDownloadedWithCorrectVersionFromAPI.not()) {
//            connectResponse = makeConnectCall(
//                context = context,
//                platformType = platformType,
//                modelToRequest = ApiManager.ModelToRequest(
//                    modelClass = getModelClass(),
//                    modelSize = getModelSize(),
//                    getDownloadLink = true
//                )
//            )
//        }

        val aes = AES(efficiency = Efficiency.CustomPerformance(204800))

        val scrambledString = scrambleString(mas, 5)

        val retrievalInfo1 = aes.decryptString(scrambledString, VisionSDK.getInstance().environment.pub)
        val retrievalInfo2 = aes.decryptString(scrambledString, VisionSDK.getInstance().environment.pri)

        val vitalInfo1 = connectResponse.data.rq.d?.k
        val vitalUrl1 = connectResponse.data.rq.d?.u

        val rsa = RSA(retrievalInfo1, retrievalInfo2, padding = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)

        val vitalInfo2 = rsa.decryptData(vitalInfo1?.decodeHex() ?: throw VisionSDKException.VitalInformationMissingException)

        val (vitalInfo3, vitalInfo4) = vitalUrl1?.substringBefore("--") to vitalUrl1?.substringAfter("--")

        val vitalUrl2 = aes.decryptData(
            vitalInfo2,
            vitalInfo4?.decodeHex() ?: throw VisionSDKException.VitalInformationMissingException,
            vitalInfo3?.decodeHex() ?: throw VisionSDKException.VitalInformationMissingException
        )

        val modelFile = getModelFileWithVersion(getModelClass(), getModelSize(), modelVersion)
        modelFile.parentFile?.mkdirs()
        val downloaded = File(modelFile.parentFile, "downloaded")

        val totalProgressSteps = 4

        Log.d(TAG, "Downloading important files...")
        val downloadDuration = measureTime {
            DownloadUtils.downloadFileSuspending(
                vitalUrl2.decodeToString().also { Log.d(TAG, it) },
                downloaded
            ) { progress ->
                val currentProgress = progress / totalProgressSteps
                progressListener?.invoke(currentProgress)
            }
        }

        var accumulatedCurrentProgress = 0.25F

        Log.d(TAG, "Downloading important files successful.")

        Log.d(TAG, "Downloading took ${downloadDuration.toReadableDuration()}.")

        // We're deleting the previous version of the model after successful download of newer version.
        previousModelVersion?.parentFile?.deleteRecursively()

        val extractFile = File(modelFile.parentFile, "extracted")

        Log.d(TAG, "Decompressing downloaded file...")
        val decompressingDuration = measureTime {
            aes.decryptFile(
                vitalInfo2,
                downloaded,
                extractFile
            ) { progress ->
                val currentProgress = accumulatedCurrentProgress + (progress / totalProgressSteps)
                progressListener?.invoke(currentProgress)
            }
        }

        accumulatedCurrentProgress = 0.5F

        Log.d(TAG, "Decompressing downloaded file successful.")

        Log.d(TAG, "Decompressing took ${decompressingDuration.toReadableDuration()}.")

        Log.d(TAG, "Deleting downloaded file...")
        downloaded.delete()
        Log.d(TAG, "Deleting downloaded file successful.")

        Log.d(TAG, "Compressing of decompressed file...")
        val compressingDuration = measureTime {
            aes.encryptFile(longLivePalestine, extractFile, modelFile) { progress ->
                val currentProgress = accumulatedCurrentProgress + (progress / totalProgressSteps)
                progressListener?.invoke(currentProgress)
            }
        }

        accumulatedCurrentProgress = 0.75F

        Log.d(TAG, "Compressing of decompressed file successful.")

        Log.d(TAG, "Compressing of decompressed file took ${compressingDuration.toReadableDuration()}.")

        Log.d(TAG, "Deleting decompressed file...")
        extractFile.delete()
        Log.d(TAG, "Deleting decompressed file successful.")

        loadModel(
            context = context,
            apiKey = apiKey,
            token = token,
            platformType = platformType,
            versionDirName = modelVersion,
            executionProvider = executionProvider
        ) { loadingProgress ->
            val currentProgress = accumulatedCurrentProgress + (loadingProgress / totalProgressSteps)
            progressListener?.invoke(currentProgress)
        }
    }

    private fun proceedWithNoInternet(
        context: Context,
        apiKey: String?,
        token: String?,
        platformType: PlatformType,
        executionProvider: ExecutionProvider,
        progressListener: ((Float) -> Unit)? = null
    ) {
        if (isModelAlreadyDownloaded().not()) {
            throw VisionSDKException.InternetRequiredForOnDeviceOCRManager
        }

        // Check if last time the license was checked was less than 15 days ago.
        val lastLicenseCheckDateTime = VisionSDKSettings.getLicenseCheckDateTime()
        val now = LocalDateTime.now()
        val lastLicenseCheckWas15DaysAgoOrMore = if (SDKHelper.hasAndroid31()) {
            val duration = Duration.between(lastLicenseCheckDateTime, now)
            duration.toDaysPart() >= 15L
        } else {
            val difference = now.toEpochSecond(ZoneOffset.UTC) - lastLicenseCheckDateTime.toEpochSecond(ZoneOffset.UTC)
            difference >= 15L * 86400L
        }

        if (lastLicenseCheckWas15DaysAgoOrMore) {
            throw VisionSDKException.InternetRequiredForOnDeviceOCRManager
        }

        loadModel(
            context = context,
            apiKey = apiKey,
            token = token,
            platformType = platformType,
            executionProvider = executionProvider,
            progressListener = progressListener
        )
    }

    fun isModelAlreadyDownloaded(): Boolean {
        return getModelFile(getModelClass(), getModelSize())?.run {
            exists() && isFile
        } ?: false
    }

    private fun isModelWithVersionAlreadyDownloaded(
        versionDirName: String
    ): Boolean {
        return getModelFileWithVersion(getModelClass(), getModelSize(), versionDirName).run {
            exists() && isFile
        }
    }

    private fun loadModel(
        context: Context,
        apiKey: String?,
        token: String?,
        platformType: PlatformType,
        versionDirName: String? = null,
        executionProvider: ExecutionProvider,
        progressListener: ((Float) -> Unit)? = null
    ) {

        if (isConfigured()) return

        val loadRequiredDataTime = measureTime { loadRequiredData(context) }
        Log.d(TAG, "Loading of required data took ${loadRequiredDataTime.toReadableDuration()}.")

        Log.d(TAG, "Loading important files...")

        val modelFile = if (versionDirName.isNeitherNullNorEmptyNorBlank()) {
            getModelFileWithVersion(getModelClass(), getModelSize(), versionDirName!!)
        } else {
            getModelFile(getModelClass(), getModelSize())
        }

        modelFile ?: throw VisionSDKException.ImportantFilesNotFound

        Log.d(TAG, "Retrieving important files successful.")

        Log.d(TAG, "Decompressing important files...")
        val aes = AES(efficiency = Efficiency.CustomPerformance(204800))
        val extracted = File(modelFile.parentFile, "extracted")
        val decompressDuration = measureTime {
            aes.decryptFile(longLivePalestine, modelFile, extracted, progressListener)
        }
        Log.d(TAG, "Decompressing important files successful.")

        Log.d(TAG, "Decompressing important files took ${decompressDuration.toReadableDuration()}.")

        Log.d(TAG, "Loading important files in memory...")
        VisionOrtSession.initiate(extracted, executionProvider)
        Log.d(TAG, "Important files loaded.")

        Log.d(TAG, "Delete important files from storage after they're loaded...")
        extracted.delete()
        Log.d(TAG, "Delete important files from storage after they're loaded successful.")

        initiatePingTimer(
            context = context,
            apiKey = apiKey,
            token = token,
            platformType = platformType,
            executionProvider = executionProvider
        )

        initiateUnloadFromMemoryTimer()

        initiateSecurityWorker(
            context = context,
            modelClass = getModelClass(),
            modelSize = getModelSize()
        )

        Log.d(TAG, "Loading important files successful.")
    }

    private fun initiatePingTimer(
        context: Context,
        apiKey: String?,
        token: String?,
        platformType: PlatformType,
        executionProvider: ExecutionProvider
    ) {
        pingTimer?.cancel()
        pingTimer = Timer()
        pingTimer?.schedule(timerTask {

            if (context.isNetworkAvailable().not()) {
                try {
                    proceedWithNoInternet(
                        context = context,
                        apiKey = apiKey,
                        token = token,
                        platformType = platformType,
                        executionProvider = executionProvider
                    )
                } catch (e: Exception) {
                    invalidateModel()
                    throw VisionSDKException.UnknownException(e)
                }
                return@timerTask
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    makeConnectCall(
                        context = context,
                        apiKey = apiKey,
                        token = token,
                        platformType = platformType,
                    )
                } catch (ignored: Exception) {

                }
            }
        }, pingTimeInMillis, pingTimeInMillis)
    }

    private fun initiateUnloadFromMemoryTimer() {
        freeMemoryTimer?.cancel()
        freeMemoryTimer = Timer()
        freeMemoryTimer?.schedule(timerTask {
            invalidateModel()
            Log.d(TAG, "Model invalidated.")
        }, invalidateTimeInMillis)
        Log.d(TAG, "Invalidate timer initiated.")
    }

    private fun initiateSecurityWorker(context: Context, modelClass: ModelClass, modelSize: ModelSize) {
        val securityWorkerTag = "SECURITY_WORKER_${modelClass.name}_${modelSize.name}"

        val workManager = WorkManager.getInstance(context)

        Log.d(TAG, "Cancelling previously scheduled SecurityWorker...")
        workManager.cancelAllWorkByTag(securityWorkerTag)
        Log.d(TAG, "Cancelling previously scheduled SecurityWorker successful.")

        Log.d(TAG, "Scheduling new SecurityWorker...")
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SecurityWorker>()
                .addTag(securityWorkerTag)
                .setInputData(
                    Data
                        .Builder()
                        .putString(WORKER_PARAM_MODEL_CLASS, modelClass.name)
                        .putString(WORKER_PARAM_MODEL_SIZE, modelSize.name)
                        .build()
                )
                .setInitialDelay(Duration.ofDays(invalidateForeverDays))
                .build()
        )
        Log.d(TAG, "Scheduling new SecurityWorker successful.")
    }

    fun invalidateModel() {
        if (isConfigured().not()) return
        Log.d(TAG, "Unloading model and vocabulary from memory...")
        cleanUp()
        VisionOrtSession.close()
        Log.d(TAG, "Unloading model and vocabulary from memory successful.")
    }

    private fun getModelFile(
        modelClass: ModelClass,
        modelSize: ModelSize
    ): File? {
        val directory = File(mainDir, "${modelClass.name}/${modelSize.name}")
        if (directory.exists().not()) {
            return null
        }
        val versionDirs = directory.listFiles()
        if (versionDirs == null || versionDirs.isEmpty()) {
            return null
        }
        val versionDir = versionDirs.first()
        return File(versionDir, "vif")
    }

    private fun getModelFileWithVersion(
        modelClass: ModelClass,
        modelSize: ModelSize,
        versionDirName: String
    ): File {
        val versionDir = File(mainDir, "${modelClass.name}/${modelSize.name}/$versionDirName")
        return File(versionDir, "vif")
    }

    fun permanentlyDeleteGivenModel() {
        Log.d(TAG, "Delete model ${getModelClass().name} / ${getModelSize().name} initialized...")
        invalidateModel()
        Log.d(TAG, "Deleting compressed model ${getModelClass().name} / ${getModelSize().name} file...")
        getModelFile(getModelClass(), getModelSize())?.delete()
        Log.d(TAG, "Deleting compressed model ${getModelClass().name} / ${getModelSize().name} file successful.")
        Log.d(TAG, "Delete model ${getModelClass().name} / ${getModelSize().name} successful.")
    }

    fun permanentlyDeleteAllModels() {
        Log.d(TAG, "Delete all models initialized...")
        invalidateModel()
        Log.d(TAG, "Deleting main directory recursively...")
        mainDir.deleteRecursively()
        Log.d(TAG, "Deleting main directory recursively successful.")
        Log.d(TAG, "Delete all models successful.")
    }

    @Suppress("SameParameterValue")
    private fun scrambleString(text: String, key: Int): String {

        return buildString {

            for (i in text.indices step key) {
                val endIndex = kotlin.math.min(i + key, text.length)
                append(
                    text.substring(
                        startIndex = i,
                        endIndex = endIndex
                    ).reversed()
                )
            }
        }
    }

    suspend fun getPredictions(
        bitmap: Bitmap,
        barcodes: List<String>
    ): String {

        if (isConfigured().not()) throw VisionSDKException.OnDeviceOCRManagerNotConfigured
        if (locationProcessor.isInitialized().not()) throw VisionSDKException.OnDeviceOCRManagerNotConfigured

        val predictionResult = predict(
            ortEnvironment = VisionOrtSession.ortEnv!!,
            ortSession = VisionOrtSession.ortSession!!,
            locationProcessor = locationProcessor,
            bitmap = bitmap,
            barcodes = barcodes
        )

        return createJsonOfGivenMLResults(
            rawText = predictionResult.ocrExtractedText,
            regexResult = predictionResult.regexResult,
            mlResults = predictionResult.mlResults?.let { MLResultsFormatter().format(it) }
        )
    }

    protected suspend fun regexProcessing(
        barcodes: List<String>,
        ocrExtractedText: String?
    ): RegexResult? {

        val regexResultFromBarcodesJob = coroutineScope {
            async { parseBarcodes(barcodes, ocrExtractedText) }
        }

        val regexResultFromTextJob = coroutineScope {
            async { parseOcrExtractedText(ocrExtractedText) }
        }

        val regexResultFromBarcodes = regexResultFromBarcodesJob.await()
        val regexResultFromText = regexResultFromTextJob.await()

        val regexResult = regexResultFromBarcodes ?: regexResultFromText//consumeBothResults(regexResultFromBarcodes, regexResultFromText)

        val extractedLabel = ocrExtractedText.ifNeitherNullNorEmptyNorBlank {
            LabelsExtraction(barcodes, it).extract()
        }

        return if (extractedLabel != null) {
            regexResult?.copy(extractedLabel = extractedLabel)
        } else {
            regexResult
        }
    }

    private fun parseBarcodes(
        barcodes: List<String>,
        ocrExtractedText: String?
    ): RegexResult? {

        return barcodes.findWithObjectOrNull { barcode ->
            val pairOfCourierAndResultantMap = barcodeReader(barcode, ocrExtractedText)
            pairOfCourierAndResultantMap?.let { (courier, regexResult) ->
                return@let if (courier != null && regexResult != null) {
                    regexResult
                } else {
                    null
                }
            }
        }
    }

    private fun barcodeReader(
        barcode: String?,
        ocrExtractedText: String?
    ): Pair<Courier?, RegexResult?>? {

        if (barcode.isNullOrEmptyOrBlank()) return null

        return allCouriers.findWithObjectOrNull { courier ->
            val regexResult = courier.readFromBarcode(barcode!!, ocrExtractedText)
            return@findWithObjectOrNull regexResult.courier?.trackingNo.ifNeitherNullNorEmptyNorBlank {
                courier to regexResult
            }
        }
    }

    private fun parseOcrExtractedText(ocrExtractedText: String?): RegexResult? {
        val pairOfCourierAndResultantMap = ocrExtractedTextReader(ocrExtractedText)
        return pairOfCourierAndResultantMap?.let { (courier, regexResult) ->
            return@let if (courier != null && regexResult != null) {
                regexResult
            } else {
                null
            }
        }
    }

    private fun ocrExtractedTextReader(ocrExtractedText: String?): Pair<Courier?, RegexResult?>? {
        return ocrExtractedText.ifNeitherNullNorEmptyNorBlank {
            allCouriers.findWithObjectOrNull { courier ->
                val regexResult = courier.readFromOCR(it)
                return@findWithObjectOrNull regexResult.courier?.trackingNo.ifNeitherNullNorEmptyNorBlank {
                    courier to regexResult
                }
            }
        }
    }

    private fun consumeBothResults(
        regexResultBarcode: RegexResult?,
        regexResultText: RegexResult?
    ): RegexResult? {
        return null
    }

    private fun createJsonOfGivenMLResults(
        rawText: String?,
        regexResult: RegexResult?,
        mlResults: MLResults?
    ): String {
        val j = JSONObject(
            mapOf(
                "data" to JSONObject(
                    mapOf(
                        "id" to null,
                        "image_url" to null,
                        "object" to null,
                        "account_id" to (regexResult?.courier?.accountId ?: regexResult?.extractedLabel?.accountId),
                        "organization_id" to null,
                        "purchase_order" to (mlResults?.logisticAttributes?.purchaseOrder ?: regexResult?.extractedLabel?.purchaseOrderNumber),
                        "provider_name" to (regexResult?.courier?.name),
                        "raw_text" to rawText,
                        "reference_number" to (mlResults?.logisticAttributes?.referenceNumber ?: regexResult?.extractedLabel?.refNumber),
                        "tracking_number" to (regexResult?.courier?.trackingNo ?: mlResults?.packageInfo?.trackingNo),
                        "invoice_number" to (mlResults?.logisticAttributes?.invoiceNumber ?: regexResult?.extractedLabel?.invoiceNumber),
                        "type" to null,
                        "rma_number" to (mlResults?.logisticAttributes?.rmaNumber ?: regexResult?.extractedLabel?.rma),
                        "service_level_name" to mlResults?.logisticAttributes?.labelShipmentType,
                        "weight" to with(mlResults?.packageInfo) {
                            val weightUnit = this?.weightUnit
                                ?.replace("lo", "lb")
                                ?.replace("ib", "lb")
                                ?.replace("kge", "kgs")
                                ?.replace("ka", "kgs")
                                ?.replace("0z", "oz")
                                ?.replace("o2", "oz")
                                ?.replace(":", "")
                                ?.replace("(", "")
                                ?.replace(")", "")

                            return@with weightUnit?.ifNeitherNullNorEmptyNorBlank { item ->
                                when (item.lowercase()) {
                                    "lb", "lbs" -> this?.weight
                                    "kg", "kgs" -> this?.weight?.let { it * 2.2 }
                                    "oz" -> this?.weight?.let { it * 0.0625 }
                                    "mg", "mgs" -> this?.weight?.let { it * 0.000002205 }
                                    "g", "gms" -> this?.weight?.let { it * 0.002205 }
                                    "t" -> this?.weight?.let { it * 2204.62 }
                                    else -> this?.weight
                                }
                            } ?: this?.weight
                        },
                        "location_id" to null,
                        "extracted_labels" to Array(0) { "" },
                        "tags" to null,
                        "recipient" to JSONObject(
                            mapOf(
                                "business" to mlResults?.receiver?.personBusinessName,
                                "contact_id" to null,
                                "email" to null,
                                "name" to mlResults?.receiver?.personName,
                                "phone" to mlResults?.receiver?.personPhone,
                                "address" to JSONObject(
                                    mapOf(
                                        "id" to null,
                                        "hash" to null,
                                        "object" to "address",
                                        "city" to mlResults?.receiver?.city,
                                        "coordinates" to null,
                                        "country" to mlResults?.receiver?.country,
                                        "country_code" to mlResults?.receiver?.countryCode,
                                        "line1" to createLine1(mlResults?.receiver),
                                        "line2" to createLine2(mlResults?.receiver),
                                        "postal_code" to mlResults?.receiver?.zipcode,
                                        "state" to mlResults?.receiver?.state,
                                        "state_code" to mlResults?.receiver?.stateCode,
                                        "textarea" to null,
                                        "timezone" to null,
                                        "formatted_address" to createFormattedAddress(mlResults?.receiver),
                                    )
                                )
                            )
                        ),
                        "sender" to JSONObject(
                            mapOf(
                                "business" to mlResults?.sender?.personBusinessName,
                                "contact_id" to null,
                                "email" to null,
                                "name" to mlResults?.sender?.personName,
                                "phone" to mlResults?.sender?.personPhone,
                                "address" to JSONObject(
                                    mapOf(
                                        "id" to null,
                                        "hash" to null,
                                        "object" to "address",
                                        "city" to mlResults?.sender?.city,
                                        "coordinates" to null,
                                        "country" to mlResults?.sender?.country,
                                        "country_code" to mlResults?.sender?.countryCode,
                                        "line1" to createLine1(mlResults?.sender),
                                        "line2" to createLine2(mlResults?.sender),
                                        "postal_code" to mlResults?.sender?.zipcode,
                                        "state" to mlResults?.sender?.state,
                                        "state_code" to mlResults?.sender?.stateCode,
                                        "textarea" to null,
                                        "timezone" to null,
                                        "formatted_address" to createFormattedAddress(mlResults?.sender),
                                    )
                                )
                            )
                        ),
                        "dimensions" to JSONObject(
                            mapOf(
                                "length" to mlResults?.packageInfo?.dimension,
                                "width" to mlResults?.packageInfo?.dimension,
                                "height" to mlResults?.packageInfo?.dimension,
                            )
                        )
                    )
                )
            )
        )

        return j.toSafeString()
    }

    private fun createLine1(exchangeInfo: ExchangeInfo?) = buildString {
        if (exchangeInfo?.floor.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.floor)
            append(" ")
        }

        if (exchangeInfo?.building.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.building)
            append(" ")
        }

        if (exchangeInfo?.street.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.street)
        }
    }.trim()

    private fun createLine2(exchangeInfo: ExchangeInfo?) = buildString {
        if (exchangeInfo?.officeNo.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.officeNo)
            append(" ")
        }

        if (exchangeInfo?.poBox.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.poBox)
        }
    }.trim()

    private fun createFormattedAddress(exchangeInfo: ExchangeInfo?) = buildString {

        val line1 = createLine1(exchangeInfo)
        val line2 = createLine2(exchangeInfo)

        if (line1.isNeitherNullNorEmptyNorBlank()) {
            append(line1)
            append(" ")
        }

        if (line2.isNeitherNullNorEmptyNorBlank()) {
            append(line2)
            append(" ")
        }

        if (exchangeInfo?.city.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.city?.capitalizeWords())
            append(", ")
        }

        if (exchangeInfo?.stateCode.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.stateCode)
            append(" ")
        } else if (exchangeInfo?.state.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.state)
            append(" ")
        }

        if (exchangeInfo?.zipcode.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.zipcode)
            append(" ")
        }

        if (exchangeInfo?.country.isNeitherNullNorEmptyNorBlank()) {
            append(exchangeInfo?.country?.capitalizeWords())
        }
    }.trim()
}