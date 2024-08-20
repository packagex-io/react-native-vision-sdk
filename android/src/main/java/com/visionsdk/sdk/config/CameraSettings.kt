package io.packagex.visionsdk.config

data class CameraSettings(
    val nthFrameToProcess: Int = 10,
    val shouldAutoSaveCapturedImage: Boolean = false
)