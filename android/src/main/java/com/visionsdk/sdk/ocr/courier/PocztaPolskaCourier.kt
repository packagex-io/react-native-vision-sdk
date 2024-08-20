package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class PocztaPolskaCourier : Courier() {

    val patternPocztaPolska by lazy {
        VisionRegex("(?i)([\\s\\.\\,]Poczta[\\s\\.\\,]Polska[\\s\\.\\,])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchPocztaPolska = patternPocztaPolska.find(ocrExtractedText)

        if (searchPocztaPolska) {
            courierInfo.name = "poczta-polska"
        }

        return RegexResult(courierInfo)
    }
}