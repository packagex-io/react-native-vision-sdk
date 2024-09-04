package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class OLDCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\b(Olesa Logistica ?i ?Distribucio)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Olesa Logistica i Distribucio"
        }

        return RegexResult(courier = courierInfo)
    }
}