package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class Weight(
    @SerializedName("unit")
    val unit: String?,
    @SerializedName("value")
    val value: String?
)