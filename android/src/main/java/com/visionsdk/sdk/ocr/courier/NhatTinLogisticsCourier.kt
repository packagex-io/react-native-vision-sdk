package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class NhatTinLogisticsCourier : Courier( listOf("") ) {

    val patternNhatTinLogistics by lazy {
        VisionRegex("(?i)([www\\s\\.\\,]*(ntlogistics|nhan[\\s]*tin[\\s]*logistics)[\\s\\.\\,vn]*)", RegexType.Default)
    }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {
        return RegexResult()
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchNhatTinLogistics = patternNhatTinLogistics.find(ocrExtractedText)

        if (searchNhatTinLogistics) {
            courierInfo.name = "nhat-tin-logistics"
        }

        return RegexResult(courierInfo)
    }
}