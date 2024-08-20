package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TransworldCourier : Courier() {

    val patternTransworld by lazy {
        VisionRegex("(?i)(\\btransworld\\b)", RegexType.Default)
    }

    val patternTransworldTracking1 by lazy {
        VisionRegex("\\b10\\d{6}(?!\\d)\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTransworldTracking1 = patternTransworldTracking1.find(barcode)

        if (searchTransworldTracking1) {
            courierInfo.name = "transworld"
            courierInfo.trackingNo = patternTransworldTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTransworld = patternTransworld.find(ocrExtractedText)
        val searchTransworldTracking1 = patternTransworldTracking1.find(ocrExtractedText)

        if (searchTransworld) {
            courierInfo.name = "transworld"
            if (searchTransworldTracking1) {
                courierInfo.trackingNo = patternTransworldTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}