package io.packagex.visionsdk.ocr.ml.dto

internal data class PackageInfo(
    val name: String,
    val trackingNo: String,
    val dimension: String,
    val weight: Double,
    val weightUnit: String
)