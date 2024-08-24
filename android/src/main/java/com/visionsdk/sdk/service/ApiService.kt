package io.packagex.visionsdk.service

import io.packagex.visionsdk.modelclasses.ocr_request.OcrRequest
import io.packagex.visionsdk.ocr.ml.model_manager.response.get_all_models_response.GetAllModelsResponse
import io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info.GetLicenseInfoResponse
import io.packagex.visionsdk.ocr.ml.model_manager.response.model_download_link_response.MLModelDownloadLinkResponse
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.request.TelemetryRequest
import io.packagex.visionsdk.service.response.ConnectResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

internal interface ApiService {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("inferences/images/shipping-labels")
    suspend fun shippingLabelApi(
        @Body data: OcrRequest,
        @Header("x-api-key") apiKey: String?
    ): ResponseBody

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("sdk/connect")
    suspend fun connect(
        @Body data: ConnectRequest,
        @Header("x-api-key") apiKey: String?,
    ): ResponseBody

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("sdk/telemetry")
    suspend fun postTelemetryData(
        @Body data: TelemetryRequest,
        @Header("x-api-key") apiKey: String?,
    ): ResponseBody

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @GET
    suspend fun getLicenseInfo(
        @Header("x-api-key") apiKey: String?,
        @Url url: String
    ): GetLicenseInfoResponse

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @GET
    suspend fun getAllModels(
        @Header("x-api-key") apiKey: String?,
        @Url url: String
    ): GetAllModelsResponse

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @GET
    suspend fun getMLModelDownloadLink(
        @Header("x-api-key") apiKey: String?,
        @Url url: String
    ): MLModelDownloadLinkResponse
}