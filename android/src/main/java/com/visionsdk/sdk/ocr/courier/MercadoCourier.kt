package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class MercadoCourier : Courier( listOf("") ) {

    val patternMercado by lazy {
        VisionRegex("(?i)(\\bmercado\\b)", RegexType.Default)
    }

    val patternMercadoTracking1 by lazy {
        VisionRegex("(?i)Contrato:? ?(\\d{10})", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchMercado = patternMercado.find(ocrExtractedText)
        val searchMercadoTracking1 = patternMercadoTracking1.find(ocrExtractedText)

        if (searchMercado) {
            courierInfo.name = "mercado"
            if (searchMercadoTracking1) {
                courierInfo.trackingNo = patternMercadoTracking1.group(ocrExtractedText, 1).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}