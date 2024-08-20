package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class NewZealandPostCourier : Courier() {

    val patternNewZealandPost by lazy {
        VisionRegex("(?i)(\\bnew zealand post\\b)", RegexType.Default)
    }

    val patternNewZealandPostTracking1 by lazy {
        VisionRegex("[A-Z]{2}[ \\d]{9,13}NZ", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchNewZealandPostTracking1 = patternNewZealandPostTracking1.find(barcode)

        if (searchNewZealandPostTracking1) {
            courierInfo.name = "newzealand-post"
            courierInfo.trackingNo = patternNewZealandPostTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchNewZealandPost = patternNewZealandPost.find(ocrExtractedText)
        val searchNewZealandPostTracking1 = patternNewZealandPostTracking1.find(ocrExtractedText)

        if (searchNewZealandPost || searchNewZealandPostTracking1) {
            courierInfo.name = "newzealand-post"
            if (searchNewZealandPostTracking1) {
                courierInfo.trackingNo = patternNewZealandPostTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}