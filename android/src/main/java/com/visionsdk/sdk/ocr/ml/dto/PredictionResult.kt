package io.packagex.visionsdk.ocr.ml.dto

import io.packagex.visionsdk.ocr.courier.RegexResult

internal data class PredictionResult(
    val ocrExtractedText: String?,
    val regexResult: RegexResult?,
    val mlResults: MLResults?
)
