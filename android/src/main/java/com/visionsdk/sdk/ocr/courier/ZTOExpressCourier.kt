package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ZTOExpressCourier : Courier() {

    val patternZTOExpressTracking1 by lazy {
        VisionRegex("8520001\\d{5}(?!\\d)", RegexType.TrackingNo)
    }

    val patternZTOExpressTracking2 by lazy {
        VisionRegex("758\\d{11}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchZTOExpressTracking1 = patternZTOExpressTracking1.find(barcode)
        val searchZTOExpressTracking2 = patternZTOExpressTracking2.find(barcode)

        if (searchZTOExpressTracking1) {
            courierInfo.name = "zto-express"
            courierInfo.trackingNo = patternZTOExpressTracking1.group(barcode, 0).removeSpaces()
        } else if (searchZTOExpressTracking2) {
            courierInfo.name = "zto-express"
            courierInfo.trackingNo = patternZTOExpressTracking2.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchZTOExpressTracking1 = patternZTOExpressTracking1.find(ocrExtractedText)
        val searchZTOExpressTracking2 = patternZTOExpressTracking2.find(ocrExtractedText)

        if (searchZTOExpressTracking1) {
            courierInfo.name = "zto-express"
            courierInfo.trackingNo = patternZTOExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchZTOExpressTracking2) {
            courierInfo.name = "zto-express"
            courierInfo.trackingNo = patternZTOExpressTracking2.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }
}