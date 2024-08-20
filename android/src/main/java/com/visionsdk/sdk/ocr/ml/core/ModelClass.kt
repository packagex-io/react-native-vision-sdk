package io.packagex.visionsdk.ocr.ml.core

enum class ModelClass(val value: String) {
    ShippingLabel("shipping_label"),
    BillOfLading("bill_of_lading"),
    PriceTag("item_label")
}