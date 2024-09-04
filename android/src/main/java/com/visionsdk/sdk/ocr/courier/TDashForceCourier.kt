package io.packagex.visionsdk.ocr.courier

internal class TDashForceCourier : Courier( listOf("") ) {

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = TForceCourier().pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "t-force"
        }

        return RegexResult(courierInfo)
    }
}