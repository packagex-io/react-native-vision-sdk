package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class UnitopCourier : Courier( listOf("") ) {

    val patternUnitop by lazy {
        VisionRegex("(?i)(全一快递)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchUnitop = patternUnitop.find(ocrExtractedText)

        if (searchUnitop) {
            courierInfo.name = "unitop"
        }

        return RegexResult(courierInfo)
    }
}