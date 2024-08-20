package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class WebpackCourier : Courier() {

    val patternWebpack by lazy {
        VisionRegex("(?i)(\\bwebpack\\b)", RegexType.Default)
    }

    val patternWebpackTracking1 by lazy {
        VisionRegex("ML41\\d{9}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchWebpack = patternWebpack.find(ocrExtractedText)
        val searchWebpackTracking1 = patternWebpackTracking1.find(ocrExtractedText)

        if (searchWebpack) {
            courierInfo.name = "webpack"
            if (searchWebpackTracking1) {
                courierInfo.trackingNo = patternWebpackTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}