package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TikiCourier : Courier( listOf("") ) {

    val patternTiki by lazy {
        VisionRegex("(?i)(\\bO?TIKI\\b)", RegexType.Default)
    }

    val patternTikiTracking1 by lazy {
        VisionRegex("6600\\d{8}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTiki = patternTiki.find(ocrExtractedText)
        val searchTikiTracking1 = patternTikiTracking1.find(ocrExtractedText)

        if (searchTiki) {
            courierInfo.name = "tiki"
            if (searchTikiTracking1) {
                courierInfo.trackingNo = patternTikiTracking1.group(ocrExtractedText, 0)
            }
        }

        return RegexResult(courierInfo)
    }
}