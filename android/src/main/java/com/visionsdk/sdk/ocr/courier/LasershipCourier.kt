package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LasershipCourier : Courier() {

    val patternLasership by lazy {
        VisionRegex("(?i)(lasership)", RegexType.Default)
    }

    val patternLasership2 by lazy {
        VisionRegex("(?i)^.*lasership\\.com\\/track\\/.*", RegexType.Default)
    }

    val patternLasershipTracking1 by lazy {
        VisionRegex("\\bL[A-Z]{7}\\d\\b", RegexType.TrackingNo)
    }

    val patternLasershipTracking1b by lazy {
        VisionRegex("\\b1L([A-Z0-9]){12,17}\\b", RegexType.TrackingNo)
    }

    val patternLasershipTracking2 by lazy {
        VisionRegex("(?i)^.*lasership\\.com\\/track\\/(?<tracking_no>L([A-Z]{7}\\d)|1L([A-Z0-9]){12,17})", RegexType.TrackingNo)
    }

    val patternLasershipTracking3 by lazy {
        VisionRegex("\\b1L([A-Z0-9]){12,17}-\\d{1}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchLasership = patternLasership.find(ocrExtractedText)
        val searchLasership2 = patternLasership2.find(barcode)
        val searchLasershipTracking1 = patternLasershipTracking1.find(barcode)
        val searchLasershipTracking1b = patternLasershipTracking1b.find(barcode)
        val searchLasershipTracking2 = patternLasershipTracking2.find(barcode)
        val searchLasershipTracking3 = patternLasershipTracking3.find(barcode)

        if (searchLasershipTracking1 || searchLasership2 || searchLasershipTracking1b || searchLasershipTracking3) {
            courierInfo.name = "lasership"
            if (searchLasershipTracking1) {
                courierInfo.trackingNo = patternLasershipTracking1.group(barcode, 0).removeSpaces()
            } else if (searchLasershipTracking3) {
                courierInfo.trackingNo = patternLasershipTracking3.group(barcode, 0).removeSpaces()
            } else if (searchLasershipTracking1b && (searchLasership || searchLasership2)) {
                courierInfo.trackingNo = patternLasershipTracking1b.group(barcode, 0).removeSpaces()
            } else if (searchLasershipTracking2) {
                courierInfo.trackingNo = patternLasershipTracking1.group(barcode, "tracking_no").removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchLasership = patternLasership.find(ocrExtractedText)
        val searchLasershipTracking1 = patternLasershipTracking1.find(ocrExtractedText)
        val searchLasershipTracking1b = patternLasershipTracking1b.find(ocrExtractedText)
        val searchLasershipTracking2 = patternLasershipTracking2.find(ocrExtractedText)
        val searchLasershipTracking3 = patternLasershipTracking3.find(ocrExtractedText)

        if (searchLasership || searchLasershipTracking1 || searchLasershipTracking1b || searchLasershipTracking2 || searchLasershipTracking3) {
            courierInfo.name = "lasership"
            if (searchLasershipTracking1) {
                courierInfo.trackingNo = patternLasershipTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchLasershipTracking3) {
                courierInfo.trackingNo = patternLasershipTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchLasershipTracking1b) {
                courierInfo.trackingNo = patternLasershipTracking1b.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchLasershipTracking2) {
                courierInfo.trackingNo = patternLasershipTracking1.group(ocrExtractedText, "tracking_no").removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}