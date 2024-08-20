package io.packagex.visionsdk.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Dimensions(
    @SerializedName("height")
    val height: String?,
    @SerializedName("length")
    val length: String?,
    @SerializedName("width")
    val width: String?
)