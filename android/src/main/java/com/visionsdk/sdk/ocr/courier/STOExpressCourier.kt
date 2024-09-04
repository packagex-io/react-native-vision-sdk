package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class STOExpressCourier : Courier( listOf("") ) {

    val pattern by lazy { VisionRegex("(?i)(\\bsto[\\s]*express\\b|申通快递|www.sto|sto.cn|www.sto.cn|95543)", RegexType.Default) }
    val patternTracking1 by lazy { VisionRegex("77\\d{13}(?!\\d)", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val pattern = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(barcode)

        if (pattern && searchTracking1) {
            courierInfo.name = "sto-express"
            courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val pattern = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (pattern) {
            courierInfo.name = "sto-express"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}