package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class FulfillmentsX(
    @SerializedName("allow_partial")
    val allowPartial: Boolean?,
    @SerializedName("check_inventory")
    val checkInventory: Boolean?,
    @SerializedName("check_parcel_weight")
    val checkParcelWeight: Boolean?,
    @SerializedName("lead_time_mins")
    val leadTimeMins: Int?,
    @SerializedName("new_packing_slip_on_partial")
    val newPackingSlipOnPartial: Boolean?,
    @SerializedName("next_order_number")
    val nextOrderNumber: String?,
    @SerializedName("packing_slip")
    val packingSlip: PackingSlip?,
    @SerializedName("predefined_packages")
    val predefinedPackages: List<Any?>?,
    @SerializedName("require_pick_step")
    val requirePickStep: Boolean?,
    @SerializedName("service_levels")
    val serviceLevels: List<Any?>?
)