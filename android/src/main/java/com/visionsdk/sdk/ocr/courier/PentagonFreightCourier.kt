package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class PentagonFreightCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)[\\s\\.\\,]*pentagon[\\s\\-]*freight|[\\s\\.\\,]*pentagon[\\s\\.\\,]*", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "pentagon-freight"
        }

        return RegexResult(courier = courierInfo)
    }
}