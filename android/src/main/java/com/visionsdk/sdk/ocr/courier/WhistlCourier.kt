package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class WhistlCourier : Courier() {

    val patternWhistl by lazy {
        VisionRegex("(?i)(\\bwhistl\\b)", RegexType.Default)
    }

    val patternWhistlTracking1 by lazy {
        VisionRegex("(SH|L1)\\d{16}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchWhistlTracking1 = patternWhistlTracking1.find(barcode)

        if (searchWhistlTracking1) {
            courierInfo.name = "whistl"
            courierInfo.trackingNo = patternWhistlTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchWhistl = patternWhistl.find(ocrExtractedText)
        val searchWhistlTracking1 = patternWhistlTracking1.find(ocrExtractedText)

        if (searchWhistl || searchWhistlTracking1) {
            courierInfo.name = "whistl"
            if (searchWhistlTracking1) {
                courierInfo.trackingNo = patternWhistlTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}