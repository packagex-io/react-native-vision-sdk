package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class NinjaVanCourier : Courier( listOf("") ) {

    val patternNinjaVan by lazy {
        VisionRegex("(?i)(\\bninja van\\b)", RegexType.Default)
    }

    val patternNinjaVanTracking1 by lazy {
        VisionRegex("[A-Z]{9}\\d{9}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchNinjaVan = patternNinjaVan.find(ocrExtractedText)
        val searchNinjaVanTracking1 = patternNinjaVanTracking1.find(ocrExtractedText)

        if (searchNinjaVan) {
            courierInfo.name = "ninja-van"
            if (searchNinjaVanTracking1) {
                courierInfo.trackingNo = patternNinjaVanTracking1.group(ocrExtractedText, 0)
            }
        }

        return RegexResult(courierInfo)
    }
}