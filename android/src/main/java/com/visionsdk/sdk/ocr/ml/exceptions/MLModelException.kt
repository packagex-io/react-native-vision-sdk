package io.packagex.visionsdk.ocr.ml.exceptions

internal sealed class MLModelException(message: String? = null, throwable: Throwable? = null) : Exception(message, throwable) {

    class InitializationFailed(message: String? = null, throwable: Throwable? = null) : MLModelException(message, throwable)
    class DownloadingFailed(message: String? = null, throwable: Throwable? = null) : MLModelException(message, throwable)
    data object RootDeviceDetected : MLModelException() {
        private fun readResolve(): Any = RootDeviceDetected
    }

    class UnknownErrorOccurred(message: String? = null, throwable: Throwable? = null) : MLModelException(message, throwable)
}