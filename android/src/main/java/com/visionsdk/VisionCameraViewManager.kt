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
      isMultipleScanEnabled = false,
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
      "onBarcodeDetected" to mapOf("registrationName" to "onBarcodeDetected")
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
    sendEvent("onError", event)
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
    barcodeBoundingBoxes: List<android.graphics.Rect>,
    qrCodeBoundingBoxes: List<android.graphics.Rect>,
    documentBoundingBox: android.graphics.Rect?
  ) {
    // Not used in minimal implementation
  }

  override fun onItemRetrievalResult(scannedCodeResults: ScannedCodeResult) {
    // Not used in minimal implementation
  }

  override fun onPriceTagResult(priceTagData: io.packagex.visionsdk.core.pricetag.PriceTagData) {
    // Not used in minimal implementation
  }

  override fun onImageSharpnessScore(imageSharpnessScore: Float) {
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
      sendEvent("onCapture", event)

      // Automatically restart scanning after image capture
      visionCameraView?.rescan()
    } catch (e: Exception) {
      val event = Arguments.createMap()
      event.putString("message", "Failed to save image: ${e.message}")
      sendEvent("onError", event)
    }
  }

  override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>) {
    // Empty stub - only the version with sharpness score is used
  }

  // CameraLifecycleCallback implementation
  override fun onCameraStarted() {
    Log.d(TAG, "✅ Camera started successfully")
  }

  override fun onCameraStopped() {
    Log.d(TAG, "⏹️ Camera stopped")
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
