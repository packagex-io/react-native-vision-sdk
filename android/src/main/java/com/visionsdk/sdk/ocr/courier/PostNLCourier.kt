package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class PostNLCourier : Courier() {

    val patternPostNL by lazy {
        VisionRegex("(?i)\\b(postnl)\\b", RegexType.Default)
    }

    val patternPostNLTracking1 by lazy {
        VisionRegex("3S[A-Z]{4}\\d{7,9}(?!\\d)", RegexType.TrackingNo)
    }

    val patternPostNLTracking2 by lazy {
        VisionRegex("(LS|CX)\\d{7,9}NL", RegexType.TrackingNo)
    }

    val patternPostNLTracking3 by lazy {
        VisionRegex("(?i)L[A-Z]{1}\\d{9}NL", RegexType.TrackingNo)
    }

    val patternPostNLTracking4 by lazy {
        VisionRegex("(?i)U[A-Z]{1} \\d{3} \\d{3} \\d{3} NL", RegexType.TrackingNo)
    }

    val patternPostNLTracking4Barcoding by lazy {
        VisionRegex("(?i)U[A-Z]{1}\\d{9}NL", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchPostNLTracking1 = patternPostNLTracking1.find(barcode)
        val searchPostNLTracking2 = patternPostNLTracking2.find(barcode)
        val searchPostNLTracking3 = patternPostNLTracking3.find(barcode)
        val searchPostNLTracking4Barcode = patternPostNLTracking4Barcoding.find(barcode)

        if (searchPostNLTracking1 || searchPostNLTracking2 || searchPostNLTracking3 || searchPostNLTracking4Barcode) {
            courierInfo.name = "postnl"
            if (searchPostNLTracking1) {
                courierInfo.trackingNo = patternPostNLTracking1.group(barcode, 0).removeSpaces()
            } else if (searchPostNLTracking2) {
                courierInfo.trackingNo = patternPostNLTracking2.group(barcode, 0).removeSpaces()
            } else if (searchPostNLTracking3) {
                courierInfo.trackingNo = patternPostNLTracking3.group(barcode, 0).removeSpaces()
            } else if (searchPostNLTracking4Barcode) {
                courierInfo.trackingNo = patternPostNLTracking4Barcoding.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchPostNL = patternPostNL.find(ocrExtractedText)
        val searchPostNLTracking1 = patternPostNLTracking1.find(ocrExtractedText)
        val searchPostNLTracking2 = patternPostNLTracking2.find(ocrExtractedText)
        val searchPostNLTracking3 = patternPostNLTracking3.find(ocrExtractedText)
        val searchPostNLTracking4 = patternPostNLTracking4.find(ocrExtractedText)

        if (searchPostNL || searchPostNLTracking1 || searchPostNLTracking2 || searchPostNLTracking3 || searchPostNLTracking4) {
            courierInfo.name = "postnl"
            if (searchPostNLTracking1) {
                courierInfo.trackingNo = patternPostNLTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchPostNLTracking2) {
                courierInfo.trackingNo = patternPostNLTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchPostNLTracking3) {
                courierInfo.trackingNo = patternPostNLTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchPostNLTracking4) {
                courierInfo.trackingNo = patternPostNLTracking4Barcoding.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}