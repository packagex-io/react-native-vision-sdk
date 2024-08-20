package io.packagex.visionsdk.interfaces

interface OCRResult {
    fun onOCRResponse(response: String?)
    fun onOCRResponseFailed(throwable: Throwable?)
}