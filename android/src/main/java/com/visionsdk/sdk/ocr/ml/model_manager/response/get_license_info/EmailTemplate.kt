package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class EmailTemplate(
    @SerializedName("use_logo")
    val useLogo: Boolean?,
    @SerializedName("use_name")
    val useName: Boolean?,
    @SerializedName("use_social_media")
    val useSocialMedia: Boolean?
)