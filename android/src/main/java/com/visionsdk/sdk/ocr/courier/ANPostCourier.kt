package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ANPostCourier : Courier( listOf("an-post") ) {

    val patternANPost by lazy {
        VisionRegex("(\\ban[\\s\\\\n]post|www.anpost.com\\b)", RegexType.Default)
    }

    val patternANPostTracking1 by lazy {
        VisionRegex("LP\\d{9}GB", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchANPost = patternANPost.find(ocrExtractedText)
        val searchANPostTracking1 = patternANPostTracking1.find(ocrExtractedText)

        if (searchANPost) {
            courierInfo.name = "an-post"
            if (searchANPostTracking1) {
                courierInfo.trackingNo = patternANPostTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}