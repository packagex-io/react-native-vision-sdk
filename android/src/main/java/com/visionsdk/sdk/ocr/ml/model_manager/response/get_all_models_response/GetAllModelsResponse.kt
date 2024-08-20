package io.packagex.visionsdk.ocr.ml.model_manager.response.get_all_models_response


import com.google.gson.annotations.SerializedName

internal data class GetAllModelsResponse(
    @SerializedName("code")
    val code: Any?,
    @SerializedName("data")
    val `data`: List<Data>?,
    @SerializedName("endpoint")
    val endpoint: Any?,
    @SerializedName("errors")
    val errors: List<Any>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pagination")
    val pagination: Any?,
    @SerializedName("status")
    val status: Int?
)