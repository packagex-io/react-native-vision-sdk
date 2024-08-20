package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class PackingSlip(
    @SerializedName("message")
    val message: Any?,
    @SerializedName("size")
    val size: String?
)