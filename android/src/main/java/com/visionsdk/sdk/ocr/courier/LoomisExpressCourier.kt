package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LoomisExpressCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*loomisexpress[\\s\\.\\,]*|loomis[\\s\\.\\,\\+]*express[\\s\\.\\,]|loomis[\\s\\.\\,\\-]*express[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    val patternBarcode by lazy {
        VisionRegex("(?i)LQM4W3K3AANET\\s*\\d{8}\\s*", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBarcode = patternBarcode.find(barcode)

        if (searchBarcode) {
            courierInfo.name = "Loomis Express"
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Loomis Express"
        }

        return RegexResult(courier = courierInfo)
    }
}