package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LaPosteCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(la poste)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)(?<tracking_no>\\b\\d{1}[A-Z]{1} \\d{3} \\d{3} \\d{4} \\d{1}\\b)", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?i)\\b[A-Z]{2} ?\\d{3} ?\\d{3} ?\\d{3} ?F ?R\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)

        if (searchTracking1 || searchTracking2) {
            courierInfo.name = "la-poste"
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
            courierInfo.name = "la-poste"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}