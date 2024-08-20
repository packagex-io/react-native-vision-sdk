package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class ProfileRoleDefault(
    @SerializedName("hidden_ui_sections")
    val hiddenUiSections: Any?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("organization_id")
    val organizationId: String?,
    @SerializedName("scopes")
    val scopes: ScopesX?,
    @SerializedName("uuid")
    val uuid: String?
)