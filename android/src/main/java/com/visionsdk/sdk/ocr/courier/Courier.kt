package io.packagex.visionsdk.ocr.courier

internal abstract class Courier(val possibleNames: List<String>) {

    abstract fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult
    abstract fun readFromOCR(ocrExtractedText: String?): RegexResult
}

internal data class RegexResult(
    val courier: CourierInfo? = null,
    val receiver: ExchangeInfo? = null,
    val sender: ExchangeInfo? = null,
    val extractedLabel: ExtractedLabel? = null
) {
    internal data class CourierInfo(
        var name: String? = null,
        var trackingNo: String? = null,
        var shipmentType: String? = null,
        var weight: String? = null,
        var dateInfo: String? = null,
        var accountId: String? = null
    )
    internal data class ExchangeInfo(
        var name: String? = null,
        var addressLine1: String? = null,
        var addressLine2: String? = null,
        var zipcode: String? = null,
        var city: String? = null,
        var state: String? = null,
        var country: String? = null,
    )

    internal data class ExtractedLabel(
        val accountId: String? = null,
        val rma: String? = null,
        val purchaseOrderNumber: String? = null,
        val refNumber: String? = null,
        val invoiceNumber: String? = null
    )
}