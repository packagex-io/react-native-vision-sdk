package io.packagex.visionsdk.ocr.ml.dto

internal data class LogisticAttributes(
    val labelShipmentType: String,
    val purchaseOrder: String,
    val referenceNumber: String,
    val rmaNumber: String,
    val invoiceNumber: String
)