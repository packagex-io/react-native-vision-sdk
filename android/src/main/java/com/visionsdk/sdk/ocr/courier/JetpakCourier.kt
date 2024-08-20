package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class JetpakCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)[\\s\\.\\,]*Jetpak[\\s\\.\\,]*", RegexType.Default)
    }

    val patternTracking by lazy {
        VisionRegex("\\b0005\\d{10}\\b|0005\\d{10}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(barcode)

        if (search) {
            courierInfo.name = "Jetpak"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking = patternTracking.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "Jetpak"
            if (searchTracking) {
                courierInfo.trackingNo = patternTracking.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}