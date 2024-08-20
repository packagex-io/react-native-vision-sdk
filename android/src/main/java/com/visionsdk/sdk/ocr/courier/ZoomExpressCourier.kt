package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ZoomExpressCourier : Courier() {

    val patternZoomExpress by lazy {
        VisionRegex("(?i)([\\s\\.\\,]Zoom[\\s\\.\\,]Express[\\s\\.\\,])", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchZoomExpress = patternZoomExpress.find(ocrExtractedText)

        if (searchZoomExpress) {
            courierInfo.name = "zoom-express"
        }

        return RegexResult(courierInfo)
    }
}