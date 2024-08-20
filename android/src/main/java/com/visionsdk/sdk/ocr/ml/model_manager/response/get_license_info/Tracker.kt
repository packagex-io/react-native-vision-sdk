package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Tracker(
    @SerializedName("create_automatically")
    val createAutomatically: Boolean?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("use_existing_tracking_number")
    val useExistingTrackingNumber: Boolean?,
    @SerializedName("use_matched_contact_props")
    val useMatchedContactProps: Boolean?
)