package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Defaults(
    @SerializedName("parcel")
    val parcel: Parcel?,
    @SerializedName("recipient")
    val recipient: Recipient?,
    @SerializedName("sender")
    val sender: Sender?,
    @SerializedName("status")
    val status: Status?
)