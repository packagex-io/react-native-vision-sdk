package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SendingCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(\\bsending\\b)", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("\\b\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "sending"
        }

        return RegexResult(courier = courierInfo)
    }
}