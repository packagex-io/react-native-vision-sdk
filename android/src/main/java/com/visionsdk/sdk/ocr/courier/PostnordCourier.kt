package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class PostnordCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("postnord", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("[A-Z]{2}\\d{9}[A-Z]{2}", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("[A-Z]{2} \\d{2} \\d{3} \\d{3} \\d [A-Z]{2}", RegexType.TrackingNo)
    }

    val patternTracking3 by lazy {
        VisionRegex("(?i)\\bShipment-ID: ?(\\d{17})\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        if (searchTracking1) {
            courierInfo.name = "Postnord"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchTracking3 = patternTracking3.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Postnord"
            if (searchTracking3) {
                courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 1).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking1) {
                courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}