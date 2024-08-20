package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class ChinaPost : Courier() {

    val patternChinaPost: VisionRegex by lazy { VisionRegex("(?i)\\b(China[ \\-]?Post|中国邮政)\\b", RegexType.Default) }
    val patternChinaPostOrigin: VisionRegex by lazy { VisionRegex("(?i)(^|\\b)(?<courier_tag>[A-Z]{2}) *( *[\\d]){9} *CN(\$|\\b)", RegexType.Default) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchChinaPostOrigin = patternChinaPostOrigin.find(barcode)

        if (searchChinaPostOrigin) {
            courierInfo.name = "china-post"
            val trackingNo = patternChinaPostOrigin.group(barcode, 0)
            courierInfo.trackingNo = trackingNo
            sender.country = "China"

            val courierTag = patternChinaPostOrigin.group(barcode, "courier_tag")
            if (courierTag.isNeitherNullNorEmptyNorBlank()) {
                val regex = VisionRegex("(?i)^E[A-Z]", RegexType.Default)
                val isChinaEMS = regex.find(courierTag)
                if (isChinaEMS) {
                    courierInfo.name = "china-ems"
                }
            }
        }

        return RegexResult(courierInfo, sender = sender)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()
        val sender = RegexResult.ExchangeInfo()

        val searchChinaPost = patternChinaPost.find(ocrExtractedText)
        val searchChinaPostOrigin = patternChinaPostOrigin.find(ocrExtractedText)

        if (searchChinaPost) {
            courierInfo.name = "china-post"
        }

        if (searchChinaPostOrigin) {
            courierInfo.name = "china-post"
            val trackingNo = patternChinaPostOrigin.group(ocrExtractedText, 0)
            courierInfo.trackingNo = trackingNo
            sender.country = "China"

            val courierTag = patternChinaPostOrigin.group(ocrExtractedText, "courier_tag")
            if (courierTag.isNeitherNullNorEmptyNorBlank()) {
                val regex = VisionRegex("(?i)^E[A-Z]", RegexType.Default)
                val isChinaEMS = regex.find(courierTag)
                if (isChinaEMS) {
                    courierInfo.name = "china-ems"
                }
            }
        }

        return RegexResult(courierInfo, sender = sender)
    }
}