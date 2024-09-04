package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CityLinkCourier : Courier( listOf("city-link") ) {

    val patternCityLink by lazy {
        VisionRegex("(?i)(\\bcity-link\\b)", RegexType.Default)
    }

    val patternCityLinkTracking1 by lazy {
        VisionRegex("(0603|8603) ?\\d{2} ?\\d{3} ?\\d{3} ?\\d{3}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCityLinkTracking1 = patternCityLinkTracking1.find(barcode)

        if (searchCityLinkTracking1) {
            courierInfo.name = "city-link"
            courierInfo.trackingNo = patternCityLinkTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCityLink = patternCityLink.find(ocrExtractedText)
        val searchCityLinkTracking1 = patternCityLinkTracking1.find(ocrExtractedText)

        if (searchCityLink || searchCityLinkTracking1) {
            courierInfo.name = "city-link"
            if (searchCityLinkTracking1) {
                courierInfo.trackingNo = patternCityLinkTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}