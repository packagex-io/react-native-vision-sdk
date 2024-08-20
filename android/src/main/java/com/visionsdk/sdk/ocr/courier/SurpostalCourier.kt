package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SurpostalCourier : Courier() {

    val patternSurpostal by lazy {
        VisionRegex("(?i)(\\bSURPOSTAL\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchSurpostal = patternSurpostal.find(ocrExtractedText)

        if (searchSurpostal) {
            courierInfo.name = "surpostal"
        }

        return RegexResult(courierInfo)
    }
}