package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BestExpressCourier : Courier() {

    val patternBestExpress by lazy {
        VisionRegex("(?i)(百世快运)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBestExpress = patternBestExpress.find(ocrExtractedText)

        if (searchBestExpress) {
            courierInfo.name = "best-express"
        }

        return RegexResult(courierInfo)
    }
}