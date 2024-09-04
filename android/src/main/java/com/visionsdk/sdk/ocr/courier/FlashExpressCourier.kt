package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class FlashExpressCourier : Courier( listOf("") ) {

    val patternFlashExpress by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*flash[\\n\\s\\.\\,]*express[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchFlashExpress = patternFlashExpress.find(ocrExtractedText)

        if (searchFlashExpress) {
            courierInfo.name = "Flash Express"
        }

        return RegexResult(courierInfo)
    }
}