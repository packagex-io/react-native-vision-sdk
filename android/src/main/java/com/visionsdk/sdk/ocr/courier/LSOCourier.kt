package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LSOCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\b(LoneStar|Lone[\\s\\.\\,]Star|LSO)\\b", RegexType.Default)
    }

    val patternBarcode1 by lazy {
        VisionRegex("\\b(\\d{13}|\\d{20})\\b", RegexType.Barcode)
    }

    val patternBarcode2 by lazy {
        VisionRegex("\\b[A-Z]{2}[A-Z0-9]{6}\\b", RegexType.Barcode)
    }

    val patternBarcode3 by lazy {
        VisionRegex("\\bLSO[A-Z]{2}\\d{10}\\b", RegexType.Barcode)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\b(PRO[\\s\\.\\,]NUMBER|PRONUMBER|Airbill( No)?)\\b[\\s\\.\\,:\\n]*([A-Z0-9]{8})\\b", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("\\bZY[0-9]{2}[A-Z0-9]{4}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBarcode3 = patternBarcode3.find(barcode)

        if (searchBarcode3) {
            courierInfo.name = "lso"
            courierInfo.trackingNo = patternBarcode3.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchBarcode3 = patternBarcode3.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "lso"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 3).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchBarcode3) {
                courierInfo.trackingNo = patternBarcode3.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}