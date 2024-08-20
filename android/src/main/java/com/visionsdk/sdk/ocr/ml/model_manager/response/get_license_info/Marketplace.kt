package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Marketplace(
    @SerializedName("application_status")
    val applicationStatus: String?,
    @SerializedName("rating")
    val rating: Int?
)