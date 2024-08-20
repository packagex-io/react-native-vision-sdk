package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class PurolatorCourier : Courier() {

    val patternPurolator: VisionRegex by lazy { VisionRegex("\\b(?i)(PUROLATOR)\\b", RegexType.Default) }

    val patternPurolatorTracking1: VisionRegex by lazy { VisionRegex("\\b33\\d{10}\\b", RegexType.TrackingNo) }
    val patternPurolatorTracking2: VisionRegex by lazy { VisionRegex("^\\d{11}(33\\d{10})\\d{11}\$", RegexType.TrackingNo) }
    val patternPurolatorTracking3: VisionRegex by lazy { VisionRegex(
        "^V01~(.*)\\|D01~(.*)\\|R01~(.*)\\|R02~(.*)\\|R03~(.*)\\|R04~(.*)\\|R05~(.*)\\|R06~(.*)\\|R07~(.*)\\|S01~(.*)\\|S02~(.*)\\|S03~(.*)\\|S04~(.*)\\|S05~(.*)\\|S06~(.*)\\|S08~(.*)\\|S09~(.*)\\|S10~(.*)\\|S11~(.*)\\|S12~(.*)\\|S13~(.*)\\|S14~(.*)\\|S15~(.*)\\|B01~(.*)\\|B02~(.*)\$",
        RegexType.TrackingNo
    ) }
    val patternPurolatorTracking4: VisionRegex by lazy { VisionRegex("^183080109\\d{6}(\\d{9})\\d{10}[\\\\]*\$", RegexType.TrackingNo) }
    val patternPurolatorTracking5: VisionRegex by lazy { VisionRegex("\\b(CWX|FGC)\\d{9}\\b", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()
        val receiver = RegexResult.ExchangeInfo()

        val searchPurolatorTracking2 = patternPurolatorTracking2.find(barcode)
        val searchPurolatorTracking3 = patternPurolatorTracking3.find(barcode)
        val searchPurolatorTracking1 = patternPurolatorTracking1.find(ocrExtractedText)
        val searchPurolatorTracking4 = patternPurolatorTracking4.find(barcode)
        val searchPurolatorTracking5 = patternPurolatorTracking5.find(ocrExtractedText)

        val searchFedEx = FedExCourier().patternFedEx.find(ocrExtractedText)

        if (
            searchFedEx.not()
            && (searchPurolatorTracking2 || searchPurolatorTracking3 || searchPurolatorTracking4 || searchPurolatorTracking1 || searchPurolatorTracking5)
        ) {
            courierInfo.name = "purolator"

            if (searchPurolatorTracking2) {
                val trackingNo = patternPurolatorTracking2.group(barcode, 1).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchPurolatorTracking1) {
                val trackingNo = patternPurolatorTracking1.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchPurolatorTracking5) {
                val trackingNo = patternPurolatorTracking5.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchPurolatorTracking4) {
                val trackingNo = patternPurolatorTracking4.group(barcode, 1).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchPurolatorTracking3) {
                sender.zipcode = patternPurolatorTracking3.group(barcode, 2)
                receiver.name = patternPurolatorTracking3.group(barcode, 3)
                receiver.addressLine1 = patternPurolatorTracking3.group(barcode, 6)
                receiver.addressLine2 = patternPurolatorTracking3.group(barcode, 7)
                receiver.city = patternPurolatorTracking3.group(barcode, 8)
                receiver.zipcode = patternPurolatorTracking3.group(barcode, 9)
                courierInfo.trackingNo = patternPurolatorTracking3.group(barcode, 10).removeSpaces()
                courierInfo.dateInfo = patternPurolatorTracking3.group(barcode, 17)
            }
        }

        return RegexResult(courier = courierInfo, receiver = receiver, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchPurolator = patternPurolator.find(ocrExtractedText)
        val searchPurolatorTracking1 = patternPurolatorTracking1.find(ocrExtractedText)
        val searchPurolatorTracking5 = patternPurolatorTracking5.find(ocrExtractedText)

        if (searchPurolator) {
            courierInfo.name = "purolator"
            if (searchPurolatorTracking1) {
                val trackingNo = patternPurolatorTracking1.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchPurolatorTracking5) {
                val trackingNo = patternPurolatorTracking5.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            }
        }

        return RegexResult(courierInfo)
    }
}