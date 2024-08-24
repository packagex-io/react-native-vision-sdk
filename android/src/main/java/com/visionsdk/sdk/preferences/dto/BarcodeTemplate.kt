package io.packagex.visionsdk.preferences.dto

import com.google.gson.annotations.SerializedName

data class BarcodeTemplate(
    @SerializedName("name")
    val name: String,

    @SerializedName("data")
    val barcodeTemplateData: List<BarcodeTemplateData>
)
data class BarcodeTemplateData(
    @SerializedName("length")
    val barcodeLength: Int,

    @SerializedName("format")
    val barcodeFormat: Int
)