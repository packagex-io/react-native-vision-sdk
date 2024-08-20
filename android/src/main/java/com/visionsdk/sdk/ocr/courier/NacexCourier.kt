package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class NacexCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(\\bnacex\\b)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("\\b(?i)NX\\d{2} ?(?i)ES\\d{7}\\b", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("\\b84100\\d{11}\\b", RegexType.TrackingNo)
    }

    val patternTracking3Barcode by lazy {
        VisionRegex("\\b001(\\d{11})\\d{6}\\b", RegexType.Barcode)
    }

    val patternTracking3 by lazy {
        VisionRegex("(?i)EXP ?:? ?\\d{4}\\/\\d{7}\\b", RegexType.TrackingNo)
    }

    val patternTracking4 by lazy {
        VisionRegex("(?i)841001\\d{10}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTnt = TNTCourier().patternTNT.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)
        val searchTracking3Barcode = patternTracking3Barcode.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "nacex"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
        } else if (searchTracking2) {
            courierInfo.name = "nacex"
            courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
        } else if (searchTracking3Barcode && searchTnt.not()) {
            courierInfo.name = "nacex"
            courierInfo.trackingNo = patternTracking3Barcode.group(barcode, 1).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchTracking3 = patternTracking3.find(ocrExtractedText)
        val searchTracking4 = patternTracking4.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "nacex"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking3) {
                courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 1).removeSpaces()
            } else if (searchTracking4) {
                courierInfo.trackingNo = patternTracking4.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}