package io.packagex.visionsdk.ocr.courier

import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex
import com.asadullah.handyutils.*

internal class CDLCourier : Courier() {

    val patternCDL: VisionRegex by lazy { VisionRegex("\\\\nCDL\\\\n[Ll]ast [Mm]ile", RegexType.Default) }
    val patternCDL1: VisionRegex by lazy { VisionRegex("(?i)\\b(CDL)(\\b|\$)", RegexType.Default) }
    val patternCDL2: VisionRegex by lazy { VisionRegex("(?i)(CDL GROUND)(\\b|\\\\n)", RegexType.Default) }
    val patternCDL3: VisionRegex by lazy { VisionRegex("(?i)(COLUMBUS distribution)(\\b)", RegexType.Default) }

    val patternCDLTracking1: VisionRegex by lazy { VisionRegex("(?i)\\b(CDDL\\d{8})(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking2: VisionRegex by lazy { VisionRegex("(?i)\\b(1CDL)[A-Za-z0-9]{11}(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking3: VisionRegex by lazy { VisionRegex("(?i)\\b(00886548009\\d{7})(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking4: VisionRegex by lazy { VisionRegex("(?i)\\b(31675\\d{7}AP1)(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking5: VisionRegex by lazy { VisionRegex("(?i)\\b(17321\\d{7})(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking6: VisionRegex by lazy { VisionRegex("(?i)\\b(9581EC\\d{7})(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking7: VisionRegex by lazy { VisionRegex("(?i)\\b(9213MW\\d{7})(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking8: VisionRegex by lazy { VisionRegex("(?i)\\b(YT221\\d{13})(\\b)", RegexType.TrackingNo) }
    val patternCDLTracking9: VisionRegex by lazy { VisionRegex("(?i)\\b(([A-Z]{2})CDL[\\s-]*(\\d{7}|\\d{8}))(\\b)", RegexType.TrackingNo) }

    override fun readFromBarcode(barcode: String, ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val searchCDLTracking1 = patternCDLTracking1.find(barcode)
        val searchCDLTracking2 = patternCDLTracking2.find(barcode)
        val searchCDLTracking3 = patternCDLTracking3.find(barcode)
        val searchCDLTracking4 = patternCDLTracking4.find(barcode)
        val searchCDLTracking5 = patternCDLTracking5.find(barcode)
        val searchCDLTracking6 = patternCDLTracking6.find(barcode)
        val searchCDLTracking7 = patternCDLTracking7.find(barcode)
        val searchCDLTracking8 = patternCDLTracking8.find(barcode)
        val searchCDLTracking9 = patternCDLTracking9.find(barcode)

        if (searchCDLTracking1) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking1.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking2) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking2.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking3) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking3.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking4) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking4.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking5) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking5.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking6) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking6.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking7) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking7.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking8) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking8.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        } else if (searchCDLTracking9) {

            courierInfo.name = "cdl"
            val trackingNo = patternCDLTracking9.group(barcode, 0).removeSpaces()
            courierInfo.trackingNo = trackingNo

        }

        return RegexResult(courierInfo)
    }

    override fun readFromOCR(ocrExtractedText: String?): RegexResult {

        val courierInfo = RegexResult.CourierInfo()

        val refinedOcrExtractedText = ocrExtractedText?.replace(" ", "")

        val searchCDL = patternCDL.find(refinedOcrExtractedText)
        val searchCDL1 = patternCDL1.find(refinedOcrExtractedText)
        val searchCDL2 = patternCDL2.find(refinedOcrExtractedText)
        val searchCDL3 = patternCDL3.find(refinedOcrExtractedText)

        val searchCDLTracking1 = patternCDLTracking1.find(refinedOcrExtractedText)
        val searchCDLTracking2 = patternCDLTracking2.find(refinedOcrExtractedText)
        val searchCDLTracking3 = patternCDLTracking3.find(refinedOcrExtractedText)
        val searchCDLTracking4 = patternCDLTracking4.find(refinedOcrExtractedText)
        val searchCDLTracking5 = patternCDLTracking5.find(refinedOcrExtractedText)
        val searchCDLTracking6 = patternCDLTracking6.find(refinedOcrExtractedText)
        val searchCDLTracking7 = patternCDLTracking7.find(refinedOcrExtractedText)
        val searchCDLTracking8 = patternCDLTracking8.find(refinedOcrExtractedText)
        val searchCDLTracking9 = patternCDLTracking9.find(refinedOcrExtractedText)

        if (searchCDL || searchCDL1 || searchCDL2 || searchCDL3) {
            courierInfo.name = "cdl"

            if (searchCDLTracking1) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking1.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking2) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking2.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking3) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking3.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking4) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking4.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking5) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking5.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking6) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking6.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking7) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking7.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking8) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking8.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            } else if (searchCDLTracking9) {

                courierInfo.name = "cdl"
                val trackingNo = patternCDLTracking9.group(refinedOcrExtractedText, 0).removeSpaces()
                courierInfo.trackingNo = trackingNo

            }
        }

        return RegexResult(courierInfo)
    }
}