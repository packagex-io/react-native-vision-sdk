package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class NeteaseCourier : Courier() {

    val patternNetease by lazy {
        VisionRegex("(?i)(网易)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchNetease = patternNetease.find(ocrExtractedText)

        if (searchNetease) {
            courierInfo.name = "netease"
        }

        return RegexResult(courierInfo)
    }
}