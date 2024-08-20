package io.packagex.visionsdk.service

import com.google.gson.Gson
import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.modelclasses.ocr_request.OcrRequest
import io.packagex.visionsdk.modelclasses.ocr_request.OutdatedOcrRequest
import io.packagex.visionsdk.ocr.ml.model_manager.response.get_all_models_response.Data
import io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info.GetLicenseInfoResponse
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.response.ConnectResponse
import retrofit2.HttpException

internal class ApiServiceImpl {

    private val apiService = ServiceBuilder.buildService(ApiService::class.java)

    suspend fun shippingLabelApi(ocrRequest: OcrRequest): String {
        return apiService.shippingLabelApi(
            data = ocrRequest,
            apiKey = when (val auth = VisionSDK.getInstance().auth) {
                is Authentication.API -> auth.apiKey
                is Authentication.BearerToken -> null
            }
        ).string()
    }

    suspend fun connect(request: ConnectRequest): ConnectResponse? {
        return try {
            Gson().fromJson(
                apiService.connect(
                    data = request,
                    apiKey = when (val auth = VisionSDK.getInstance().auth) {
                        is Authentication.API -> auth.apiKey
                        is Authentication.BearerToken -> null
                    }
                ).string(),
                ConnectResponse::class.java
            )
        } catch (e: HttpException) {
            Gson().fromJson(
                e.response()?.errorBody()?.string(),
                ConnectResponse::class.java
            )
        }
    }

    suspend fun getLicenseInfo(): GetLicenseInfoResponse {
        val response = apiService.getLicenseInfo(
            apiKey = when (val auth = VisionSDK.getInstance().auth) {
                is Authentication.API -> auth.apiKey
                is Authentication.BearerToken -> null
            },
            "https://dev--api.packagex.io/v1/org"
        )
        return response
    }

    suspend fun getAllModels(): List<Data>? {
        val response = apiService.getAllModels(
            apiKey = when (val auth = VisionSDK.getInstance().auth) {
                is Authentication.API -> auth.apiKey
                is Authentication.BearerToken -> null
            },
            "https://dev--api.packagex.io/v1/org/models"
        )
        return response.data
    }

    suspend fun getMLModelDownloadLink(modelName: String): String? {
        val mlModelDownloadLinkResponse = apiService.getMLModelDownloadLink(
            apiKey = when (val auth = VisionSDK.getInstance().auth) {
                is Authentication.API -> auth.apiKey
                is Authentication.BearerToken -> null
            },
            "https://dev--api.packagex.io/v1/org/models/$modelName/signed-url"
        )
        return mlModelDownloadLinkResponse.data?.url
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
        return when (VisionSDK.getInstance().environment!!) {
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