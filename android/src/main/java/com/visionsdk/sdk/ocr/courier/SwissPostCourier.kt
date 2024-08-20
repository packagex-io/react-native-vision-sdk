package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SwissPostCourier : Courier() {

    val pattern by lazy {
        VisionRegex("(?i)(Swiss[ \\-]?Post)", RegexType.Default)
    }

    val pattern1 by lazy {
        VisionRegex("(?i)(Die[ \\-]?Post)", RegexType.Default)
    }

    val patternTracking1 by lazy {
        VisionRegex("^(?<=\\D\\n)([A-Za-z]{2}[0-9]+C[H]\$)(?=\\s*\\b)", RegexType.TrackingNo)
    }

    val patternTracking2 by lazy {
        VisionRegex("(?<=\\D\\n)\\b98\\.\\d{2}\\.\\d{6}\\.\\d{8}(?=\\s*\\b)", RegexType.TrackingNo)
    }

    val patternTracking3 by lazy {
        VisionRegex("(?<=\\D\\n)\\b99\\.\\d{2}\\.\\d{6}\\.\\d{8}(?=\\s*\\b)", RegexType.TrackingNo)
    }

    val patternTracking4 by lazy {
        VisionRegex("(?<=\\D\\n)\\b97\\.\\d{2}\\.\\d{6}\\.\\d{8}(?=\\s*\\b)", RegexType.TrackingNo)
    }

    val patternTracking5 by lazy {
        VisionRegex("(?<=\\D\\n)\\b91\\.\\d{2}\\.\\d{4}\\.\\d{2}\\.\\d{6}(?=\\s*\\b)", RegexType.TrackingNo)
    }

    val patternTracking6 by lazy {
        VisionRegex("(?<=\\D\\n)\\b97\\.\\d{2}\\.\\d{8}(?=\\s*\\b)", RegexType.TrackingNo)
    }

    val patternTracking2Barcode by lazy {
        VisionRegex("\\b98\\d{2}\\d{6}\\d{8}\\s*\$", RegexType.Barcode)
    }

    val patternTracking3Barcode by lazy {
        VisionRegex("\\b99\\d{2}\\d{6}\\d{8}\\s*\$", RegexType.Barcode)
    }

    val patternTracking4Barcode by lazy {
        VisionRegex("\\b97\\d{2}\\d{6}\\d{8}\\s*\$", RegexType.Barcode)
    }

    val patternTracking5Barcode by lazy {
        VisionRegex("\\b91\\d{2}\\d{4}\\d{2}\\d{6}\\s*\$", RegexType.Barcode)
    }

    val patternTracking6Barcode by lazy {
        VisionRegex("\\b97\\d{2}\\d{8}\\s*\$", RegexType.Barcode)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchTracking1 = patternTracking1.find(barcode)
        val searchTracking2Barcode = patternTracking2Barcode.find(barcode)
        val searchTracking3Barcode = patternTracking3Barcode.find(barcode)
        val searchTracking4Barcode = patternTracking4Barcode.find(barcode)
        val searchTracking5Barcode = patternTracking5Barcode.find(barcode)
        val searchTracking6Barcode = patternTracking6Barcode.find(barcode)

        if (searchTracking1 || searchTracking2Barcode || searchTracking3Barcode || searchTracking4Barcode || searchTracking5Barcode || searchTracking6Barcode) {
            courierInfo.name = "swiss-post"
            courierInfo.trackingNo = barcode
        }

        return RegexResult(courier = courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val search = pattern.find(ocrExtractedText)
        val search1 = pattern1.find(ocrExtractedText)
        val searchTracking1 = patternTracking1.find(ocrExtractedText)
        val searchTracking2 = patternTracking2.find(ocrExtractedText)
        val searchTracking3 = patternTracking3.find(ocrExtractedText)
        val searchTracking4 = patternTracking4.find(ocrExtractedText)
        val searchTracking5 = patternTracking5.find(ocrExtractedText)
        val searchTracking6 = patternTracking6.find(ocrExtractedText)

        if (search || search1) {
            courierInfo.name = "swiss-post"
        }

        if (searchTracking1) {
            courierInfo.trackingNo = patternTracking1.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking2) {
            courierInfo.trackingNo = patternTracking2.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking3) {
            courierInfo.trackingNo = patternTracking3.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking4) {
            courierInfo.trackingNo = patternTracking4.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking5) {
            courierInfo.trackingNo = patternTracking5.group(ocrExtractedText, 0).removeSpaces()
        } else if (searchTracking6) {
            courierInfo.trackingNo = patternTracking6.group(ocrExtractedText, 0).removeSpaces()
        }

        return RegexResult(courier = courierInfo)
    }
}