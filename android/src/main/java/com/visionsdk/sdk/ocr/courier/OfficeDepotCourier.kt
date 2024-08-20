package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class OfficeDepotCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\bOFFICE DEPOT\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("88[0-9]{6}010253", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("000003525528[0-9]{8}", RegexType.TrackingNo)
    }

    val patternTracking3 by lazy {
        VisionRegex("D2[0-9]{11}2530001", RegexType.TrackingNo)
    }

    val patternTracking4 by lazy {
        VisionRegex("D3[0-9]{11}1340002", RegexType.TrackingNo)
    }

    val patternTracking4b by lazy {
        VisionRegex("D[0-9]{19}", RegexType.TrackingNo)
    }

    val patternTracking4c by lazy {
        VisionRegex("D ?\\d{12} ?\\d{3} ?\\d001", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)
        val searchTracking3 = patternTracking3.find(barcode)
        val searchTracking4 = patternTracking4.find(barcode)
        val searchTracking4b = patternTracking4b.find(barcode)

        if (searchTracking1 || searchTracking2 || searchTracking3 || searchTracking4 || searchTracking4b) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = barcode
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchTracking3 = patternTracking3.find(ocrExtractedText)
        val searchTracking4 = patternTracking4.find(ocrExtractedText)
        val searchTracking4b = patternTracking4b.find(ocrExtractedText)
        val searchTracking4c = patternTracking4c.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "office-depot"
        }

        if (searchTracking1) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking2) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking3) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking4) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = patternTracking4.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking4b) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = patternTracking4b.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking4c) {
            courierInfo.name = "office-depot"
            courierInfo.trackingNo = patternTracking4c.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }
}