package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class YunExpressCourier : Courier() {

    val patternYunExpressTracking1 by lazy {
        VisionRegex("YT22\\d{14}(?!\\d)", RegexType.TrackingNo)
    }

    val patternYunExpressTracking2 by lazy {
        VisionRegex("NEXAU100\\d{7}YQ", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchYunExpressTracking1 = patternYunExpressTracking1.find(barcode)
        val searchYunExpressTracking2 = patternYunExpressTracking2.find(barcode)

        if (searchYunExpressTracking1) {
            courierInfo.name = "yunexpress"
            courierInfo.trackingNo = patternYunExpressTracking1.group(barcode, 0).removeSpaces()
        } else if (searchYunExpressTracking2) {
            courierInfo.name = "yunexpress"
            courierInfo.trackingNo = patternYunExpressTracking2.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchYunExpressTracking1 = patternYunExpressTracking1.find(ocrExtractedText)
        val searchYunExpressTracking2 = patternYunExpressTracking2.find(ocrExtractedText)

        if (searchYunExpressTracking1) {
            courierInfo.name = "yunexpress"
            courierInfo.trackingNo = patternYunExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchYunExpressTracking2) {
            courierInfo.name = "yunexpress"
            courierInfo.trackingNo = patternYunExpressTracking2.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }
}