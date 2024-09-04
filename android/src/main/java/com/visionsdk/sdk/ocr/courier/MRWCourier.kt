package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class MRWCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)(\\bMRW\\b)", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("\\b\\d{1}00631 ?([A-Z0-9]{12,13}) ?001\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking = patternTracking.find(barcode)

        if (searchTracking) {
            courierInfo.name = "mrw"
            courierInfo.trackingNo = patternTracking.group(barcode, 1)
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "mrw"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 1).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}