package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class PaymentProcessing(
    @SerializedName("amount")
    val amount: Int?,
    @SerializedName("percent")
    val percent: Double?
)