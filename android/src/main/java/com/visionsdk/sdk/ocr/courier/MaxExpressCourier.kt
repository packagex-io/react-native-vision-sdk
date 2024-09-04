package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class MaxExpressCourier : Courier( listOf("") ) {

    val patternMaxExpress by lazy {
        VisionRegex("max express| MAX EXPRESS", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchJapanPost = patternMaxExpress.find(ocrExtractedText)

        if (searchJapanPost) {
            courierInfo.name = "maxx"
        }

        return RegexResult(courierInfo)
    }
}