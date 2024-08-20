package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class DeutschePostCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(deutsche post)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?<tracking_no>\\b[A-Z]{2} ?\\d{2} ?\\d{3} ?\\d{3} ?\\d{1}[A-Z]{2} ?\\d{3}\\b)", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?<tracking_no>\\b[A-Z]{2} ?\\d{3} ?\\d{3} ?\\d{3} ?[A-Z]{2}\\b)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)

        if (search) {
            courierInfo.name = "deutsche-post"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "deutsche-post"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}