package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Wallet(
    @SerializedName("active")
    val active: Boolean?,
    @SerializedName("alert_threshold")
    val alertThreshold: Int?,
    @SerializedName("balance")
    val balance: Int?,
    @SerializedName("credit_line")
    val creditLine: Int?,
    @SerializedName("default")
    val default: Boolean?,
    @SerializedName("reload_threshold")
    val reloadThreshold: Int?,
    @SerializedName("source")
    val source: Any?
)