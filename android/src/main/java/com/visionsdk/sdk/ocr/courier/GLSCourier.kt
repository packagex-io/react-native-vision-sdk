package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class GLSCourier : Courier( listOf("") ) {

    val patternGLS by lazy {
        VisionRegex("((I|\\b)GLS)\\b", RegexType.Default) // TODO: Adding |gls works
    }

    val patternGLSLink by lazy {
        VisionRegex("\\bwww\\.gls", RegexType.Default)
    }

    val patternGLSTracking1 by lazy {
        VisionRegex("PG[ A-Z0-9]{8,12}M1", RegexType.TrackingNo)
    }

    val patternGLSTracking2 by lazy {
        VisionRegex("(?i)\\bTRACKING ?#?:? ?(\\d{9})\\b", RegexType.TrackingNo)
    }

    val patternGLSTracking3 by lazy {
        VisionRegex("\\b[A-Z]{3}[A-Z0-9]{4}(\\d|O)\\b", RegexType.TrackingNo)
    }

    val patternGLSTracking4Barcode by lazy {
        VisionRegex("\\b\\d{9,13}\\b", RegexType.Barcode)
    }

    val patternGLSTracking5 by lazy {
        VisionRegex("\\b(^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]*\$){8}\\b", RegexType.TrackingNo)
    }

    val patternGLSTracking6 by lazy {
        VisionRegex("\\b[A-Z]{3}\\d[A-Z]{2}(\\d|O)\\b", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchGLS = patternGLS.find(ocrExtractedText)
        val searchGLSTracking1 = patternGLSTracking1.find(barcode)
        val searchGLSTracking4Barcode = patternGLSTracking4Barcode.find(barcode)

        if (searchGLSTracking1) {
            courierInfo.name = "gls"
            courierInfo.trackingNo = patternGLSTracking1.group(barcode, 0).removeSpaces()
        } else if (searchGLS && searchGLSTracking4Barcode) {
            courierInfo.name = "gls"
            courierInfo.trackingNo = patternGLSTracking4Barcode.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchGLS = patternGLS.find(ocrExtractedText)
        val searchGLSTracking1 = patternGLSTracking1.find(ocrExtractedText)
        val searchGLSTracking2 = patternGLSTracking2.find(ocrExtractedText)
        val searchGLSTracking3 = patternGLSTracking3.find(ocrExtractedText)
        val searchGLSTracking5 = patternGLSTracking5.find(ocrExtractedText)

        if (searchGLS && searchGLSTracking1) {
            courierInfo.name = "gls"
            courierInfo.trackingNo = patternGLSTracking1.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchGLS && searchGLSTracking2) {
            courierInfo.name = "gls"
            courierInfo.trackingNo = patternGLSTracking2.group(ocrExtractedText, 1).removeSpaces()
        } else if (searchGLS && searchGLSTracking3) {
            courierInfo.name = "gls"
            courierInfo.trackingNo = patternGLSTracking3.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchGLS && searchGLSTracking5) {
            courierInfo.name = "gls"
            courierInfo.trackingNo = patternGLSTracking5.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }
}