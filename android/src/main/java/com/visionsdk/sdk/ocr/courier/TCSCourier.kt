package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TCSCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("[Tt]cs|TCS", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("\\b(801078|3142|5101|414)[\\d]{5,8}(?!\\d)\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)

        if (search) {
            courierInfo.name = "tcs"
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

        if (searchTracking1) {
            courierInfo.name = "tcs"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        if (search) {
            courierInfo.name = "tcs"
        }

        return RegexResult(courier = courierInfo)
    }
}