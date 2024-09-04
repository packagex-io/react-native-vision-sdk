package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ChilExpressCourier : Courier( listOf("chilexpress") ) {

    val patternChilExpress by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*chilexpress[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchChilExpress = patternChilExpress.find(ocrExtractedText)

        if (searchChilExpress) {
            courierInfo.name = "chilexpress"
        }

        return RegexResult(courierInfo)
    }
}