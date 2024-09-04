package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TokopediaCourier : Courier( listOf("") ) {

    val patternTokopedia by lazy {
        VisionRegex("(?i)(\\btokopedia\\b)", RegexType.Default)
    }

    val patternTokopediaTracking1 by lazy {
        VisionRegex("(?i)\\bTK[A-Z]{2}-[A-Z0-9]{11}\\b", RegexType.TrackingNo)
    }

    val patternTokopediaTracking1Barcode by lazy {
        VisionRegex("(?i)\\bTK[A-Z]{2}[A-Z0-9]{11}\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTokopediaTracking1Barcode = patternTokopediaTracking1Barcode.find(barcode)

        if (searchTokopediaTracking1Barcode) {
            courierInfo.name = "tokopedia"
            courierInfo.trackingNo = patternTokopediaTracking1Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTokopedia = patternTokopedia.find(ocrExtractedText)
        val searchTokopediaTracking1 = patternTokopediaTracking1.find(ocrExtractedText)

        if (searchTokopedia) {
            courierInfo.name = "tokopedia"
            if (searchTokopediaTracking1) {
                courierInfo.trackingNo = patternTokopediaTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}