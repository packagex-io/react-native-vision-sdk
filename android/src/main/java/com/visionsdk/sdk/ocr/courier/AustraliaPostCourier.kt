package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class AustraliaPostCourier : Courier() {

    val patternAustraliaPost: VisionRegex by lazy { VisionRegex("(?i)(eparcel|parcel ?post|express ?post|australia ?post)", RegexType.Default) }
    val patternAustraliaPostTracking1: VisionRegex by lazy { VisionRegex("(?i)ap article id[: ]*([A-Za-z0-9]*)", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchAustraliaPostTracking1 = patternAustraliaPostTracking1.find(ocrExtractedText)

        if (searchAustraliaPostTracking1) {
            courierInfo.name = "australia-post"
            courierInfo.trackingNo = patternAustraliaPostTracking1.group(ocrExtractedText, 1).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchAustraliaPost = patternAustraliaPost.find(ocrExtractedText)
        val searchAustraliaPostTracking1 = patternAustraliaPostTracking1.find(ocrExtractedText)

        if (searchAustraliaPost || searchAustraliaPostTracking1) {
            courierInfo.name = "australia-post"
            if (searchAustraliaPostTracking1) {
                courierInfo.trackingNo = patternAustraliaPostTracking1.group(ocrExtractedText, 1).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}