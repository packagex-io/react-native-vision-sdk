package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class AramexCourier : Courier( listOf("aramex") ) {

    val patternAramex = VisionRegex("(?i)(\\b[\\s\\.\\,]*aramex[\\s\\.\\,]*\\b)", RegexType.Default)

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchJne = patternAramex.find(ocrExtractedText)

        if (searchJne) {
            courierInfo.name = "aramex"
        }

        return RegexResult(courierInfo)
    }
}