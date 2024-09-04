package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class CanparCourier : Courier( listOf("canpar") ) {

    val patternCanpar: VisionRegex by lazy { VisionRegex("(?i)([Cc]anpar|TransForce)", RegexType.Default) }
    val patternCanparTracking1: VisionRegex by lazy { VisionRegex("D[ 0-9]{17,25}01", RegexType.TrackingNo) }
    val patternCanparTracking2: VisionRegex by lazy { VisionRegex("(?i)(D|S)4\\d{2}\\d{3}\\d{2}(\\d{3}\\d{3}\\d{3}\\d{3})\$", RegexType.TrackingNo) }
    val patternCanparTracking3: VisionRegex by lazy { VisionRegex("(?<!\\d)\\*111\\d{13}(?!\\d)", RegexType.TrackingNo) }

    val shipmentTypes: List<Pair<String, VisionRegex>> by lazy {
        listOf(
            "Express" to VisionRegex("EXPRESS", RegexType.Default),
            "Ground" to VisionRegex("GROUND", RegexType.Default),
            "Home Delivery" to VisionRegex("GUARANTEED", RegexType.Default),
            "select" to VisionRegex("SELECT", RegexType.Default),
            "mid-day" to VisionRegex("MID-DAY", RegexType.Default),
        )
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCanparTracking1 = patternCanparTracking1.find(barcode)
        val searchCanparTracking2 = patternCanparTracking2.find(barcode)
        val searchCanparTracking3 = patternCanparTracking3.find(barcode)
        val searchOfficeDepotTracking1 = OfficeDepotCourier().patternTracking1.find(barcode)

        if ((searchCanparTracking1 && searchOfficeDepotTracking1.not()) || searchCanparTracking2 || searchCanparTracking3) {

            courierInfo.name = "canpar"

            if (searchCanparTracking1) {
                val trackingNo = patternCanparTracking1.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanparTracking2) {
                val trackingNo = patternCanparTracking2.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanparTracking3) {
                val trackingNo = patternCanparTracking3.group(barcode, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCanpar = patternCanpar.find(ocrExtractedText)
        val searchCanparTracking1 = patternCanparTracking1.find(ocrExtractedText)
        val searchCanparTracking2 = patternCanparTracking2.find(ocrExtractedText)
        val searchCanparTracking3 = patternCanparTracking3.find(ocrExtractedText)

        if (searchCanpar || (searchCanpar && searchCanparTracking1) || searchCanparTracking2 || searchCanparTracking3) {

            courierInfo.name = "canpar"

            if (searchCanparTracking1) {
                val trackingNo = patternCanparTracking1.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanparTracking2) {
                val trackingNo = patternCanparTracking2.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchCanparTracking3) {
                val trackingNo = patternCanparTracking3.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }

            for ((shipmentName, regex) in shipmentTypes) {
                if (regex.find(ocrExtractedText)) {
                    courierInfo.shipmentType = shipmentName
                    break
                }
            }
        }

        return RegexResult(courierInfo)
    }
}