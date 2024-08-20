package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class GetLicenseInfoResponse(
    @SerializedName("code")
    val code: Any?,
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("endpoint")
    val endpoint: Any?,
    @SerializedName("errors")
    val errors: List<Any>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pagination")
    val pagination: Any?,
    @SerializedName("status")
    val status: Int?
)