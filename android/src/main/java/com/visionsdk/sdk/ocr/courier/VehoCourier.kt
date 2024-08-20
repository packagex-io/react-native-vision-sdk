package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class VehoCourier : Courier() {

    val pattern by lazy {
        VisionRegex("\\bveho\\b", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("[\\da-zA-Z]{15,17}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "veho"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}