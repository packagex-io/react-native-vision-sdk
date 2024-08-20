package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Requirements(
    @SerializedName("errors")
    val errors: List<Any?>?,
    @SerializedName("pending_verification")
    val pendingVerification: List<Any?>?,
    @SerializedName("required_now")
    val requiredNow: List<Any?>?
)