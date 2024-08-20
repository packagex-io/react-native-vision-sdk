package io.packagex.visionsdk.ocr.ml.dto

internal data class WordWithBoundingBox(val word: String, val boundingBox: List<Int>)