package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Upcharging(
    @SerializedName("active")
    val active: Boolean?,
    @SerializedName("max")
    val max: Int?,
    @SerializedName("min")
    val min: Int?,
    @SerializedName("model")
    val model: String?,
    @SerializedName("multiplier")
    val multiplier: Double?,
    @SerializedName("percent")
    val percent: Int?
)