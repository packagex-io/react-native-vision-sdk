package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CorreiosCourier : Courier( listOf("correios") ) {

    val pattern by lazy { VisionRegex("(?i)\\bcorreios?\\b", RegexType.Default) }
    val patternTracking by lazy { VisionRegex("(?i)\\b[A-Z]{2} \\d{8} \\d BR\\b", RegexType.TrackingNo) }
    val patternTrackingBarcode by lazy { VisionRegex("(?i)\\b[A-Z]{2}\\d{9}BR\\b", RegexType.Barcode) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTrackingBarcode = patternTrackingBarcode.find(barcode)

        if (search && searchTrackingBarcode) {
            courierInfo.name = "correios"
            courierInfo.trackingNo = patternTrackingBarcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "correios"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}