package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class KoreaPostCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\bKOREA POST\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("\\b(?i)[A-Z]{2} [0-9]{6} [0-9]{3} KR\\b", RegexType.TrackingNo)
    }

    val patternTracking1Barcode by lazy {
        VisionRegex("\\b(?i)[A-Z]{2}[0-9]{9}KR\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1Barcode.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "korea-post"
            courierInfo.trackingNo = patternTracking1Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "korea-post"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}