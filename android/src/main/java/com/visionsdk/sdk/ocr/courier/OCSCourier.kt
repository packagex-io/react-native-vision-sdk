package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class OCSCourier : Courier() {

    val patternOCS by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*ocs[\\s\\.\\,thailand]*[\\s\\.\\,]*co[\\s\\.\\,]ltd[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOCS = patternOCS.find(ocrExtractedText)

        if (searchOCS) {
            courierInfo.name = "OCS"
        }

        return RegexResult(courierInfo)
    }
}