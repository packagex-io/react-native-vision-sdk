package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class USPSCourier : Courier( listOf("") ) {

    val patternUSPS: VisionRegex by lazy { VisionRegex("\\b(USPS)|((?i)United[\\s]+States[\\s]+Postal[\\s]+Service)\\b", RegexType.Default) }

    val patternUSOrigin: VisionRegex by lazy { VisionRegex("(?i)\\b[A-Z]{2} *( *[\\d]){9} *US\\b", RegexType.Default) }

    val patternUSPSTracking1: VisionRegex by lazy { VisionRegex("E\\D{1}\\d{9}\\D{2}|9\\d{15,21}", RegexType.TrackingNo) }
    val patternUSPSTracking2: VisionRegex by lazy { VisionRegex("[A-Za-z]{2}[0-9]+US", RegexType.TrackingNo) }
    val patternUSPSTracking3: VisionRegex by lazy { VisionRegex("9\\d{3} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{2}", RegexType.TrackingNo) }
    val patternUSPSTracking3Barcode: VisionRegex by lazy { VisionRegex("9\\d{21}(?!\\d)", RegexType.Barcode) }
    val patternUSPSTracking4: VisionRegex by lazy { VisionRegex("(^420|^C1420)(?<zipcode>[\\d]{5,11})[^\\d]*(?:[\\\\x1d]+|[\\\\u001d]+)?(?<tracking_no>\\d{22,26})\$", RegexType.TrackingNo) }
    val patternUSPSTracking5: VisionRegex by lazy { VisionRegex("(^|\\b)([A-Za-z]{2} ?[0-9]{3} ?[0-9]{3} ?[0-9]{3} ?US)(\\b|\$)", RegexType.TrackingNo) }
    val patternUSPSTracking6: VisionRegex by lazy { VisionRegex("^\\bC1(?<tracking_no>\\d{22})", RegexType.TrackingNo) }
    val patternUSPSTracking7: VisionRegex by lazy { VisionRegex("9\\d{3} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{4} ?\\d{2}", RegexType.TrackingNo) }
    val patternUSPSTracking7Barcode: VisionRegex by lazy { VisionRegex("9\\d{25}(?!\\d)", RegexType.Barcode) }

    val shipmentTypes: List<Pair<String, VisionRegex>> by lazy {
        listOf(
            "First Class Mail" to VisionRegex("(?i)\\b(USPS)?[ ]{0,1}FIRST[ |-]?CLASS[ |-]?MAIL", RegexType.Default),
            "First Class Package Return Service" to VisionRegex("(?i)\\b(USPS)?[ |\\\\n]{0,1}FIRST[ |-]?CLASS ?[(]?(\\u2122|tm)?[)]?M? ?(PACKAGE|PKG) ?RETURN ?(SERVICE|SVC)", RegexType.Default),
            "First Class PKG SVC-RTL" to VisionRegex("(?i)\\b(USPS)?[ ]{0,1}FIRST[ |-]?CLASS[ ]{0,1}(PKG)?[ ]{0,1}(SVC)?[ |-]?RTL", RegexType.Default),
            "First Class PKG SVC" to VisionRegex("(?i)\\b(USPS)?[ ]{0,1}FIRST[ |-]?CLASS[ ]{0,1}(PKG)?[ ]{0,1}(SVC)?\\b", RegexType.Default),
            "Media Mail" to VisionRegex("(?i)\\b(USPS)?[ ]{0,1}MEDIA[ ]{0,1}MAIL\\b", RegexType.Default),
            "Priority Mail N-Day" to VisionRegex("(?i)\\bPRIORITY[ ]{0,1}MAIL[ ]{0,1}\\d{1}[ |-]DAY\\b", RegexType.Default),
            "Priority Mail" to VisionRegex("(?i)(USPS)?[\\n\\\\b]?PRIORITY\\s{0,1}MAIL\\b", RegexType.Default),
            "Ground Advantage" to VisionRegex("(?i)\\b(USPS)?[ ]{0,1}GROUND[ ]{0,1}ADVANTAGE", RegexType.Default),
            "Global Express Guaranteed" to VisionRegex("(?i)\\b(USPS)?[ ]{0,1}GLOBAL[ ]{0,1}EXPRESS[ ]{0,1}GUARANTEED", RegexType.Default),
        )
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val receiver = RegexResult.ExchangeInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchUSPSTracking3 = patternUSPSTracking3Barcode.find(barcode)
        val searchUSPSTracking4 = patternUSPSTracking4.find(barcode)
        val searchUSPSTracking7 = patternUSPSTracking7Barcode.find(barcode)

        val fedExCourier = FedExCourier()
        val searchFedEx = fedExCourier.patternFedEx.find(ocrExtractedText)

        val searchUSPSTracking6 = patternUSPSTracking6.find(barcode)
        val searchUSOrigin = patternUSOrigin.find(barcode)

        if (!searchFedEx && (searchUSPSTracking3 || searchUSPSTracking4 || searchUSPSTracking7 || searchUSPSTracking6)) {

            courierInfo.name = "usps"

            if (searchUSPSTracking7) {
                courierInfo.trackingNo = patternUSPSTracking7Barcode.group(barcode, 0).removeSpaces()
            } else if (searchUSPSTracking3) {
                courierInfo.trackingNo = patternUSPSTracking3Barcode.group(barcode, 0).removeSpaces()
            } else if (searchUSPSTracking4) {
                receiver.zipcode = patternUSPSTracking4.group(barcode, "zipcode")
                courierInfo.trackingNo = patternUSPSTracking4.group(barcode, "tracking_no").removeSpaces()
            } else if (searchUSPSTracking6) {
                courierInfo.trackingNo = patternUSPSTracking6.group(barcode, "tracking_no").removeSpaces()
            }
        }

        if (searchUSOrigin) {
            courierInfo.trackingNo = patternUSOrigin.group(barcode, 0).removeSpaces()
            sender.country = "United States"
        }

        return RegexResult(courier = courierInfo, receiver = receiver, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchUSPS = patternUSPS.find(ocrExtractedText)

        val searchUSPSTracking1 = patternUSPSTracking1.find(ocrExtractedText)
        val searchUSPSTracking2 = patternUSPSTracking2.find(ocrExtractedText)
        val searchUSPSTracking3 = patternUSPSTracking3.find(ocrExtractedText)
        val searchUSPSTracking7 = patternUSPSTracking7.find(ocrExtractedText)
        val searchUSPSTracking5 = patternUSPSTracking5.find(ocrExtractedText)

        val searchUSOrigin = patternUSOrigin.find(ocrExtractedText)

        if (searchUSPS) {
            courierInfo.name = "usps"

            if (searchUSPSTracking7) {
                courierInfo.trackingNo = patternUSPSTracking7.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchUSPSTracking1) {
                courierInfo.trackingNo = patternUSPSTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchUSPSTracking2) {
                courierInfo.trackingNo = patternUSPSTracking2.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchUSPSTracking3) {
                courierInfo.trackingNo = patternUSPSTracking3.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchUSPSTracking5) {
                courierInfo.trackingNo = patternUSPSTracking5.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        if (searchUSOrigin) {
            courierInfo.trackingNo = patternUSOrigin.group(ocrExtractedText, 0).removeSpaces()
            sender.country = "United States"
        }

        for ((shipmentName, regex) in shipmentTypes) {
            if (!regex.find(ocrExtractedText)) {
                continue
            }

            if (shipmentName == "Priority Mail N-Day") {
                val result = regex.group(ocrExtractedText, 0)
                val daysRegex = VisionRegex("\\d", RegexType.Default)
                val days = daysRegex.group(result, 0)
                courierInfo.shipmentType = shipmentName.replace("N", days ?: "N")
            } else {
                courierInfo.shipmentType = shipmentName
            }
            break
        }

        return RegexResult(courierInfo, sender = sender)
    }
}