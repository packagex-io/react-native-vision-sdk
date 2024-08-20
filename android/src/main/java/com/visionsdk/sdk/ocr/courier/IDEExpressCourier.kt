package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class IDEExpressCourier : Courier() {

    val patternIDEExpress by lazy {
        VisionRegex("(?i)(\\bide express\\b)", RegexType.Default)
    }

    val patternIDEExpressTracking1 by lazy {
        VisionRegex("U21\\d{20}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchIDEExpressTracking1 = patternIDEExpressTracking1.find(barcode)

        if (searchIDEExpressTracking1) {
            courierInfo.name = "ide-express"
            courierInfo.trackingNo = patternIDEExpressTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchIDEExpress = patternIDEExpress.find(ocrExtractedText)
        val searchIDEExpressTracking1 = patternIDEExpressTracking1.find(ocrExtractedText)

        if (searchIDEExpress || searchIDEExpressTracking1) {
            courierInfo.name = "ide-express"
            if (searchIDEExpressTracking1) {
                courierInfo.trackingNo = patternIDEExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}