package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TwoFourSeven247ExpressCourier : Courier() {

    val patternTwoFourSeven247Express by lazy {
        VisionRegex("(?i)(\\b247[\\s\\,\\.]*Express[\\s\\,\\.]*\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTwoFourSeven247Express = patternTwoFourSeven247Express.find(ocrExtractedText)

        if (searchTwoFourSeven247Express) {
            courierInfo.name = "247 Express"
        }

        return RegexResult(courierInfo)
    }
}