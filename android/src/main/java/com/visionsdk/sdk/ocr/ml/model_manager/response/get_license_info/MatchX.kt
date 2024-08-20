package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class MatchX(
    @SerializedName("exhaust_all")
    val exhaustAll: Boolean?,
    @SerializedName("location")
    val location: Boolean?,
    @SerializedName("required_match_properties")
    val requiredMatchProperties: List<Any?>?,
    @SerializedName("search")
    val search: Any?,
    @SerializedName("search_score_threshold")
    val searchScoreThreshold: Double?,
    @SerializedName("use_best_match")
    val useBestMatch: Boolean?
)