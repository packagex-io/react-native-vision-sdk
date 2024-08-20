package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class ModelVersions(
    @SerializedName("bill_of_lading_cloud")
    val billOfLadingCloud: List<String?>?,
    @SerializedName("bill_of_lading_ondevice")
    val billOfLadingOndevice: List<String?>?,
    @SerializedName("shipping_label_cloud")
    val shippingLabelCloud: List<String?>?,
    @SerializedName("shipping_label_ondevice")
    val shippingLabelOndevice: List<String?>?
)