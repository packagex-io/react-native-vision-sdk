package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ShopeeXpressCourier : Courier( listOf("") ) {

    val patternShopeeXpress by lazy {
        VisionRegex("(?i)([\\s\\.\\,]shopee[\\s\\.\\,]express[\\s\\.\\,])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchShopeeXpress = patternShopeeXpress.find(ocrExtractedText)

        if (searchShopeeXpress) {
            courierInfo.name = "shopee-xpress"
        }

        return RegexResult(courierInfo)
    }
}