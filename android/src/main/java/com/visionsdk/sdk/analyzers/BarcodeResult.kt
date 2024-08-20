package io.packagex.visionsdk.analyzers

import com.google.mlkit.vision.barcode.common.Barcode

data class BarcodeResult(
    val barcode: Barcode,
    val imageWidth: Int,
    val imageHeight: Int
)