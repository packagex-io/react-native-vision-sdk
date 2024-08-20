package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class CurbsidePickup(
    @SerializedName("message_template")
    val messageTemplate: String?,
    @SerializedName("reply_trigger")
    val replyTrigger: String?
)