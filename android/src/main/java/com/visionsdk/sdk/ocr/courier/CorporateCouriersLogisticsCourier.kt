package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CorporateCouriersLogisticsCourier : Courier( listOf("corporate-couriers-logistics") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\bCORPORATE[\\n| ]{0,1}COURIERS[\\n| ]{0,1}LOGISTICS\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "corporate-couriers-logistics"
        }

        return RegexResult(courier = courierInfo)
    }
}