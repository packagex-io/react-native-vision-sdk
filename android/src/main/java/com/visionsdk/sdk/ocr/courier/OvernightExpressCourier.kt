package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class OvernightExpressCourier : Courier( listOf("") ) {

    val patternOvernightExpress by lazy {
        VisionRegex("(?i)(\\bOvernight Express\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOvernightExpress = patternOvernightExpress.find(ocrExtractedText)

        if (searchOvernightExpress) {
            courierInfo.name = "overnight-express"
        }

        return RegexResult(courierInfo)
    }
}