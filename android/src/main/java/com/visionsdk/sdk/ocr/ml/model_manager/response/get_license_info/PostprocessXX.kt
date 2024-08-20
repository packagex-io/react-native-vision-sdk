package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class PostprocessXX(
    @SerializedName("parse_addresses")
    val parseAddresses: List<String?>?,
    @SerializedName("require_substring_order")
    val requireSubstringOrder: Boolean?,
    @SerializedName("require_unique_hash")
    val requireUniqueHash: Boolean?,
    @SerializedName("required_scan_properties")
    val requiredScanProperties: List<Any?>?,
    @SerializedName("substring_set_options")
    val substringSetOptions: List<Any?>?
)