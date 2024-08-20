package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class RhymeExpressCourier : Courier() {

    val patternRhymeExpress by lazy {
        VisionRegex("(?i)(韵达)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchRhymeExpress = patternRhymeExpress.find(ocrExtractedText)

        if (searchRhymeExpress) {
            courierInfo.name = "rhyme-express"
        }

        return RegexResult(courierInfo)
    }
}