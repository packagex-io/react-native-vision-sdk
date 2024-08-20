package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import com.asadullah.handyutils.toLettersOrDigits
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class KeeneyCourier : Courier() {

    val pattern by lazy {
        VisionRegex("[Kk](eeney|EENEY)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("[Kk]001[\\d]{4}(?!\\d)(-1)?", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "keeney"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).toLettersOrDigits().removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (searchTracking1) {
            courierInfo.name = "keeney"
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).toLettersOrDigits().removeSpaces()
        }

        if (search) {
            courierInfo.name = "keeney"
        }

        return RegexResult(courier = courierInfo)
    }
}