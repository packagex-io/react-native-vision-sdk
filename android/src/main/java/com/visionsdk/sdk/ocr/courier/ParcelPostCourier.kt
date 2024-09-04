package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ParcelPostCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\bParcel Post\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\b9979\\d{14}\\b", RegexType.TrackingNo)
    }

    val patternTracking1Barcode by lazy {
        VisionRegex("(?i)\\b9979\\d{14}\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (searchTracking1) {
            courierInfo.name = "Parcel Post"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 1).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Parcel Post"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 1).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}