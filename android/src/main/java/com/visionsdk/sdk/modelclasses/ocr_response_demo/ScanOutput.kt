package io.packagex.visionsdk.modelclasses.ocr_response_demo


import io.packagex.visionsdk.modelclasses.ocr_response.CourierInfo
import io.packagex.visionsdk.modelclasses.ocr_response.ItemInfo
import com.google.gson.annotations.SerializedName

data class ScanOutput(
    @SerializedName("address")
    val address: Address?,
    @SerializedName("courierInfo")
    val courierInfo: CourierInfo?,
    @SerializedName("data")
    val data: Data?,
    @SerializedName("itemInfo")
    val itemInfo: ItemInfo?,
    @SerializedName("packageId")
    val packageId: String?
)