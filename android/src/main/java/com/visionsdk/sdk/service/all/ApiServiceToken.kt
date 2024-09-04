package io.packagex.visionsdk.service.all

import io.packagex.visionsdk.modelclasses.ocr_request.OcrRequest
import io.packagex.visionsdk.service.request.ConnectRequest
import io.packagex.visionsdk.service.request.TelemetryRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface ApiServiceToken {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("inferences/images/shipping-labels")
    suspend fun shippingLabelApi(
        @Body data: OcrRequest,
        @Header("Authorization") token: String?
    ): ResponseBody

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("sdk/connect")
    suspend fun connect(
        @Body data: ConnectRequest,
        @Header("Authorization") token: String?,
    ): ResponseBody

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("sdk/telemetry")
    suspend fun postTelemetryData(
        @Body data: TelemetryRequest,
        @Header("Authorization") token: String?,
    ): ResponseBody
}