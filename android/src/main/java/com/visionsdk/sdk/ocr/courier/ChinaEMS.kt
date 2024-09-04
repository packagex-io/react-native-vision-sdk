package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ChinaEMS : Courier( listOf("china_ems") ) {

    val patternChinaEMS: VisionRegex by lazy { VisionRegex("(?i)\\b(全球邮政特快专递)\\b|\\b(Express Mail Service)\\b", RegexType.Default) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchChinaEMS = patternChinaEMS.find(ocrExtractedText)
        if (searchChinaEMS) {
            courierInfo.name = "china-ems"
            sender.country = "China"
        }

        return RegexResult(courierInfo, sender = sender)
    }
}