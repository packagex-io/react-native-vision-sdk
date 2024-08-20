package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TNTCourier : Courier() {

    val patternTNT by lazy {
        VisionRegex("TNT|tnt", RegexType.Default)
    }

    val patternTNTTracking1 by lazy {
        VisionRegex("(?<!\\d)\\d{30}(?!\\d)", RegexType.TrackingNo)
    }

    val patternTNTTracking2 by lazy {
        VisionRegex("\\b\\d{24}\\b", RegexType.TrackingNo)
    }

    val patternTNTTracking3 by lazy {
        VisionRegex("\\b\\d{9}\\b", RegexType.TrackingNo)
    }

    val patternTNTTracking4 by lazy {
        VisionRegex("(?i)CON NO.? ?\\n+(\\d{8} ?\\d)", RegexType.TrackingNo)
    }

    val patternTNTTracking4b by lazy {
        VisionRegex("(?i)\\bCN: ?(ZF[A-Z]\\d{9})\\b", RegexType.TrackingNo)
    }

    val patternTNTTracking5 by lazy {
        VisionRegex("\\b\\d{5} ?\\d{4}\\b", RegexType.TrackingNo)
    }

    val patternTNTTracking6 by lazy {
        VisionRegex("\\b\\d{5}\\d{4}\\b", RegexType.TrackingNo)
    }

    val patternTNTTracking7 by lazy {
        VisionRegex("\\b[A-Z]{3}\\d{9}\\b", RegexType.TrackingNo)
    }

    val patternTNTTracking8 by lazy {
        VisionRegex("\\bMY\\d{4} \\d001\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTNT = patternTNT.find(ocrExtractedText)
        val searchTNTTracking2 = patternTNTTracking2.find(barcode)
        val searchTNTTracking3 = patternTNTTracking3.find(barcode)

        if (searchTNT) {
            if (searchTNTTracking2) {
                courierInfo.name = "tnt"
                courierInfo.trackingNo = patternTNTTracking2.group(barcode, 0).removeSpaces()
            } else if (searchTNTTracking3) {
                courierInfo.name = "tnt"
                courierInfo.trackingNo = patternTNTTracking3.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTNT = patternTNT.find(ocrExtractedText)
        val searchTNTTracking1 = patternTNTTracking1.find(ocrExtractedText)
        val searchTNTTracking2 = patternTNTTracking2.find(ocrExtractedText)
        val searchTNTTracking4 = patternTNTTracking4.find(ocrExtractedText)
        val searchTNTTracking4b = patternTNTTracking4b.find(ocrExtractedText)
        val searchTNTTracking5 = patternTNTTracking5.find(ocrExtractedText)
        val searchTNTTracking6 = patternTNTTracking6.find(ocrExtractedText)
        val searchTNTTracking7 = patternTNTTracking7.find(ocrExtractedText)
        val searchTNTTracking8 = patternTNTTracking8.find(ocrExtractedText)

        if (searchTNT) {
            courierInfo.name = "tnt"
            if (searchTNTTracking1) {
                courierInfo.trackingNo = patternTNTTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTNTTracking2) {
                courierInfo.trackingNo = patternTNTTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTNTTracking4) {
                courierInfo.trackingNo = patternTNTTracking4.group(ocrExtractedText, 1).removeSpaces()
            } else if (searchTNTTracking4b) {
                courierInfo.trackingNo = patternTNTTracking4b.group(ocrExtractedText, 1).removeSpaces()
            } else if (searchTNTTracking5) {
                courierInfo.trackingNo = patternTNTTracking5.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTNTTracking6) {
                courierInfo.trackingNo = patternTNTTracking6.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTNTTracking7) {
                courierInfo.trackingNo = patternTNTTracking7.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTNTTracking8) {
                courierInfo.trackingNo = patternTNTTracking8.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}