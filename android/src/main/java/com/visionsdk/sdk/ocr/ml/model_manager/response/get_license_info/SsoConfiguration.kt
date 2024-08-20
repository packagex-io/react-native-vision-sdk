package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class SsoConfiguration(
    @SerializedName("checksum")
    val checksum: String?,
    @SerializedName("created_at")
    val createdAt: Int?,
    @SerializedName("domains")
    val domains: List<Domain>?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("identity_provider")
    val identityProvider: String?,
    @SerializedName("idp_saml_certificate")
    val idpSamlCertificate: String?,
    @SerializedName("idp_saml_entity_id")
    val idpSamlEntityId: String?,
    @SerializedName("idp_saml_sso_url")
    val idpSamlSsoUrl: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("organization_id")
    val organizationId: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("updated_at")
    val updatedAt: Int?,
    @SerializedName("updated_by")
    val updatedBy: String?
)