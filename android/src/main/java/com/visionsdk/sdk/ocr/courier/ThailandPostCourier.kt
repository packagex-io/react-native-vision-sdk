package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ThailandPostCourier : Courier() {

    val patternThailandPost by lazy {
        VisionRegex("(?i)([\\s\\.\\,]thailand[\\s\\.\\,]post[\\s\\.\\,])", RegexType.Default)
    }

    val patternThailandPostTracking1 by lazy {
        VisionRegex("[A-Z]{2}[\\d ]{9,13}[A-Z]{2}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchThailandPost = patternThailandPost.find(ocrExtractedText)
        val searchThailandPostTracking1 = patternThailandPostTracking1.find(ocrExtractedText)

        if (searchThailandPost) {
            courierInfo.name = "thailand-post"
            if (searchThailandPostTracking1) {
                courierInfo.trackingNo = patternThailandPostTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}