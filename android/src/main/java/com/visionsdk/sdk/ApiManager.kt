package io.packagex.visionsdk

import android.graphics.Bitmap
import com.asadullah.handyutils.format
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.PlatformType
import io.packagex.visionsdk.exceptions.APIErrorResponse
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.service.ApiServiceImpl
import io.packagex.visionsdk.service.manifest.ManifestApiServiceImpl
import io.packagex.visionsdk.service.request.ConnectModelRequest
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.response.ConnectResponse
import io.packagex.visionsdk.utils.BitmapUtils
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
                extractTime = Date().format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                barcodesList = barcodeList,
                baseImage = string64
            )
        )
    }

    internal suspend fun connectCallSync(
        sdkId: String,
        deviceId: String,
        platformType: PlatformType = PlatformType.Native,
        modelToRequest: ModelToRequest? = null
    ): ConnectResponse? {
        return apiServiceImpl.connect(
            ConnectRequest(
                _i = sdkId,
                _d = deviceId,
                _f = platformType.value,
                _m = modelToRequest?.toConnectModelRequest()
            )
        )
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