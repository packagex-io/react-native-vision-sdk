package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class Mark3Courier : Courier() {

    val patternMark3 by lazy {
        VisionRegex("Mark ?3 International|MARK ?3 INTERNATIONAL", RegexType.Default)
    }

    val patternMark3Tracking1 by lazy {
        VisionRegex("(?<!\\d)\\d{7}(?!\\d)", RegexType.TrackingNo)
    }

    val patternMark3ShipmentType by lazy {
        VisionRegex("(?i)(^|\\b)next ?day(\\b|\$)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchMark3ShipmentType = patternMark3ShipmentType.find(ocrExtractedText)

        if (searchMark3ShipmentType) {
            courierInfo.shipmentType = "Next Day"
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchMark3 = patternMark3.find(ocrExtractedText)
        val searchMark3Tracking1 = patternMark3Tracking1.find(ocrExtractedText)
        val searchMark3ShipmentType = patternMark3ShipmentType.find(ocrExtractedText)

        if (searchMark3) {
            courierInfo.name = "mark3"
            if (searchMark3Tracking1) {
                courierInfo.trackingNo = patternMark3Tracking1.group(ocrExtractedText, 0).removeSpaces()
            }
            if (searchMark3ShipmentType) {
                courierInfo.shipmentType = "Next Day"
            }
        }

        return RegexResult(courierInfo)
    }
}