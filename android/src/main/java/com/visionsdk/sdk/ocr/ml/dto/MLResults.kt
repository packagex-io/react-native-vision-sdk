package io.packagex.visionsdk.ocr.ml.dto

internal data class MLResults(
    val packageInfo: PackageInfo,
    val sender: ExchangeInfo,
    val receiver: ExchangeInfo,
    val logisticAttributes: LogisticAttributes
)