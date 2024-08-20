package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class YamatoCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(ヤマト|ヤマト運輸|yamato)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("A[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]A", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("B[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]B", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2 = patternTracking2.find(barcode)

        if (searchTracking1 || searchTracking2) {
            courierInfo.name = "yamato"
            sender.country = "Japan"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(barcode, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "yamato"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}