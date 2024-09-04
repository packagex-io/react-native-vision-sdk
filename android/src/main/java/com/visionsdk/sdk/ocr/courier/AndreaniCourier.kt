package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class AndreaniCourier : Courier( listOf("andreani") ) {

    val patternAndreani by lazy {
        VisionRegex("(?i)(\\bANDREANI\\b)", RegexType.Default)
    }

    val patternAndreaniTracking1 by lazy {
        VisionRegex("3\\d{14}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchAndreani = patternAndreani.find(ocrExtractedText)
        val searchAndreaniTracking1 = patternAndreaniTracking1.find(ocrExtractedText)

        if (searchAndreani) {
            courierInfo.name = "andreani"
            if (searchAndreaniTracking1) {
                courierInfo.trackingNo = patternAndreaniTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}