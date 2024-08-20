package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class UrgentCouriersCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\bURGENT\\s+COURIERS\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\b[A-Z]{1}[0-9]{2,5}[A-Z]{1}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "urgent-couriers"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (searchTracking1) {
            courierInfo.name = "urgent-couriers"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }
}