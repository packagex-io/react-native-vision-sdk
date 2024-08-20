package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class ShippingLabelX(
    @SerializedName("match")
    val match: MatchXX?,
    @SerializedName("postprocess")
    val postprocess: PostprocessXX?,
    @SerializedName("transform")
    val transform: TransformX?
)