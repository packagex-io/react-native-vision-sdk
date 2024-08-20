package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class VietnamPostCourier : Courier() {

    val patternVietnamPost by lazy {
        VisionRegex("(?i)([\\s\\.\\,]vietnam[\\s\\.\\,]post[\\s\\.\\,])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchVietnamPost = patternVietnamPost.find(ocrExtractedText)

        if (searchVietnamPost) {
            courierInfo.name = "Vietnam Post"
        }

        return RegexResult(courierInfo)
    }
}