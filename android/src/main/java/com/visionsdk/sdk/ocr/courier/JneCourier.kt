package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class JneCourier : Courier( listOf("") ) {

    val patternJne by lazy {
        VisionRegex("(?i)((\\bjne\\b)|(\\.jne\\.))", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchJne = patternJne.find(ocrExtractedText)

        if (searchJne) {
            courierInfo.name = "jne"
        }

        return RegexResult(courierInfo)
    }
}