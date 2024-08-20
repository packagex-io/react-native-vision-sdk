package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class StatOvernightCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\b([\\s\\.\\,]*statovernight[\\s\\.\\,]*|stat[\\s\\.\\,]overnight|statover[\\s\\.\\,]night|stat[\\s\\.\\,]over[\\s\\.\\,]night)\\b", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("(?<!\\d)\\d{7}(?!\\d)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking = patternTracking.find(barcode)

        if (searchTracking) {
            courierInfo.name = "statovernight"
            courierInfo.trackingNo = patternTracking.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "statovernight"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}