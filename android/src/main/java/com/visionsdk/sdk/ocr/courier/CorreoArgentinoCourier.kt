package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CorreoArgentinoCourier : Courier( listOf("correo-argentino") ) {

    val patternCorreoArgentino by lazy {
        VisionRegex("(?i)(\\bCORREO\\nARGENTINO\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCorreoArgentino = patternCorreoArgentino.find(ocrExtractedText)

        if (searchCorreoArgentino) {
            courierInfo.name = "correo-argentino"
        }

        return RegexResult(courierInfo)
    }
}