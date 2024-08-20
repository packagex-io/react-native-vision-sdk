package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class SenderFound(
    @SerializedName("businessName")
    val businessName: String?,
    @SerializedName("combinedInfo")
    val combinedInfo: String?,
    @SerializedName("name")
    val name: String?
)