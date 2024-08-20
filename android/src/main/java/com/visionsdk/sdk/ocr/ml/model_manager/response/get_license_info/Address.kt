package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Address(
    @SerializedName("city")
    val city: String?,
    @SerializedName("coordinates")
    val coordinates: List<Double?>?,
    @SerializedName("country")
    val country: String?,
    @SerializedName("country_code")
    val countryCode: String?,
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    @SerializedName("hash")
    val hash: String?,
    @SerializedName("id")
    val id: String?,
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
    val timezone: String?,
    @SerializedName("verified")
    val verified: Boolean?
)