package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class ReceiverAddress(
    @SerializedName("addressLine1") val addressLine1: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("completeAddress") val completeAddress: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("unitNo") val unitNo: String?,
    @SerializedName("zipCodeLine") val zipCodeLine: String?,
    @SerializedName("zipcode") val zipcode: String?
)