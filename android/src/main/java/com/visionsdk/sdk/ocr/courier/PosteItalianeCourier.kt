package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class PosteItalianeCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)(poste ?italiane|postaraccomandata|promopacco\\s?plus)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?<tracking_no>\\b[A-Z]{1}\\d{3}[A-Z]{1}\\d{8}\\b)", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?<tracking_no>\\b\\d{6}[A-Z]{1}\\d{6}\\b)", RegexType.TrackingNo)
    }

    val patternTracking3 by lazy {
        VisionRegex("(?<tracking_no>\\b\\d{3}[A-Z]{1}\\d{8}[A-Z]{1}\\b)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)
        val searchTracking3 = patternTracking3.find(barcode)

        if (searchTracking1 || searchTracking2 || searchTracking3) {
            courierInfo.name = "posteitaliane"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
            } else if (searchTracking3) {
                courierInfo.trackingNo = patternTracking3.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchTracking3 = patternTracking3.find(ocrExtractedText)

        if (search || searchTracking1 || searchTracking2 || searchTracking3) {
            courierInfo.name = "posteitaliane"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking3) {
                courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}