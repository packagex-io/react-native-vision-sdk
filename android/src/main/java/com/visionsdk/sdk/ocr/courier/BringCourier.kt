package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BringCourier : Courier( listOf("bring") ) {

    val pattern by lazy {
        VisionRegex("\\b([\\s\\.\\,\\r\\n]*bring[\\s\\.\\,\\r\\n]*)\\b", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("(\\(00\\))?37\\d{16}", RegexType.TrackingNo)
    }

    val patternBarcode by lazy {
        VisionRegex("\\(?\\d{2}\\)?(\\s*3707\\s*)\\d{14}", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchBarcode = patternBarcode.find(barcode)

        if (search && searchBarcode) {
            courierInfo.name = "bring"
            courierInfo.trackingNo = patternTracking.group(barcode, 1).removeSpaces()?.filter { it.isDigit() }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "bring"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()?.filter { it.isDigit() }
            }
        }

        return RegexResult(courierInfo)
    }
}