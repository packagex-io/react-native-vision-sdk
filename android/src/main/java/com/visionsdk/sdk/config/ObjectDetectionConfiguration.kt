package io.packagex.visionsdk.config

data class ObjectDetectionConfiguration(

    val isTextIndicationOn: Boolean = true,

    val isBarcodeOrQRCodeIndicationOn: Boolean = true,

    val isDocumentIndicationOn: Boolean = true,

    val secondsToWaitBeforeDocumentCapture: Int = 3,
)