package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BEXExpressCourier : Courier( listOf("bex-express") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*bex[\\s\\.\\,]*(express|national)|www[\\s\\.\\,]*bex[\\s\\.\\,]*co[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "bex-express"
        }

        return RegexResult(courierInfo)
    }
}