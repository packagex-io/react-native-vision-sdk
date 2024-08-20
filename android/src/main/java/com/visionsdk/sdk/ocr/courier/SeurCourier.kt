package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SeurCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)\\bSEUR\\b", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("\\b\\d{2} \\d{2} \\d{2} \\d{7} \\d{1}\\b", RegexType.TrackingNo)
    }

    val patternTracking1Barcode by lazy {
        VisionRegex("\\b\\d{14}\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1Barcode = patternTracking1Barcode.find(barcode)

        if (search && searchTracking1Barcode) {
            courierInfo.name = "seur"
            courierInfo.trackingNo = patternTracking1Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val searchGLS = GLSCourier().patternGLS.find(ocrExtractedText)
        val searchGLSLink = GLSCourier().patternGLSLink.find(ocrExtractedText)

        if (searchGLS || searchGLSLink) {
            return RegexResult()
        }

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)

        if (search) {
            courierInfo.name = "seur"
            if (searchTracking1) {
                courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courier = courierInfo)
    }
}