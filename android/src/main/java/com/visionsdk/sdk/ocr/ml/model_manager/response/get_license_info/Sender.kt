package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Sender(
    @SerializedName("address")
    val address: Any?,
    @SerializedName("email")
    val email: Any?,
    @SerializedName("name")
    val name: Any?,
    @SerializedName("phone")
    val phone: Any?
)