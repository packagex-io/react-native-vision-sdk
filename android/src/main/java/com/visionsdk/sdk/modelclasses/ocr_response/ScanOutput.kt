package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class ScanOutput(
    @SerializedName("address")
    val address: Address?,
    @SerializedName("courierInfo")
    val courierInfo: CourierInfo?,
    @SerializedName("data")
    val `data`: DataX?,
    @SerializedName("itemInfo")
    val itemInfo: ItemInfo?,
    @SerializedName("packageId")
    val packageId: String?,
    @SerializedName("success")
    val success: String?,
)