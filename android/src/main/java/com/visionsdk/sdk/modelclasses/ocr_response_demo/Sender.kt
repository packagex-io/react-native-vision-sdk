package io.packagex.visionsdk.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Sender(
    @SerializedName("address")
    val address: Address?,
    @SerializedName("email")
    val email: Any?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("phone")
    val phone: String?
)