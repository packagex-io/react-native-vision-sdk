package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Domain(
    @SerializedName("checksum")
    val checksum: String?,
    @SerializedName("created_at")
    val createdAt: Int?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("organization_id")
    val organizationId: String?,
    @SerializedName("ownership_verification_key")
    val ownershipVerificationKey: String?,
    @SerializedName("ownership_verified")
    val ownershipVerified: Boolean?,
    @SerializedName("ownership_verified_at")
    val ownershipVerifiedAt: Int?,
    @SerializedName("profile_require_approval")
    val profileRequireApproval: Boolean?,
    @SerializedName("profile_role_default")
    val profileRoleDefault: ProfileRoleDefault?,
    @SerializedName("sso_id")
    val ssoId: String?,
    @SerializedName("sso_identity_provider")
    val ssoIdentityProvider: String?,
    @SerializedName("sso_name")
    val ssoName: String?,
    @SerializedName("sso_type")
    val ssoType: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("updated_at")
    val updatedAt: Int?,
    @SerializedName("updated_by")
    val updatedBy: String?
)