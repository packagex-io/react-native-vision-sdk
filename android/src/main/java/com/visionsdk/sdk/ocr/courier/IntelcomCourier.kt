package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class IntelcomCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(INTELCOM|Intelcom)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("^(?<tracking_no>INTL[A-Z]{3}\\d{9})", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?<tracking_no>INTL[A-Z]{3}( )?\\d{9})", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)

        if (searchTracking1) {
            courierInfo.name = "intelcom"
            courierInfo.trackingNo = patternTracking1.group(barcode, "tracking_no").removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)

        if (search || searchTracking2) {
            courierInfo.name = "intelcom"
            if (searchTracking2) {
                courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, "tracking_no").removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}