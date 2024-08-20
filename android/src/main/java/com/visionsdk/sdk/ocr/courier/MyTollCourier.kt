package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class MyTollCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)TOLL", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("\\(00\\) ?\\d{18}", RegexType.TrackingNo)
    }

    val patternTracking1Barcode by lazy {
        VisionRegex("\\b00?\\d{18}\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1Barcode.find(barcode)

        if (search && searchTracking1) {
            courierInfo.name = "mytoll"
            courierInfo.trackingNo = patternTracking1Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "mytoll"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}