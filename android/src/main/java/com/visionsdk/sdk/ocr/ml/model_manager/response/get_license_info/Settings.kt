package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Settings(
    @SerializedName("contacts")
    val contacts: Contacts?,
    @SerializedName("containers")
    val containers: ContainersX?,
    @SerializedName("deliveries")
    val deliveries: DeliveriesX?,
    @SerializedName("fulfillments")
    val fulfillments: FulfillmentsX?,
    @SerializedName("inferences")
    val inferences: Inferences?,
    @SerializedName("locations")
    val locations: Locations?,
    @SerializedName("login")
    val login: Login?,
    @SerializedName("manifests")
    val manifests: ManifestsX?,
    @SerializedName("predefined_packages")
    val predefinedPackages: List<Any>?,
    @SerializedName("recipient_notifications")
    val recipientNotifications: RecipientNotifications?,
    @SerializedName("routes")
    val routes: RoutesX?,
    @SerializedName("scans")
    val scans: ScansX?,
    @SerializedName("shipments")
    val shipments: ShipmentsX?,
    @SerializedName("threads")
    val threads: Threads?,
    @SerializedName("trackers")
    val trackers: TrackersX?
)