package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CouriersPleaseCourier : Courier() {

    val pattern by lazy {
        VisionRegex("[cC]ouriersplease", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("CPAFXLT15[0-9]{6}(?!\\d)|CPW[A-Z0-9]{14}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "couriers-please"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (searchTracking1) {
            courierInfo.name = "couriers-please"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        if (search) {
            courierInfo.name = "couriers-please"
        }

        return RegexResult(courier = courierInfo)
    }
}