package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class JDCourier : Courier( listOf("") ) {

    val patternJDTracking1 by lazy {
        VisionRegex("^(\\d{11})\\-\\d{,2}\\-\\d{,2}\\-\\d{,2}\$", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchJDTracking1 = patternJDTracking1.find(barcode)

        if (searchJDTracking1) {
            courierInfo.name = "jd"
            courierInfo.trackingNo = patternJDTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }
}