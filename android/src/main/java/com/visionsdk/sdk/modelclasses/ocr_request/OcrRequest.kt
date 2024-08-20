package io.packagex.visionsdk.modelclasses.ocr_request

class OutdatedOcrRequest(
    image_url: String,
    val type: String,
    barcode_values: List<String>,
    location_id: String? = null,
    recipient: Map<String, Any>? = null,
    sender: Map<String, Any>? = null,
    options: Map<String, Any>? = null,
    metadata: Map<String, Any>?,
    val transform: Map<String, String>? = mapOf("object" to "delivery")

//    "transform":{"object":"delivery"}
) : OcrRequest(
    image_url, barcode_values, location_id, recipient, sender, options, metadata
)

open class OcrRequest(
    val image_url: String,
    val barcode_values: List<String>,
    val location_id: String? = null,
    val recipient: Map<String, Any>? = null,
    val sender: Map<String, Any>? = null,
    val options: Map<String, Any>? = null,
    val metadata: Map<String, Any>? = null
)