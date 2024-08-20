package io.packagex.visionsdk.interfaces

import com.google.mlkit.vision.text.Text
import io.packagex.visionsdk.analyzers.BarcodeResult

internal interface CameraXBarcodeCallback {
    fun barcodeIndicator(barcodes: List<BarcodeResult>)
    fun aggregatedBarcodeResults(barcodes: List<BarcodeResult>)
    fun onBarcodesScanned(barcodes: List<BarcodeResult>)
    fun onTextDetected(text: Text, width: Int, height: Int)
}

internal open class CameraXBarcodeCallbackAdapter : CameraXBarcodeCallback {
    override fun barcodeIndicator(barcodes: List<BarcodeResult>) {

    }

    override fun aggregatedBarcodeResults(barcodes: List<BarcodeResult>) {

    }

    override fun onBarcodesScanned(barcodes: List<BarcodeResult>) {

    }

    override fun onTextDetected(text: Text, width: Int, height: Int) {

    }
}