package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class StarTrackCourier : Courier() {

    val patternStarTrack by lazy {
        VisionRegex("(?i)star *track", RegexType.Default)
    }

    val patternStarTrackTracking1 by lazy {
        VisionRegex("\\b[A-Z0-9]{4}\\d{8}\\b", RegexType.TrackingNo)
    }

    val patternStarTrackTracking2 by lazy {
        VisionRegex("\\b[A-Z]{4}\\d{8}[A-Z]{3}\\d{5}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchStarTrack = patternStarTrack.find(ocrExtractedText)
        val searchStarTrackTracking1 = patternStarTrackTracking1.find(barcode)
        val searchStarTrackTracking2 = patternStarTrackTracking2.find(barcode)

        if (searchStarTrack) {
            courierInfo.name = "star-track"
            if (searchStarTrackTracking2) {
                courierInfo.trackingNo = patternStarTrackTracking2.group(barcode, 0).removeSpaces()
            } else if (searchStarTrackTracking1) {
                courierInfo.trackingNo = patternStarTrackTracking1.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchStarTrack = patternStarTrack.find(ocrExtractedText)
        val searchStarTrackTracking1 = patternStarTrackTracking1.find(ocrExtractedText)
        val searchStarTrackTracking2 = patternStarTrackTracking2.find(ocrExtractedText)

        if (searchStarTrack) {
            courierInfo.name = "star-track"
            if (searchStarTrackTracking2) {
                courierInfo.trackingNo = patternStarTrackTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchStarTrackTracking1) {
                courierInfo.trackingNo = patternStarTrackTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}