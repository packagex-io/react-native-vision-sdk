package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class WPSExpressCourier : Courier() {

    val patternWPSExpress by lazy {
        VisionRegex("(?i)([\\s\\.\\,]wsp[\\s\\.\\,]Express[\\s\\.\\,])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchWPSExpress = patternWPSExpress.find(ocrExtractedText)

        if (searchWPSExpress) {
            courierInfo.name = "wsp-express"
        }

        return RegexResult(courierInfo)
    }
}