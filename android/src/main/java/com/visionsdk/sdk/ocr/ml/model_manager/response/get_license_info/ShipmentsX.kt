package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class ShipmentsX(
    @SerializedName("defaults")
    val defaults: Defaults?,
    @SerializedName("labels")
    val labels: Labels?,
    @SerializedName("marketplace")
    val marketplace: MarketplaceX?,
    @SerializedName("providers")
    val providers: List<Provider>?,
    @SerializedName("same_day_tip_amount")
    val sameDayTipAmount: Int?,
    @SerializedName("service_levels")
    val serviceLevels: List<Any>?,
    @SerializedName("sort_by")
    val sortBy: String?,
    @SerializedName("upcharging")
    val upcharging: Upcharging?
)