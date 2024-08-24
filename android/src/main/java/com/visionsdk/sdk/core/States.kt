package io.packagex.visionsdk.core

sealed interface ScanningMode {
    data object Manual : ScanningMode
    data object Auto : ScanningMode
}

sealed interface ConnectivityMode {
    data object Online : ConnectivityMode
    data object Offline : ConnectivityMode
}

sealed interface DetectionMode {
    data object Barcode : DetectionMode
    data object QRCode : DetectionMode
    data object OCR : DetectionMode
    data object PriceTag : DetectionMode
    data object Photo : DetectionMode
}

data class VisionViewState(
    val isMultipleScanEnabled: Boolean = false,
    val detectionMode: DetectionMode = DetectionMode.Barcode,
    val scanningMode: ScanningMode = ScanningMode.Manual,
    val isFlashTurnedOn: Boolean = false
)