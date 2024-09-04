package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ChronopostCourier : Courier( listOf("chronopost") ) {

    val patternChronopost by lazy {
        VisionRegex("(?i)(\\bchronopost\\b)|(www\\.chronopost\\.fr)", RegexType.Default)
    }

    val patternChronopostTracking1 by lazy {
        VisionRegex("[A-Z]{2} ?\\d{3} ?\\d{3} ?\\d{3} ?(FR|JB)", RegexType.TrackingNo)
    }

    val patternChronopostTracking2 by lazy {
        VisionRegex("[A-Z]{2} ?\\d{3} ?\\d{3} ?\\d{3} ?\\dY\\b", RegexType.TrackingNo)
    }

    val patternChronopostTracking3 by lazy {
        VisionRegex("[A-Z]{2}\\d{2} ?\\d{4} ?\\d{4} ?\\d{2}[A-Z]\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchChronopostTracking1 = patternChronopostTracking1.find(barcode)

        if (searchChronopostTracking1) {
            courierInfo.name = "chronopost"
            courierInfo.trackingNo = patternChronopostTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchChronopost = patternChronopost.find(ocrExtractedText)
        val searchChronopostTracking1 = patternChronopostTracking1.find(ocrExtractedText)
        val searchChronopostTracking3 = patternChronopostTracking3.find(ocrExtractedText)

        if (searchChronopost) {
            courierInfo.name = "chronopost"
        }

        if (searchChronopostTracking3) {
            courierInfo.name = "chronopost"
            courierInfo.trackingNo = patternChronopostTracking3.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchChronopostTracking1) {
            courierInfo.name = "chronopost"
            courierInfo.trackingNo = patternChronopostTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }
}