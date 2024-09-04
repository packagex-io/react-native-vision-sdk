package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class JAndTExpressCourier : Courier( listOf("") ) {

    val patternJAndTExpress by lazy {
        VisionRegex("(?i)((\\bJ&T EXPRESS\\b)|(\\.jet\\.co\\.))", RegexType.Default)
    }

    val patternJAndTExpressTracking1 by lazy {
        VisionRegex("J[A-Z]\\d{10}", RegexType.TrackingNo)
    }

    val patternJAndTExpressTracking2 by lazy {
        VisionRegex("J[A-Z]{2}\\d{12}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTiki = patternJAndTExpress.find(ocrExtractedText)
        val searchTikiTracking1 = patternJAndTExpressTracking1.find(ocrExtractedText)

        if (searchTiki) {
            courierInfo.name = "j&t-express"
            if (searchTikiTracking1) {
                courierInfo.trackingNo = patternJAndTExpressTracking1.group(ocrExtractedText, 0)
            }
        }

        return RegexResult(courierInfo)
    }
}