package io.packagex.visionsdk.interfaces

import io.packagex.visionsdk.exceptions.VisionSDKException

interface OCRResult {
    fun onOCRResponse(response: String?)
    fun onOCRResponseFailed(visionException: VisionSDKException)
}