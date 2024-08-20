package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class ShippingLabel(
    @SerializedName("match")
    val match: Match?,
    @SerializedName("postprocess")
    val postprocess: Postprocess?,
    @SerializedName("tracker")
    val tracker: Tracker?,
    @SerializedName("transform")
    val transform: Transform?
)