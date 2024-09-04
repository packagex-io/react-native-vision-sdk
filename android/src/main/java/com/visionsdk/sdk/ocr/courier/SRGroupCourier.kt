package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SRGroupCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,\\r\\n]*SR([\\s\\.\\,\\r\\n]?)Group[\\s\\.\\,\\r\\n]*)\\b", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("(\\(00\\))?37\\d{16}", RegexType.TrackingNo)
    }

    val patternBarcode by lazy {
        VisionRegex("\\(?\\d{2}\\)?(\\s*3707\\s*)\\d{14}", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOtherNorway = patternOtherNorway.find(barcode)
        val search = pattern.find(ocrExtractedText)

        if (search && searchOtherNorway) {
            courierInfo.trackingNo = patternOtherNorway.group(barcode, 1).removeSpaces()?.filter { it.isDigit() }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "sr-group"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()?.filter { it.isDigit() }
            }
        }

        return RegexResult(courierInfo)
    }
}