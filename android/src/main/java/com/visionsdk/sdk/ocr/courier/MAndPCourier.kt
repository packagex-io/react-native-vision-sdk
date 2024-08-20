package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class MAndPCourier : Courier() {

    val pattern by lazy {
        VisionRegex("[mM]&[pP]", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("(15100|12100|32100)[\\d]{7}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "m&p"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (searchTracking1) {
            courierInfo.name = "m&p"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        if (search) {
            courierInfo.name = "m&p"
        }

        return RegexResult(courier = courierInfo)
    }
}