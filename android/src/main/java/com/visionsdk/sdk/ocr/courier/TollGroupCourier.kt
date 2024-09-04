package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class TollGroupCourier : Courier( listOf("") ) {

    val patternTollGroup: VisionRegex by lazy { VisionRegex("(?i)(www\\.tollgroup\\.com)", RegexType.TrackingNo) }
    val patternTollGroupTracking1: VisionRegex by lazy { VisionRegex("T\\d{6}[A-Z]{4}\\d{10}", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTollGroupTracking1 = patternTollGroupTracking1.find(barcode)

        if (searchTollGroupTracking1) {
            courierInfo.name = "toll-group"
            courierInfo.trackingNo = patternTollGroupTracking1.group(barcode, 0)
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTollGroup = patternTollGroup.find(ocrExtractedText)
        val searchTollGroupTracking1 = patternTollGroupTracking1.find(ocrExtractedText)

        if (searchTollGroup) {
            courierInfo.name = "toll-group"
            if (searchTollGroupTracking1) {
                courierInfo.trackingNo = patternTollGroupTracking1.group(ocrExtractedText, 0)
            }
        }

        return RegexResult(courierInfo)
    }
}