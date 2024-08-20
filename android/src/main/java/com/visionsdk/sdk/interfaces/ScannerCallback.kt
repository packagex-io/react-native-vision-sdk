package io.packagex.visionsdk.interfaces

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.common.Barcode
import io.packagex.visionsdk.analyzers.BarcodeResult
import io.packagex.visionsdk.exceptions.ScannerException
import java.io.File

interface ScannerCallback {
    fun detectionCallbacks(
        barcodeDetected: Boolean,
        qrCodeDetected: Boolean,
        textDetected: Boolean,
        documentDetected: Boolean
    )
    fun onBarcodesDetected(barcodeList: List<BarcodeResult>)
    fun onFailure(exception: ScannerException)
    fun onImageCaptured(bitmap: Bitmap, imageFile: File?, value: List<BarcodeResult>)
}