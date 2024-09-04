package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TmallCourier : Courier( listOf("") ) {

    val patternTmall by lazy {
        VisionRegex("(?i)(天猫)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTmall = patternTmall.find(ocrExtractedText)

        if (searchTmall) {
            courierInfo.name = "tmall"
        }

        return RegexResult(courierInfo)
    }
}