package io.packagex.visionsdk.service.manifest

import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.modelclasses.ocr_request.Barcode
import io.packagex.visionsdk.modelclasses.ocr_request.Frame
import io.packagex.visionsdk.modelclasses.ocr_request.ManifestRequest
import io.packagex.visionsdk.service.ServiceBuilder

internal class ManifestApiServiceImpl {

    private val apiServiceKey = ServiceBuilder.buildService(ManifestApiServiceKey::class.java)
    private val apiServiceToken = ServiceBuilder.buildService(ManifestApiServiceToken::class.java)

    suspend fun manifestApiAsync(apiKey: String?, token: String?, manifestRequest: ManifestRequest): String {
        return apiKey.ifNeitherNullNorEmptyNorBlank {
            apiServiceKey.manifestApi(
                data = manifestRequest,
                apiKey = it
            ).string()
        } ?: token.ifNeitherNullNorEmptyNorBlank {
            apiServiceToken.manifestApi(
                data = manifestRequest,
                token = "Bearer $it"
            ).string()
        } ?: throw VisionSDKException.BillOfLadingAuthorizationNotProvided
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
}