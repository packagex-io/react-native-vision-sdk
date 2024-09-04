package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class AmazonCourier : Courier( listOf("amazon") ) {

    val patternAmazon: VisionRegex by lazy { VisionRegex("(?is)((\\bAmazon Parcel\\b)|(\\bamazon\\.co)|(\\b[A-Za-z]{3}\\d\\b\\s(.*?)Amazon)|(\\bcycle.1\\b)|(Amazon Seller Services)|(.ww\\.amazon\\.in))", RegexType.Default) }
    val patternAmazonTracking1: VisionRegex by lazy { VisionRegex("(TB|TE)(?:[A-Z])[\\d]{12}", RegexType.TrackingNo) }
    val patternAmazonTracking2: VisionRegex by lazy { VisionRegex("(BA)[\\d]{10}", RegexType.TrackingNo) }
    val patternAmazonTracking3: VisionRegex by lazy { VisionRegex("\\b[A-Z]{2}\\d{10}\\b", RegexType.TrackingNo) }
    val patternAmazonTracking4: VisionRegex by lazy { VisionRegex("\\b[A-Z]{1}\\d{11}\\b", RegexType.TrackingNo) }
    val patternAmazonTracking5: VisionRegex by lazy { VisionRegex("(?i)\\bAWB\\s?(\\d{12})\\b", RegexType.TrackingNo) }
    val patternAmazonTracking6: VisionRegex by lazy { VisionRegex("FBA15[A-Z\\d]{8}000001", RegexType.TrackingNo) }

    val patternAmazonBarcode1: VisionRegex by lazy { VisionRegex("^\\{\\\"lastNode\\\":.*", RegexType.Barcode) }
    val patternAmazonBarcode2: VisionRegex by lazy { VisionRegex("^amzn\\.to\\/socialqr\$", RegexType.Barcode) }

    val patternAmazonPhone: VisionRegex by lazy { VisionRegex("1\\s*of\\s*1[\\/\\d]?([\\d]{10})", RegexType.Phone) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchAmazonTracking1 = patternAmazonTracking1.find(barcode)
        val searchAmazonBarcode1 = patternAmazonBarcode1.find(barcode)
        val searchAmazonTracking6 = patternAmazonTracking6.find(barcode)

        if (searchAmazonTracking1 || searchAmazonBarcode1) {
            courierInfo.name = "amazon-fba-us"
            if (searchAmazonTracking1) {
                courierInfo.trackingNo = patternAmazonTracking1.group(barcode, 0).removeSpaces()
            }
        }

        if (searchAmazonTracking6) {
            courierInfo.name = "amazon-fba-us"
            courierInfo.trackingNo = patternAmazonTracking6.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchAmazon = patternAmazon.find(ocrExtractedText)
        val searchAmazonTracking1 = patternAmazonTracking1.find(ocrExtractedText)
        val searchAmazonTracking2 = patternAmazonTracking2.find(ocrExtractedText)
        val searchAmazonTracking3 = patternAmazonTracking3.find(ocrExtractedText)
        val searchAmazonTracking4 = patternAmazonTracking4.find(ocrExtractedText)
        val searchAmazonTracking5 = patternAmazonTracking5.find(ocrExtractedText)
        val searchAmazonTracking6 = patternAmazonTracking6.find(ocrExtractedText)
        val searchPhone = patternAmazonPhone.find(ocrExtractedText)

        if (searchAmazon || searchAmazonTracking1 || searchPhone) {
            courierInfo.name = "amazon-fba-us"
            if (searchAmazonTracking1) {
                val trackingNo = patternAmazonTracking1.group(ocrExtractedText, 0)?.replace("TE", "TB").removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchAmazonTracking5) {
                val trackingNo = patternAmazonTracking5.group(ocrExtractedText, 1).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        if (searchAmazon && searchAmazonTracking1.not() && searchAmazonTracking2) {
            val trackingNo = patternAmazonTracking2.group(ocrExtractedText, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo
        }

        if (
            searchAmazon
            && courierInfo.trackingNo.isNullOrEmptyOrBlank()
            && (searchAmazonTracking3 || searchAmazonTracking4)
        ) {
            if (searchAmazonTracking3) {
                val trackingNo = patternAmazonTracking3.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchAmazonTracking4) {
                val trackingNo = patternAmazonTracking4.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        if (searchAmazonTracking6) {
            courierInfo.name = "amazon-fba-us"
            val trackingNo = patternAmazonTracking6.group(ocrExtractedText, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo
        }

        return RegexResult(courierInfo)
    }
}