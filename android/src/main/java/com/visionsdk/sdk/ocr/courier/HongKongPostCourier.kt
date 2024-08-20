package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class HongKongPostCourier : Courier() {

    val patternHongKongPost by lazy {
        VisionRegex("(?i)(hong[ \\-]?kong Post)", RegexType.Default)
    }

    val patternHongKongOrigin by lazy {
        VisionRegex("(?i)(^|\\b)[A-Z]{2} *( *[\\d]){9} *HK(\$|\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchHongKongOrigin = patternHongKongOrigin.find(barcode)

        if (searchHongKongOrigin) {
            courierInfo.name = "hong-kong-post"
            courierInfo.trackingNo = patternHongKongOrigin.group(barcode, 0).removeSpaces()
            sender.country = "Hong-Kong"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchHongKongPost = patternHongKongPost.find(ocrExtractedText)
        val searchHongKongOrigin = patternHongKongOrigin.find(ocrExtractedText)

        if (searchHongKongPost) {
            courierInfo.name = "hong-kong-post"
        }

        if (searchHongKongOrigin) {
            courierInfo.trackingNo = patternHongKongOrigin.group(ocrExtractedText, 0).removeSpaces()
            sender.country = "Hong-Kong"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }
}