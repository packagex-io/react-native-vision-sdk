package io.packagex.visionsdk.service.all

import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import com.google.gson.Gson
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.modelclasses.ocr_request.OcrRequest
import io.packagex.visionsdk.modelclasses.ocr_request.OutdatedOcrRequest
import io.packagex.visionsdk.service.ServiceBuilder
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.request.TelemetryRequest
import io.packagex.visionsdk.service.response.ConnectResponse
import retrofit2.HttpException

internal class ApiServiceImpl {

    private val apiServiceKey by lazy { ServiceBuilder.buildService(ApiServiceKey::class.java) }
    private val apiServiceToken by lazy { ServiceBuilder.buildService(ApiServiceToken::class.java) }

    suspend fun shippingLabelApi(apiKey: String?, token: String?, ocrRequest: OcrRequest): String {

        return apiKey.ifNeitherNullNorEmptyNorBlank {
            apiServiceKey.shippingLabelApi(
                data = ocrRequest,
                apiKey = it
            ).string()
        } ?: token.ifNeitherNullNorEmptyNorBlank {
            apiServiceToken.shippingLabelApi(
                data = ocrRequest,
                token = "Bearer $it"
            ).string()
        } ?: throw VisionSDKException.AuthorizationNotProvided
    }

    suspend fun connect(apiKey: String?, token: String?, request: ConnectRequest): ConnectResponse? {
        return try {
            Gson().fromJson(

                apiKey.ifNeitherNullNorEmptyNorBlank {
                    apiServiceKey.connect(
                        data = request,
                        apiKey = it
                    ).string()
                } ?: token.ifNeitherNullNorEmptyNorBlank {
                    apiServiceToken.connect(
                        data = request,
                        token = "Bearer $it"
                    ).string()
                } ?: throw VisionSDKException.AuthorizationNotProvided,

                ConnectResponse::class.java
            )
        } catch (e: HttpException) {
            Gson().fromJson(
                e.response()?.errorBody()?.string(),
                ConnectResponse::class.java
            )
        }
    }

    suspend fun postTelemetryData(apiKey: String?, token: String?, request: TelemetryRequest): String? {
        return apiKey.ifNeitherNullNorEmptyNorBlank {
            apiServiceKey.postTelemetryData(
                data = request,
                apiKey = it
            ).string()
        } ?: token.ifNeitherNullNorEmptyNorBlank {
            apiServiceToken.postTelemetryData(
                data = request,
                token = "Bearer $it"
            ).string()
        } ?: throw VisionSDKException.AuthorizationNotProvided
    }

    fun createOCRRequestObject(
        barcodesList: List<String>,
        baseImage: String,
        locationId: String?,
        recipient: Map<String, Any>? = null,
        sender: Map<String, Any>? = null,
        options: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null,
    ): OcrRequest {
        return when (VisionSDK.getInstance().environment) {
            Environment.DEV, Environment.QA, Environment.STAGING -> {
                OcrRequest(
                    image_url = "data:image/jpeg;base64,$baseImage",
                    location_id = locationId,
                    recipient = recipient,
                    sender = sender,
                    options = options,
                    metadata = metadata,
                    barcode_values = barcodesList
                )
            }
            Environment.PRODUCTION, Environment.SANDBOX -> {
                OutdatedOcrRequest(
                    image_url = "data:image/jpeg;base64,$baseImage",
                    type = "shipping_label",
                    location_id = locationId,
                    recipient = recipient,
                    sender = sender,
                    transform = mapOf("object" to "delivery"),
                    options = options,
                    metadata = metadata,
                    barcode_values = barcodesList
                )
            }
        }
    }
}