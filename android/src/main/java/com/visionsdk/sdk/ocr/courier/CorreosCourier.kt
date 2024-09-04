package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CorreosCourier : Courier( listOf("correos") ) {

    val patternCorreos by lazy {
        VisionRegex("(?i)(\\bCorreos\\b)", RegexType.Default)
    }

    val patternCorreosTracking1 by lazy {
        VisionRegex("(?i)\\b[A-Z]{2}\\d{9}ES\\b", RegexType.TrackingNo)
    }

    val patternCorreosTracking2 by lazy {
        VisionRegex("(?i)\\b[A-Z]{2}\\d{2}[A-Z]{2}\\d{16}[A-Z]{1}\\b", RegexType.TrackingNo)
    }

    val patternCorreosTracking3 by lazy {
        VisionRegex("(?i)\\b[A-Z]{2} \\d{3} \\d{3} \\d{3} BR\\b", RegexType.TrackingNo)
    }

    val patternCorreosTracking3Barcode by lazy {
        VisionRegex("(?i)\\b[A-Z]{2}\\d{9}BR\\b", RegexType.Barcode)
    }

    val patternCorreosTracking4 by lazy {
        VisionRegex("(?i)\\bCodigo Correos: ? PK\\dBM\\d{16}\\b", RegexType.TrackingNo)
    }

    val patternCorreosTracking4Barcode by lazy {
        VisionRegex("(?i)\\bPK\\dBM\\d{16}\\b", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCorreosTracking1 = patternCorreosTracking1.find(barcode)
        val searchCorreosTracking2 = patternCorreosTracking2.find(barcode)
        val searchCorreosTracking3Barcode = patternCorreosTracking3Barcode.find(barcode)
        val searchCorreosTracking4Barcode = patternCorreosTracking4Barcode.find(barcode)

        if (searchCorreosTracking1) {
            courierInfo.name = "correos"
            courierInfo.trackingNo = patternCorreosTracking1.group(barcode, 0).removeSpaces()
        } else if (searchCorreosTracking2) {
            courierInfo.name = "correos"
            courierInfo.trackingNo = patternCorreosTracking1.group(barcode, 0).removeSpaces()
        } else if (searchCorreosTracking3Barcode) {
            courierInfo.name = "correos"
            courierInfo.trackingNo = patternCorreosTracking3Barcode.group(barcode, 0).removeSpaces()
        } else if (searchCorreosTracking4Barcode) {
            courierInfo.name = "correos"
            courierInfo.trackingNo = patternCorreosTracking4Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCorreosExpress = CorreosExpressCourier().patternCorreosExpress.find(ocrExtractedText)
        val searchCorreosExpress1 = CorreosExpressCourier().patternCorreosExpress1.find(ocrExtractedText)

        if (searchCorreosExpress || searchCorreosExpress1) {
            return RegexResult()
        }

        val searchCorreos = patternCorreos.find(ocrExtractedText)
        val searchCorreosTracking1 = patternCorreosTracking1.find(ocrExtractedText)
        val searchCorreosTracking2 = patternCorreosTracking2.find(ocrExtractedText)
        val searchCorreosTracking3 = patternCorreosTracking3.find(ocrExtractedText)
        val searchCorreosTracking4 = patternCorreosTracking4.find(ocrExtractedText)

        if (searchCorreos) {
            courierInfo.name = "correos"
            if (searchCorreosTracking1) {
                courierInfo.trackingNo = patternCorreosTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchCorreosTracking2) {
                courierInfo.trackingNo = patternCorreosTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchCorreosTracking3) {
                courierInfo.trackingNo = patternCorreosTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchCorreosTracking4) {
                courierInfo.trackingNo = patternCorreosTracking4.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}