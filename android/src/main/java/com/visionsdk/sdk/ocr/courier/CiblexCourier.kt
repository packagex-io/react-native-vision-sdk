package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CiblexCourier : Courier( listOf("ciblex") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\b(ciblex)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchInPost = pattern.find(ocrExtractedText)

        if (searchInPost) {
            courierInfo.name = "ciblex"
        }

        return RegexResult(courierInfo)
    }
}