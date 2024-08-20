package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Parcel(
    @SerializedName("height")
    val height: Any?,
    @SerializedName("length")
    val length: Any?,
    @SerializedName("type")
    val type: Any?,
    @SerializedName("weight")
    val weight: Any?,
    @SerializedName("width")
    val width: Any?
)