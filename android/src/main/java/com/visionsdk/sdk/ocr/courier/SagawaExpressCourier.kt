package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class SagawaExpressCourier : Courier( listOf("") ) {

    val patternSagawaExpress by lazy {
        VisionRegex("(?i)(佐川|佐川急便|sagawa)", RegexType.Default)
    }

    val patternSagawaExpressTracking1 by lazy {
        VisionRegex("\\d{4}-\\d{4}-\\d{4}", RegexType.TrackingNo)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchSagawaExpress = patternSagawaExpress.find(ocrExtractedText)
        val searchSagawaExpressTracking1 = patternSagawaExpressTracking1.find(ocrExtractedText)

        if (searchSagawaExpress) {
            courierInfo.name = "sagawa-express"
            if (searchSagawaExpressTracking1) {
                courierInfo.trackingNo = patternSagawaExpressTracking1.group(ocrExtractedText, 0).removeSpaces()
            }
        }

        return RegexResult(courierInfo)
    }
}