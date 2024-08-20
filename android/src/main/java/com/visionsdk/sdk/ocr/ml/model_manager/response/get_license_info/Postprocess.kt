package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Postprocess(
    @SerializedName("parse_addresses")
    val parseAddresses: List<Any?>?,
    @SerializedName("require_unique_hash")
    val requireUniqueHash: Boolean?,
    @SerializedName("required_properties")
    val requiredProperties: List<Any?>?
)