package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class DynamexCourier : Courier( listOf("dynamex") ) {

    val patternDynamex: VisionRegex by lazy { VisionRegex("(dynamex|DYNAMEX)(-NEXT)?", RegexType.Default) }
    val patternDynamexTracking1: VisionRegex by lazy { VisionRegex("(^|\\b)DX[A-Z]{4}\\d{7}(\\b|\$)", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDynamexTracking1 = patternDynamexTracking1.find(barcode)

        if (searchDynamexTracking1) {
            courierInfo.name = "dynamex"
            courierInfo.trackingNo = patternDynamexTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDynamex = patternDynamex.find(ocrExtractedText)
        val searchDynamexTracking1 = patternDynamexTracking1.find(ocrExtractedText)

        if (searchDynamex) {
            courierInfo.name = "dynamex"
            if (searchDynamexTracking1) {
                courierInfo.trackingNo = patternDynamexTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}