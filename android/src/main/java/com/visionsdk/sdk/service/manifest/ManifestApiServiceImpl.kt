package io.packagex.visionsdk.service.manifest

import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.modelclasses.ocr_request.Barcode
import io.packagex.visionsdk.modelclasses.ocr_request.Frame
import io.packagex.visionsdk.modelclasses.ocr_request.ManifestRequest
import io.packagex.visionsdk.service.ServiceBuilder

internal class ManifestApiServiceImpl {

    private val apiService = ServiceBuilder.buildService(ManifestApiService::class.java)

    suspend fun manifestApiAsync(manifestRequest: ManifestRequest): String {
        return apiService.manifestApi(
            data = manifestRequest,
            apiKey = when (val apiKey = getAuthentication()) {
                is Authentication.API -> apiKey.apiKey
                is Authentication.BearerToken -> null
            },
        ).string()
    }

    fun createManifestRequest(
        extractTime: String,
        barcodesList: List<String>,
        baseImage: String,
    ): ManifestRequest {
        return ManifestRequest(
            extractTime = extractTime,
            barcode = Barcode(
                barcodesList.map { Frame(it, "") }
            ),
            image = baseImage
        )
    }

    private fun getAuthentication(): Authentication {
        val visionSDK = VisionSDK.getInstance()
        if (visionSDK.manifestAuth == null) {
            throw Exception("Authorization mechanism not set")
        }
        return visionSDK.manifestAuth!!
    }
}