package io.packagex.visionsdk.preferences.dto

data class BarcodeTemplate(
    val name: String,
    val barcodeTemplateData: List<BarcodeTemplateData>
)
data class BarcodeTemplateData(
    val barcodeLength: Int,
    val barcodeFormat: Int
)