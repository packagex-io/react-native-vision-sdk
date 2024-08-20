package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class YTOExpressCourier : Courier() {

    val patternYTOExpress by lazy {
        VisionRegex("(?i)(yto[\\s]*express|ytoexpress\\.com|圆通速递)", RegexType.Default)
    }

    val patternYTOExpressTracking1 by lazy {
        VisionRegex("YT(?<!\\d)\\d{8,20}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchYTOExpressTracking1 = patternYTOExpressTracking1.find(barcode)
        val searchYunExpressTracking1 = YunExpressCourier().patternYunExpressTracking1.find(barcode)

        if (searchYunExpressTracking1.not() && searchYTOExpressTracking1) {
            courierInfo.name = "yto-express"
            courierInfo.trackingNo = patternYTOExpressTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchYTOExpress = patternYTOExpress.find(ocrExtractedText)
        val searchYTOExpressTracking1 = patternYTOExpressTracking1.find(ocrExtractedText)

        if (searchYTOExpress) {
            courierInfo.name = "yto-express"
            if (searchYTOExpressTracking1) {
                courierInfo.trackingNo = patternYTOExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}