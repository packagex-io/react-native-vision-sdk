package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Chaining(
    @SerializedName("default_parcel")
    val defaultParcel: DefaultParcel?,
    @SerializedName("default_status")
    val defaultStatus: String?,
    @SerializedName("label")
    val label: Label?,
    @SerializedName("reuse_tracking_number")
    val reuseTrackingNumber: Boolean?
)