package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BlueDartCourier : Courier( listOf("bluedart") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\b(Blue\\s?dart)\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("\\b\\d{11}\\b", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?i)\\b((Blue\\s?dart|AWB\\s?No)\\s?#\\s?)(\\d{11})\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)

        if (search) {
            courierInfo.name = "bluedart"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "bluedart"
            if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 3).removeSpaces()
            } else if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}