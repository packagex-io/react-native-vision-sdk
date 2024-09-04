package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class InPostCourier : Courier( listOf("") ) {

    val patternInPost by lazy {
        VisionRegex("(?i)([\\s\\.\\,]inpost[\\s\\.\\,]*)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchInPost = patternInPost.find(ocrExtractedText)

        if (searchInPost) {
            courierInfo.name = "InPost"
        }

        return RegexResult(courierInfo)
    }
}