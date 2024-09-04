package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class DPDCourier : Courier( listOf("dpd") ) {

    val patternDPD by lazy {
        VisionRegex("(?i)(www\\.?dpd\\.?co\\.?uk)|(www\\.?dpd\\.?com)|(\\bdpd\\b)", RegexType.Default)
    }

    val patternDPDTracking1 by lazy {
        VisionRegex("(?i)(^|\\b)([0|1]5\\d{2}\\s*\\d{4}\\s*\\d{4}\\s*\\d{2}\\s*([A-Z]|[0-9]))(\\b|\$)", RegexType.TrackingNo)
    }

    val patternDPDTracking2 by lazy {
        VisionRegex("(?i)(\\b|^)(%[a-z0-9]{7}([0|1]5\\d{13})826)(\\b|\$)", RegexType.TrackingNo)
    }

    val patternDPDTracking3 by lazy {
        VisionRegex("(?i)(^|\\b)(09\\d{2}\\s*\\d{4}\\s*\\d{4}\\s*\\d{2}\\s*([A-Z]|[0-9]))(\\b|\$)", RegexType.TrackingNo)
    }

    val patternDPDTracking4 by lazy {
        VisionRegex("(?i)(\\b|^)(%[a-z0-9]{7}(09\\d{13})826)(\\b|\$)", RegexType.TrackingNo)
    }

    val patternDPDTracking5 by lazy {
        VisionRegex("(?i)\\b%?0{5}\\d{2} (\\d{13}[A-Z]{1}) 101 616 [A-Z]{1}\\b", RegexType.TrackingNo)
    }

    val patternDPDTracking5Barcode by lazy {
        VisionRegex("(?i)\\b%?0{4,5}\\d{2,3}([A-Z0-9]{14})101616\\b", RegexType.Barcode)
    }

    val patternDPDTracking6Barcode by lazy {
        VisionRegex("(?i)(\\d{14})\\d{6}\\b", RegexType.Barcode)
    }

    val patternDPDTracking7 by lazy {
        VisionRegex("(?i)\\d{14}R\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDPD = patternDPD.find(ocrExtractedText)
        val searchDPDTracking1 = patternDPDTracking1.find(barcode)
        val searchDPDTracking2 = patternDPDTracking2.find(barcode)
        val searchDPDTracking4 = patternDPDTracking4.find(barcode)
        val searchDPDTracking5Barcode = patternDPDTracking5Barcode.find(barcode)
        val searchDPDTracking6Barcode = patternDPDTracking6Barcode.find(barcode)
        val searchDPDTracking7 = patternDPDTracking7.find(barcode)

        if (searchDPDTracking1 || searchDPDTracking2 || searchDPDTracking4 || searchDPDTracking5Barcode || (searchDPD && searchDPDTracking6Barcode) || searchDPDTracking7) {
            courierInfo.name = "dpd"
            if (searchDPDTracking2) {
                courierInfo.trackingNo = patternDPDTracking2.group(barcode, 3).removeSpaces()
            } else if (searchDPDTracking4) {
                courierInfo.trackingNo = patternDPDTracking4.group(barcode, 3).removeSpaces()
            } else if (searchDPDTracking1) {
                courierInfo.trackingNo = patternDPDTracking1.group(barcode, 0).removeSpaces()
            } else if (searchDPDTracking5Barcode) {
                courierInfo.trackingNo = patternDPDTracking5Barcode.group(barcode, 1).removeSpaces()
            } else if (searchDPD && searchDPDTracking6Barcode) {
                courierInfo.trackingNo = patternDPDTracking6Barcode.group(barcode, 1).removeSpaces()
            } else if (searchDPDTracking7) {
                courierInfo.trackingNo = patternDPDTracking7.group(barcode, 3).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDPD = patternDPD.find(ocrExtractedText)
        val searchDPDTracking1 = patternDPDTracking1.find(ocrExtractedText)
        val searchDPDTracking3 = patternDPDTracking3.find(ocrExtractedText)
        val searchDPDTracking5 = patternDPDTracking5.find(ocrExtractedText)
        val searchFedEx = FedExCourier().patternFedEx.find(ocrExtractedText)

        if (searchDPD) {
            courierInfo.name = "dpd"
        }

        if (searchDPDTracking1 || searchDPDTracking3 || searchDPDTracking5) {
            if (searchDPDTracking1) {
                courierInfo.trackingNo = patternDPDTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchDPDTracking3) {
                courierInfo.trackingNo = patternDPDTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchDPDTracking5) {
                courierInfo.trackingNo = patternDPDTracking5.group(ocrExtractedText, 1).removeSpaces()
            }
            if (searchFedEx.not()) {
                courierInfo.name = "dpd"
            }
        }

        return RegexResult(courierInfo)
    }
}