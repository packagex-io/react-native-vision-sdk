package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class CanadaPostCourier : Courier( listOf("canada_post") ) {

    val patternCanadaPost: VisionRegex by lazy { VisionRegex("(?i)(Canada[\\s]*Postes|Canada[\\s]*Post|Postes[\\s]*Canada)", RegexType.Default) }
    val patternCanadaPostTracking1: VisionRegex by lazy { VisionRegex("(?i)\\d{4} ?\\d{4} ?\\d{4} ?\\d{4}\\b", RegexType.TrackingNo) }
    val patternCanadaPostTracking2: VisionRegex by lazy { VisionRegex("(?i)\\b[A-Z]{2} ?\\d{3} ?\\d{3} ?\\d{3} ?[A-Z]{2}\\b", RegexType.TrackingNo) }
    val patternCanadaPostTracking3: VisionRegex by lazy { VisionRegex("\\b\\d{2} ?\\d{3} ?\\d{3} ?\\d{3}\\b", RegexType.TrackingNo) }
    val patternCanadaPostTracking4: VisionRegex by lazy { VisionRegex("(?i)^\\d([A-Z]{3}\\d{3})(?<tracking_no>\\d{16})\\d{5}\$", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCanadaPost = patternCanadaPost.find(ocrExtractedText)
        val searchCanadaPostTracking4 = patternCanadaPostTracking4.find(barcode)
        val searchCanadaPostTracking1 = patternCanadaPostTracking1.find(barcode)
        val searchCanadaPostTracking2 = patternCanadaPostTracking2.find(barcode)
        val searchCanadaPostTracking3 = patternCanadaPostTracking3.find(barcode)

        if (searchCanadaPostTracking4) {

            courierInfo.name = "canada-post"
            val trackingNo = patternCanadaPostTracking4.group(barcode, "tracking_no").removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCanadaPost && (searchCanadaPostTracking1 || searchCanadaPostTracking2 || searchCanadaPostTracking3)) {

            courierInfo.name = "canada-post"

            if (searchCanadaPostTracking1) {
                val trackingNo = patternCanadaPostTracking1.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanadaPostTracking2) {
                val trackingNo = patternCanadaPostTracking2.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanadaPostTracking3) {
                val trackingNo = patternCanadaPostTracking3.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCanadaPost = patternCanadaPost.find(ocrExtractedText)
        val searchCanadaPostTracking1 = patternCanadaPostTracking1.find(ocrExtractedText)
        val searchCanadaPostTracking2 = patternCanadaPostTracking2.find(ocrExtractedText)
        val searchCanadaPostTracking3 = patternCanadaPostTracking3.find(ocrExtractedText)

        if (searchCanadaPost && (searchCanadaPostTracking1 || searchCanadaPostTracking2 || searchCanadaPostTracking3)) {

            courierInfo.name = "canada-post"

            if (searchCanadaPostTracking1) {
                val trackingNo = patternCanadaPostTracking1.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanadaPostTracking2) {
                val trackingNo = patternCanadaPostTracking2.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanadaPostTracking3) {
                val trackingNo = patternCanadaPostTracking3.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        return RegexResult(courierInfo)
    }
}