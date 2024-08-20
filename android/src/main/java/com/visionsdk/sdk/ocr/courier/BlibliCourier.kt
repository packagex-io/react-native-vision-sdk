package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class BlibliCourier : Courier() {

    val patternBlibli by lazy {
        VisionRegex("(?i)(\\bblibli\\.?com\\b)", RegexType.Default)
    }

    val patternBlibliTracking1 by lazy {
        VisionRegex("Airway Bill: ?[A-Z\\d]{12,16}", RegexType.Default)
    }

    val patternBlibliTracking1Clean by lazy {
        VisionRegex("[A-Z\\d]{12,16}", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBlibli = patternBlibli.find(ocrExtractedText)
        val searchBlibliTracking1 = patternBlibliTracking1.find(ocrExtractedText)
        val searchBlibliTracking1Clean = patternBlibliTracking1Clean.find(ocrExtractedText)

        if (searchBlibli) {
            courierInfo.name = "blibli"
            if (searchBlibliTracking1 && searchBlibliTracking1Clean) {
                courierInfo.trackingNo = patternBlibliTracking1Clean.group(ocrExtractedText, 0)
            }
        }

        return RegexResult(courierInfo)
    }
}