package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BPostCourier : Courier( listOf("bpost") ) {

    val patternBPost by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*bpost[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    val patternBPostTracking1 by lazy {
        VisionRegex("3232{20}(?!\\d)", RegexType.TrackingNo)
    }

    val patternBPostTracking2 by lazy {
        VisionRegex("(?i)\\b[A-Z]{2}\\d{9}BE\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBPostTracking1 = patternBPostTracking1.find(barcode)
        val searchBPostTracking2 = patternBPostTracking2.find(barcode)

        if (searchBPostTracking1 || searchBPostTracking2) {
            courierInfo.name = "bpost"
            if (searchBPostTracking1) {
                courierInfo.trackingNo = patternBPostTracking1.group(barcode, 0).removeSpaces()
            } else if (searchBPostTracking2) {
                courierInfo.trackingNo = patternBPostTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBPost = patternBPost.find(ocrExtractedText)
        val searchBPostTracking1 = patternBPostTracking1.find(ocrExtractedText)
        val searchBPostTracking2 = patternBPostTracking2.find(ocrExtractedText)

        if (searchBPost) {
            courierInfo.name = "bpost"
            if (searchBPostTracking1) {
                courierInfo.trackingNo = patternBPostTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchBPostTracking2) {
                courierInfo.trackingNo = patternBPostTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}