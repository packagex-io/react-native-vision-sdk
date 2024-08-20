package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class JapanPostCourier : Courier() {

    val patternJapanPost by lazy {
        VisionRegex("(?i)(JAPAN[ \\-]?Post|日本郵便)", RegexType.Default)
    }

    val patternJapanOrigin by lazy {
        VisionRegex("(?i)(^|\\b)[A-Z]{2} *( *[\\d]){9} *JP(\$|\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchJapanOrigin = patternJapanOrigin.find(barcode)

        if (searchJapanOrigin) {
            courierInfo.name = "japan-post"
            courierInfo.trackingNo = patternJapanOrigin.group(barcode, 0).removeSpaces()
            sender.country = "Japan"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchJapanPost = patternJapanPost.find(ocrExtractedText)
        val searchJapanOrigin = patternJapanOrigin.find(ocrExtractedText)

        if (searchJapanPost) {
            courierInfo.name = "japan-post"
        }

        if (searchJapanOrigin) {
            courierInfo.trackingNo = patternJapanOrigin.group(ocrExtractedText, 0).removeSpaces()
            sender.country = "Japan"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }
}