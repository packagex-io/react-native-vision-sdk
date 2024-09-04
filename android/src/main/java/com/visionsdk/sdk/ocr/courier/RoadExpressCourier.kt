package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class RoadExpressCourier : Courier( listOf("") ) {

    val patternRoadExpress by lazy {
        VisionRegex("(?i)(\\broad express\\b)", RegexType.Default)
    }

    val patternRoadExpressTracking1 by lazy {
        VisionRegex("642000\\d{12}0001", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchRoadExpressTracking1 = patternRoadExpressTracking1.find(barcode)

        if (searchRoadExpressTracking1) {
            courierInfo.name = "road-express"
            courierInfo.trackingNo = patternRoadExpressTracking1.group(barcode, 0).removeSpaces()
        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val searchTNT = TNTCourier().patternTNT.find(ocrExtractedText)
        if (searchTNT) {
            return RegexResult()
        }

        val courierInfo = RegexResult.CourierInfo()

        val searchRoadExpress = patternRoadExpress.find(ocrExtractedText)
        val searchRoadExpressTracking1 = patternRoadExpressTracking1.find(ocrExtractedText)

        if (searchRoadExpress || searchRoadExpressTracking1) {
            courierInfo.name = "road-express"
            if (searchRoadExpressTracking1) {
                courierInfo.trackingNo = patternRoadExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}