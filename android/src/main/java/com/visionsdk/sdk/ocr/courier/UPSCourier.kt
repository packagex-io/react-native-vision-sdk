package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class UPSCourier : Courier( listOf("") ) {

    val patternUPS: VisionRegex by lazy { VisionRegex("\\b(UPS)|(ups)\\b", RegexType.Default) }

    val patternUPSTracking1: VisionRegex by lazy { VisionRegex("\\b(1Z ?[0-9A-Za-z]{3} ?[0-9A-Za-z]{3} ?[0-9A-Za-z]{2} ?[0-9A-Za-z]{4} ?[0-9A-Za-z]{3} ?[0-9A-Za-z])\\b", RegexType.TrackingNo) }
    val patternUPSTracking2: VisionRegex by lazy { VisionRegex("\\b[kKJj]{1}[0-9]{10}\\b", RegexType.TrackingNo) }
    val patternUPSTracking1Z: VisionRegex by lazy { VisionRegex("\\b(1[z27] ?([0-9A-Za-z]{3} ?[0-9A-Za-z]{3} ?[0-9A-Za-z]{2} ?[0-9A-Za-z]{4} ?[0-9A-Za-z]{3} ?[0-9A-Za-z]))\\b", RegexType.TrackingNo) }

    val shipmentTypes: List<Pair<String, VisionRegex>> by lazy {
        listOf(
            "Ground" to VisionRegex("(?i)\\bUPS[ ]{0,1}GROUND\\b", RegexType.Default),
            "2nd Day Air A.M" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}2ND[ ]{0,1}DAY[ ]{0,1}AIR[ ]{0,1}A.?M\\b", RegexType.Default),
            "2nd Day Air" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}2ND[ ]{0,1}DAY[ ]{0,1}AIR\\b", RegexType.Default),
            "3 Day Select" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}3[ ]{0,1}DAY[ ]{0,1}SELECT\\b", RegexType.Default),
            "Mail Innovations" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}MAIL[ ]{0,1}INNOVATIONS\\b", RegexType.Default),
            "Next Day Air Saver" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}NEXT[ ]{0,1}DAY[ ]{0,1}AIR[ ]{0,1}SAVER\\b", RegexType.Default),
            "Next Day Air Early" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}NEXT[ ]{0,1}DAY[ ]{0,1}AIR[ ]{0,1}EARLY\\b", RegexType.Default),
            "Next Day Air" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}NEXT[ ]{0,1}DAY[ ]{0,1}AIR\\b", RegexType.Default),
            "Standard" to VisionRegex("(?i)\\bUPS[ ]{0,1}STANDARD\\b", RegexType.Default),
            "Saver" to VisionRegex("(?i)\\bUPS[ ]{0,1}SAVER\\b", RegexType.Default),
            "SurePost" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}SUREPOST\\b", RegexType.Default),
            "Expedited" to VisionRegex("(?i)\\bUPS[ ]{0,1}EXPEDITED\\b", RegexType.Default),
            "Day Definite, by end of the day" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}DAY[ ]{0,1}DEFINITE[ ,]{0,2}BY[ ]{0,1}END[ ]{0,1}OF[ ]{0,1}THE[ ]{0,1}DAY\\b", RegexType.Default),
            "World Wide Express Plus" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS[ ]{0,1}PLUS\\b", RegexType.Default),
            "World Wide Express Freight Midday" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS[ ]{0,1}FRIEGHT[ ]{0,1}MIDDAY\\b", RegexType.Default),
            "World Wide Express Freight" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS[ ]{0,1}FREIGHT\\b", RegexType.Default),
            "World Wide Express Saver" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS[ ]{0,1}SAVER\\b", RegexType.Default),
            "World Wide Express Expedited" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS[ ]{0,1}EXPEDITED\\b", RegexType.Default),
            "World Wide Express Economy" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS[ ]{0,1}ECONOMY\\b", RegexType.Default),
            "World Wide Express" to VisionRegex("(?i)\\b(UPS)?[ ]{0,1}WORLD[ ]{0,1}WIDE[ ]{0,1}EXPRESS\\b", RegexType.Default),
        )
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchUPSTracking1 = patternUPSTracking1.find(barcode)
        val searchUPSTracking2 = patternUPSTracking2.find(barcode)

        if (searchUPSTracking1 || searchUPSTracking2) {
            courierInfo.name = "ups"
            if (searchUPSTracking1) {
                val trackingNo = patternUPSTracking1.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchUPSTracking2) {
                val trackingNo = patternUPSTracking2.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        // Checking for Shipment Type in OCR Full Text
        for ((shipmentName, regex) in shipmentTypes) {
            if (regex.find(ocrExtractedText)) {
                courierInfo.shipmentType = shipmentName
                break
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchUPS = patternUPS.find(ocrExtractedText)
        val searchUPSTracking1 = patternUPSTracking1.find(ocrExtractedText)
        val searchUPSTracking2 = patternUPSTracking2.find(ocrExtractedText)
        val searchUPSTracking1Z = patternUPSTracking1Z.find(ocrExtractedText)

        if (searchUPS) {
            courierInfo.name = "ups"
            if (searchUPSTracking1) {
                val trackingNo = patternUPSTracking1.group(ocrExtractedText, 0).removeSpaces()
                if (trackingNo != null && trackingNo.isNeitherNullNorEmptyNorBlank()) {
                    courierInfo.trackingNo = trackingNo.replace("O", "0")
                }
            } else if (searchUPSTracking2) {
                val trackingNo = patternUPSTracking2.group(ocrExtractedText, 0).removeSpaces()
                if (trackingNo != null && trackingNo.isNeitherNullNorEmptyNorBlank()) {
                    courierInfo.trackingNo = trackingNo
                }
            } else if (searchUPSTracking1Z) {
                val trackingNo = patternUPSTracking1Z.group(ocrExtractedText, 2).removeSpaces()
                if (trackingNo != null && trackingNo.isNeitherNullNorEmptyNorBlank()) {
                    courierInfo.trackingNo = "1Z${trackingNo.replace("O", "0")}"
                }
            }
        }

        // Checking for Shipment Type in OCR Full Text
        for ((shipmentName, regex) in shipmentTypes) {
            if (regex.find(ocrExtractedText)) {
                courierInfo.shipmentType = shipmentName
                break
            }
        }

        return RegexResult(courierInfo)
    }
}