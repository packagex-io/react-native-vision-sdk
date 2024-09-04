package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class EnvialiaCourier : Courier( listOf("") ) {

    val pattern by lazy {
        VisionRegex("(?i)((\\benvialia\\b)|(\\.envialia\\.))", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("00\\d{14}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "envialia"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}