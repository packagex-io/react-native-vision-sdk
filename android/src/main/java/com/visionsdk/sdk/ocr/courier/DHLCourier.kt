package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class DHLCourier : Courier() {

    val patternDHL: VisionRegex by lazy { VisionRegex("\\b(DHL)\\b", RegexType.Default) }

    val patternDHLEurope: VisionRegex by lazy { VisionRegex("(?i)\\b(DHL[\\s|\\n]{0,1}PAKET)\\b", RegexType.Default) }
    val patternDHLWayBill: VisionRegex by lazy { VisionRegex("(?i)(?:WAYBILL) ?(\\d ?\\d ?\\d ?\\d ?\\d ?\\d ?\\d ?\\d ?\\d ?\\d)", RegexType.Default) }
    val patternDHLTracking2: VisionRegex by lazy { VisionRegex("(GM\\d{18})", RegexType.TrackingNo) }
    val patternDHLTracking3: VisionRegex by lazy { VisionRegex("^(\\d[A-Z])?US\\d{5,11}\\+\\d+\$", RegexType.TrackingNo) }
    val patternDHLTracking4: VisionRegex by lazy { VisionRegex("^JJD\\d+", RegexType.TrackingNo) }
    val patternDHLTracking4BOcr: VisionRegex by lazy { VisionRegex("(?i)[/(]?(J)[/)]?[\\s]?(JD)(\\d{2}|\\d{0})[\\s]?(\\d{4}|\\d{2})[\\s]?(\\d{4})[\\s]?(\\d{4})[\\s]?(\\d{4})\\b", RegexType.TrackingNo) }
    val patternDHLTracking5: VisionRegex by lazy { VisionRegex("\\b(003\\d{17})\\b", RegexType.TrackingNo) }
    val patternDHLTracking6: VisionRegex by lazy { VisionRegex("\\bC1(003\\d{17})\\b", RegexType.TrackingNo) }
    val patternDHLTracking7: VisionRegex by lazy { VisionRegex("(?i)\\b(sendungsnummer:|sendungsnr:)[\\s|\\n]?(\\d{12})\\b", RegexType.TrackingNo) }

    val shipmentTypes: List<Pair<String, VisionRegex>> by lazy {
        listOf(
            "Express Worldwide" to VisionRegex("\\bEXPRESS[ ]{0,1}WORLDWIDE\\b", RegexType.Default),
            "Express Domestic" to VisionRegex("\\bEXPRESS[ ]{0,1}DOMESTIC\\b", RegexType.Default),
            "Express Envelope" to VisionRegex("\\bEXPRESS[ ]{0,1}ENVELOPE\\b", RegexType.Default),
            "Economy Select" to VisionRegex("\\bECONOMY[ ]{0,1}SELECT\\b", RegexType.Default),
            "Express" to VisionRegex("\\b(EXPRESS)[ ]{0,1}\\d{1,2}:\\b", RegexType.Default),
            "Domestic" to VisionRegex("\\b(DOMESTIC)[ ]{0,1}\\d{1,2}:\\b", RegexType.Default),
        )
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDHLPaket = patternDHLEurope.find(ocrExtractedText)
        val searchDHLWayBill = patternDHLWayBill.find(barcode)
        val searchDHLTracking2 = patternDHLTracking2.find(barcode)
        val searchDHLTracking3 = patternDHLTracking3.find(barcode)
        val searchDHLTracking4 = patternDHLTracking4.find(barcode)
        val searchDHLTracking6 = patternDHLTracking6.find(barcode)

        val searchDHLWayBillText = patternDHLWayBill.find(ocrExtractedText)
        val searchDHLTracking2Text = patternDHLTracking2.find(ocrExtractedText)

        if (searchDHLTracking2Text) {
            courierInfo.name =  "dhl"
            courierInfo.trackingNo = patternDHLTracking2.group(ocrExtractedText, 0).removeSpaces()
        }

        if (searchDHLTracking2 || searchDHLTracking3 || searchDHLTracking4 || searchDHLWayBill || searchDHLTracking6) {
            courierInfo.name =  "dhl"
            if (searchDHLTracking4) {
                courierInfo.trackingNo = patternDHLTracking4.group(barcode, 0).removeSpaces()
            } else if (searchDHLTracking2) {
                courierInfo.trackingNo = patternDHLTracking2.group(barcode, 0).removeSpaces()
            } else if (searchDHLTracking6) {
                if (searchDHLPaket) {
                    courierInfo.trackingNo = patternDHLTracking6.group(barcode, 1).removeSpaces()
                }
            }
        }

        if (searchDHLWayBillText) {
            courierInfo.name =  "dhl"
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchDHL = patternDHL.find(ocrExtractedText)
        val searchDHLWayBill = patternDHLWayBill.find(ocrExtractedText)
        val searchDHLTracking2 = patternDHLTracking2.find(ocrExtractedText)
        val searchDHLTracking4BOcr = patternDHLTracking4BOcr.find(ocrExtractedText)

        if (searchDHL) {
            courierInfo.name =  "dhl"
            if (searchDHLTracking2) {
                courierInfo.trackingNo = patternDHLTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        if (searchDHLTracking4BOcr) {
            courierInfo.name =  "dhl"
            val trackingNo = patternDHLTracking4BOcr
                .group(ocrExtractedText, 0)
                ?.replace("(", "")
                ?.replace(")", "")
                .removeSpaces()
            courierInfo.trackingNo = trackingNo
        }

        if (searchDHLWayBill) {
            courierInfo.name =  "dhl"
        }

        for ((shipmentName, regex) in shipmentTypes) {
            if (regex.find(ocrExtractedText)) {
                courierInfo.shipmentType = shipmentName
                break
            }
        }

        return RegexResult(courierInfo)
    }
}