package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Scopes(
    @SerializedName("addresses")
    val addresses: Int?,
    @SerializedName("analytics")
    val analytics: Int?,
    @SerializedName("api_keys")
    val apiKeys: Int?,
    @SerializedName("assets")
    val assets: Int?,
    @SerializedName("audits")
    val audits: Int?,
    @SerializedName("contacts")
    val contacts: Int?,
    @SerializedName("containers")
    val containers: Int?,
    @SerializedName("deliveries")
    val deliveries: Int?,
    @SerializedName("events")
    val events: Int?,
    @SerializedName("fulfillments")
    val fulfillments: Int?,
    @SerializedName("groups")
    val groups: Int?,
    @SerializedName("inferences")
    val inferences: Int?,
    @SerializedName("items")
    val items: Int?,
    @SerializedName("locations")
    val locations: Int?,
    @SerializedName("manifests")
    val manifests: Int?,
    @SerializedName("organizations")
    val organizations: Int?,
    @SerializedName("payment_methods")
    val paymentMethods: Int?,
    @SerializedName("payments")
    val payments: Int?,
    @SerializedName("scans")
    val scans: Int?,
    @SerializedName("shipments")
    val shipments: Int?,
    @SerializedName("threads")
    val threads: Int?,
    @SerializedName("trackers")
    val trackers: Int?,
    @SerializedName("users")
    val users: Int?,
    @SerializedName("webhooks")
    val webhooks: Int?
)