package io.packagex.visionsdk

import android.content.Context
import android.graphics.Bitmap
import com.asadullah.handyutils.isNullOrEmptyOrBlank
import com.asadullah.handyutils.withContextMain
import io.packagex.visionsdk.exceptions.APIErrorResponse
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.PlatformType
import io.packagex.visionsdk.preferences.VisionSDKSettings
import io.packagex.visionsdk.service.all.ApiServiceImpl
import io.packagex.visionsdk.service.manifest.ManifestApiServiceImpl
import io.packagex.visionsdk.service.request.ConnectModelRequest
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.request.ModelInfo
import io.packagex.visionsdk.service.request.ModelVersion
import io.packagex.visionsdk.service.request.TelemetryData
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
        apiKey: String? = null,
        token: String? = null,
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
                    apiKey,
                    token,
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
                    onScanResult.onOCRResponseFailed(VisionSDKException.UnknownException(APIErrorResponse(e)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponseFailed(VisionSDKException.UnknownException(e))
                }
            }
        }
    }

    suspend fun shippingLabelApiCallSync(
        apiKey: String? = null,
        token: String? = null,
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
            apiKey,
            token,
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
        apiKey: String? = null,
        token: String? = null,
        bitmap: Bitmap,
        barcodeList: List<String>,
        onScanResult: OCRResult
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = manifestApiCallSync(apiKey, token, bitmap, barcodeList)
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponse(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onScanResult.onOCRResponseFailed(VisionSDKException.UnknownException(e))
                }
            }
        }
    }

    suspend fun manifestApiCallSync(
        apiKey: String? = null,
        token: String? = null,
        bitmap: Bitmap,
        barcodeList: List<String>,
    ): String {

        val resizedBitmap = if (bitmap.width > 1000 || bitmap.height > 1000) {
            BitmapUtils.bitmapResize(bitmap)
        } else bitmap

        val string64 = BitmapUtils.convertBitmapToBase64(resizedBitmap).toString()

        return manifestApiServiceImpl.manifestApiAsync(
            apiKey,
            token,
            manifestApiServiceImpl.createManifestRequest(
                extractTime = Date().isoFormat(),
                barcodesList = barcodeList,
                baseImage = string64
            )
        )
    }

    suspend fun reportAnIssueSync(
        context: Context,
        apiKey: String? = null,
        token: String? = null,
        platformType: PlatformType = PlatformType.Native,
        modelClass: ModelClass,
        modelSize: ModelSize,
        report: String,
        customData: Map<String, Any?>? = null,
        base64ImageToReportOn: String? = null
    ) {

        if (report.length > 1000) throw VisionSDKException.ReportTextLengthExceeded

        val modelId = VisionSDKSettings.getModelId(modelClass, modelSize)
        val modelVersionId = VisionSDKSettings.getModelVersionId(modelClass, modelSize)

        if (modelId.isNullOrEmptyOrBlank()) throw VisionSDKException.OnDeviceOCRManagerNotConfigured
        if (modelVersionId.isNullOrEmptyOrBlank()) throw VisionSDKException.OnDeviceOCRManagerNotConfigured

        internalTelemetryCallSync(
            apiKey = apiKey,
            token = token,
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
        apiKey: String? = null,
        token: String? = null,
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
                apiKey = apiKey,
                token = token,
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
        apiKey: String? = null,
        token: String? = null,
        sdkId: String,
        deviceId: String,
        platformType: PlatformType = PlatformType.Native,
        modelToRequest: ModelToRequest? = null,
    ): ConnectResponse? {
        return apiServiceImpl.connect(
            apiKey,
            token,
            ConnectRequest(
                _i = sdkId,
                _d = deviceId,
                _f = platformType.value,
                _m = modelToRequest?.toConnectModelRequest(),
            )
        )
    }

    internal suspend fun internalTelemetryCallSync(
        apiKey: String? = null,
        token: String? = null,
        sdkId: String,
        deviceId: String,
        platformType: PlatformType = PlatformType.Native,
        telemetryDataList: List<TelemetryData>
    ) {
        /*apiServiceImpl.postTelemetryData(
            apiKey,
            token,
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
        val getDownloadLink: Boolean?,
        val usageCounter: Int,
        val usageDuration: Long
    ) {
        fun toConnectModelRequest(): ConnectModelRequest {
            return ConnectModelRequest(
                _t = modelClass.value,
                _s = modelSize?.value,
                _d = getDownloadLink,
                _uc = usageCounter,
                _tc = usageDuration
            )
        }
    }
}