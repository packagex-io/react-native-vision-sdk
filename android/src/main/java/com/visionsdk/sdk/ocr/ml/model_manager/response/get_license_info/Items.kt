package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Items(
    @SerializedName("interval_count")
    val intervalCount: Int?,
    @SerializedName("price")
    val price: Int?,
    @SerializedName("total_count")
    val totalCount: Int?
)