package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SpeedxCourier : Courier( listOf("") ) {

    val pattern by lazy { VisionRegex("(?i)\\bSPEEDX\\b", RegexType.Default) }
    val patternTracking1 by lazy { VisionRegex("(?i)\\bSPX1(DM|EG)\\d{12}\\b", RegexType.TrackingNo) }
    val patternTracking2 by lazy { VisionRegex("(?i)\\bSPX1[A-Z]{2}\\d{12}\\b", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "speedx"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
            if (searchTracking2 && search) {
                courierInfo.name = "speedx"
                courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "speedx"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}