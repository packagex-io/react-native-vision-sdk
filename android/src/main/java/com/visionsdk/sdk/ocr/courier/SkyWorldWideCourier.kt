package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SkyWorldWideCourier : Courier() {

    val patternSkyWorldWide by lazy {
        VisionRegex("(?i)(www\\.skynetworldwide\\.com)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOnTrac = patternSkyWorldWide.find(ocrExtractedText)

        if (searchOnTrac) {
            courierInfo.name = "skyworldwide"
        }

        return RegexResult(courierInfo)
    }
}