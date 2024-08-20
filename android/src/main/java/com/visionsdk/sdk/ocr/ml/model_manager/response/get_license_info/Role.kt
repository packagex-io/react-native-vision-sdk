package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Role(
    @SerializedName("hidden_ui_sections")
    val hiddenUiSections: List<Any?>?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("notification_events")
    val notificationEvents: List<Any?>?,
    @SerializedName("scopes")
    val scopes: Scopes?,
    @SerializedName("uuid")
    val uuid: String?
)