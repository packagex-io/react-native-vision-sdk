package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class FukuyamaTransportationCourier : Courier( listOf("") ) {

    val patternFukuyamaTransportation by lazy {
        VisionRegex("(?i)(福山通運)|(www.fukutsu.co.jp)", RegexType.Default)
    }

    val patternFukuyamaTransportationTracking1 by lazy {
        VisionRegex("[aA][\\d ]{13,27}[aA]", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchFukuyamaTransportationTracking1 = patternFukuyamaTransportationTracking1.find(barcode)

        if (searchFukuyamaTransportationTracking1) {
            courierInfo.name = "fukuyama-transportation"
            courierInfo.trackingNo = patternFukuyamaTransportationTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchFukuyamaTransportation = patternFukuyamaTransportation.find(ocrExtractedText)
        val searchFukuyamaTransportationTracking1 = patternFukuyamaTransportationTracking1.find(ocrExtractedText)

        if (searchFukuyamaTransportation) {
            courierInfo.name = "fukuyama-transportation"
            if (searchFukuyamaTransportationTracking1) {
                courierInfo.trackingNo = patternFukuyamaTransportationTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}