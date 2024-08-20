package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class CrawfordsDeliveryServicesCourier : Courier() {

    val patternCDS by lazy {
        VisionRegex("(?i)(\\bcds\\b)|(Crawfords Delivery Services)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCDS = patternCDS.find(ocrExtractedText)

        if (searchCDS) {
            courierInfo.name = "crawfords-delivery-services"
        }

        return RegexResult(courierInfo)
    }
}