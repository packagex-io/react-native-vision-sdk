package io.packagex.visionsdk.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Address(
    @SerializedName("city")
    val city: String?,
    @SerializedName("coordinates")
    val coordinates: Any?,
    @SerializedName("country")
    val country: Any?,
    @SerializedName("country_code")
    val countryCode: Any?,
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    @SerializedName("hash")
    val hash: Any?,
    @SerializedName("id")
    val id: Any?,
    @SerializedName("line1")
    val line1: String?,
    @SerializedName("line2")
    val line2: String?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("postal_code")
    val postalCode: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("state_code")
    val stateCode: String?,
    @SerializedName("textarea")
    val textarea: String?,
    @SerializedName("timezone")
    val timezone: Any?,
    @SerializedName("verified")
    val verified: Boolean?,
    @SerializedName("unit_no")
    val unitNo: String?
)