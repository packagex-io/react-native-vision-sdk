package io.packagex.visionsdk.ocr.courier

import com.asadullah.handyutils.executeForTrueNullForFalse
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.removeSpaces
import io.packagex.visionsdk.analyzers.BarcodeResult
import io.packagex.visionsdk.ocr.regex.RegexType
import io.packagex.visionsdk.ocr.regex.VisionRegex

internal class LabelsExtraction(private val barcodeList: List<String>, private val ocrExtractedText: String) {

    fun extract(): RegexResult.ExtractedLabel {
        return RegexResult.ExtractedLabel(
            accountId = getAccountId(),
            rma = getRma(),
            purchaseOrderNumber = getPurchaseOrderNumber(),
            refNumber = getRefNumber(),
            invoiceNumber = getInvoiceNumber()
        )
    }

    private fun getAccountId(): String? {
        val accountIdRegex = VisionRegex("(?<![-])(?:^|\\b)\\d{2}( )?-( )?(\\d{1}|\\d{6,7})(?:\$|\\b)", RegexType.Default)
        val searchAccountIdRegex = accountIdRegex.find(ocrExtractedText)
        val accountId = searchAccountIdRegex.executeForTrueNullForFalse {
            accountIdRegex.group(ocrExtractedText, 0).removeSpaces()
        }

        if (accountId.isNeitherNullNorEmptyNorBlank()) {
            return accountId
        }

        for (barcode in barcodeList) {
            if (accountIdRegex.find(barcode)) {
                return accountIdRegex.group(barcode, 0).removeSpaces()
            }
        }

        return null
    }

    private fun getRma(): String? {
        val rmaRegex = VisionRegex("RMA[\\s#:]+(RMA)?[\\d|-]+([A-Z]+)?", RegexType.Default)
        val searchRmaRegex = rmaRegex.find(ocrExtractedText)

        if (searchRmaRegex.not()) {
            return null
        }

        var rma = rmaRegex.group(ocrExtractedText, 0).removeSpaces()
        val cleaner = VisionRegex("\\d.*\\d?", RegexType.Default)
        val searchCleaner = cleaner.find(rma)
        if (searchCleaner) {
            rma = cleaner.group(rma, 0).removeSpaces()
        }

        return rma
    }

    private fun getPurchaseOrderNumber(): String? {
        val poNumberRegex = VisionRegex("(([Pp][oO])|([Pp]urchase [Nn]o))[ #:]+[0-9a-zA-Z]+", RegexType.Default)
        val searchPoNumberRegex = poNumberRegex.find(ocrExtractedText)

        if (searchPoNumberRegex.not()) {
            return null
        }

        var poNumber = poNumberRegex.group(ocrExtractedText, 0).removeSpaces()
        val cleaner = VisionRegex("[#:]+.+[\\da-zA-Z]", RegexType.Default)
        val searchCleaner = cleaner.find(poNumber)
        if (searchCleaner) {
            poNumber = cleaner.group(poNumber, 0).removeSpaces()
        }

        return poNumber
    }

    private fun getRefNumber(): String? {
        val refNumberRegex = VisionRegex("(?i)((rec.ref)|(referenznr)|(referenz)|(ref code dn)|(ref ?.?code)|(re?f client)|(reference no)|(r1 client)|(reference)|(ref.? no)|(ref destinataire)|(ref)|(rif.? ?sped)|(rif.? consegna)|(rif.tel)|(rif))[ #:123.-]+[0-9a-zA-Z-_]+", RegexType.Default)
        val searchRefNumberRegex = refNumberRegex.find(ocrExtractedText)
        val refNumber = searchRefNumberRegex.executeForTrueNullForFalse {
            var refNumber = refNumberRegex.group(ocrExtractedText, 0).removeSpaces()
            val substituteRegex = VisionRegex("(?i)((rec.ref)|(referenznr)|(referenz)|(ref code dn)|(ref ?.?code)|(re?f client)|(reference no)|(r1 client)|(reference)|(ref.? no)|(ref destinataire)|(ref)|(rif.? ?sped)|(rif.? consegna)|(rif.tel)|(rif))", RegexType.Default)
            refNumber = substituteRegex.replaceAll(refNumber, "").removeSpaces()
            val cleaner = VisionRegex("[#:]+.+[\\da-zA-Z]", RegexType.Default)
            val searchCleaner = cleaner.find(refNumber)
            if (searchCleaner) {
                refNumber = cleaner.group(refNumber, 0).removeSpaces()
            }
            refNumber
        }

        return if (listOf('-', '.', '#', ':').contains(refNumber?.get(0))) {
            refNumber?.substring(1)
        } else {
            refNumber
        }
    }

    private fun getInvoiceNumber(): String? {
        val invoicePatterns = listOf(
            VisionRegex("(?i)(?<=inv: )(\\d+\\.)+\\d+", RegexType.Default),
            VisionRegex("(?i)(?<=inv: )order# \\d+ [a-z]+ \\d+", RegexType.Default),
            VisionRegex("(?i)(?<=inv: )\\d+(\\/[a-z0-9]+)+", RegexType.Default),
            VisionRegex("(?i)(?<=inv: )po:\\d+|(?<=inv: )pkg id: \\d+", RegexType.Default),
            VisionRegex("(?i)(?<=inv )po\\.\\d+\\/[a-z ]+", RegexType.Default),
            VisionRegex("(?i)(?<=inv )[a-z]+-\\d+-\\d+-\\d+", RegexType.Default),
            VisionRegex("(?i)(?<=inv )[a-z]+[ -]?([a-z0-9 ]+)?", RegexType.Default),
            VisionRegex("(?i)(?<=inv )(\\d+.\\d)*", RegexType.Default),
            VisionRegex("(?i)(?<=inv: )plan.\\d+-\\d+_dao", RegexType.Default),
            VisionRegex("(?i)(?<=invoice no\\.: )\\d+", RegexType.Default),
            VisionRegex("(?i)\binvoice number#?:?( )*([a-z0-9]+[- ]?)+\b(\\.\\d+)?", RegexType.Default),
            VisionRegex("(?i)invoice( )?#?(no)?(number)?\\.?:? ?[a-z0-9-]+ ?[a-z0-9-]+", RegexType.Default),
            VisionRegex("(?i)\binv#?:?( )*([a-z0-9]+[- ]?)+\b(\\.\\d+)?", RegexType.Default),
            VisionRegex("(?i)(?<=inv:) ?[a-z0-9- ]+", RegexType.Default),
        )

        for (invoicePattern in invoicePatterns) {
            if (invoicePattern.find(ocrExtractedText)) {
                val invoiceNumber = invoicePattern.group(ocrExtractedText, 0).removeSpaces()
                val pattern = VisionRegex("(?i)invoice\\s?#?(no)?(number)?\\.?:? ?|inv#:? ?|inv#?: ?", RegexType.Default)
                return pattern.replaceAll(invoiceNumber, "")
            }
        }

        return null
    }
}