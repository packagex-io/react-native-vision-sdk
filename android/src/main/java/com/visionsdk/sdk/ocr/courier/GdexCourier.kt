package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class GdexCourier : Courier() {

    val patternGdex by lazy {
        VisionRegex("(?i)((\\bgdex\\b)|(\\.gdexpress\\.))", RegexType.Default)
    }

    val patternGdexTracking1 by lazy {
        VisionRegex("MY\\d{11}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchGdexTracking1 = patternGdexTracking1.find(barcode)

        if (searchGdexTracking1) {
            courierInfo.name = "gdex"
            courierInfo.trackingNo = patternGdexTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchGdex = patternGdex.find(ocrExtractedText)
        val searchGdexTracking1 = patternGdexTracking1.find(ocrExtractedText)

        if (searchGdex || searchGdexTracking1) {
            courierInfo.name = "gdex"
            if (searchGdexTracking1) {
                courierInfo.trackingNo = patternGdexTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}