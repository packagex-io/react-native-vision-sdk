package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CollectPlusCourier : Courier( listOf("collect-plus") ) {

    val patternCollectPlus by lazy {
        VisionRegex("\\b(www\\.collectplus\\.co\\.uk/our-services)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCollectPlus = patternCollectPlus.find(ocrExtractedText)

        if (searchCollectPlus) {
            courierInfo.name = "collect-plus"
        }

        return RegexResult(courierInfo)
    }
}