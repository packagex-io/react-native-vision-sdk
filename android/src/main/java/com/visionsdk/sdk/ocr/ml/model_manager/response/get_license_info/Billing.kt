package io.packagex.visionsdk.ocr.ml.model_manager.response.get_license_info


import com.google.gson.annotations.SerializedName

internal data class Billing(
    @SerializedName("address_validations")
    val addressValidations: AddressValidations?,
    @SerializedName("assets")
    val assets: Assets?,
    @SerializedName("collect_automatically")
    val collectAutomatically: Boolean?,
    @SerializedName("containers")
    val containers: Containers?,
    @SerializedName("credits")
    val credits: Int?,
    @SerializedName("delinquent")
    val delinquent: Boolean?,
    @SerializedName("deliveries")
    val deliveries: Deliveries?,
    @SerializedName("discounts")
    val discounts: List<Any?>?,
    @SerializedName("downgraded_plan")
    val downgradedPlan: Any?,
    @SerializedName("fees")
    val fees: List<Any?>?,
    @SerializedName("fulfillments")
    val fulfillments: Fulfillments?,
    @SerializedName("interval")
    val interval: String?,
    @SerializedName("items")
    val items: Items?,
    @SerializedName("managing_organization_id")
    val managingOrganizationId: Any?,
    @SerializedName("manifests")
    val manifests: Manifests?,
    @SerializedName("next_billable_month")
    val nextBillableMonth: Int?,
    @SerializedName("object")
    val objectX: String?,
    @SerializedName("payment_method")
    val paymentMethod: Any?,
    @SerializedName("payment_processing")
    val paymentProcessing: PaymentProcessing?,
    @SerializedName("payment_terms")
    val paymentTerms: String?,
    @SerializedName("plan")
    val plan: String?,
    @SerializedName("price")
    val price: Int?,
    @SerializedName("prorated_at")
    val proratedAt: Any?,
    @SerializedName("routes")
    val routes: Routes?,
    @SerializedName("scans")
    val scans: Scans?,
    @SerializedName("shipments")
    val shipments: Shipments?,
    @SerializedName("sms")
    val sms: Sms?,
    @SerializedName("tax_percent")
    val taxPercent: Int?,
    @SerializedName("tenants")
    val tenants: Tenants?,
    @SerializedName("trackers")
    val trackers: Trackers?,
    @SerializedName("vision_sdk_enabled")
    val visionSdkEnabled: Boolean?,
    @SerializedName("vision_sdk_license_expires_at")
    val visionSdkLicenseExpiresAt: Int?
)