package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Contacts(
    @SerializedName("create_via_recipient")
    val createViaRecipient: Boolean?
)