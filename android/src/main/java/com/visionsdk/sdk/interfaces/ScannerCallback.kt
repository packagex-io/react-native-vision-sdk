package io.packagex.visionsdk.interfaces

import android.graphics.Bitmap
import io.packagex.visionsdk.exceptions.VisionSDKException
import java.io.File

interface ScannerCallback {
    fun onIndications(
        barcodeDetected: Boolean,
        qrCodeDetected: Boolean,
        textDetected: Boolean,
        documentDetected: Boolean
    )
    fun onCodesScanned(barcodeList: List<String>)
    fun onFailure(exception: VisionSDKException)
    fun onImageCaptured(bitmap: Bitmap, value: List<String>)
}