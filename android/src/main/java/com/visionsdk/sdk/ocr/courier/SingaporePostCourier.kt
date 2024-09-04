package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SingaporePostCourier : Courier( listOf("") ) {

    val patternSingaporePost by lazy {
        VisionRegex("(?i)(Singapore[ \\-]?Post)", RegexType.Default)
    }

    val patternSingaporePostOrigin by lazy {
        VisionRegex("(?i)(^|\\b)[A-Z]{2} *( *[\\d]){9} *SG(\$|\\b)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchSingaporePostOrigin = patternSingaporePostOrigin.find(barcode)

        if (searchSingaporePostOrigin) {
            courierInfo.name = "singapore-post"
            courierInfo.trackingNo = patternSingaporePostOrigin.group(barcode, 0).removeSpaces()
            sender.country = "Singapore"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchSingaporePost = patternSingaporePost.find(ocrExtractedText)
        val searchSingaporePostOrigin = patternSingaporePostOrigin.find(ocrExtractedText)

        if (searchSingaporePost) {
            courierInfo.name = "singapore-post"
        }

        if (searchSingaporePostOrigin) {
            courierInfo.trackingNo = patternSingaporePostOrigin.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }
}