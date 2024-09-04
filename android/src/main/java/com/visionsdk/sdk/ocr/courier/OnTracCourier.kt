package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class OnTracCourier : Courier( listOf("") ) {

    val patternOnTrac by lazy {
        VisionRegex("\\b(?i)(OnTrac)\\b", RegexType.Default)
    }

    val patternOnTracTracking1 by lazy {
        VisionRegex("\\b((C|D)\\d{14})\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOnTracTracking1 = patternOnTracTracking1.find(barcode)

        if (searchOnTracTracking1) {
            courierInfo.name = "ontrac"
            courierInfo.trackingNo = patternOnTracTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOnTrac = patternOnTrac.find(ocrExtractedText)
        val searchOnTracTracking1 = patternOnTracTracking1.find(ocrExtractedText)

        if (searchOnTrac) {
            courierInfo.name = "ontrac"
            if (searchOnTracTracking1) {
                courierInfo.trackingNo = patternOnTracTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}