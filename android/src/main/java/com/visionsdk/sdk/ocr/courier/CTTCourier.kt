package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CTTCourier : Courier( listOf("ctt") ) {

    val pattern by lazy {
        VisionRegex("(?i)(\\bctt\\b)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\bEXPED ?.? ?(\\d{6} ?- ?\\d{6} ?- ?\\d{10} ?- ?001)\\b", RegexType.TrackingNo)
    }

    val patternTracking1Barcode by lazy {
        VisionRegex("\\b\\d{22}001\\b", RegexType.Barcode)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?i)\\b[A-Z]{2}\\d{9}PT\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1Barcode = patternTracking1Barcode.find(barcode)

        if (searchTracking1Barcode) {
            courierInfo.name = "ctt"
            courierInfo.trackingNo = patternTracking1Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "ctt"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 1).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}