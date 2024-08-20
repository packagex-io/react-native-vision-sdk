package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class MarketplaceX(
    @SerializedName("min_rating")
    val minRating: Int?,
    @SerializedName("rate_count")
    val rateCount: Int?,
    @SerializedName("sort_by")
    val sortBy: String?
)