package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Provider(
    @SerializedName("active")
    val active: Boolean?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("is_custom_rate_card")
    val isCustomRateCard: Boolean?,
    @SerializedName("rate_card")
    val rateCard: Any?
)