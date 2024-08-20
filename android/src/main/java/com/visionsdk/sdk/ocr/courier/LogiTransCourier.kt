package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LogiTransCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)[\\s\\.\\,]*LogiTrans[\\s\\.\\,]*|[\\s\\.\\,]*Logi[\\s\\.\\,\\-\\_]*Trans[\\s\\.\\,]*", RegexType.Default)
    }

    val patternBarcode1 by lazy {
        VisionRegex("\\b40170726205192\\d{6}\\b|40170726205192\\d{6}", RegexType.Barcode)
    }

    val patternBarcode2 by lazy {
        VisionRegex("\\b401707\\d{14}\\b|401707\\d{14}", RegexType.Barcode)
    }

    val patternBarcode3 by lazy {
        VisionRegex("\\b40170703\\d{12}\\b|40170703\\d{12}", RegexType.Barcode)
    }

    val patternBarcode4 by lazy {
        VisionRegex("\\(401\\)70703\\d{12}", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchBarcode1 = patternBarcode1.find(barcode)
        val searchBarcode2 = patternBarcode2.find(barcode)
        val searchBarcode3 = patternBarcode3.find(barcode)

        if (searchBarcode1 || searchBarcode2 || searchBarcode3) {
            courierInfo.name = "Logi Trans"

            if (searchBarcode1) {
                courierInfo.trackingNo = patternBarcode1.group(barcode, 0).removeSpaces()
            } else if (searchBarcode2) {
                courierInfo.trackingNo = patternBarcode2.group(barcode, 0).removeSpaces()
            } else if (searchBarcode3) {
                courierInfo.trackingNo = patternBarcode3.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchBarcode1 = patternBarcode1.find(ocrExtractedText)
        val searchBarcode2 = patternBarcode2.find(ocrExtractedText)
        val searchBarcode3 = patternBarcode3.find(ocrExtractedText)
        val searchBarcode4 = patternBarcode4.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Logi Trans"

            if (searchBarcode1) {
                courierInfo.trackingNo = patternBarcode1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchBarcode2) {
                courierInfo.trackingNo = patternBarcode2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchBarcode3) {
                courierInfo.trackingNo = patternBarcode3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchBarcode4) {
                courierInfo.trackingNo = patternBarcode4.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}