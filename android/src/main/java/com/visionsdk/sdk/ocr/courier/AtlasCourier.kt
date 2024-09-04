package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class AtlasCourier : Courier( listOf("atlas-couriers") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\bATLAS[\\n| ]{0,1}COURIERS?([\\n| ]{0,1}EXPRESS)?\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "atlas-courier"
        }

        return RegexResult(courier = courierInfo)
    }
}