package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class PosLajuCourier : Courier( listOf("") ) {

    val patternPosLaju by lazy {
        VisionRegex("(?i)(\\bP[a-z]S laju\\b)", RegexType.Default)
    }

    val patternPosLajuTracking1 by lazy {
        VisionRegex("E[A-Z]{2}\\d{9}MY", RegexType.TrackingNo)
    }

    val patternPosLajuTracking2 by lazy {
        VisionRegex("PL\\d{12}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchPosLajuTracking1 = patternPosLajuTracking1.find(barcode)
        val searchPosLajuTracking2 = patternPosLajuTracking2.find(barcode)

        if (searchPosLajuTracking1 || searchPosLajuTracking2) {
            courierInfo.name = "pos-laju"
            if (searchPosLajuTracking1) {
                courierInfo.trackingNo = patternPosLajuTracking1.group(barcode, 0).removeSpaces()
            } else if (searchPosLajuTracking2) {
                courierInfo.trackingNo = patternPosLajuTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchPosLaju = patternPosLaju.find(ocrExtractedText)
        val searchPosLajuTracking1 = patternPosLajuTracking1.find(ocrExtractedText)
        val searchPosLajuTracking2 = patternPosLajuTracking2.find(ocrExtractedText)

        if (searchPosLaju || searchPosLajuTracking1 || searchPosLajuTracking2) {
            courierInfo.name = "pos-laju"
            if (searchPosLajuTracking1) {
                courierInfo.trackingNo = patternPosLajuTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchPosLajuTracking2) {
                courierInfo.trackingNo = patternPosLajuTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}