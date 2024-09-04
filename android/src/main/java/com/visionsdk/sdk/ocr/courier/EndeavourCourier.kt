package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class EndeavourCourier : Courier( listOf("") ) {

    val patternEndeavour by lazy {
        VisionRegex("Endeavour[ \\\\n]Delivery", RegexType.Default)
    }

    val patternEndeavourTracking1 by lazy {
        VisionRegex("\\b21\\d{8}01\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchEndeavourTracking1 = patternEndeavourTracking1.find(barcode)

        if (searchEndeavourTracking1) {
            courierInfo.name = "endeavour"
            courierInfo.trackingNo = patternEndeavourTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchEndeavour = patternEndeavour.find(ocrExtractedText)
        val searchEndeavourTracking1 = patternEndeavourTracking1.find(ocrExtractedText)

        if (searchEndeavour || searchEndeavourTracking1) {
            courierInfo.name = "endeavour"
            if (searchEndeavourTracking1) {
                courierInfo.trackingNo = patternEndeavourTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}