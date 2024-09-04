package io.packagex.visionsdk.service.manifest

import io.packagex.visionsdk.modelclasses.ocr_request.ManifestRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface ManifestApiServiceToken {

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json",
    )
    @POST("extract")
    suspend fun manifestApi(
        @Body data: ManifestRequest,
        @Header("Authorization") token: String?
    ): ResponseBody
}