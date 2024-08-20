package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class ParcelForceCourier : Courier() {

    val patternParcelForce by lazy {
        VisionRegex("(?i)\\b(parcel)?((force)|(foce))\\b", RegexType.Default)
    }

    val patternParcelForceTracking1 by lazy {
        VisionRegex("(?i)(\\b|^)(P\\s*B\\s*[A-Z0-9]\\s*[A-Z0-9]{8}\\s*[A-Z0-9]{3})(\\b|\$)", RegexType.TrackingNo)
    }

    val patternParcelForceTracking2 by lazy {
        VisionRegex("(?i)(\\b|^)(P\\s*B\\s*[A-Z0-9]\\s*[A-Z0-9]{8})(\\b|\$)", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchParcelForceTracking1 = patternParcelForceTracking1.find(barcode)
        val searchParcelForceTracking2 = patternParcelForceTracking2.find(barcode)

        if (searchParcelForceTracking1 || searchParcelForceTracking2) {
            courierInfo.name = "parcel-force"
            if (searchParcelForceTracking1) {
                courierInfo.trackingNo = patternParcelForceTracking1.group(barcode, 0).removeSpaces()
            } else if (searchParcelForceTracking2) {
                courierInfo.trackingNo = patternParcelForceTracking2.group(barcode, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchParcelForce = patternParcelForce.find(ocrExtractedText)
        val searchParcelForceTracking1 = patternParcelForceTracking1.find(ocrExtractedText)
        val searchParcelForceTracking2 = patternParcelForceTracking2.find(ocrExtractedText)

        if (searchParcelForceTracking1 || searchParcelForceTracking2 || searchParcelForce) {
            courierInfo.name = "parcel-force"
            if (searchParcelForceTracking1) {
                courierInfo.trackingNo = patternParcelForceTracking1.group(ocrExtractedText, 0).removeSpaces()
            } else if (searchParcelForceTracking2) {
                courierInfo.trackingNo = patternParcelForceTracking2.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}