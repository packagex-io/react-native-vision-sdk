package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class FedExCourier : Courier( listOf("") ) {

    val patternFedEx: VisionRegex by lazy { VisionRegex("(?i)\\b(FEDEX)\\b|[Ff]edex", RegexType.Default) }

    val patternFedExTracking2: VisionRegex by lazy { VisionRegex("\\b((98\\d{8,9}|98\\d{2}) ?\\d{4} ?\\d{4}( ?\\d{3})?)\\b", RegexType.TrackingNo) }
    val patternFedExTracking3: VisionRegex by lazy { VisionRegex(
        "^\\[\\)\\>[\\x1e](01)[\\x1d](?<zipcode>[^\\x1d]+)?[\\x1d](?<receiver_country>[^\\x1d]+)?[\\x1d]([^\\x1d]+)?[\\x1d](?<tracking_no>[^\\x1d]+)?[\\x1d](?<shipment_type>[^\\x1d]+)?[\\x1d]([^\\x1d]+)?[\\x1d]([^\\x1d]+)?[\\x1d]([^\\x1d]+)?[\\x1d]([^\\x1d]+)?[\\x1d](?<weight_info>[^\\x1d]+)?[\\x1d]([^\\x1d]+)?[\\x1d](?<receiver_address_line1>[^\\x1d]+)?[\\x1d](?<receiver_city>[^\\x1d]+)?[\\x1d](?<receiver_state>[^\\x1d]+)?[\\x1d](?<receiver_name>[^\\x1d\\x1e]+)?[\\x1e]([^\\x1d\\x1e]+)?[\\x1d]([^\\x1d]+)?[\\x1d]",
        RegexType.TrackingNo
    ) }
    val patternFedExTracking4: VisionRegex by lazy { VisionRegex("(\\d{4} \\d{4} \\d{4}( \\d{4})?)", RegexType.TrackingNo) }
    val patternFedExTracking4b: VisionRegex by lazy { VisionRegex("TRK#\\s?(\\d{4}\\s?\\d{4}\\s?\\d{4}(\\s?\\d{4})?)", RegexType.TrackingNo) }
    val patternFedExTracking5: VisionRegex by lazy { VisionRegex("^\\d{22}(?<tracking_no>\\d{12})[\\x1d]?(\$|\\b)", RegexType.TrackingNo) }
    val patternFedExTracking5b: VisionRegex by lazy { VisionRegex("Z\\d{22}(?<tracking_no>\\d{12})[\\x1d]?", RegexType.TrackingNo) }
    val patternFedExTracking6: VisionRegex by lazy { VisionRegex("(?:^|\\b)96\\d{5}(?<tracking_no>\\d{15})[\\x1d]?(\$|\\b)", RegexType.TrackingNo) }
    val patternFedExTracking6b: VisionRegex by lazy { VisionRegex("96\\d{5}(?<tracking_no>\\d{15})[\\x1d]?", RegexType.TrackingNo) }

    val shipmentTypes: List<Pair<String, VisionRegex>> by lazy {
        listOf(
            "Ground" to VisionRegex("(?i)Fedex(\\\\n|\\s)?Ground\\b", RegexType.Default),
            "Home Delivery" to VisionRegex("(?i)Fedex(\\\\n|\\s)?HOME[ ]{0,1}(DELIVERY)?\\b", RegexType.Default),
            "2Day" to VisionRegex("(?i)\\b(Fedex)?(\\\\n|\\s)?2DAY?\\b", RegexType.Default),
            "Standard Overnight" to VisionRegex("(?i)\\b(Fedex)?(\\\\n|\\s)?STANDARD[ ]{0,1}OVERNIGHT\\b", RegexType.Default),
            "Priority Overnight" to VisionRegex("(?i)\\b(Fedex)?(\\\\n|\\s)?PRIORITY[ ]{0,1}OVERNIGHT\\b", RegexType.Default),
            "First Overnight" to VisionRegex("(?i)\\b(Fedex)?(\\\\n|\\s)?FIRST[ ]{0,1}OVERNIGHT\\b", RegexType.Default),
            "Same Day" to VisionRegex("(?i)\\b(Fedex)?(\\\\n|\\s)?SAME[ ]{0,1}DAY\\b", RegexType.Default),
            "Express Saver" to VisionRegex("(?i)\\b(Fedex)?(\\\\n|\\s)?EXPRESS[ ]{0,1}SAVER", RegexType.Default),
        )
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val receiver = RegexResult.ExchangeInfo()

        val purolatorCourier = PurolatorCourier()
        val searchPurolator = purolatorCourier.patternPurolator.find(ocrExtractedText)

        val searchFedExTracking3 = patternFedExTracking3.find(barcode)
        val searchFedExTracking5 = patternFedExTracking5.find(barcode)
        val searchFedExTracking5b = patternFedExTracking5b.find(barcode)
        val searchFedExTracking6 = patternFedExTracking6.find(barcode)
        val searchFedExTracking2 = patternFedExTracking2.find(barcode)

        if (!searchPurolator && (searchFedExTracking2 || searchFedExTracking3 || searchFedExTracking5 || searchFedExTracking5b || searchFedExTracking6)) {
            courierInfo.name =  "fedex"

            if (searchFedExTracking5) {
                courierInfo.trackingNo = patternFedExTracking5.group(barcode, "tracking_no").removeSpaces()
            } else if (searchFedExTracking5b) {
                courierInfo.trackingNo = patternFedExTracking5b.group(barcode, "tracking_no").removeSpaces()
            } else if (searchFedExTracking2) {
                courierInfo.trackingNo = patternFedExTracking2.group(barcode, 0).removeSpaces()
            } else if (searchFedExTracking6) {
                courierInfo.trackingNo = patternFedExTracking6.group(barcode, "tracking_no").removeSpaces()
            } else if (searchFedExTracking3) {

                val zipcode = patternFedExTracking3.group(barcode, "zipcode")
                if (zipcode?.length == 7 || zipcode?.length == 8) {
                    receiver.zipcode = zipcode.substring(2)
                }

                val receiverCountry = patternFedExTracking3.group(barcode, "receiver_country")
                if (receiverCountry?.length == 3) {
                    if (receiverCountry.trim() == "840") {
                        receiver.country = "USA"
                    } else if (receiverCountry.trim() == "124") {
                        receiver.country = "CANADA"
                    } else if (receiverCountry.trim() == "156") {
                        receiver.country = "CHINA"
                    }
                }

                val trackingNo = patternFedExTracking3.group(barcode, "tracking_no").removeSpaces()
                if (trackingNo != null) {
                    if (trackingNo.length < 20) {
                        courierInfo.trackingNo = trackingNo
                        if (trackingNo.length > 12) {
                            courierInfo.trackingNo = trackingNo.replace("0201\$", "")
                        }
                    }
                }

                val shipmentType = patternFedExTracking3.group(barcode, "shipment_type")?.trim()
                if (shipmentType != null) {
                    if (shipmentType.length in 2..4 && shipmentType == "FDEG") {
                        courierInfo.shipmentType = "FEDEX GROUND"
                    }
                }

                val weightInfo = patternFedExTracking3.group(barcode, "weight_info")
                if (weightInfo != null && weightInfo.isNeitherNullNorEmptyNorBlank()) {
                    courierInfo.weight = weightInfo.trim()
                }

                val receiverAddressLine1 = patternFedExTracking3.group(barcode, "receiver_address_line1")
                if (receiverAddressLine1 != null && receiverAddressLine1.isNeitherNullNorEmptyNorBlank()) {
                    receiver.addressLine1 = receiverAddressLine1.trim().uppercase()
                }

                val receiverCity = patternFedExTracking3.group(barcode, "receiver_city")
                if (receiverCity != null && receiverCity.isNeitherNullNorEmptyNorBlank()) {
                    receiver.city = receiverCity.trim().uppercase()
                }

                val receiverState = patternFedExTracking3.group(barcode, "receiver_state")?.trim()
                if (receiverState?.length == 2) {
                    receiver.state = receiverState.uppercase()
                }

                val receiverName = patternFedExTracking3.group(barcode, "receiver_name")
                if (receiverName != null && receiverName.isNeitherNullNorEmptyNorBlank()) {
                    receiver.name = receiverName.trim().uppercase()
                }
            }
        }

        return RegexResult(courierInfo, receiver = receiver)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchFedEx = patternFedEx.find(ocrExtractedText)
        val searchFedExTracking6b = patternFedExTracking6b.find(ocrExtractedText)
        val searchFedExTracking2 = patternFedExTracking2.find(ocrExtractedText)
        val searchFedExTracking4 = patternFedExTracking4.find(ocrExtractedText)
        val searchFedExTracking4b = patternFedExTracking4b.find(ocrExtractedText)

        if (searchFedEx) {
            courierInfo.name = "fedex"
            if (searchFedExTracking2) {
                val trackingNo = patternFedExTracking2.group(ocrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchFedExTracking6b) {
                val trackingNo = patternFedExTracking6b.group(ocrExtractedText, "tracking_no").removeSpaces()
                courierInfo.trackingNo = trackingNo
            } else if (searchFedExTracking4) {
                val trackingNo = patternFedExTracking4.group(ocrExtractedText, 0).removeSpaces()
                if (trackingNo != null) {
                    var processedTrackingNo = trackingNo
                    if (processedTrackingNo.length > 12) {
                        if (processedTrackingNo.endsWith("0201") || processedTrackingNo.endsWith("0263")) {
                            processedTrackingNo = processedTrackingNo.substring(0 until 12)
                        } else {
                            processedTrackingNo = processedTrackingNo.substring(4)
                        }
                    }
                    courierInfo.trackingNo = processedTrackingNo
                }
            } else if (searchFedExTracking4b) {
                val trackingNo = patternFedExTracking4b.group(ocrExtractedText, 0)?.split("TRK#")
                val processedTrackingNo = trackingNo?.get(1).removeSpaces()
                courierInfo.trackingNo = processedTrackingNo
            }
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