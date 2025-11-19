package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.visionsdk.utils.toDp
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.dto.ScannedCodeResult
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.ui.views.VisionCameraView

class VisionCameraViewManager(private val appContext: ReactApplicationContext) :
  SimpleViewManager<VisionCameraView>() {

  private var context: Context? = null
  private var visionCameraView: VisionCameraView? = null
  private var hasStarted = false
  private var currentCallback: ViewCallback? = null
  private var pendingScanArea: com.facebook.react.bridge.ReadableMap? = null
  private var isCameraReady = false
  private var consecutiveFailures = 0
  private var lastFailureTime = 0L
  private val maxConsecutiveFailures = 3
  private val failureResetWindowMs = 2000L // Reset counter after 2 seconds

  companion object {
    private const val TAG = "VisionCameraViewManager"
    const val REACT_CLASS = "VisionCameraView"

    // Commands
    private const val COMMAND_CAPTURE = 0
    private const val COMMAND_STOP = 1
    private const val COMMAND_START = 2
    private const val COMMAND_TOGGLE_FLASH = 3
    private const val COMMAND_SET_ZOOM = 4
  }

  override fun getName() = REACT_CLASS

  override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {
    val activity = appContext.currentActivity as? androidx.fragment.app.FragmentActivity
      ?: throw IllegalStateException("Activity must be a FragmentActivity")

    context = activity

    // Always create a fresh view instance
    val newView = VisionCameraView(activity, null)

    // Set layout parameters to ensure view is visible
    newView.layoutParams = android.view.ViewGroup.LayoutParams(
      android.view.ViewGroup.LayoutParams.MATCH_PARENT,
      android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )

    newView.configure(
      isMultipleScanEnabled = true,
      detectionMode = io.packagex.visionsdk.core.DetectionMode.Photo,
      scanningMode = io.packagex.visionsdk.core.ScanningMode.Manual
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

    // Only start camera for the currently active view
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
    } else if (visionCameraView != view) {
      Log.w(TAG, "Skipping camera start - view is not the active instance")
    }
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

  // Props
  @ReactProp(name = "enableFlash")
  fun setEnableFlash(view: VisionCameraView, enableFlash: Boolean) {
    view.setFlashTurnedOn(enableFlash)
  }

  @ReactProp(name = "zoomLevel")
  fun setZoomLevel(view: VisionCameraView, zoomLevel: Float) {
    view.setZoomRatio(zoomLevel)
  }

  @ReactProp(name = "scanMode")
  fun setScanMode(view: VisionCameraView, scanMode: String?) {
    val mode = when (scanMode?.lowercase()) {
      "ocr" -> io.packagex.visionsdk.core.DetectionMode.OCR
      "barcode", "barcodesinglecapture" -> io.packagex.visionsdk.core.DetectionMode.Barcode
      "photo" -> io.packagex.visionsdk.core.DetectionMode.Photo
      "barcodeorqrcode" -> io.packagex.visionsdk.core.DetectionMode.BarcodeOrQRCode
      "qrcode" -> io.packagex.visionsdk.core.DetectionMode.QRCode
      else -> io.packagex.visionsdk.core.DetectionMode.Barcode
    }
    view.setDetectionMode(mode)
    // Only restart scanning if camera is already started
    if (view.isCameraStarted()) {
      view.rescan()
    }
  }

  @ReactProp(name = "autoCapture")
  fun setAutoCapture(view: VisionCameraView, autoCapture: Boolean) {
    val mode = if (autoCapture) io.packagex.visionsdk.core.ScanningMode.Auto else io.packagex.visionsdk.core.ScanningMode.Manual
    view.setScanningMode(mode)
  }

  @ReactProp(name = "scanArea")
  fun setScanArea(view: VisionCameraView, scanArea: com.facebook.react.bridge.ReadableMap?) {
    // Store the scanArea for later application
    pendingScanArea = scanArea

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

        val density = appContext.resources.displayMetrics.density
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
        Log.d(TAG, "Scan area applied - single scan mode enabled")
      } else {
        // If no scan area, enable multiple scan mode
        view.setMultipleScanEnabled(true)

        val focusSettings = io.packagex.visionsdk.config.FocusSettings(
          context = appContext,
          showCodeBoundariesInMultipleScan = false,
          showDocumentBoundaries = false
        )
        view.getFocusRegionManager()?.setFocusSettings(focusSettings)
        Log.d(TAG, "No scan area - multiple scan mode enabled")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to apply scanArea: ${e.message}")
    }
  }

  @ReactProp(name = "detectionConfig")
  fun setDetectionConfig(view: VisionCameraView, config: com.facebook.react.bridge.ReadableMap?) {
    if (config != null) {
      val detectionConfig = io.packagex.visionsdk.config.ObjectDetectionConfiguration(
        isTextIndicationOn = if (config.hasKey("text")) config.getBoolean("text") else true,
        isBarcodeOrQRCodeIndicationOn = if (config.hasKey("barcode")) config.getBoolean("barcode") else true,
        isDocumentIndicationOn = if (config.hasKey("document")) config.getBoolean("document") else true,
        secondsToWaitBeforeDocumentCapture = if (config.hasKey("documentCaptureDelay")) config.getDouble("documentCaptureDelay").toInt() else 2
      )
      android.util.Log.d(TAG, "Detection config - text: ${detectionConfig.isTextIndicationOn}, barcode: ${detectionConfig.isBarcodeOrQRCodeIndicationOn}, document: ${detectionConfig.isDocumentIndicationOn}")
      view.setObjectDetectionConfiguration(detectionConfig)
      android.util.Log.d(TAG, "Detection config applied successfully")
    }
  }

  @ReactProp(name = "frameSkip")
  fun setFrameSkip(view: VisionCameraView, frameSkip: Int) {
    val cameraSettings = io.packagex.visionsdk.config.CameraSettings(
      nthFrameToProcess = frameSkip
    )
    view.setCameraSettings(cameraSettings)
  }

  @ReactProp(name = "cameraFacing")
  fun setCameraFacing(view: VisionCameraView, cameraFacing: String?) {
    // TODO: Implement camera facing/position switching for Android
    // This will require updating the VisionSDK Android implementation
    // to support CameraPosition enum (similar to iOS)
    Log.d(TAG, "Camera facing prop received: $cameraFacing (Android implementation pending)")
  }

  // Commands
  override fun getCommandsMap(): Map<String, Int> {
    return mapOf(
      "capture" to COMMAND_CAPTURE,
      "stop" to COMMAND_STOP,
      "start" to COMMAND_START,
      "toggleFlash" to COMMAND_TOGGLE_FLASH,
      "setZoom" to COMMAND_SET_ZOOM
    )
  }

  override fun receiveCommand(root: VisionCameraView, commandId: Int, args: ReadableArray?) {
    when (commandId) {
      COMMAND_CAPTURE -> root.capture()
      COMMAND_STOP -> root.stopCamera()
      COMMAND_START -> root.startCamera()
      COMMAND_TOGGLE_FLASH -> {
        val enabled = args?.getBoolean(0) ?: false
        root.setFlashTurnedOn(enabled)
      }
      COMMAND_SET_ZOOM -> {
        val level = args?.getDouble(0)?.toFloat() ?: 1.0f
        root.setZoomRatio(level)
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

  // Utility to convert pixels to DP
  private val density = appContext.resources.displayMetrics.density

  // Inner class for per-view callbacks
  inner class ViewCallback(
    private val view: VisionCameraView,
    private val context: ReactApplicationContext
  ) : ScannerCallback, CameraLifecycleCallback {

    private fun sendEvent(eventName: String, params: com.facebook.react.bridge.WritableMap) {
      if (view.isAttachedToWindow) {
        Log.d(TAG, "Sending event: $eventName to view ${view.id}")
        try {
          context.getJSModule(com.facebook.react.uimanager.events.RCTEventEmitter::class.java)
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

      // Reset failure counter on successful scan
      consecutiveFailures = 0

      // Automatically restart scanning after barcode detection
      visionCameraView?.rescan()
    }

    override fun onFailure(exception: VisionSDKException) {
      val event = Arguments.createMap()
      event.putString("message", exception.message ?: "Unknown error")
      event.putInt("code", exception.errorCode ?: -1)
      sendEvent("onError", event)

      // Check if enough time has passed since last failure to reset counter
      val currentTime = System.currentTimeMillis()
      if (currentTime - lastFailureTime > failureResetWindowMs) {
        consecutiveFailures = 0
      }
      lastFailureTime = currentTime

      // Track consecutive failures to prevent infinite loop
      consecutiveFailures++

      if (consecutiveFailures <= maxConsecutiveFailures) {
        Log.d(TAG, "Failure detected (${consecutiveFailures}/${maxConsecutiveFailures}), rescanning...")
        // Delay rescan slightly to avoid immediate recursion
        view.postDelayed({
          visionCameraView?.rescan()
        }, 100)
      } else {
        Log.e(TAG, "Too many consecutive failures (${consecutiveFailures}), pausing auto-rescan")
      }
    }

    override fun onIndications(
      barcodeDetected: Boolean,
      qrCodeDetected: Boolean,
      textDetected: Boolean,
      documentDetected: Boolean
    ) {
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
      Log.d(TAG, "onIndicationsBoundingBoxes - barcodes: ${barcodeBoundingBoxes.size}, qrCodes: ${qrCodeBoundingBoxes.size}, document: ${documentBoundingBox != null}")
      val event = Arguments.createMap()

      // Convert barcode bounding boxes with full metadata (Android SDK v2.4.23+)
      val barcodeBoxesArray = Arguments.createArray()
      barcodeBoundingBoxes.forEach { code ->
        val boxMap = Arguments.createMap()
        boxMap.putString("scannedCode", code.scannedCode)
        boxMap.putString("symbology", code.symbology.toString())

        // Add GS1 extracted info if available
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
          putInt("x", code.boundingBox.left.toDp(density))
          putInt("y", code.boundingBox.top.toDp(density))
          putInt("width", code.boundingBox.width().toDp(density))
          putInt("height", code.boundingBox.height().toDp(density))
        })
        barcodeBoxesArray.pushMap(boxMap)
      }
      event.putArray("barcodeBoundingBoxes", barcodeBoxesArray)

      // Convert QR code bounding boxes with full metadata (Android SDK v2.4.23+)
      val qrCodeBoxesArray = Arguments.createArray()
      qrCodeBoundingBoxes.forEach { code ->
        val boxMap = Arguments.createMap()
        boxMap.putString("scannedCode", code.scannedCode)
        boxMap.putString("symbology", code.symbology.toString())

        // Add GS1 extracted info if available
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
          putInt("x", code.boundingBox.left.toDp(density))
          putInt("y", code.boundingBox.top.toDp(density))
          putInt("width", code.boundingBox.width().toDp(density))
          putInt("height", code.boundingBox.height().toDp(density))
        })
        qrCodeBoxesArray.pushMap(boxMap)
      }
      event.putArray("qrCodeBoundingBoxes", qrCodeBoxesArray)

      // Convert document bounding box (px to dp)
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
      Log.d(TAG, "onImageSharpnessScore called: $imageSharpnessScore")
      val event = Arguments.createMap()
      event.putDouble("sharpnessScore", imageSharpnessScore.toDouble())
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

      // Apply pending scanArea if it exists
      if (pendingScanArea != null || pendingScanArea == null) {
        applyScanArea(view, pendingScanArea)
      }
    }

    override fun onCameraStopped() {
      Log.d(TAG, "⏹️ Camera stopped")
    }
  }
}
