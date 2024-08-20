package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class PickupReminders(
    @SerializedName("check_contact_availability")
    val checkContactAvailability: Boolean?,
    @SerializedName("check_location_availability")
    val checkLocationAvailability: Boolean?,
    @SerializedName("interval_mins")
    val intervalMins: Int?,
    @SerializedName("max_reminder_count")
    val maxReminderCount: Int?
)