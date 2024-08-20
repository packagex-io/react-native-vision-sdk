package io.packagex.visionsdk.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Recipient(
    @SerializedName("address")
    val address: Address?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("phone")
    val phone: String?
)