package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class JRSExpressCourier : Courier() {

    val pattern by lazy {
        VisionRegex("([\\s\\.\\,]jrs[\\n\\s\\.\\,]*express[\\s\\.\\,]*)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "JRS Express"
        }

        return RegexResult(courierInfo)
    }
}