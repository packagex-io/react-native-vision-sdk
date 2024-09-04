package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class DepponCourier : Courier( listOf("deppon") ) {

    val patternDeppon by lazy {
        VisionRegex("(?i)(德邦)|(www\\.deppon\\.com)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDeppon = patternDeppon.find(ocrExtractedText)

        if (searchDeppon) {
            courierInfo.name = "deppon"
        }

        return RegexResult(courierInfo)
    }
}