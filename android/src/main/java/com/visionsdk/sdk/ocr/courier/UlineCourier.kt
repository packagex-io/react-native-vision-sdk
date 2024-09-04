package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class UlineCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\buline\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\bTRACKING ?#?:? ?(\\d{15})\\b", RegexType.TrackingNo)
    }

    val patternTracking1Barcode by lazy {
        VisionRegex("\\b[A-Z]{1}\\d\\|\\d{5}\\|(\\d{15})\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1Barcode.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "uline"
            courierInfo.trackingNo = patternTracking1Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1Barcode.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "uline"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1Barcode.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}