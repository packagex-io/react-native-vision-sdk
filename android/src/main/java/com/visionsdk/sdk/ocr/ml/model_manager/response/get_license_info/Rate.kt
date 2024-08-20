package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Rate(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("packages")
    val packages: List<Any?>?,
    @SerializedName("service_level")
    val serviceLevel: String?
)