package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class HermesCourier : Courier( listOf("") ) {

    val patternHermes by lazy {
        VisionRegex("(?i)\\b(HERMES?)\\b", RegexType.Default)
    }

    val patternHermesTracking1 by lazy {
        VisionRegex("[A-Z]-[A-Z0-9]{4}-[A-Z0-9]-[0-9]{9}-[0-9]", RegexType.TrackingNo)
    }

    val patternHermesTracking2 by lazy {
        VisionRegex("H10\\d{17}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchHermesTracking1 = patternHermesTracking1.find(barcode)
        val searchHermesTracking2 = patternHermesTracking2.find(barcode)

        if (searchHermesTracking1) {
            courierInfo.trackingNo = patternHermesTracking1.group(barcode, 0).removeSpaces()
        } else if (searchHermesTracking2) {
            courierInfo.trackingNo = patternHermesTracking2.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchHermes = patternHermes.find(ocrExtractedText)
        val searchHermesTracking1 = patternHermesTracking1.find(ocrExtractedText)
        val searchHermesTracking2 = patternHermesTracking2.find(ocrExtractedText)

        if (searchHermes) {
            courierInfo.name = "hermes"
            if (searchHermesTracking1) {
                courierInfo.trackingNo = patternHermesTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchHermesTracking2) {
                courierInfo.trackingNo = patternHermesTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}