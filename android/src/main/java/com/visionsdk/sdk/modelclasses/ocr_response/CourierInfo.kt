package io.packagex.visionsdk.modelclasses.ocr_response


import com.google.gson.annotations.SerializedName

data class CourierInfo(
    @SerializedName("barcodePresent") val barcodePresent: Boolean?,
    @SerializedName("courierFromBarcode") val courierFromBarcode: Boolean?,
    @SerializedName("courierFromOcr") val courierFromOcr: Boolean?,
    @SerializedName("courierName") val courierName: String?,
    @SerializedName("courierNameDetected") val courierNameDetected: String?,
    @SerializedName("dynamicExtractedLabels") val dynamicExtractedLabels: List<Any?>?,
    @SerializedName("parcelDimensions") val parcelDimensions: ParcelDimensions?,
    @SerializedName("presetLabels") val presetLabels: List<Any?>?,
    @SerializedName("refNumber") val refNumber: String?,
    @SerializedName("trackingFromBarcode") val trackingFromBarcode: Boolean?,
    @SerializedName("miscellaneous") val miscellaneous: Miscellaneous,
    @SerializedName("trackingFromOcr") val trackingFromOcr: Boolean?,
    @SerializedName("trackingNo") val trackingNo: String?,
    @SerializedName("shipmentType") val shipmentType: String?,
    @SerializedName("weight") val weight: Weight?,
    @SerializedName("dynamicLabels") val dynamicLabels: Weight?,
    @SerializedName("rma") val rma: String?,
    @SerializedName("weightInfo") val weightInfo: String,
    @SerializedName("locationBasedLabels") val locationBasedLabels: List<Any>,
)