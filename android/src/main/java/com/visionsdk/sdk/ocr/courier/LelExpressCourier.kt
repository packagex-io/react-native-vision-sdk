package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LelExpressCourier : Courier() {

    val patternLelExpress by lazy {
        VisionRegex("(?i)(\\blelexpress\\b)", RegexType.Default)
    }

    val patternLelExpressTracking1 by lazy {
        VisionRegex("MYMPA\\d{9}(?!\\d)", RegexType.TrackingNo)
    }

    val patternLelExpressTracking2 by lazy {
        VisionRegex("LXST\\d{9}MY", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchLelExpressTracking1 = patternLelExpressTracking1.find(barcode)
        val searchLelExpressTracking2 = patternLelExpressTracking2.find(barcode)

        if (searchLelExpressTracking1 || searchLelExpressTracking2) {
            courierInfo.name = "lel-express"
            if (searchLelExpressTracking1) {
                courierInfo.trackingNo = patternLelExpressTracking1.group(barcode, 0).removeSpaces()
            } else if (searchLelExpressTracking2) {
                courierInfo.trackingNo = patternLelExpressTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchLelExpress = patternLelExpress.find(ocrExtractedText)
        val searchLelExpressTracking1 = patternLelExpressTracking1.find(ocrExtractedText)
        val searchLelExpressTracking2 = patternLelExpressTracking2.find(ocrExtractedText)

        if (searchLelExpress || searchLelExpressTracking1 || searchLelExpressTracking2) {
            courierInfo.name = "lel-express"
            if (searchLelExpressTracking1) {
                courierInfo.trackingNo = patternLelExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchLelExpressTracking2) {
                courierInfo.trackingNo = patternLelExpressTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}