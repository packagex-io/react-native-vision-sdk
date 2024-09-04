package io.packagex.visionsdk.exceptions

import io.packagex.visionsdk.core.DetectionMode

sealed class VisionSDKException(
    val errorCode: Int,
    val errorMessage: String,
    val detailedMessage: String = "",
    cause: Throwable? = null
) : java.lang.Exception(errorMessage, cause) {

    data object CameraUsageNotAuthorized : VisionSDKException(
        errorCode = 0,
        errorMessage = "Camera usage in not authorized"
    ) {
        private fun readResolve(): Any = CameraUsageNotAuthorized
    }

    data object NoTextDetected : VisionSDKException(
        errorCode = 1,
        errorMessage = "No text was detected"
    ) {
        private fun readResolve(): Any = NoTextDetected
    }

    data object NoBarcodeDetected : VisionSDKException(
        errorCode = 2,
        errorMessage = "No barcode was detected"
    ) {
        private fun readResolve(): Any = NoBarcodeDetected
    }

    data object NoQRCodeDetected : VisionSDKException(
        errorCode = 3,
        errorMessage = "No QRCode was detected"
    ) {
        private fun readResolve(): Any = NoQRCodeDetected
    }

    data object NoDocumentDetected : VisionSDKException(
        errorCode = 5,
        errorMessage = "No document was detected"
    ) {
        private fun readResolve(): Any = NoDocumentDetected
    }

    data object EnvironmentNotSet : VisionSDKException(
        errorCode = 12,
        errorMessage = "Environment not set"
    ) {
        private fun readResolve(): Any = EnvironmentNotSet
    }

    data object AuthorizationNotProvided : VisionSDKException(
        errorCode = 13,
        errorMessage = "Authorization not provided"
    ) {
        private fun readResolve(): Any = AuthorizationNotProvided
    }

    data object BillOfLadingAuthorizationNotProvided : VisionSDKException(
        errorCode = 14,
        errorMessage = "Bill of lading authorization not provided"
    ) {
        private fun readResolve(): Any = BillOfLadingAuthorizationNotProvided
    }

    data object RootDeviceDetected : VisionSDKException(
        errorCode = 15,
        errorMessage = "Your device is rooted. VisionSDK doesn't work on rooted devices."
    ) {
        private fun readResolve(): Any = RootDeviceDetected
    }

    data object OnDeviceOCRConfigurationFailed : VisionSDKException(
        errorCode = 16,
        errorMessage = "Failed to load model"
    ) {
        private fun readResolve(): Any = OnDeviceOCRConfigurationFailed
    }

    data object OnDeviceOCRDownloadingFailed : VisionSDKException(
        errorCode = 17,
        errorMessage = "Failed to load model"
    ) {
        private fun readResolve(): Any = OnDeviceOCRDownloadingFailed
    }

    class SdkInvalidPlatformException(errorMessage: String) : VisionSDKException(
        errorCode = 18,
        errorMessage = errorMessage
    )

    class SdkDisabledException(errorMessage: String) : VisionSDKException(
        errorCode = 19,
        errorMessage = errorMessage
    )

    class SdkInvalidModelException(errorMessage: String) : VisionSDKException(
        errorCode = 20,
        errorMessage = errorMessage
    )

    class SdkInvalidModelVersionException(errorMessage: String) : VisionSDKException(
        errorCode = 21,
        errorMessage = errorMessage
    )

    class SdkCloudDisabledException(errorMessage: String) : VisionSDKException(
        errorCode = 22,
        errorMessage = errorMessage
    )

    class SdkOnDeviceDisabledException(errorMessage: String) : VisionSDKException(
        errorCode = 23,
        errorMessage = errorMessage
    )

    class SdkProcessingDisabledException(errorMessage: String) : VisionSDKException(
        errorCode = 24,
        errorMessage = errorMessage
    )

    data object UserRestrictedException : VisionSDKException(
        errorCode = 25,
        errorMessage = "You are currently being restricted from using OnDeviceOCRManager. Please contact our admins at support@packagex.io."
    ) {
        private fun readResolve(): Any = UserRestrictedException
    }

    data object VitalInformationMissingException : VisionSDKException(
        errorCode = 26,
        errorMessage = "API did not provide vital information to proceed"
    ) {
        private fun readResolve(): Any = VitalInformationMissingException
    }

    data object InternetRequiredForOnDeviceOCRManager : VisionSDKException(
        errorCode = 27,
        errorMessage = "Internet is required to configure OnDeviceOCRManager"
    ) {
        private fun readResolve(): Any = VitalInformationMissingException
    }

    data object ImportantFilesNotFound : VisionSDKException(
        errorCode = 28,
        errorMessage = "Important files were not found. Please connect to internet and call configure function."
    ) {
        private fun readResolve(): Any = VitalInformationMissingException
    }

    data object CallStartCameraOrRescanBeforeCapture : VisionSDKException(
        errorCode = 29,
        errorMessage = "You need to call startCamera() or rescan() function before calling capture()"
    ) {
        private fun readResolve(): Any = CallStartCameraOrRescanBeforeCapture
    }

    class IllegalDetectionMode(detectionMode: DetectionMode) : VisionSDKException(
        errorCode = 30,
        errorMessage = "If detectionMode is set to ${detectionMode}, then this whole section should have been skipped. This is an issue in SDK and needs fixing immediately."
    )

    data object IllegalFileException : VisionSDKException(
        errorCode = 31,
        errorMessage = "The provided file was not an image file"
    ) {
        private fun readResolve(): Any = IllegalFileException
    }

    data object OnDeviceOCRManagerNotConfigured : VisionSDKException(
        errorCode = 32,
        errorMessage = "OnDeviceOCRManager is not configured yet"
    ) {
        private fun readResolve(): Any = OnDeviceOCRManagerNotConfigured
    }

    data object FocusRegionManagerNotAvailable : VisionSDKException(
        errorCode = 33,
        errorMessage = "FocusRegionManager will be available after camera is started"
    ) {
        private fun readResolve(): Any = FocusRegionManagerNotAvailable
    }

    data object ReportTextLengthExceeded : VisionSDKException(
        errorCode = 34,
        errorMessage = "Report is exceeding the character limit. (Max characters 1000)"
    ) {
        private fun readResolve(): Any = FocusRegionManagerNotAvailable
    }

    class UnknownException(cause: Throwable) : VisionSDKException(
        errorCode = -1,
        errorMessage = "Something went wrong",
        detailedMessage = cause.message ?: "Unknown exception",
        cause = cause
    )
}