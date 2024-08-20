package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class DeliveriesX(
    @SerializedName("chaining")
    val chaining: Chaining?,
    @SerializedName("default_rate_id")
    val defaultRateId: String?,
    @SerializedName("lead_time_mins")
    val leadTimeMins: Int?,
    @SerializedName("marketplace")
    val marketplace: Marketplace?,
    @SerializedName("notification_emails")
    val notificationEmails: List<Any?>?,
    @SerializedName("rates")
    val rates: List<Rate?>?,
    @SerializedName("support_email")
    val supportEmail: Any?,
    @SerializedName("support_phone")
    val supportPhone: Any?,
    @SerializedName("support_url")
    val supportUrl: Any?
)