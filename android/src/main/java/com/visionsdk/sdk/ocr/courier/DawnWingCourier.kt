package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class DawnWingCourier : Courier( listOf("dawn-wing") ) {

    val patternDawnWing by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*dawn[\\n\\s\\.\\,]*wing[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDawnWing = patternDawnWing.find(ocrExtractedText)

        if (searchDawnWing) {
            courierInfo.name = "dawn-wing"
        }

        return RegexResult(courierInfo)
    }
}