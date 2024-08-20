package io.packagex.visionsdk.ocr.ml.model_manager.response.get_all_models_response


import com.google.gson.annotations.SerializedName

internal data class Data(
    @SerializedName("created_at")
    val createdAt: Int?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("platform")
    val platform: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("version")
    val version: String?
)