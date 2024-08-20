package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class RoyalMailCourier : Courier() {

    val patternRoyalMail by lazy {
        VisionRegex("(?i)(Royal[ \\-]?Mail)", RegexType.Default)
    }

    val patternGreatBritainOrigin by lazy {
        VisionRegex("(?i)(^|\\b)[A-Z]{2} *( *[\\d]){9} *GB(\$|\\b)", RegexType.TrackingNo)
    }

    val shipmentTypes: List<Pair<String, VisionRegex>> by lazy {
        listOf(
            "N-Class" to VisionRegex("\\b(?i)(\\d{1}[n|r|t][d|h][ ]{0,1}Class)\\b", RegexType.Default),
            "Tracked N" to VisionRegex("\\b(?i)(Tracked[ ]{0,1}(\\d{2}))\\b", RegexType.Default),
            "Royal Mail N" to VisionRegex("\\b((?i)ROYAL MAIL (\\d{1,2}))\\b", RegexType.Default),
        )
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchGreatBritainOrigin = patternGreatBritainOrigin.find(barcode)

        if (searchGreatBritainOrigin) {
            courierInfo.name = "royal-mail"
            courierInfo.trackingNo = patternGreatBritainOrigin.group(barcode, 0).removeSpaces()
            sender.country = "Great Britain"
        }

        return RegexResult(courier = courierInfo, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchRoyalMail = patternRoyalMail.find(ocrExtractedText)
        val searchGreatBritainOrigin = patternGreatBritainOrigin.find(ocrExtractedText)

        if (searchRoyalMail) {
            courierInfo.name = "royal-mail"
        }

        if (searchGreatBritainOrigin) {
            courierInfo.trackingNo = patternGreatBritainOrigin.group(ocrExtractedText, 0).removeSpaces()
        }

        for ((shipmentName, regex) in shipmentTypes) {
            if (regex.find(ocrExtractedText)) {
                if (shipmentName == "Tracked N" || shipmentName == "Royal Mail N") {
                    if (shipmentName == "Tracked N") {
                        courierInfo.shipmentType = buildString {
                            append("Tracked ")
                            append(regex.group(ocrExtractedText, 2).removeSpaces())
                            append(" (Ships within ")
                            append(regex.group(ocrExtractedText, 2).removeSpaces())
                            append(" hrs)")
                        }
                    } else {
                        courierInfo.shipmentType = buildString {
                            append("Royal Mail ")
                            append(regex.group(ocrExtractedText, 2).removeSpaces())
                            append(" (Ships within ")
                            append(regex.group(ocrExtractedText, 2).removeSpaces())
                            append(" hrs)")
                        }
                    }
                } else {
                    courierInfo.shipmentType = regex.group(ocrExtractedText, 1)
                }
                break
            }
        }

        return RegexResult(courierInfo)
    }
}