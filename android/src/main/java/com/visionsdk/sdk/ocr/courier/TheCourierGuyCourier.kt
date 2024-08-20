package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TheCourierGuyCourier : Courier() {

    val patternTheCourierGuy by lazy {
        VisionRegex("(?i)([\\s\\.\\,]the[\\n\\s\\.\\,]courier[\\n\\s\\.\\,]guy[\\s\\.\\,])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTheCourierGuy = patternTheCourierGuy.find(ocrExtractedText)

        if (searchTheCourierGuy) {
            courierInfo.name = "The Courier Guy"
        }

        return RegexResult(courierInfo)
    }
}