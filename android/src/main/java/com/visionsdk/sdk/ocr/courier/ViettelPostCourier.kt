package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ViettelPostCourier : Courier() {

    val patternViettelPost by lazy {
        VisionRegex("(?i)([www\\s\\.\\,]viettel[\\n\\s\\.\\,]post[\\s\\.\\,comvn])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchViettelPost = patternViettelPost.find(ocrExtractedText)

        if (searchViettelPost) {
            courierInfo.name = "viettel-post"
        }

        return RegexResult(courierInfo)
    }
}