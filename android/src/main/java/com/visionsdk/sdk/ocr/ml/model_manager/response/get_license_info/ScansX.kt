package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class ScansX(
    @SerializedName("bill_of_lading")
    val billOfLading: BillOfLading?,
    @SerializedName("shipping_label")
    val shippingLabel: ShippingLabelX?
)