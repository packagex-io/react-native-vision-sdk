package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Labels(
    @SerializedName("autoprint")
    val autoprint: Boolean?,
    @SerializedName("format")
    val format: String?,
    @SerializedName("size")
    val size: String?
)