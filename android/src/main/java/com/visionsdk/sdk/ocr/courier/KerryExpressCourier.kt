package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class KerryExpressCourier : Courier() {

    val patternKerryExpress by lazy {
        VisionRegex("(?i)\\b([www\\s\\.\\,]*kerry[\\n\\s\\.\\,]*express[\\s\\.\\,com]*[\\s\\.\\,vn]*)\\b", RegexType.Default)
    }

    val patternKerryExpressTracking1 by lazy {
        VisionRegex("[A-Z\\d]{13,15}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchKerryExpress = patternKerryExpress.find(ocrExtractedText)
        val searchKerryExpressTracking1 = patternKerryExpressTracking1.find(ocrExtractedText)

        if (searchKerryExpress) {
            courierInfo.name = "kerry-express"
            if (searchKerryExpressTracking1) {
                courierInfo.trackingNo = patternKerryExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}