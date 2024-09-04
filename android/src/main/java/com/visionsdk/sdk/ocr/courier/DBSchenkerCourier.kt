package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class DBSchenkerCourier : Courier( listOf("db-schenker") ) {

    val pattern by lazy {
        VisionRegex("(?i)(?<!C\\/O\\s)DB ?SCHENKER", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("(\\(00\\)[\\s\\.\\,]*)?3707\\d{14}", RegexType.Default)
    }

    val patternBarcode1 by lazy {
        VisionRegex("00373316833002\\d{6}\\b", RegexType.Barcode)
    }

    val patternBarcode2 by lazy {
        VisionRegex("\\(00\\)[\\s\\.\\,]*373316833002\\d{6}", RegexType.Barcode)
    }

    val patternBarcode3 by lazy {
        VisionRegex("401733\\d{14}\\b", RegexType.Barcode)
    }

    val patternBarcode4 by lazy {
        VisionRegex("\\(401\\)733\\d{14}", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBarcode1 = patternBarcode1.find(barcode)
        val searchBarcode2 = patternBarcode2.find(barcode)
        val searchBarcode3 = patternBarcode3.find(barcode)
        val searchBarcode4 = patternBarcode4.find(barcode)

        if (searchBarcode1 || searchBarcode2 || searchBarcode3 || searchBarcode4) {
            courierInfo.name = "db-schenker"

            if (searchBarcode1) {
                courierInfo.trackingNo = patternBarcode1.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "db-schenker"

            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}