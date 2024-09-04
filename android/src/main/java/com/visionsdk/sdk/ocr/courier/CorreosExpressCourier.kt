package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CorreosExpressCourier : Courier( listOf("correos-express") ) {

    val patternCorreosExpress by lazy {
        VisionRegex("(?i)(\\bCorreos\\b)(?i)(Express)", RegexType.Default)
    }

    val patternCorreosExpress1 by lazy {
        VisionRegex("(?i)(\\bCorreos\\b)\\s+(Express)", RegexType.Default)
    }

    val patternCorreosExpressTracking by lazy {
        VisionRegex("\\b\\d{23}\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCorreosExpressTracking = patternCorreosExpressTracking.find(barcode)

        if (searchCorreosExpressTracking) {
            courierInfo.name = "correos-express"
            courierInfo.trackingNo = patternCorreosExpressTracking.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCorreosExpress = patternCorreosExpress.find(ocrExtractedText)
        val searchCorreosExpress1 = patternCorreosExpress1.find(ocrExtractedText)
        val searchCorreosExpressTracking = patternCorreosExpressTracking.find(ocrExtractedText)

        if (searchCorreosExpress || searchCorreosExpress1) {
            courierInfo.name = "correos-express"
            if (searchCorreosExpressTracking) {
                courierInfo.trackingNo = patternCorreosExpressTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}