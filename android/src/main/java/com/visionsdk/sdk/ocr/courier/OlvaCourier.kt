package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class OlvaCourier : Courier() {

    val patternOlva by lazy {
        VisionRegex("(?i)([\\s\\.\\,]*olva[\\s\\.\\,]*courier[\\s\\.\\,]*)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOlva = patternOlva.find(ocrExtractedText)

        if (searchOlva) {
            courierInfo.name = "Olva Courier"
        }

        return RegexResult(courierInfo)
    }
}