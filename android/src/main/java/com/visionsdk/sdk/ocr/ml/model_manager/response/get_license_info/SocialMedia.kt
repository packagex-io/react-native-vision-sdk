package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class SocialMedia(
    @SerializedName("facebook")
    val facebook: Any?,
    @SerializedName("instagram")
    val instagram: Any?,
    @SerializedName("linkedin")
    val linkedin: Any?,
    @SerializedName("tiktok")
    val tiktok: Any?,
    @SerializedName("twitter")
    val twitter: Any?,
    @SerializedName("youtube")
    val youtube: Any?
)