package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class QuillDotComCourier : Courier( listOf("") ) {

    val pattern by lazy { VisionRegex("(?i)\\bQuill.com\\b", RegexType.Default) }
    val patternTracking by lazy { VisionRegex("(?i)\\b\\d{16}001\\b", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val searchStaples = StaplesCourier().pattern.find(ocrExtractedText)
        if (searchStaples) {
            return RegexResult()
        }

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking = patternTracking.find(barcode)

        if (searchTracking) {
            courierInfo.name = "Quill"
            courierInfo.trackingNo = patternTracking.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val searchStaples = StaplesCourier().pattern.find(ocrExtractedText)
        if (searchStaples) {
            return RegexResult()
        }

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Quill"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}