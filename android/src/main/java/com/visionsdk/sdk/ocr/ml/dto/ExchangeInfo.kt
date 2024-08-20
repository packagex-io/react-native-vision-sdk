package io.packagex.visionsdk.ocr.ml.dto

internal data class ExchangeInfo(
    val building: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val floor: String,
    val officeNo: String,
    val state: String,
    val stateCode: String,
    val street: String,
    val zipcode: String,
    val personBusinessName: String,
    val personName: String,
    val personPhone: String,
    val poBox: String
)