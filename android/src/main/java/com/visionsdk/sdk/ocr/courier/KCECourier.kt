package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class KCECourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\b(KCE|Курьер Сервис Экспресс|kypb\\u0435p \\u0441\\u0435p\\u0432\\u0438\\u0441 \\u0437\\u043ac\\u043fp\\u0435c\\u0441)\\b", RegexType.Default)
    }

    val patternTrackingEnglish by lazy {
        VisionRegex("(?i)\\b(CSE|Courier Service Express)\\b", RegexType.TrackingNo)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\b\\d{3}-\\d{7}\\b", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?i)\\b\\d{8}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(barcode)

        if (search && searchTracking2) {
            courierInfo.name = "KCE"
            courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "KCE"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}