package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class YodelCourier : Courier() {

    val patternYodel by lazy {
        VisionRegex("(?i)\\b(yodel)\\b", RegexType.Default)
    }

    val patternYodelTracking1 by lazy {
        VisionRegex("87[A-Z]{1}[-A-Z0-9]{13,14}", RegexType.TrackingNo)
    }

    val patternYodelTracking2 by lazy {
        VisionRegex("2LGB[A-Z0-9]{5,9}\\+\\d{8}(?!\\d)", RegexType.TrackingNo)
    }

    val patternYodelTracking3 by lazy {
        VisionRegex("RB\\d{14}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchYodelTracking1 = patternYodelTracking1.find(barcode)
        val searchYodelTracking2 = patternYodelTracking2.find(barcode)
        val searchYodelTracking3 = patternYodelTracking3.find(barcode)

        if (searchYodelTracking1 || searchYodelTracking2 || searchYodelTracking3) {
            courierInfo.name = "yodel"
            if (searchYodelTracking1) {
                courierInfo.trackingNo = patternYodelTracking1.group(barcode, 0).removeSpaces()
            } else if (searchYodelTracking3) {
                courierInfo.trackingNo = patternYodelTracking3.group(barcode, 0).removeSpaces()
            } else if (searchYodelTracking2) {
                courierInfo.trackingNo = patternYodelTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchYodel = patternYodel.find(ocrExtractedText)
        val searchYodelTracking1 = patternYodelTracking1.find(ocrExtractedText)
        val searchYodelTracking2 = patternYodelTracking2.find(ocrExtractedText)
        val searchYodelTracking3 = patternYodelTracking3.find(ocrExtractedText)

        if (searchYodel || searchYodelTracking1 || searchYodelTracking2 || searchYodelTracking3) {
            courierInfo.name = "yodel"
            if (searchYodelTracking1) {
                courierInfo.trackingNo = patternYodelTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchYodelTracking3) {
                courierInfo.trackingNo = patternYodelTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchYodelTracking2) {
                courierInfo.trackingNo = patternYodelTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}