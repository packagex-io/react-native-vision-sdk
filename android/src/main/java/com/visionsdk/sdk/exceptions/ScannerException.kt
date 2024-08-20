package io.packagex.visionsdk.exceptions

sealed class ScannerException(message: String) : Exception(message) {
    class QRCodeNotDetected(message: String = "No qrcode detected") : ScannerException(message)
    class BarcodeNotDetected(message: String = "No barcode detected") : ScannerException(message)
    class TextNotDetected(message: String = "No text detected") : ScannerException(message)
    class UnknownErrorDetected(message: String = "Unknown error detected") : ScannerException(message)
}