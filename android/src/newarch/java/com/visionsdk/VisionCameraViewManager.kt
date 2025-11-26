package com.visionsdk

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.dto.ScannedCodeResult
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.ui.views.VisionCameraView
import com.visionsdk.utils.toDp

/**
 * Fabric-compatible ViewManager for VisionCameraView
 * This wraps the actual VisionCameraView from the VisionSDK
 *
 * Note: On Android, both VisionCameraView and VisionSdkView use the same
 * underlying VisionCameraView from the SDK.
 */
@ReactModule(name = VisionCameraViewManager.REACT_CLASS)
class VisionCameraViewManager(private val appContext: ReactApplicationContext) :
    ViewGroupManager<VisionCameraView>() {

    companion object {
        const val REACT_CLASS = "VisionCameraView"
        const val TAG = "VisionCameraView Fabric"
    }

    private var visionCameraView: VisionCameraView? = null
    private var hasStarted = false
    private var currentCallback: ViewCallback? = null
    private var isCameraReady = false
    private var pendingScanArea: com.facebook.react.bridge.ReadableMap? = null
    private var hasScanAreaBeenSet = false // Track if scanArea prop was explicitly set
    private var currentDetectionMode: DetectionMode = DetectionMode.Photo // Track current detection mode
    private val density = appContext.resources.displayMetrics.density

    // Event throttling - timestamps for last emitted events
    private var lastRecognitionUpdateTime = 0L
    private var lastBoundingBoxesUpdateTime = 0L
    private var lastSharpnessScoreUpdateTime = 0L

    // Throttle intervals in milliseconds
    private val RECOGNITION_UPDATE_THROTTLE_MS = 100L // 10 FPS
    private val BOUNDING_BOXES_UPDATE_THROTTLE_MS = 150L // ~6.7 FPS (heavier payload)
    private val SHARPNESS_SCORE_UPDATE_THROTTLE_MS = 200L // 5 FPS

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): VisionCameraView {
        Log.d(TAG, "createViewInstance")

        val activity = appContext.currentActivity as? androidx.fragment.app.FragmentActivity
            ?: throw IllegalStateException("Activity must be a FragmentActivity")

        val newView = VisionCameraView(activity, null)

        // Set layout parameters to ensure view is visible
        newView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Initialize with default settings
        newView.configure(
            isMultipleScanEnabled = false,
            detectionMode = DetectionMode.Photo,
            scanningMode = ScanningMode.Manual
        )

        // Create a new callback instance for this view
        val callback = ViewCallback(newView, appContext)
        newView.setCameraLifecycleCallback(callback)
        newView.setScannerCallback(callback)

        // Update the current view reference and callback
        visionCameraView = newView
        currentCallback = callback

        Log.d(TAG, "VisionCameraView created and configured (id: ${newView.id})")

        return newView
    }

    override fun onAfterUpdateTransaction(view: VisionCameraView) {
        super.onAfterUpdateTransaction(view)

        // Request layout to ensure proper sizing
        view.requestLayout()

        // Only start camera once
        if (visionCameraView == view && !hasStarted) {
            hasStarted = true
            Log.d(TAG, "Starting camera for view id: ${view.id}")

            // Wait for view to be attached to window
            if (view.isAttachedToWindow) {
                view.post {
                    Log.d(TAG, "Starting camera with view dimensions: ${view.width}x${view.height}")
                    view.startCamera()
                }
            } else {
                view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        Log.d(TAG, "View attached to window, starting camera")
                        view.removeOnAttachStateChangeListener(this)
                        view.post {
                            Log.d(TAG, "Starting camera with view dimensions: ${view.width}x${view.height}")
                            view.startCamera()
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        // No-op
                    }
                })
            }
        }
    }

    override fun onDropViewInstance(view: VisionCameraView) {
        super.onDropViewInstance(view)
        Log.d(TAG, "Dropping view instance with id: ${view.id}")

        // Only reset if this is the currently active view
        if (visionCameraView == view) {
            Log.d(TAG, "Resetting state for currently active view")
            hasStarted = false
            visionCameraView = null
            currentCallback = null
        }

        // Stop the camera
        try {
            view.stopCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera: ${e.message}")
        }
    }

    /**
     * Check if enough time has passed since the last event emission to throttle high-frequency events
     * @param lastTime The timestamp of the last emission
     * @param throttleMs The throttle interval in milliseconds
     * @return true if the event should be emitted, false if it should be skipped
     */
    private fun shouldEmitThrottledEvent(lastTime: Long, throttleMs: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTime) >= throttleMs
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "onCapture" to mapOf("registrationName" to "onCapture"),
            "onError" to mapOf("registrationName" to "onError"),
            "onRecognitionUpdate" to mapOf("registrationName" to "onRecognitionUpdate"),
            "onSharpnessScoreUpdate" to mapOf("registrationName" to "onSharpnessScoreUpdate"),
            "onBarcodeDetected" to mapOf("registrationName" to "onBarcodeDetected"),
            "onBoundingBoxesUpdate" to mapOf("registrationName" to "onBoundingBoxesUpdate")
        )
    }

    // MARK: - Props

    @ReactProp(name = "enableFlash")
    fun setEnableFlash(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setEnableFlash: $enabled")
        view.setFlashTurnedOn(enabled)
    }

    @ReactProp(name = "zoomLevel")
    fun setZoomLevel(view: VisionCameraView, level: Double) {
        Log.d(TAG, "setZoomLevel: $level")
        view.setZoomRatio(level.toFloat())
    }

    @ReactProp(name = "scanMode")
    fun setScanMode(view: VisionCameraView, mode: String?) {
        Log.d(TAG, "setScanMode: $mode")
        val detectionMode = when (mode?.lowercase()) {
            "ocr" -> DetectionMode.OCR
            "barcode" -> DetectionMode.Barcode
            "qrcode" -> DetectionMode.QRCode
            "photo" -> DetectionMode.Photo
            "barcodeorqrcode" -> DetectionMode.BarcodeOrQRCode
            else -> DetectionMode.Barcode
        }
        currentDetectionMode = detectionMode // Track current mode
        view.setDetectionMode(detectionMode)
    }

    @ReactProp(name = "autoCapture")
    fun setAutoCapture(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setAutoCapture: $enabled")
        val scanningMode = if (enabled) ScanningMode.Auto else ScanningMode.Manual
        view.setScanningMode(scanningMode)
    }

    @ReactProp(name = "cameraFacing")
    fun setCameraFacing(view: VisionCameraView, facing: String?) {
        Log.d(TAG, "setCameraFacing: $facing")
        // TODO: Implement camera facing/position switching for Android
        // This will require updating the VisionSDK Android implementation
        // to support CameraPosition enum (similar to iOS)
        Log.d(TAG, "Camera facing prop received: $facing (Android implementation pending)")
    }

    @ReactProp(name = "frameSkip")
    fun setFrameSkip(view: VisionCameraView, skip: Int) {
        Log.d(TAG, "setFrameSkip: $skip")
        // Frame skip would be configured via CameraSettings
    }

    @ReactProp(name = "scanAreaJson")
    fun setScanArea(view: VisionCameraView, scanAreaJson: String?) {
        Log.d(TAG, "setScanArea: $scanAreaJson")

        // Parse JSON string to ReadableMap
        val scanArea = if (!scanAreaJson.isNullOrEmpty()) {
            try {
                val jsonObject = org.json.JSONObject(scanAreaJson)
                com.facebook.react.bridge.Arguments.makeNativeMap(
                    mapOf(
                        "x" to jsonObject.getDouble("x"),
                        "y" to jsonObject.getDouble("y"),
                        "width" to jsonObject.getDouble("width"),
                        "height" to jsonObject.getDouble("height")
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse scanArea JSON: ${e.message}")
                null
            }
        } else {
            null
        }

        // Store the scanArea for later application
        pendingScanArea = scanArea
        hasScanAreaBeenSet = true // Mark that scanArea has been explicitly set

        // Only apply if camera is ready
        if (!isCameraReady) {
            Log.d(TAG, "Camera not ready, storing scanArea for later application")
            return
        }

        applyScanArea(view, scanArea)
    }

    private fun applyScanArea(view: VisionCameraView, scanArea: com.facebook.react.bridge.ReadableMap?) {
        try {
            // Skip FocusSettings only for Photo mode (OCR mode works fine with scan areas)
            if (currentDetectionMode == DetectionMode.Photo) {
                Log.d(TAG, "Skipping scan area application in Photo mode")
                return
            }

            if (scanArea != null) {
                // When scan area is defined, disable multiple scan mode
                view.setMultipleScanEnabled(false)

                val x = scanArea.getDouble("x")
                val y = scanArea.getDouble("y")
                val width = scanArea.getDouble("width")
                val height = scanArea.getDouble("height")

                val xPx = (x * density).toFloat()
                val yPx = (y * density).toFloat()
                val widthPx = (width * density).toFloat()
                val heightPx = (height * density).toFloat()

                val focusRect = android.graphics.RectF(xPx, yPx, xPx + widthPx, yPx + heightPx)
                val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                    context = appContext,
                    shouldScanInFocusImageRect = true,
                    focusImageRect = focusRect,
                    showCodeBoundariesInMultipleScan = false,
                    showDocumentBoundaries = false
                )
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "Scan area applied - single scan mode enabled with focus rect: $focusRect")
            } else {
                // If no scan area, enable multiple scan mode
                view.setMultipleScanEnabled(true)

                // Create FocusSettings without shouldScanInFocusImageRect or focusImageRect
                // This matches the oldarch implementation
                val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                    context = appContext,
                    showCodeBoundariesInMultipleScan = false,
                    showDocumentBoundaries = false
                )
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "No scan area - multiple scan mode enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply scanArea: ${e.message}", e)
        }
    }

    @ReactProp(name = "detectionConfigJson")
    fun setDetectionConfig(view: VisionCameraView, configJson: String?) {
        Log.d(TAG, "setDetectionConfig")
        // Parse JSON and configure ObjectDetectionConfiguration
        // TODO: Implement using ObjectDetectionConfiguration
    }

    // MARK: - Commands

    override fun getCommandsMap(): Map<String, Int>? {
        return mapOf(
            "capture" to 0,
            "stop" to 1,
            "start" to 2,
            "toggleFlash" to 3,
            "setZoom" to 4
        )
    }

    override fun receiveCommand(
        root: VisionCameraView,
        commandId: Int,
        args: ReadableArray?
    ) {
        Log.d(TAG, "receiveCommand called with commandId: $commandId, args: $args")

        // Handle numeric command IDs (Fabric uses integers)
        when (commandId) {
            0 -> capture(root)
            1 -> stop(root)
            2 -> start(root)
            3 -> {
                val enabled = args?.getBoolean(0) ?: false
                toggleFlash(root, enabled)
            }
            4 -> {
                val level = args?.getDouble(0) ?: 1.0
                setZoomCommand(root, level)
            }
            else -> Log.w(TAG, "Unknown command: $commandId")
        }
    }

    private fun capture(view: VisionCameraView) {
        Log.d(TAG, "capture called (mode: $currentDetectionMode)")

        // In Photo mode, just capture directly - don't call rescan
        // The camera is already running and ready to capture
        view.capture()
    }

    private fun stop(view: VisionCameraView) {
        Log.d(TAG, "stop called")
        view.stopCamera()
    }

    private fun start(view: VisionCameraView) {
        Log.d(TAG, "start called")
        view.startCamera()
    }

    private fun toggleFlash(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "toggleFlash called with enabled: $enabled")
        view.setFlashTurnedOn(enabled)
    }

    private fun setZoomCommand(view: VisionCameraView, level: Double) {
        Log.d(TAG, "setZoom called with level: $level")
        view.setZoomRatio(level.toFloat())
    }

    // MARK: - ViewCallback inner class
    inner class ViewCallback(
        private val view: VisionCameraView,
        private val context: ReactApplicationContext
    ) : ScannerCallback, CameraLifecycleCallback {

        private fun sendEvent(eventName: String, params: com.facebook.react.bridge.WritableMap) {
            if (view.isAttachedToWindow) {
                Log.d(TAG, "Sending event: $eventName to view ${view.id}")
                try {
                    context.getJSModule(RCTEventEmitter::class.java)
                        .receiveEvent(view.id, eventName, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending event $eventName: ${e.message}")
                }
            } else {
                Log.w(TAG, "Cannot send event $eventName - view is not attached")
            }
        }

        // ScannerCallback implementation
        override fun onScanResult(barcodeList: List<ScannedCodeResult>) {
            Log.d(TAG, "onScanResult called with ${barcodeList.size} codes")
            val event = Arguments.createMap()
            val codesArray = Arguments.createArray()

            for (code in barcodeList) {
                Log.d(TAG, "Barcode detected: ${code.scannedCode}, symbology: ${code.symbology}")
                val codeMap = Arguments.createMap()
                codeMap.putString("scannedCode", code.scannedCode)
                codeMap.putString("symbology", code.symbology.toString())

                val boundingBox = Arguments.createMap()
                code.boundingBox?.let { box ->
                    boundingBox.putDouble("x", box.left.toDouble())
                    boundingBox.putDouble("y", box.top.toDouble())
                    boundingBox.putDouble("width", box.width().toDouble())
                    boundingBox.putDouble("height", box.height().toDouble())
                }
                codeMap.putMap("boundingBox", boundingBox)

                if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                    val gs1Map = Arguments.createMap()
                    code.gs1ExtractedInfo?.forEach { (key, value) ->
                        gs1Map.putString(key, value)
                    }
                    codeMap.putMap("gs1ExtractedInfo", gs1Map)
                }

                codesArray.pushMap(codeMap)
            }

            event.putArray("codes", codesArray)
            sendEvent("onBarcodeDetected", event)

            // Automatically restart scanning after barcode detection
            visionCameraView?.rescan()
        }

        override fun onFailure(exception: VisionSDKException) {
            val event = Arguments.createMap()
            event.putString("message", exception.message ?: "Unknown error")
            event.putInt("code", exception.errorCode ?: -1)
            sendEvent("onError", event)

            // Delay rescan slightly to avoid immediate recursion
            view.postDelayed({
                visionCameraView?.rescan()
            }, 100)
        }

        override fun onIndications(
            barcodeDetected: Boolean,
            qrCodeDetected: Boolean,
            textDetected: Boolean,
            documentDetected: Boolean
        ) {
            // Throttle recognition updates to avoid overwhelming the JS bridge
            if (!shouldEmitThrottledEvent(lastRecognitionUpdateTime, RECOGNITION_UPDATE_THROTTLE_MS)) {
                return
            }
            lastRecognitionUpdateTime = System.currentTimeMillis()

            Log.d(TAG, "onIndications - barcode: $barcodeDetected, qr: $qrCodeDetected, text: $textDetected, doc: $documentDetected")
            val event = Arguments.createMap()
            event.putBoolean("text", textDetected)
            event.putBoolean("barcode", barcodeDetected)
            event.putBoolean("qrcode", qrCodeDetected)
            event.putBoolean("document", documentDetected)
            sendEvent("onRecognitionUpdate", event)
        }

        override fun onIndicationsBoundingBoxes(
            barcodeBoundingBoxes: List<ScannedCodeResult>,
            qrCodeBoundingBoxes: List<ScannedCodeResult>,
            documentBoundingBox: android.graphics.Rect?
        ) {
            // Throttle bounding box updates (heavier payload, more processing)
            if (!shouldEmitThrottledEvent(lastBoundingBoxesUpdateTime, BOUNDING_BOXES_UPDATE_THROTTLE_MS)) {
                return
            }
            lastBoundingBoxesUpdateTime = System.currentTimeMillis()

            Log.d(TAG, "onIndicationsBoundingBoxes - barcodes: ${barcodeBoundingBoxes.size}, qrCodes: ${qrCodeBoundingBoxes.size}, document: ${documentBoundingBox != null}")
            val event = Arguments.createMap()

            // Convert barcode bounding boxes
            val barcodeBoxesArray = Arguments.createArray()
            barcodeBoundingBoxes.forEach { code ->
                val boxMap = Arguments.createMap()
                boxMap.putString("scannedCode", code.scannedCode)
                boxMap.putString("symbology", code.symbology.toString())

                if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                    val gs1Map = Arguments.createMap()
                    code.gs1ExtractedInfo?.forEach { (key, value) ->
                        gs1Map.putString(key, value)
                    }
                    boxMap.putMap("gs1ExtractedInfo", gs1Map)
                } else {
                    boxMap.putMap("gs1ExtractedInfo", Arguments.createMap())
                }

                boxMap.putMap("boundingBox", Arguments.createMap().apply {
                    code.boundingBox?.let { box ->
                        putInt("x", box.left.toDp(density))
                        putInt("y", box.top.toDp(density))
                        putInt("width", box.width().toDp(density))
                        putInt("height", box.height().toDp(density))
                    }
                })
                barcodeBoxesArray.pushMap(boxMap)
            }
            event.putArray("barcodeBoundingBoxes", barcodeBoxesArray)

            // Convert QR code bounding boxes
            val qrCodeBoxesArray = Arguments.createArray()
            qrCodeBoundingBoxes.forEach { code ->
                val boxMap = Arguments.createMap()
                boxMap.putString("scannedCode", code.scannedCode)
                boxMap.putString("symbology", code.symbology.toString())

                if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                    val gs1Map = Arguments.createMap()
                    code.gs1ExtractedInfo?.forEach { (key, value) ->
                        gs1Map.putString(key, value)
                    }
                    boxMap.putMap("gs1ExtractedInfo", gs1Map)
                } else {
                    boxMap.putMap("gs1ExtractedInfo", Arguments.createMap())
                }

                boxMap.putMap("boundingBox", Arguments.createMap().apply {
                    code.boundingBox?.let { box ->
                        putInt("x", box.left.toDp(density))
                        putInt("y", box.top.toDp(density))
                        putInt("width", box.width().toDp(density))
                        putInt("height", box.height().toDp(density))
                    }
                })
                qrCodeBoxesArray.pushMap(boxMap)
            }
            event.putArray("qrCodeBoundingBoxes", qrCodeBoxesArray)

            // Convert document bounding box
            documentBoundingBox?.let { box ->
                val boxMap = Arguments.createMap()
                boxMap.putInt("x", box.left.toDp(density))
                boxMap.putInt("y", box.top.toDp(density))
                boxMap.putInt("width", box.width().toDp(density))
                boxMap.putInt("height", box.height().toDp(density))
                event.putMap("documentBoundingBox", boxMap)
            }

            sendEvent("onBoundingBoxesUpdate", event)
        }

        override fun onItemRetrievalResult(scannedCodeResults: ScannedCodeResult) {
            // Not used in minimal implementation
        }

        override fun onPriceTagResult(priceTagData: io.packagex.visionsdk.core.pricetag.PriceTagData) {
            // Not used in minimal implementation
        }

        override fun onImageSharpnessScore(imageSharpnessScore: Double) {
            // Throttle sharpness score updates
            if (!shouldEmitThrottledEvent(lastSharpnessScoreUpdateTime, SHARPNESS_SCORE_UPDATE_THROTTLE_MS)) {
                return
            }
            lastSharpnessScoreUpdateTime = System.currentTimeMillis()

            Log.d(TAG, "onImageSharpnessScore called: $imageSharpnessScore")
            val event = Arguments.createMap()
            event.putDouble("sharpnessScore", imageSharpnessScore)
            sendEvent("onSharpnessScoreUpdate", event)
        }

        override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, imageSharpnessScore: Float) {
            try {
                val tempDir = appContext.cacheDir
                val fileName = "camera_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(tempDir, fileName)

                java.io.FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }

                val event = Arguments.createMap()
                event.putString("image", file.absolutePath)
                event.putString("nativeImage", file.toURI().toString())
                event.putDouble("sharpnessScore", imageSharpnessScore.toDouble())

                // Add barcodes array
                val barcodesArray = Arguments.createArray()
                scannedCodeResults.forEach { code ->
                    val barcodeMap = Arguments.createMap()
                    barcodeMap.putString("scannedCode", code.scannedCode)
                    barcodeMap.putString("symbology", code.symbology.toString())

                    val boundingBox = Arguments.createMap()
                    code.boundingBox?.let { box ->
                        boundingBox.putDouble("x", box.left.toDouble())
                        boundingBox.putDouble("y", box.top.toDouble())
                        boundingBox.putDouble("width", box.width().toDouble())
                        boundingBox.putDouble("height", box.height().toDouble())
                    }
                    barcodeMap.putMap("boundingBox", boundingBox)

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Map = Arguments.createMap()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Map.putString(key, value)
                        }
                        barcodeMap.putMap("gs1ExtractedInfo", gs1Map)
                    }

                    barcodesArray.pushMap(barcodeMap)
                }
                event.putArray("barcodes", barcodesArray)

                sendEvent("onCapture", event)

                // Automatically restart scanning after image capture
                visionCameraView?.rescan()
            } catch (e: Exception) {
                val event = Arguments.createMap()
                event.putString("message", "Failed to save image: ${e.message}")
                sendEvent("onError", event)

                // Restart scanning after error
                visionCameraView?.rescan()
            }
        }

        override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>) {
            // Empty stub - only the version with sharpness score is used
        }

        // CameraLifecycleCallback implementation
        override fun onCameraStarted() {
            Log.d(TAG, "✅ Camera started successfully")
            isCameraReady = true

            // Only apply scan area if it was explicitly set via props
            if (hasScanAreaBeenSet) {
                Log.d(TAG, "Applying pending scan area settings")
                applyScanArea(view, pendingScanArea)
            } else {
                Log.d(TAG, "No scan area set, skipping focus settings application")
            }
        }

        override fun onCameraStopped() {
            Log.d(TAG, "⏹️ Camera stopped")
            isCameraReady = false
        }
    }
}
