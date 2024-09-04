package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class UrbanoCourier : Courier( listOf("") ) {

    val patternUrbano by lazy {
        VisionRegex("(?i)(\\bURBANO\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchUrbano = patternUrbano.find(ocrExtractedText)

        if (searchUrbano) {
            courierInfo.name = "urbano"
        }

        return RegexResult(courierInfo)
    }
}