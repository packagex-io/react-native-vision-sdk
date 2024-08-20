package io.packagex.visionsdk.modelclasses


import com.google.gson.annotations.SerializedName

data class ErrorModel(
    @SerializedName("code")
    val code: String?,
    @SerializedName("endpoint")
    val endpoint: Any?,
    @SerializedName("errors")
    val errors: List<String?>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("pagination")
    val pagination: Any?,
    @SerializedName("status")
    val status: Int?
)