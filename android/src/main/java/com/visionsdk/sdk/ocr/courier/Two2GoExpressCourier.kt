package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class Two2GoExpressCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(\\b2GO[\\s\\,\\.]*Express[\\s\\,\\.]*|express[\\s\\.\\,]*2go[\\s\\.\\,]*com)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "2GO Express"
        }

        return RegexResult(courierInfo)
    }
}