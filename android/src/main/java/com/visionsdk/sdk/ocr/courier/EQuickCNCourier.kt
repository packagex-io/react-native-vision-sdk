package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class EQuickCNCourier : Courier() {

    val patternEQuickCNTracking1: VisionRegex by lazy { VisionRegex("EQ[A-Z0-9]{8,13}YQ", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchEQuickCNTracking1 = patternEQuickCNTracking1.find(barcode)

        if (searchEQuickCNTracking1) {
            courierInfo.name = "equick-cn"
            courierInfo.trackingNo = patternEQuickCNTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchEQuickCNTracking1 = patternEQuickCNTracking1.find(ocrExtractedText)

        if (searchEQuickCNTracking1) {
            courierInfo.name = "equick-cn"
            courierInfo.trackingNo = patternEQuickCNTracking1.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }
}