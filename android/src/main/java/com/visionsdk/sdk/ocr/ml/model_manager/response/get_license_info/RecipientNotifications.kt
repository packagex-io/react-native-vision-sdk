package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class RecipientNotifications(
    @SerializedName("curbside_pickup")
    val curbsidePickup: CurbsidePickup?,
    @SerializedName("delay_mins")
    val delayMins: Int?,
    @SerializedName("email_template")
    val emailTemplate: EmailTemplate?,
    @SerializedName("methods")
    val methods: List<String?>?,
    @SerializedName("pickup_reminders")
    val pickupReminders: PickupReminders?,
    @SerializedName("tracking_statuses")
    val trackingStatuses: List<String?>?,
    @SerializedName("use_batches")
    val useBatches: Boolean?
)