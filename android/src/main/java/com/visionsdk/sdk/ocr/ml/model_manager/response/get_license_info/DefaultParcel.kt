package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class DefaultParcel(
    @SerializedName("height")
    val height: Int?,
    @SerializedName("length")
    val length: Int?,
    @SerializedName("weight")
    val weight: Int?,
    @SerializedName("width")
    val width: Int?
)