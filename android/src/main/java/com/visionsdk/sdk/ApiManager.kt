package io.packagex.visionsdk

import android.content.Context
import android.graphics.Bitmap
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.withContextMain
import io.packagex.visionsdk.exceptions.APIErrorResponse
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.PlatformType
import io.packagex.visionsdk.preferences.VisionSdkSettings
import io.packagex.visionsdk.service.ApiServiceImpl
import io.packagex.visionsdk.service.manifest.ManifestApiServiceImpl
import io.packagex.visionsdk.service.request.ConnectModelRequest
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.request.ModelInfo
import io.packagex.visionsdk.service.request.ModelVersion
import io.packagex.visionsdk.service.request.TelemetryData
import io.packagex.visionsdk.service.request.TelemetryRequest
import io.packagex.visionsdk.service.response.ConnectResponse
import io.packagex.visionsdk.utils.BitmapUtils
import io.packagex.visionsdk.utils.getAndroidDeviceId
import io.packagex.visionsdk.utils.isoFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.Date

class ApiManager {

    private val apiServiceImpl = ApiServiceImpl()
    private val manifestApiServiceImpl = ManifestApiServiceImpl()

    fun shippingLabelApiCallAsync(
        bitmap: Bitmap,
        barcodeList: List<String>,
        locationId: String? = null,
        recipient: Map<String, Any>? = null,
        sender: Map<String, Any>? = null,
        options: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null,
        onScanResult: OCRResult
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            try {
                val response = shippingLabelApiCallSync(
                    bitmap = bitmap,
                    barcodeList = barcodeList,
                    locationId = locationId,
                    recipient = recipient,
                    sender = sender,
                    options = options,
                    metadata = metadata
                )
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponse(response)
                }
            } catch (e: HttpException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponseFailed(APIErrorResponse(e))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponseFailed(e)
                }
            }
        }
    }

    suspend fun shippingLabelApiCallSync(
        bitmap: Bitmap,
        barcodeList: List<String>,
        locationId: String?,
        recipient: Map<String, Any>? = null,
        sender: Map<String, Any>? = null,
        options: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null,
    ): String {

        val resizedBitmap = if (bitmap.width > 1000 || bitmap.height > 1000) {
            BitmapUtils.bitmapResize(bitmap)
        } else bitmap

        val string64 = BitmapUtils.convertBitmapToBase64(resizedBitmap).toString()

        return apiServiceImpl.shippingLabelApi(
            apiServiceImpl.createOCRRequestObject(
                barcodesList = barcodeList,
                baseImage = string64,
                locationId = locationId,
                recipient = recipient,
                sender = sender,
                options = options,
                metadata = metadata
            )
        )
    }

    fun manifestApiCallAsync(
        bitmap: Bitmap,
        barcodeList: List<String>,
        onScanResult: OCRResult
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = manifestApiCallSync(bitmap, barcodeList)
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponse(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponseFailed(e)
                }
            }
        }
    }

    suspend fun manifestApiCallSync(
        bitmap: Bitmap,
        barcodeList: List<String>,
    ): String {

        val resizedBitmap = if (bitmap.width > 1000 || bitmap.height > 1000) {
            BitmapUtils.bitmapResize(bitmap)
        } else bitmap

        val string64 = BitmapUtils.convertBitmapToBase64(resizedBitmap).toString()

        return manifestApiServiceImpl.manifestApiAsync(
            manifestApiServiceImpl.createManifestRequest(
                extractTime = Date().isoFormat(),
                barcodesList = barcodeList,
                baseImage = string64
            )
        )
    }

    suspend fun reportAnIssueSync(
        context: Context,
        platformType: PlatformType = PlatformType.Native,
        modelClass: ModelClass,
        modelSize: ModelSize,
        report: String,
        customData: Map<String, Any?>? = null,
        base64ImageToReportOn: String? = null
    ) {

        assert(report.length <= 1000) { "Report is exceeding the character limit. (Max characters 1000)" }

        val modelId = VisionSdkSettings.getModelId(modelClass, modelSize)
        val modelVersionId = VisionSdkSettings.getModelVersionId(modelClass, modelSize)

        assert(modelId.isNeitherNullNorEmptyNorBlank()) { "Model Id was not found. You should call connect API before calling report info." }
        assert(modelVersionId.isNeitherNullNorEmptyNorBlank()) { "Model Version Id was not found. You should call connect API before calling report info." }

        val apiManager = ApiManager()
        apiManager.internalTelemetryCallSync(
            sdkId = VisionSDK.getInstance().environment.sdkId,
            deviceId = context.getAndroidDeviceId(),
            platformType = platformType,
            telemetryDataList = listOf(
                TelemetryData(
                    action = "shipping_label_extraction",
                    actionPerformedAt = Date().isoFormat(),
                    extractionTimeInMillis = 0L,
                    modelInfo = ModelInfo(
                        modelId = modelId!!,
                        modelVersion = ModelVersion(
                            modelVersionId = modelVersionId!!
                        )
                    ),
                    report = report,
                    object1 = customData,
                    base64ImageToReportOn = base64ImageToReportOn
                )
            )
        )
    }

    fun reportAnIssueAsync(
        context: Context,
        platformType: PlatformType = PlatformType.Native,
        modelClass: ModelClass,
        modelSize: ModelSize,
        report: String,
        customData: Map<String, Any?>? = null,
        base64ImageToReportOn: String? = null,
        onComplete: (success: Boolean) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            reportAnIssueSync(
                context = context,
                platformType = platformType,
                report = report,
                modelClass = modelClass,
                modelSize = modelSize,
                customData = customData,
                base64ImageToReportOn = base64ImageToReportOn
            )
            withContextMain {
                onComplete(true)
            }
        }
    }

    internal suspend fun connectCallSync(
        sdkId: String,
        deviceId: String,
        platformType: PlatformType = PlatformType.Native,
        modelToRequest: ModelToRequest? = null,
        usageCounter: Int? = null,
        timeCounter: Long? = null
    ): ConnectResponse? {
        return apiServiceImpl.connect(
            ConnectRequest(
                _i = sdkId,
                _d = deviceId,
                _f = platformType.value,
                _m = modelToRequest?.toConnectModelRequest(),
                _uc = usageCounter,
                _tc = timeCounter
            )
        )
    }

    internal suspend fun internalTelemetryCallSync(
        sdkId: String,
        deviceId: String,
        platformType: PlatformType = PlatformType.Native,
        telemetryDataList: List<TelemetryData>
    ) {
        /*apiServiceImpl.postTelemetryData(
            TelemetryRequest(
                deviceId = deviceId,
                sdkId = sdkId,
                framework = platformType.value,
                telemetryDataList = telemetryDataList
            )
        )*/
    }

    internal data class ModelToRequest(
        val modelClass: ModelClass,
        val modelSize: ModelSize? = null,
        val getDownloadLink: Boolean?
    ) {
        fun toConnectModelRequest(): ConnectModelRequest {
            return ConnectModelRequest(
                _t = modelClass.value,
                _s = modelSize?.value,
                _d = getDownloadLink
            )
        }
    }
}