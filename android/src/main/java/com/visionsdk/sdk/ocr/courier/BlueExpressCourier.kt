package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BlueExpressCourier : Courier( listOf("blue-express") ) {

    val patternBlueExpress by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*blue[\\n\\s\\.\\,]*express[\\s\\.\\,]*)\\b", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBlueExpress = patternBlueExpress.find(ocrExtractedText)

        if (searchBlueExpress) {
            courierInfo.name = "Blue Express"
        }

        return RegexResult(courierInfo)
    }
}