package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class FastwayCourier : Courier() {

    val patternFastway: VisionRegex by lazy { VisionRegex("(fast( )?way)|FASTWAY", RegexType.Default) }
    val patternFastwayTracking1: VisionRegex by lazy { VisionRegex("(QH|AM|NR|FR|VR)(?<!\\d)\\d{10}(?!\\d)", RegexType.TrackingNo) }
    val patternFastwayTracking2: VisionRegex by lazy { VisionRegex("^(?<name>[^|]*)\\|([^|]*)\\|(?<building>[^|]*)\\|(?<address_line1>[^|]*)\\|(?<city>[^|]*)\\|(?<state>[^|]*)\\|(?<zipcode>[^|]*)\\|(?<phone_number>[^|]*)\\|(?<instructions>[^|]*)\\|([^|]*)\$", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val receiver = RegexResult.ExchangeInfo()

        val searchFastwayTracking1 = patternFastwayTracking1.find(barcode)
        val searchFastwayTracking2 = patternFastwayTracking2.find(barcode)

        val tollGroupCourier = TollGroupCourier()
        val searchTollGroupTracking1 = tollGroupCourier.patternTollGroupTracking1.find(barcode)

        if ((searchFastwayTracking1 || searchFastwayTracking2) && searchTollGroupTracking1.not()) {
            courierInfo.name = "fastway"
            if (searchFastwayTracking1) {
                courierInfo.trackingNo = patternFastwayTracking1.group(barcode, 0)
            } else if (searchFastwayTracking2) {

                val nameCandidate = patternFastwayTracking2.group(barcode, "name")
                    ?.replace(Regex("(?i)(ship( to)?|attn|name)[.:-]*"), "")
                    ?.trim()

                if (nameCandidate.isNeitherNullNorEmptyNorBlank()) {
                    receiver.name = nameCandidate
                }

                val addressLine1Candidate = patternFastwayTracking2.group(barcode, "address_line1")?.trim()
                if (addressLine1Candidate.isNeitherNullNorEmptyNorBlank()) {
                    receiver.addressLine1 = addressLine1Candidate
                }

                val addressLine2Candidate = patternFastwayTracking2.group(barcode, "building")?.trim()
                if (addressLine2Candidate.isNeitherNullNorEmptyNorBlank()) {
                    receiver.addressLine1 = addressLine2Candidate
                }

                val cityCandidate = patternFastwayTracking2.group(barcode, "city")?.trim()
                if (cityCandidate.isNeitherNullNorEmptyNorBlank()) {
                    receiver.city = cityCandidate
                }

                val stateCandidate = patternFastwayTracking2.group(barcode, "state")?.trim()
                if (stateCandidate.isNeitherNullNorEmptyNorBlank()) {
                    receiver.state = stateCandidate
                }

                val zipcodeCandidate = patternFastwayTracking2.group(barcode, "zipcode")?.trim()
                if (zipcodeCandidate.isNeitherNullNorEmptyNorBlank()) {
                    receiver.zipcode = zipcodeCandidate
                }
            }
        }

        return RegexResult(
            courier = courierInfo,
            receiver = receiver
        )
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchFastway = patternFastway.find(ocrExtractedText)
        val searchFastwayTracking1 = patternFastwayTracking1.find(ocrExtractedText)

        if (searchFastway) {
            courierInfo.name = "fastway"
            if (searchFastwayTracking1) {
                courierInfo.trackingNo = patternFastwayTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}