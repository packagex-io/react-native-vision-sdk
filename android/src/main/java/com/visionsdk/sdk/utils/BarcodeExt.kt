package io.packagex.visionsdk.utils

import com.google.mlkit.vision.barcode.common.Barcode

private val oneDimensionalFormats = listOf(
    Barcode.FORMAT_CODABAR,
    Barcode.FORMAT_CODE_39,
    Barcode.FORMAT_CODE_93,
    Barcode.FORMAT_CODE_128,
    Barcode.FORMAT_EAN_8,
    Barcode.FORMAT_EAN_13,
    Barcode.FORMAT_ITF,
    Barcode.FORMAT_UPC_A,
    Barcode.FORMAT_UPC_E,
    Barcode.FORMAT_DATA_MATRIX,
    Barcode.FORMAT_AZTEC,
    Barcode.FORMAT_PDF417,
)

private val twoDimensionalFormats = listOf(
    Barcode.FORMAT_QR_CODE,
    /*Barcode.FORMAT_DATA_MATRIX,
    Barcode.FORMAT_AZTEC,
    Barcode.FORMAT_PDF417,*/
)

internal fun Barcode.isQRCode() = twoDimensionalFormats.contains(format)

internal fun Barcode.isOneDimensional() = oneDimensionalFormats.contains(format)