package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class BillOfLading(
    @SerializedName("match")
    val match: MatchX?,
    @SerializedName("postprocess")
    val postprocess: PostprocessX?,
    @SerializedName("transform")
    val transform: TransformX?
)