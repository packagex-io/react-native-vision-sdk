package io.packagex.visionsdk.modelclasses.ocr_request

import java.util.UUID

data class ManifestRequest(
    val extractTime: String,
    val barcode: Barcode,
    val image: String
) {
    val callType = "bill_of_lading"
    val platform = "API"
    val orgUuid = UUID.randomUUID().toString()
}

data class Barcode(
    val frames: List<Frame>
)

data class Frame(
    val payload: String,
    val type: String
)