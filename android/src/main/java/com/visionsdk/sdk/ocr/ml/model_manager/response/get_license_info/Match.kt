package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Match(
    @SerializedName("location")
    val location: Boolean?,
    @SerializedName("required_properties")
    val requiredProperties: List<Any?>?,
    @SerializedName("search")
    val search: List<Any?>?,
    @SerializedName("search_score_threshold")
    val searchScoreThreshold: Double?,
    @SerializedName("use_best_match")
    val useBestMatch: Boolean?
)