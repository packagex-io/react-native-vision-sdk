package io.packagex.visionsdk.modelclasses.ocr_response_demo


import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("dimensions") val dimensions: Dimensions?,
    @SerializedName("extracted_labels") val extractedLabels: List<Any>?,
    @SerializedName("id") val id: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("location_id") val locationId: Any?,
    @SerializedName("metadata") val metadata: Metadata?,
    @SerializedName("object") val objectX: String?,
    @SerializedName("organization_id") val organizationId: String?,
    @SerializedName("provider_name") val providerName: String?,
    @SerializedName("purchase_order") val purchaseOrder: String?,
    @SerializedName("raw_text") val rawText: Any?,
    @SerializedName("recipient") val recipient: Recipient?,
    @SerializedName("reference_number") val referenceNumber: String?,
    @SerializedName("rma_number") val rmaNumber: String?,
    @SerializedName("sender") val sender: Sender?,
    @SerializedName("service_level_name") val serviceLevelName: String?,
    @SerializedName("tracking_number") val trackingNumber: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("weight") val weight: String?
)