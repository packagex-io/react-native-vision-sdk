package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Login(
    @SerializedName("domains")
    val domains: List<Domain>?,
    @SerializedName("login_sso_required_preference")
    val loginSsoRequiredPreference: String?,
    @SerializedName("sp_auth_callback")
    val spAuthCallback: String?,
    @SerializedName("sp_saml_entity_id")
    val spSamlEntityId: String?,
    @SerializedName("sso_configurations")
    val ssoConfigurations: List<SsoConfiguration>?
)