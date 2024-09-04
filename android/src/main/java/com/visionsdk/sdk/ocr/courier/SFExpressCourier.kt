package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SFExpressCourier : Courier( listOf("") ) {

    val patternSFExpress by lazy {
        VisionRegex("(sf-express)|(SF (EXPRESS)?)", RegexType.Default)
    }

    val patternSFExpressTracking1 by lazy {
        VisionRegex("SF(?<!\\d\\s)[\\d\\s]{17}(?!\\d\\s)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchSFExpressTracking1 = patternSFExpressTracking1.find(barcode)

        if (searchSFExpressTracking1) {
            courierInfo.name = "sf-express"
            courierInfo.trackingNo = patternSFExpressTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchSFExpress = patternSFExpress.find(ocrExtractedText)
        val searchSFExpressTracking1 = patternSFExpressTracking1.find(ocrExtractedText)

        if (searchSFExpressTracking1) {
            courierInfo.name = "sf-express"
            courierInfo.trackingNo = patternSFExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchSFExpress) {
            courierInfo.name = "sf-express"
        }

        return RegexResult(courierInfo)
    }
}