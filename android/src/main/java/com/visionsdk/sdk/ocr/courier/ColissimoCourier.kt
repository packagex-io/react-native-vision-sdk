package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ColissimoCourier : Courier( listOf("colissimo") ) {

    val patternColissimo by lazy {
        VisionRegex("(?i)\\b(colissimo)\\b", RegexType.Default)
    }

    val patternColissimoTracking1 by lazy {
        VisionRegex("\\b[0-9][A-Z][\\d ]{11,15}\\b", RegexType.TrackingNo)
    }

    val patternColissimoTracking2 by lazy {
        VisionRegex("(?i)\\b\\d{3}[A-Z0-9]{1} ?\\d{10}[A-Z0-9]{1}\\b", RegexType.TrackingNo)
    }

    val patternColissimoTracking3Barcode by lazy {
        VisionRegex("(?i)\\b0075\\d{3}([A-Z0-9]{14})801250\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchColissimoTracking1 = patternColissimoTracking1.find(barcode)
        val searchColissimoTracking2 = patternColissimoTracking2.find(barcode)
        val searchColissimoTracking3Barcode = patternColissimoTracking3Barcode.find(barcode)

        if (searchColissimoTracking1) {
            courierInfo.name = "colissimo"
            courierInfo.trackingNo = patternColissimoTracking1.group(barcode, 0).removeSpaces()
        } else if (searchColissimoTracking2) {
            courierInfo.name = "colissimo"
            courierInfo.trackingNo = patternColissimoTracking2.group(barcode, 0).removeSpaces()
        } else if (searchColissimoTracking3Barcode) {
            courierInfo.name = "colissimo"
            courierInfo.trackingNo = patternColissimoTracking3Barcode.group(barcode, 1).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val searchSeur = SeurCourier().pattern.find(ocrExtractedText)
        if (searchSeur) {
            return RegexResult()
        }

        val courierInfo = RegexResult.CourierInfo()

        val searchColissimo = patternColissimo.find(ocrExtractedText)
        val searchColissimoTracking1 = patternColissimoTracking1.find(ocrExtractedText)
        val searchColissimoTracking2 = patternColissimoTracking2.find(ocrExtractedText)

        if (searchColissimo) {
            courierInfo.name = "colissimo"
            if (searchColissimoTracking2) {
                courierInfo.trackingNo = patternColissimoTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchColissimoTracking1) {
                courierInfo.trackingNo = patternColissimoTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}