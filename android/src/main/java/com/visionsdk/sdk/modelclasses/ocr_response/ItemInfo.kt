package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class ItemInfo(
    @SerializedName("poNumber")
    val poNumber: String?,
    @SerializedName("refNumber")
    val refNumber: String?
)