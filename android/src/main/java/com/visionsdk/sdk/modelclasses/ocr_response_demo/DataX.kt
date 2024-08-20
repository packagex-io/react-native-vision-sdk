package io.packagex.visionsdk.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class DataX(
    @SerializedName("output")
    val output: Output?,
    @SerializedName("rawText")
    val rawText: String?,
    @SerializedName("status")
    val status: Int?,
    @SerializedName("uuid")
    val uuid: String?
)