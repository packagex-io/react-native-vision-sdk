package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class StaplesCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\b(STAPLES)\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(?i)\\b(TRACKING ?[:#]{0,2}?)? ?[A-Z]{1}[0-9]{1} ?[0-9]{3} ?[0-9]{3} ?[0-9]{2} ?[0-9]{4} [0-9]{1}\\b", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("\\b0{4,7}68(6024|3182)\\d{7,10}\\b", RegexType.TrackingNo)
    }

    val patternTracking2Ocr by lazy {
        VisionRegex("\\(00\\) 0 0686024 \\d{9} \\d", RegexType.TrackingNo)
    }

    val patternTracking3 by lazy {
        VisionRegex("(?i)\\b0{4,7}\\d{13,16}\\b", RegexType.TrackingNo)
    }

    val patternTracking4 by lazy {
        VisionRegex("\\b0{4,7}68318\\d{8,11}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)
        val searchTracking3 = patternTracking3.find(barcode)
        val searchTracking4 = patternTracking4.find(barcode)

        val searchFedEx = FedExCourier().patternFedEx.find(ocrExtractedText)
        val searchUPSTracking1 = UPSCourier().patternUPSTracking1.find(barcode)

        if (searchFedEx) {
            return RegexResult()
        }

        if (searchTracking1) {
            courierInfo.name = "staples"
            val trackingNo = patternTracking1.group(barcode, 0).removeSpaces()?.lowercase()
            courierInfo.trackingNo = if (trackingNo?.contains("tracking") == true) {
                trackingNo.split("tracking")[1]
            } else {
                trackingNo
            }?.uppercase()
        } else if (searchTracking2) {
            courierInfo.name = "staples"
            courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
        } else if (searchTracking3 && search) {
            courierInfo.name = "staples"
            courierInfo.trackingNo = patternTracking3.group(barcode, 0).removeSpaces()
        } else if (searchTracking4) {
            if (searchUPSTracking1) {
                courierInfo.name = "ups"
                courierInfo.trackingNo = UPSCourier().patternUPSTracking1.group(barcode, 0)
            } else {
                courierInfo.name = "staples"
                courierInfo.trackingNo = patternTracking4.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchTracking2Ocr = patternTracking2Ocr.find(ocrExtractedText)
        val searchTracking3 = patternTracking3.find(ocrExtractedText)

        if (search || searchTracking1 || searchTracking2 || searchTracking2Ocr || searchTracking3) {
            courierInfo.name = "staples"
            if (searchTracking1) {
                val trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()?.lowercase()
                courierInfo.trackingNo = if (trackingNo?.contains("tracking") == true) {
                    trackingNo.split("tracking")[1]
                } else {
                    trackingNo
                }?.uppercase()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2Ocr) {
                courierInfo.trackingNo = patternTracking2Ocr.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking3) {
                courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}