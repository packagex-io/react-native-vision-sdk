package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Data(
    @SerializedName("active")
    val active: Boolean?,
    @SerializedName("billing")
    val billing: Billing?,
    @SerializedName("checksum")
    val checksum: String?,
    @SerializedName("created_at")
    val createdAt: Int?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("enable_scoped_payment_methods")
    val enableScopedPaymentMethods: Boolean?,
    @SerializedName("_experimental")
    val experimental: Experimental?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("logo_url")
    val logoUrl: String?,
    @SerializedName("metadata")
    val metadata: Metadata?,
    @SerializedName("model_versions")
    val modelVersions: ModelVersions?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("profile")
    val profile: Profile?,
    @SerializedName("requirements")
    val requirements: Requirements?,
    @SerializedName("roles")
    val roles: List<Role>?,
    @SerializedName("_search")
    val search: Any?,
    @SerializedName("settings")
    val settings: Settings?,
    @SerializedName("social_media")
    val socialMedia: SocialMedia?,
    @SerializedName("tenant_ids")
    val tenantIds: List<Any>?,
    @SerializedName("tenants")
    val tenants: List<Any>?,
    @SerializedName("updated_at")
    val updatedAt: Int?,
    @SerializedName("updated_by")
    val updatedBy: String?,
    @SerializedName("wallet")
    val wallet: Wallet?
)