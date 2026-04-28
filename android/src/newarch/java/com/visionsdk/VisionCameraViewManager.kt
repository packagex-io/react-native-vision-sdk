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
    ViewGroupManager<VisionCameraView>(),
    com.facebook.react.viewmanagers.VisionCameraViewManagerInterface<VisionCameraView> {

    companion object {
        const val REACT_CLASS = "VisionCameraView"
        const val TAG = "VisionCameraView Fabric"
    }

    // Fabric delegate — without this, Fabric can't route props to the @ReactProp
    // setters below and every prop (scanMode, zoomLevel, detectionConfigJson, …)
    // is silently dropped.
    private val viewManagerDelegate: com.facebook.react.uimanager.ViewManagerDelegate<VisionCameraView> =
        com.facebook.react.viewmanagers.VisionCameraViewManagerDelegate<VisionCameraView, VisionCameraViewManager>(this)

    override fun getDelegate(): com.facebook.react.uimanager.ViewManagerDelegate<VisionCameraView> =
        viewManagerDelegate

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
    private var lastSharpnessScoreUpdateTime = 0L

    // Throttle intervals in milliseconds
    private val RECOGNITION_UPDATE_THROTTLE_MS = 100L // 10 FPS
    private val SHARPNESS_SCORE_UPDATE_THROTTLE_MS = 200L // 5 FPS

    override fun getName(): String = REACT_CLASS

    // Let the native VisionCameraView layout its own children (camera preview surface)
    // Without this, Yoga overrides child layout and the preview gets cropped/zoomed
    override fun needsCustomLayoutForChildren(): Boolean = true

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
    override fun setEnableFlash(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setEnableFlash: $enabled")
        view.setFlashTurnedOn(enabled)
    }

    @ReactProp(name = "zoomLevel")
    override fun setZoomLevel(view: VisionCameraView, level: Double) {
        Log.d(TAG, "setZoomLevel: $level")
        view.setZoomRatio(level.toFloat())
    }

    @ReactProp(name = "scanMode")
    override fun setScanMode(view: VisionCameraView, mode: String?) {
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
    override fun setAutoCapture(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setAutoCapture: $enabled")
        val scanningMode = if (enabled) ScanningMode.Auto else ScanningMode.Manual
        view.setScanningMode(scanningMode)
    }

    @ReactProp(name = "cameraFacing")
    override fun setCameraFacing(view: VisionCameraView, facing: String?) {
        Log.d(TAG, "setCameraFacing: $facing")
        // TODO: Implement camera facing/position switching for Android
        // This will require updating the VisionSDK Android implementation
        // to support CameraPosition enum (similar to iOS)
        Log.d(TAG, "Camera facing prop received: $facing (Android implementation pending)")
    }

    @ReactProp(name = "frameSkip")
    override fun setFrameSkip(view: VisionCameraView, skip: Int) {
        Log.d(TAG, "setFrameSkip: $skip")
        // Frame skip would be configured via CameraSettings
    }

    @ReactProp(name = "scanAreaJson")
    override fun setScanAreaJson(view: VisionCameraView, scanAreaJson: String?) {
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
                )
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "Scan area applied - single scan mode enabled with focus rect: $focusRect")
            } else {
                // If no scan area, enable multiple scan mode
                view.setMultipleScanEnabled(true)

                val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                    context = appContext,
                    showCodeBoundariesInMultipleScan = false,
                )
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "No scan area - multiple scan mode enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply scanArea: ${e.message}", e)
        }
    }

    @ReactProp(name = "detectionConfigJson")
    override fun setDetectionConfigJson(view: VisionCameraView, configJson: String?) {
        Log.d(TAG, "setDetectionConfig")
        // Parse JSON and configure ObjectDetectionConfiguration
        // TODO: Implement using ObjectDetectionConfiguration
    }

    @ReactProp(name = "templateJson")
    override fun setTemplateJson(view: VisionCameraView, templateJson: String?) {
        if (templateJson.isNullOrEmpty()) {
            view.removeTemplate()
            return
        }

        try {
            val success = view.applyTemplateJson(templateJson)
            if (!success) {
                Log.e(TAG, "Failed to apply template - applyTemplateJson returned false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying template: ${e.message}", e)
        }
    }

    // MARK: - Commands

    override fun receiveCommand(
        root: VisionCameraView,
        commandId: String,
        args: ReadableArray?
    ) {
        Log.d(TAG, "receiveCommand called with commandId: '$commandId', view id: ${root.id}")

        // Handle string command names (Fabric codegen)
        when (commandId) {
            "capture" -> capture(root)
            "stop" -> stop(root)
            "start" -> start(root)
            "toggleFlash" -> {
                val enabled = args?.getBoolean(0) ?: false
                toggleFlash(root, enabled)
            }
            "setZoom" -> {
                val level = args?.getDouble(0) ?: 1.0
                setZoom(root, level.toFloat())
            }
            "setFocusSettings" -> {
                val settingsJson = args?.getString(0) ?: "{}"
                setFocusSettings(root, settingsJson)
            }
            else -> Log.w(TAG, "Unknown command: $commandId")
        }
    }

    override fun capture(view: VisionCameraView) {
        Log.d(TAG, "capture called")
        view.capture()
    }

    override fun stop(view: VisionCameraView) {
        Log.d(TAG, "stop called")
        view.stopCamera()
    }

    override fun start(view: VisionCameraView) {
        Log.d(TAG, "start called")
        view.startCamera()
    }

    override fun toggleFlash(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "toggleFlash called with enabled: $enabled")
        view.setFlashTurnedOn(enabled)
    }

    override fun setZoom(view: VisionCameraView, level: Float) {
        Log.d(TAG, "setZoom called with level: $level")
        view.setZoomRatio(level)
    }

    private fun parseColor(hex: String?, default: Int): Int {
        if (hex.isNullOrEmpty()) return default
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (e: Exception) {
            default
        }
    }

    override fun setFocusSettings(view: VisionCameraView, settingsJson: String) {
        Log.d(TAG, "setFocusSettings called with: $settingsJson")
        try {
            val json = org.json.JSONObject(settingsJson)

            val shouldScanInFocusImageRect = json.optBoolean("shouldScanInFocusImageRect", false)
            val showCodeBoundariesInMultipleScan = json.optBoolean("showCodeBoundariesInMultipleScan", true)
            val showDocumentBoundaries = json.optBoolean("showDocumentBoundaries", false)

            val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                context = appContext,
                shouldScanInFocusImageRect = shouldScanInFocusImageRect,
                showCodeBoundariesInMultipleScan = showCodeBoundariesInMultipleScan,
                showDocumentBoundaries = showDocumentBoundaries,
                validCodeBoundaryBorderColor = parseColor(
                    json.optString("validCodeBoundaryBorderColor", null),
                    android.graphics.Color.GREEN
                ),
                validCodeBoundaryBorderWidth = json.optInt("validCodeBoundaryBorderWidth", 2),
                validCodeBoundaryFillColor = parseColor(
                    json.optString("validCodeBoundaryFillColor", null),
                    android.graphics.Color.argb(76, 0, 255, 0)
                ),
                invalidCodeBoundaryBorderColor = parseColor(
                    json.optString("inValidCodeBoundaryBorderColor", null),
                    android.graphics.Color.RED
                ),
                invalidCodeBoundaryBorderWidth = json.optInt("inValidCodeBoundaryBorderWidth", 2),
                invalidCodeBoundaryFillColor = parseColor(
                    json.optString("inValidCodeBoundaryFillColor", null),
                    android.graphics.Color.argb(76, 255, 0, 0)
                ),
                documentBoundaryBorderColor = parseColor(
                    json.optString("documentBoundaryBorderColor", null),
                    android.graphics.Color.YELLOW
                ),
                documentBoundaryFillColor = parseColor(
                    json.optString("documentBoundaryFillColor", null),
                    android.graphics.Color.argb(76, 255, 255, 0)
                ),
            )

            view.getFocusRegionManager()?.setFocusSettings(focusSettings)
            Log.d(TAG, "Focus settings applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply focus settings: ${e.message}", e)
        }
    }

    // MARK: - ViewCallback inner class
    inner class ViewCallback(
        private val view: VisionCameraView,
        private val context: ReactApplicationContext
    ) : ScannerCallback, CameraLifecycleCallback {

        // Dedup state for onBoundingBoxesUpdate. The native BarcodeOverlayView's
        // Choreographer emits empty callbacks every frame while no boxes are
        // visible, which causes JS consumers to thrash setState({ []: [] })
        // between real detections — visible as overlay flicker. We forward the
        // first "became-empty" transition only; subsequent empties are dropped
        // until a non-empty event arrives.
        private var lastBoundingBoxesEmpty = false

        private fun sendEvent(eventName: String, params: com.facebook.react.bridge.WritableMap) {
            if (view.isAttachedToWindow) {
                try {
                    context.getJSModule(RCTEventEmitter::class.java)
                        .receiveEvent(view.id, eventName, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending event $eventName: ${e.message}")
                }
            }
        }

        // ScannerCallback implementation
        override fun onScanResult(barcodeList: List<ScannedCodeResult>) {
            // Build codes array as JSON for Fabric
            val codesArray = org.json.JSONArray()
            for (code in barcodeList) {
                val codeObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    code.boundingBox?.let { box ->
                        put("boundingBox", org.json.JSONObject().apply {
                            put("x", box.left.toDouble())
                            put("y", box.top.toDouble())
                            put("width", box.width().toDouble())
                            put("height", box.height().toDouble())
                        })
                    }

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Obj = org.json.JSONObject()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Obj.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Obj)
                    }
                }
                codesArray.put(codeObj)
            }

            val event = Arguments.createMap()
            event.putString("codesJson", codesArray.toString())
            sendEvent("onBarcodeDetected", event)

            // DIAGNOSTIC: auto-rescan disabled. rescan() tears down the camera,
            // analyzer, and overlay view, then rebuilds everything — matches the
            // "feels like restart" flicker. Consumer can call rescan imperatively.
            // visionCameraView?.rescan()
        }

        override fun onFailure(exception: VisionSDKException) {
            val event = Arguments.createMap()
            event.putString("message", exception.message ?: "Unknown error")
            event.putInt("code", exception.errorCode ?: -1)
            sendEvent("onError", event)

            // DIAGNOSTIC: auto-rescan-on-failure disabled for the same reason.
            // view.postDelayed({ visionCameraView?.rescan() }, 100)
        }

        override fun onIndications(
            barcodeDetected: Boolean,
            qrCodeDetected: Boolean,
            textDetected: Boolean,
            documentDetected: Boolean
        ) {
            Log.d(TAG, "onIndications: barcode=$barcodeDetected qr=$qrCodeDetected text=$textDetected doc=$documentDetected")
            // Throttle recognition updates to avoid overwhelming the JS bridge
            if (!shouldEmitThrottledEvent(lastRecognitionUpdateTime, RECOGNITION_UPDATE_THROTTLE_MS)) {
                return
            }
            lastRecognitionUpdateTime = System.currentTimeMillis()

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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onIndicationsBoundingBoxes called - barcodes: ${barcodeBoundingBoxes.size}, qr: ${qrCodeBoundingBoxes.size}, doc: ${documentBoundingBox != null}")
            }

            // Suppress repeat empty events. The native overlay fires empty
            // every idle frame; forwarding them all causes JS overlay flicker.
            val isEmpty = barcodeBoundingBoxes.isEmpty() &&
                qrCodeBoundingBoxes.isEmpty() &&
                documentBoundingBox == null
            if (isEmpty && lastBoundingBoxesEmpty) {
                return
            }
            lastBoundingBoxesEmpty = isEmpty
            // Build barcode bounding boxes JSON array
            val barcodeRectsJsonArray = org.json.JSONArray()
            barcodeBoundingBoxes.forEach { code ->
                val boxObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Obj = org.json.JSONObject()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Obj.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Obj)
                    }

                    code.boundingBox?.let { box ->
                        put("boundingBox", org.json.JSONObject().apply {
                            put("x", box.left.toDp(density))
                            put("y", box.top.toDp(density))
                            put("width", box.width().toDp(density))
                            put("height", box.height().toDp(density))
                        })
                    }
                }
                barcodeRectsJsonArray.put(boxObj)
            }

            // Build QR code bounding boxes JSON array
            val qrCodeRectsJsonArray = org.json.JSONArray()
            qrCodeBoundingBoxes.forEach { code ->
                val boxObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Obj = org.json.JSONObject()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Obj.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Obj)
                    }

                    code.boundingBox?.let { box ->
                        put("boundingBox", org.json.JSONObject().apply {
                            put("x", box.left.toDp(density))
                            put("y", box.top.toDp(density))
                            put("width", box.width().toDp(density))
                            put("height", box.height().toDp(density))
                        })
                    }
                }
                qrCodeRectsJsonArray.put(boxObj)
            }

            val event = Arguments.createMap()
            event.putString("barcodeBoundingBoxesJson", barcodeRectsJsonArray.toString())
            event.putString("qrCodeBoundingBoxesJson", qrCodeRectsJsonArray.toString())

            // Convert document bounding box (this stays as object)
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

            val event = Arguments.createMap()
            event.putDouble("sharpnessScore", imageSharpnessScore)
            sendEvent("onSharpnessScoreUpdate", event)
        }

        override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, imageSharpnessScore: Float) {
            Log.d(TAG, "onImageCaptured called with ${scannedCodeResults.size} barcodes, sharpnessScore: $imageSharpnessScore")
            try {
                val tempDir = appContext.cacheDir
                val fileName = "camera_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(tempDir, fileName)

                java.io.FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }

                // Build barcodes array as JSON for Fabric
                val barcodesArray = org.json.JSONArray()
                scannedCodeResults.forEach { code ->
                    val codeObj = org.json.JSONObject().apply {
                        put("scannedCode", code.scannedCode)
                        put("symbology", code.symbology.toString())

                        code.boundingBox?.let { box ->
                            put("boundingBox", org.json.JSONObject().apply {
                                put("x", box.left.toDouble())
                                put("y", box.top.toDouble())
                                put("width", box.width().toDouble())
                                put("height", box.height().toDouble())
                            })
                        }

                        if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                            val gs1Obj = org.json.JSONObject()
                            code.gs1ExtractedInfo?.forEach { (key, value) ->
                                gs1Obj.put(key, value)
                            }
                            put("gs1ExtractedInfo", gs1Obj)
                        }
                    }
                    barcodesArray.put(codeObj)
                }

                Log.d(TAG, "barcodesJson: ${barcodesArray.toString()}")

                val event = Arguments.createMap()
                event.putString("image", file.absolutePath)
                event.putString("nativeImage", file.toURI().toString())
                event.putDouble("sharpnessScore", imageSharpnessScore.toDouble())
                event.putString("barcodesJson", barcodesArray.toString())

                Log.d(TAG, "Sending onCapture event with barcodesJson length: ${barcodesArray.toString().length}")
                sendEvent("onCapture", event)

                // DIAGNOSTIC: auto-rescan-after-capture disabled.
                // visionCameraView?.rescan()
            } catch (e: Exception) {
                val event = Arguments.createMap()
                event.putString("message", "Failed to save image: ${e.message}")
                sendEvent("onError", event)

                // DIAGNOSTIC: auto-rescan-on-save-error disabled.
                // visionCameraView?.rescan()
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
