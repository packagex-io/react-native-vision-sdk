package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class WishPostCourier : Courier( listOf("") ) {

    val patternOnTrac by lazy {
        VisionRegex("(?i)(\\bwish post\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOnTrac = patternOnTrac.find(ocrExtractedText)

        if (searchOnTrac) {
            courierInfo.name = "wish-post"
        }

        return RegexResult(courierInfo)
    }
}