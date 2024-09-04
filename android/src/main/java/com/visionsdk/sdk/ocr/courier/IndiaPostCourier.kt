package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class IndiaPostCourier : Courier( listOf("") ) {

    val patternIndiaPost by lazy {
        VisionRegex("(?i)(India[ \\-]?Post)", RegexType.Default)
    }

    val patternIndiaOrigin by lazy {
        VisionRegex("(?i)(^|\\b)[A-Z]{2} *( *[\\d]){9} *IN(\$|\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchIndiaOrigin = patternIndiaOrigin.find(barcode)

        if (searchIndiaOrigin) {
            courierInfo.name = "india-post"
            courierInfo.trackingNo = patternIndiaOrigin.group(barcode, 0).removeSpaces()
            sender.country = "India"
        }

        return RegexResult(courier = courierInfo, sender = sender)

    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchIndiaPost = patternIndiaPost.find(ocrExtractedText)
        val searchIndiaOrigin = patternIndiaOrigin.find(ocrExtractedText)

        if (searchIndiaPost) {
            courierInfo.name = "india-post"
        }

        if (searchIndiaOrigin) {
            courierInfo.trackingNo = patternIndiaOrigin.group(ocrExtractedText, 0).removeSpaces()
            sender.country = "India"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }
}