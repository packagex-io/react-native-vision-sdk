package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Profile(
    @SerializedName("address")
    val address: Address?,
    @SerializedName("dba_name")
    val dbaName: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("phone")
    val phone: Any?,
    @SerializedName("website")
    val website: Any?
)