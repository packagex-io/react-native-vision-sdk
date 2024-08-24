package io.packagex.visionsdk.ocr.ml.dto

internal data class WordWithLineNumber(val word: String, val lineNumber: Int)

internal data class WordWithBoundingBoxAndLineNumber(val word: String, val boundingBox: List<Int>, val lineNumber: Int)
