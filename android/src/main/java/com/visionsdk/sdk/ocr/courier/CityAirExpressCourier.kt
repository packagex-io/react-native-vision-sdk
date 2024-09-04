package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CityAirExpressCourier : Courier( listOf("city-air-express") ) {

    val patternCityAirExpress by lazy {
        VisionRegex("(?i)(\\bCITY AIR EXPRESS\\b)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchOnTrac = patternCityAirExpress.find(ocrExtractedText)

        if (searchOnTrac) {
            courierInfo.name = "city-air-express"
        }

        return RegexResult(courierInfo)
    }
}