package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import com.asadullah.handyutils.save
import java.io.File
import java.util.Date
import androidx.lifecycle.coroutineScope
import com.asadullah.handyutils.launchOnIO
import com.asadullah.handyutils.toDecimalPoints
import com.asadullah.handyutils.withContextMain
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.visionsdk.utils.EventUtils
import com.visionsdk.utils.toDp
import com.visionsdk.utils.uriToBitmap
import io.packagex.visionsdk.ApiManager
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.config.CameraSettings
import io.packagex.visionsdk.config.FocusSettings
import io.packagex.visionsdk.config.ObjectDetectionConfiguration
import io.packagex.visionsdk.core.TemplateManager
import io.packagex.visionsdk.core.pricetag.PriceTagData
import io.packagex.visionsdk.ui.startCreateTemplateScreen
import androidx.fragment.app.FragmentActivity
import io.packagex.visionsdk.dto.ScannedCodeResult
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.ocr.ml.core.enums.ExecutionProvider
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import io.packagex.visionsdk.ocr.ml.core.enums.PlatformType
import io.packagex.visionsdk.service.dto.BOLModelToReport
import io.packagex.visionsdk.service.dto.DCModelToReport
import io.packagex.visionsdk.service.dto.ILModelToReport
import io.packagex.visionsdk.service.dto.SLModelToReport
import io.packagex.visionsdk.ui.views.VisionCameraView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import android.net.Uri
import android.util.Base64
import com.facebook.react.bridge.ReadableMap
import java.io.FileInputStream

/**
 * Fabric-compatible ViewManager for VisionSdkView
 * This wraps the actual VisionCameraView from the VisionSDK
 */
@ReactModule(name = VisionSdkViewManager.REACT_CLASS)
class VisionSdkViewManager(private val appContext: ReactApplicationContext) :
    ViewGroupManager<VisionCameraView>(), ScannerCallback, CameraLifecycleCallback, OCRResult {

    companion object {
        const val REACT_CLASS = "VisionSdkView"
        const val TAG = "VisionSdkView Fabric"
    }

    private var visionCameraView: VisionCameraView? = null
    private var context: Context? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var isCameraReady: Boolean = false
    private var imagePath: String = ""
    private var detectionMode: DetectionMode = DetectionMode.Barcode

    // Throttling for events (200ms = 5 times per second)
    private var lastDetectionEventTime: Long = 0
    private val detectionEventThrottleMs: Long = 200
    private var lastSharpnessEventTime: Long = 0
    private val sharpnessEventThrottleMs: Long = 500 // 500ms = 2 times per second
    private var lastBoundingBoxEventTime: Long = 0
    private val boundingBoxEventThrottleMs: Long = 200

    // Model configuration state
    private var modelSize: ModelSize = ModelSize.Large
    private var modelType: OCRModule = OCRModule.ShippingLabel(modelSize)
    private var executionProvider: ExecutionProvider = ExecutionProvider.NNAPI
    private var token: String? = null
    private var apiKey: String? = null
    private var environment: Environment = Environment.PRODUCTION

    // OCR and scanning state
    private var ocrMode: String = "cloud" // Mode for OCR, default is "cloud" for server-side OCR
    private var ocrType: String = "shipping_label" // Defines model type selected as string
    private var locationId: String? = null // Location identifier for the scanning process
    private var shouldResizeImage: Boolean = true
    private var isEnableAutoOcrResponseWithImage: Boolean = false
    private var optionsJson: String? = null

    // Pending settings to apply after camera starts
    private var pendingFocusSettings: FocusSettings? = null

    // Density for converting pixels to dp
    private val density: Float
        get() = appContext.resources.displayMetrics.density

    // Metadata, recipient, and sender for cloud predictions
    private var metaData: Map<String, Any>? = null
    private var recipient: Map<String, Any>? = null
    private var sender: Map<String, Any>? = null

    override fun getName(): String = REACT_CLASS

    private fun sendEvent(eventName: String, params: WritableMap) {
        try {
            appContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending event $eventName: ${e.message}")
        }
    }

    override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {
        Log.d(TAG, "createViewInstance")

        context = appContext.currentActivity
        lifecycleOwner = context as? LifecycleOwner

        visionCameraView = VisionCameraView(context!!, null)

        // Set layout parameters to ensure view is visible
        visionCameraView?.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Initialize with default settings
        visionCameraView?.configure(
            isMultipleScanEnabled = false,
            detectionMode = DetectionMode.Barcode,
            scanningMode = ScanningMode.Manual
        )

        // Set up callbacks
        visionCameraView?.setCameraLifecycleCallback(this)
        visionCameraView?.setScannerCallback(this)

        Log.d(TAG, "VisionSdkView created and configured")

        return visionCameraView!!
    }

    override fun onAfterUpdateTransaction(view: VisionCameraView) {
        super.onAfterUpdateTransaction(view)
        // Request layout to ensure proper sizing
        view.requestLayout()
    }

    // ScannerCallback implementations
    override fun onIndications(
        barcodeDetected: Boolean,
        qrCodeDetected: Boolean,
        textDetected: Boolean,
        documentDetected: Boolean
    ) {
        // Throttle events to prevent performance issues (emit max 5 times per second)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionEventTime < detectionEventThrottleMs) {
            return
        }
        lastDetectionEventTime = currentTime

        val event = Arguments.createMap().apply {
            putBoolean("barcode", barcodeDetected)
            putBoolean("qrcode", qrCodeDetected)
            putBoolean("text", textDetected)
            putBoolean("document", documentDetected)
            putBoolean("test", false)
        }
        sendEvent("onDetected", event)
    }

    override fun onIndicationsBoundingBoxes(
        barcodeBoundingBoxes: List<ScannedCodeResult>,
        qrCodeBoundingBoxes: List<ScannedCodeResult>,
        documentBoundingBox: android.graphics.Rect?
    ) {
        // Throttle events to prevent performance issues (emit max 5 times per second)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBoundingBoxEventTime < boundingBoxEventThrottleMs) {
            return
        }
        lastBoundingBoxEventTime = currentTime

        try {
            // Build barcode bounding boxes JSON array
            val barcodeRectsJsonArray = org.json.JSONArray()
            if (barcodeBoundingBoxes.isNotEmpty()) {
                barcodeBoundingBoxes.forEach { code ->
                    val codeObj = org.json.JSONObject().apply {
                        put("scannedCode", code.scannedCode)
                        put("symbology", code.symbology.toString())

                        // Add GS1 extracted info if available
                        if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                            val gs1Map = org.json.JSONObject()
                            code.gs1ExtractedInfo?.forEach { (key, value) ->
                                gs1Map.put(key, value)
                            }
                            put("gs1ExtractedInfo", gs1Map)
                        } else {
                            put("gs1ExtractedInfo", org.json.JSONObject())
                        }

                        val boundingBox = org.json.JSONObject()
                        code.boundingBox?.let { box ->
                            boundingBox.put("x", box.left.toDp(density))
                            boundingBox.put("y", box.top.toDp(density))
                            boundingBox.put("width", box.width().toDp(density))
                            boundingBox.put("height", box.height().toDp(density))
                        }
                        put("boundingBox", boundingBox)
                    }
                    barcodeRectsJsonArray.put(codeObj)
                }
            }

            // Build QR code bounding boxes JSON array
            val qrCodeRectsJsonArray = org.json.JSONArray()
            if (qrCodeBoundingBoxes.isNotEmpty()) {
                qrCodeBoundingBoxes.forEach { code ->
                    val codeObj = org.json.JSONObject().apply {
                        put("scannedCode", code.scannedCode)
                        put("symbology", code.symbology.toString())

                        // Add GS1 extracted info if available
                        if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                            val gs1Map = org.json.JSONObject()
                            code.gs1ExtractedInfo?.forEach { (key, value) ->
                                gs1Map.put(key, value)
                            }
                            put("gs1ExtractedInfo", gs1Map)
                        } else {
                            put("gs1ExtractedInfo", org.json.JSONObject())
                        }

                        val boundingBox = org.json.JSONObject()
                        code.boundingBox?.let { box ->
                            boundingBox.put("x", box.left.toDp(density))
                            boundingBox.put("y", box.top.toDp(density))
                            boundingBox.put("width", box.width().toDp(density))
                            boundingBox.put("height", box.height().toDp(density))
                        }
                        put("boundingBox", boundingBox)
                    }
                    qrCodeRectsJsonArray.put(codeObj)
                }
            }

            // Build event with JSON strings for arrays
            val event = Arguments.createMap().apply {
                putString("barcodeBoundingBoxesJson", barcodeRectsJsonArray.toString())
                putString("qrCodeBoundingBoxesJson", qrCodeRectsJsonArray.toString())

                // Document bounding box can remain as object
                val documentsRect = Arguments.createMap()
                if (documentBoundingBox != null) {
                    documentsRect.putInt("x", documentBoundingBox.left.toDp(density))
                    documentsRect.putInt("y", documentBoundingBox.top.toDp(density))
                    documentsRect.putInt("width", documentBoundingBox.width().toDp(density))
                    documentsRect.putInt("height", documentBoundingBox.height().toDp(density))
                }
                putMap("documentBoundingBox", documentsRect)
            }

            sendEvent("onBoundingBoxesDetected", event)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onIndicationsBoundingBoxes: ${e.message}")
        }
    }

    override fun onItemRetrievalResult(scannedCodeResults: ScannedCodeResult) {
        Log.d(TAG, "onItemRetrievalResult: ${scannedCodeResults.scannedCode}")
        // Not used in current implementation - matching old architecture behavior
    }

    override fun onPriceTagResult(priceTagData: PriceTagData) {
        Log.d(TAG, "onPriceTagResult")

        val boundingBoxMap = Arguments.createMap().apply {
            putInt("x", priceTagData.boundingBox.left.toDp(density))
            putInt("y", priceTagData.boundingBox.top.toDp(density))
            putInt("width", priceTagData.boundingBox.width().toDp(density))
            putInt("height", priceTagData.boundingBox.height().toDp(density))
        }

        val event = Arguments.createMap().apply {
            putString("sku", priceTagData.productSKU)
            putString("price", priceTagData.productPrice)
            putMap("boundingBox", boundingBoxMap)
        }

        Log.d(TAG, "Emitting event: onPriceTagDetected: $event")
        sendEvent("onPriceTagDetected", event)
    }

    override fun onFailure(exception: VisionSDKException) {
        Log.e(TAG, "onFailure: ${exception.message}, errorCode: ${exception.errorCode}, cause: ${exception.cause?.message}")
        exception.printStackTrace()
        val event = Arguments.createMap().apply {
            putString("message", exception.message ?: "Unknown error")
            putInt("code", exception.errorCode ?: -1)
        }
        sendEvent("onError", event)
    }

    override fun onScanResult(barcodeList: List<ScannedCodeResult>) {
        Log.d(TAG, "onScanResult: ${barcodeList.size} barcodes")

        if (barcodeList.isEmpty()) {
            Log.w(TAG, "barcodeList is empty, skipping event emission")
            return
        }

        try {
            // Build codes array
            val codesArray = org.json.JSONArray()

            barcodeList.forEach { code ->
                val codeObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    val boundingBox = org.json.JSONObject()
                    code.boundingBox?.let { box ->
                        boundingBox.put("x", box.left)
                        boundingBox.put("y", box.top)
                        boundingBox.put("width", box.width())
                        boundingBox.put("height", box.height())
                    }
                    put("boundingBox", boundingBox)

                    code.gs1ExtractedInfo?.let { gs1Info ->
                        val gs1Map = org.json.JSONObject()
                        gs1Info.forEach { (key, value) ->
                            gs1Map.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Map)
                    }
                }
                codesArray.put(codeObj)
            }

            // Send as JSON string for Fabric architecture
            val event = Arguments.createMap().apply {
                putString("codesJson", codesArray.toString())
            }
            sendEvent("onBarcodeScan", event)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onScanResult: ${e.message}", e)
        }
    }

    override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>) {
        // Empty stub - only the version with sharpness score is used
    }

    override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, imageSharpnessScore: Float) {
        Log.d(TAG, "onImageCaptured with sharpness: $imageSharpnessScore")
        this.imagePath = ""
        val savedImagePath = saveBitmapAndSendEvent(bitmap, scannedCodeResults, imageSharpnessScore)

        // Check if OCR mode is enabled and auto-response is required
        if (detectionMode == DetectionMode.OCR && isEnableAutoOcrResponseWithImage) {
            this.imagePath = savedImagePath
            handleOcrMode(bitmap, scannedCodeResults.map { it.scannedCode })
        }
    }

    private fun saveBitmapAndSendEvent(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, sharpnessScore: Float): String {
        val parentDir = File(context?.filesDir, "VisionSdkSavedBitmaps")
        parentDir.mkdirs()

        val savedBitmapFile = File(parentDir, "${Date().time}.jpg")
        bitmap.save(
            fileToSaveBitmapTo = savedBitmapFile,
            compressFormat = Bitmap.CompressFormat.JPEG
        )

        // Delete the oldest image if there are more than 10 in the directory
        val filesList = parentDir.list() ?: emptyArray()
        if (filesList.size > 10) {
            val sortedFiles = filesList.sortedBy { it }
            val fileToDelete = File(parentDir, sortedFiles[0])
            fileToDelete.delete()
        }

        val event = Arguments.createMap().apply {
            putString("image", savedBitmapFile.toUri().toString())
            putDouble("sharpnessScore", sharpnessScore.toDouble())

            val codesArray = Arguments.createArray()
            scannedCodeResults.forEach { scannedCodeResult ->
                val codeMap = Arguments.createMap().apply {
                    putString("scannedCode", scannedCodeResult.scannedCode)
                    putString("symbology", scannedCodeResult.symbology.toString())

                    val boundingBoxMap = Arguments.createMap().apply {
                        scannedCodeResult.boundingBox?.let { box ->
                            putInt("x", box.left)
                            putInt("y", box.top)
                            putInt("width", box.width())
                            putInt("height", box.height())
                        }
                    }
                    putMap("boundingBox", boundingBoxMap)

                    scannedCodeResult.gs1ExtractedInfo?.let { gs1Info ->
                        val gs1Map = Arguments.createMap()
                        for ((key, value) in gs1Info) {
                            gs1Map.putString(key, value)
                        }
                        putMap("gs1ExtractedInfo", gs1Map)
                    }
                }
                codesArray.pushMap(codeMap)
            }
            putArray("barcodes", codesArray)
        }
        sendEvent("onImageCaptured", event)

        return savedBitmapFile.absolutePath
    }

    private fun handleOcrMode(bitmap: Bitmap, barcodes: List<String>) {
        Log.d(TAG, "handleOcrMode: ocrMode=$ocrMode, ocrType=$ocrType")

        when (ocrMode) {
            "cloud" -> when (ocrType.lowercase().replace("-", "_")) {
                "shipping_label" -> predictShippingLabelCloud(bitmap, barcodes)
                "item_label" -> predictItemLabelCloud(bitmap)
                "bill_of_lading" -> predictBillOfLadingCloud(bitmap, barcodes)
                "document_classification" -> predictDocumentClassificationCloud(bitmap)
                else -> Log.w(TAG, "Unsupported OCR type for cloud mode: $ocrType")
            }
            "on-device", "on_device" -> {
                // Perform on-device prediction
                getPrediction(bitmap, barcodes)
            }
            else -> Log.w(TAG, "Unsupported OCR mode: $ocrMode")
        }
    }

    private fun predictShippingLabelCloud(bitmap: Bitmap, barcodes: List<String>) {
        Log.d(TAG, "predictShippingLabelCloud")

        // Parse options JSON string to Map
        val optionsMap = try {
            optionsJson?.let { json ->
                val jsonObject = JSONObject(json)
                jsonObject.keys().asSequence().associateWith { key ->
                    jsonObject.get(key)
                }
            } ?: emptyMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse options JSON", e)
            emptyMap()
        }

        // Convert empty string to null for API compatibility
        val normalizedLocationId = if (locationId.isNullOrEmpty()) null else locationId

        ApiManager().shippingLabelApiCallAsync(
            apiKey = apiKey,
            token = token,
            bitmap = bitmap,
            barcodeList = barcodes,
            locationId = normalizedLocationId,
            options = optionsMap,
            metadata = emptyMap(),
            recipient = null,
            sender = null,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    private fun predictItemLabelCloud(bitmap: Bitmap) {
        Log.d(TAG, "predictItemLabelCloud")

        ApiManager().itemLabelApiCallAsync(
            apiKey = apiKey,
            token = token,
            bitmap = bitmap,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    private fun predictBillOfLadingCloud(bitmap: Bitmap, barcodes: List<String>) {
        Log.d(TAG, "predictBillOfLadingCloud")

        // Parse options JSON string to Map
        val optionsMap = try {
            optionsJson?.let { json ->
                val jsonObject = JSONObject(json)
                jsonObject.keys().asSequence().associateWith { key ->
                    jsonObject.get(key)
                }
            } ?: emptyMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse options JSON", e)
            emptyMap()
        }

        // Convert empty string to null for API compatibility
        val normalizedLocationId = if (locationId.isNullOrEmpty()) null else locationId

        ApiManager().billOfLadingApiCallAsync(
            apiKey = apiKey,
            token = token,
            bitmap = bitmap,
            barcodeList = barcodes,
            locationId = normalizedLocationId,
            options = optionsMap,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    private fun predictDocumentClassificationCloud(bitmap: Bitmap) {
        Log.d(TAG, "predictDocumentClassificationCloud")

        ApiManager().documentClassificationApiCallAsync(
            apiKey = apiKey,
            token = token,
            bitmap = bitmap,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    private fun getPrediction(bitmap: Bitmap, barcodes: List<String>) {
        val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context!!, modelType)

        lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
            try {
                val result = onDeviceOCRManager.getPredictions(bitmap, barcodes)
                withContextMain {
                    onOCRResponse(result)
                }
            } catch (e: VisionSDKException) {
                withContextMain {
                    onOCRResponseFailed(e)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                withContextMain {
                    onOCRResponseFailed(VisionSDKException.UnknownException(e))
                }
            }
        }
    }

    override fun onImageSharpnessScore(imageSharpnessScore: Double) {
        // Throttle events to prevent performance issues (emit max 2 times per second)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSharpnessEventTime < sharpnessEventThrottleMs) {
            return
        }
        lastSharpnessEventTime = currentTime

        val event = Arguments.createMap().apply {
            putDouble("sharpnessScore", imageSharpnessScore)
        }
        sendEvent("onSharpnessScore", event)
    }

    // CameraLifecycleCallback implementations
    override fun onCameraStarted() {
        Log.d(TAG, "onCameraStarted")
        isCameraReady = true

        // Apply pending focus settings if any
        pendingFocusSettings?.let { settings ->
            try {
                visionCameraView?.getFocusRegionManager()?.setFocusSettings(settings)
                Log.d(TAG, "Pending focus settings applied successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error applying pending focus settings: ${e.message}")
            }
        }

        val event = Arguments.createMap().apply {
            putBoolean("started", true)
        }
        sendEvent("onCameraStarted", event)
    }

    override fun onCameraStopped() {
        Log.d(TAG, "onCameraStopped")
        isCameraReady = false
        val event = Arguments.createMap().apply {
            putBoolean("stopped", true)
        }
        sendEvent("onCameraStopped", event)
    }

    // OCRResult implementations
    override fun onOCRResponse(response: String) {
        Log.d(TAG, "onOCRResponse: ${response.take(100)}...")
        val event = Arguments.createMap().apply {
            // Response is already a JSON string from the SDK, send as dataJson for Fabric
            putString("dataJson", response)
            putString("imagePath", imagePath)
        }
        sendEvent("onOCRScan", event)
    }

    override fun onOCRResponseFailed(exception: VisionSDKException) {
        Log.e(TAG, "onOCRResponseFailed: ${exception.message}", exception)
        Log.e(TAG, "Error code: ${exception.errorCode}")
        Log.e(TAG, "Error type: ${exception.javaClass.simpleName}")
        Log.e(TAG, "Stack trace:", exception)
        val event = Arguments.createMap().apply {
            putString("message", exception.message ?: "OCR failed")
            putInt("code", exception.errorCode ?: -1)
        }
        sendEvent("onError", event)
    }

    override fun onDropViewInstance(view: VisionCameraView) {
        super.onDropViewInstance(view)
        Log.d(TAG, "onDropViewInstance")
        visionCameraView?.stopCamera()
        visionCameraView = null
    }

    // MARK: - Props

    @ReactProp(name = "mode")
    fun setMode(view: VisionCameraView, mode: String?) {
        Log.d(TAG, "setMode: $mode, isCameraReady: $isCameraReady")
        val newDetectionMode = when (mode?.lowercase()) {
            "ocr" -> DetectionMode.OCR
            "barcode" -> DetectionMode.Barcode
            "qrcode" -> DetectionMode.QRCode
            "photo" -> DetectionMode.Photo
            "barcodeorqrcode" -> DetectionMode.BarcodeOrQRCode
            else -> DetectionMode.Barcode
        }

        // Only restart camera if mode actually changed
        val modeChanged = this.detectionMode != newDetectionMode
        this.detectionMode = newDetectionMode
        view.setDetectionMode(this.detectionMode)

        // If camera is already running and mode changed, restart it to rebind use cases
        if (isCameraReady && modeChanged) {
            Log.d(TAG, "Restarting camera for mode change to: $mode")
            isCameraReady = false
            view.stopCamera()
            // Use post to allow stopCamera to complete before starting again
            view.post {
                Log.d(TAG, "Starting camera after mode change")
                view.startCamera()
            }
        }
    }

    @ReactProp(name = "captureMode")
    fun setCaptureMode(view: VisionCameraView, captureMode: String?) {
        Log.d(TAG, "setCaptureMode: $captureMode")
        val scanningMode = when (captureMode?.lowercase()) {
            "auto" -> ScanningMode.Auto
            "manual" -> ScanningMode.Manual
            else -> ScanningMode.Auto
        }
        view.setScanningMode(scanningMode)
    }

    @ReactProp(name = "flash")
    fun setFlash(view: VisionCameraView, flash: Boolean) {
        Log.d(TAG, "setFlash: $flash")
        view.setFlashTurnedOn(flash)
    }

    @ReactProp(name = "zoomLevel")
    fun setZoomLevel(view: VisionCameraView, zoomLevel: Double) {
        Log.d(TAG, "setZoomLevel: $zoomLevel")
        view.setZoomRatio(zoomLevel.toFloat())
    }

    @ReactProp(name = "isMultipleScanEnabled")
    fun setMultipleScanEnabled(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setMultipleScanEnabled: $enabled")
        view.setMultipleScanEnabled(enabled)
    }

    @ReactProp(name = "environment")
    fun setEnvironment(view: VisionCameraView, env: String?) {
        Log.d(TAG, "setEnvironment: $env")
        this.environment = when (env?.lowercase()) {
            "dev" -> Environment.DEV
            "staging" -> Environment.STAGING
            "sandbox" -> Environment.SANDBOX
            "prod" -> Environment.PRODUCTION
            else -> Environment.PRODUCTION
        }
        // Initialize SDK with environment
        VisionSdkSingleton.initializeSdk(appContext.currentActivity ?: appContext, this.environment)
    }

    @ReactProp(name = "apiKey")
    fun setApiKey(view: VisionCameraView, apiKey: String?) {
        Log.d(TAG, "setApiKey: ${if (apiKey != null) "***" else "null"}")
        this.apiKey = apiKey
    }

    @ReactProp(name = "token")
    fun setToken(view: VisionCameraView, token: String?) {
        Log.d(TAG, "setToken: ${if (token != null) "***" else "null"}")
        this.token = token
    }

    @ReactProp(name = "ocrMode")
    fun setOcrMode(view: VisionCameraView, ocrMode: String?) {
        Log.d(TAG, "setOcrMode: $ocrMode")
        this.ocrMode = ocrMode ?: "cloud"
    }

    @ReactProp(name = "ocrType")
    fun setOcrType(view: VisionCameraView, ocrType: String?) {
        Log.d(TAG, "setOcrType: $ocrType")
        this.ocrType = ocrType ?: "shipping_label"
        setModelType(this.ocrType)
    }

    @ReactProp(name = "locationId")
    fun setLocationId(view: VisionCameraView, locationId: String?) {
        Log.d(TAG, "setLocationId: $locationId")
        this.locationId = locationId
    }

    @ReactProp(name = "shouldResizeImage")
    fun setShouldResizeImage(view: VisionCameraView, shouldResize: Boolean) {
        Log.d(TAG, "setShouldResizeImage: $shouldResize")
        this.shouldResizeImage = shouldResize
    }

    @ReactProp(name = "isEnableAutoOcrResponseWithImage")
    fun setEnableAutoOcrResponseWithImage(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setEnableAutoOcrResponseWithImage: $enabled")
        this.isEnableAutoOcrResponseWithImage = enabled
    }

    @ReactProp(name = "modelExecutionProviderAndroid")
    fun setModelExecutionProvider(view: VisionCameraView, provider: String?) {
        Log.d(TAG, "setModelExecutionProvider: $provider")
        // Configure execution provider for on-device OCR
    }

    @ReactProp(name = "optionsJson")
    fun setOptions(view: VisionCameraView, optionsJson: String?) {
        Log.d(TAG, "setOptions: $optionsJson")
        this.optionsJson = optionsJson
    }

    // MARK: - Commands

    override fun getCommandsMap(): Map<String, Int>? {
        return mapOf(
            "captureImage" to 0,
            "stopRunning" to 1,
            "startRunning" to 2,
            "restartScanning" to 3,
            "setMetaData" to 4,
            "setRecipient" to 5,
            "setSender" to 6,
            "configureOnDeviceModel" to 7,
            "setFocusSettings" to 8,
            "setObjectDetectionSettings" to 9,
            "setCameraSettings" to 10,
            "getPrediction" to 11,
            "getPredictionWithCloudTransformations" to 12,
            "getPredictionShippingLabelCloud" to 13,
            "getPredictionBillOfLadingCloud" to 14,
            "getPredictionItemLabelCloud" to 15,
            "getPredictionDocumentClassificationCloud" to 16,
            "reportError" to 17,
            "createTemplate" to 18,
            "getAllTemplates" to 19,
            "deleteTemplateWithId" to 20,
            "deleteAllTemplates" to 21
        )
    }

    override fun receiveCommand(
        root: VisionCameraView,
        commandId: String,
        args: ReadableArray?
    ) {
        Log.d(TAG, "receiveCommand: commandId=$commandId")
        when (commandId) {
            "captureImage", "0" -> captureImage(root)
            "stopRunning", "1" -> stopRunning(root)
            "startRunning", "2" -> startRunning(root)
            "restartScanning", "3" -> restartScanning(root)
            "setMetaData", "4" -> setMetaData(args)
            "setRecipient", "5" -> setRecipient(args)
            "setSender", "6" -> setSender(args)
            "configureOnDeviceModel", "7" -> configureOnDeviceModel(args)
            "setFocusSettings", "8" -> setFocusSettings(root, args)
            "setObjectDetectionSettings", "9" -> setObjectDetectionSettings(root, args)
            "setCameraSettings", "10" -> setCameraSettings(root, args)
            "getPrediction", "11" -> getPrediction(args)
            "getPredictionWithCloudTransformations", "12" -> getPredictionWithCloudTransformations(args)
            "getPredictionShippingLabelCloud", "13" -> getPredictionShippingLabelCloud(args)
            "getPredictionBillOfLadingCloud", "14" -> getPredictionBillOfLadingCloud(args)
            "getPredictionItemLabelCloud", "15" -> getPredictionItemLabelCloud(args)
            "getPredictionDocumentClassificationCloud", "16" -> getPredictionDocumentClassificationCloud(args)
            "reportError", "17" -> reportError(args)
            "createTemplate", "18" -> createTemplate(root)
            "getAllTemplates", "19" -> getAllTemplates(root)
            "deleteTemplateWithId", "20" -> deleteTemplateWithId(args)
            "deleteAllTemplates", "21" -> deleteAllTemplates()
            else -> Log.w(TAG, "Unknown command: $commandId")
        }
    }

    private fun captureImage(view: VisionCameraView) {
        Log.d(TAG, "captureImage called, isCameraReady: $isCameraReady")
        if (!isCameraReady) {
            Log.w(TAG, "Camera not ready yet, capture may fail")
        }
        view.capture()
    }

    private fun stopRunning(view: VisionCameraView) {
        Log.d(TAG, "stopRunning called")
        view.stopCamera()
    }

    private fun startRunning(view: VisionCameraView) {
        Log.d(TAG, "startRunning called")
        view.startCamera()
    }

    private fun restartScanning(view: VisionCameraView) {
        Log.d(TAG, "restartScanning called")
        view.rescan()
    }

    private fun configureOnDeviceModel(args: ReadableArray?) {
        val onDeviceConfigs = args?.getString(0) ?: "{}"
        Log.d(TAG, "configureOnDeviceModel: $onDeviceConfigs")
        val argToken = args?.getString(1)
        val argApiKey = args?.getString(2)

        Log.d(TAG, "Args - token: '${argToken ?: "null"}', apiKey: '${argApiKey ?: "null"}'")
        Log.d(TAG, "Stored - token: ${if (!this.token.isNullOrEmpty()) "provided" else "null/empty"}, apiKey: ${if (!this.apiKey.isNullOrEmpty()) "provided" else "null/empty"}")

        // Use arg values if not null/empty, otherwise fall back to stored values
        val resolvedToken = argToken?.takeIf { it.isNotEmpty() } ?: this.token
        val resolvedApiKey = argApiKey?.takeIf { it.isNotEmpty() } ?: this.apiKey

        Log.d(TAG, "Resolved - token: ${if (!resolvedToken.isNullOrEmpty()) "provided" else "null/empty"}, apiKey: ${if (!resolvedApiKey.isNullOrEmpty()) "provided" else "null/empty"}")

        // Parse configs and set model size/type
        if (onDeviceConfigs != "null" && onDeviceConfigs.isNotEmpty()) {
            try {
                JSONObject(onDeviceConfigs).apply {
                    setModelSize(optString("size", ""))
                    setModelType(optString("type", "shipping_label"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing onDeviceConfigs: ${e.message}")
            }
        }

        // Ensure SDK is initialized with environment before model configuration
        VisionSdkSingleton.initializeSdk(context!!, this.environment)

        val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context!!, modelType)

        Log.d(TAG, "Model execution provider: $executionProvider")

        lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
            try {
                if (!OnDeviceOCRManagerSingleton.isModelConfigured(modelType)) {
                    var lastProgress = 0.0
                    val isModelAlreadyDownloaded = onDeviceOCRManager.isModelAlreadyDownloaded()
                    onDeviceOCRManager.configure(resolvedApiKey, resolvedToken, executionProvider) { progress ->
                        val progressValue = progress.toDecimalPoints(2).toDouble()
                        if (progressValue != lastProgress) {
                            lastProgress = progressValue
                            Log.d(TAG, "Download Progress: ${(progress * 100).toInt()}%")
                            EventUtils.sendModelDownloadProgressEvent(
                                appContext,
                                progress = lastProgress,
                                downloadStatus = isModelAlreadyDownloaded,
                                isReady = false
                            )
                        }
                    }
                }
                // Emit success event when model is downloaded
                EventUtils.sendModelDownloadProgressEvent(
                    appContext,
                    progress = 1.0,
                    downloadStatus = true,
                    isReady = true
                )
            } catch (e: VisionSDKException) {
                Log.e(TAG, "Model configuration failed: ${e.message}")
                withContextMain {
                    onOCRResponseFailed(e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model configuration error: ${e.message}")
                if (e is CancellationException) throw e
                withContextMain {
                    onOCRResponseFailed(VisionSDKException.UnknownException(e))
                }
            }
        }
    }

    private fun setFocusSettings(view: VisionCameraView, args: ReadableArray?) {
        val focusSettingsJson = args?.getString(0) ?: "{}"
        Log.d(TAG, "setFocusSettings: $focusSettingsJson")

        try {
            val json = JSONObject(focusSettingsJson)
            val density = appContext.resources.displayMetrics.density

            // Parse focus rect if provided
            val hasFocusRect = json.has("focusImageRect")
            val focusRect = if (hasFocusRect) {
                val rectJson = json.getJSONObject("focusImageRect")
                val x = (rectJson.optDouble("x", 0.0) * density).toFloat()
                val y = (rectJson.optDouble("y", 0.0) * density).toFloat()
                val width = (rectJson.optDouble("width", 0.0) * density).toFloat()
                val height = (rectJson.optDouble("height", 0.0) * density).toFloat()
                RectF(x, y, x + width, y + height)
            } else {
                RectF(0f, 0f, 0f, 0f) // Default empty rect
            }

            val focusSettings = FocusSettings(
                context = context!!,
                shouldScanInFocusImageRect = hasFocusRect && json.optBoolean("shouldScanInFocusImageRect", false),
                focusImageRect = focusRect,
                showCodeBoundariesInMultipleScan = json.optBoolean("showCodeBoundariesInMultipleScan", false),
                showDocumentBoundaries = json.optBoolean("showDocumentBoundaries", false)
            )

            // Store for later application if camera not ready
            pendingFocusSettings = focusSettings

            // Try to apply immediately if camera is ready
            if (isCameraReady) {
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "Focus settings applied successfully")
            } else {
                Log.d(TAG, "Camera not ready, focus settings will be applied when camera starts")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying focus settings: ${e.message}")
        }
    }

    private fun setObjectDetectionSettings(view: VisionCameraView, args: ReadableArray?) {
        val settingsJson = args?.getString(0) ?: "{}"

        try {
            val json = JSONObject(settingsJson)

            val detectionConfig = ObjectDetectionConfiguration(
                isTextIndicationOn = json.optBoolean("isTextIndicationOn", true),
                isBarcodeOrQRCodeIndicationOn = json.optBoolean("isBarcodeOrQRCodeIndicationOn", true),
                isDocumentIndicationOn = json.optBoolean("isDocumentIndicationOn", true),
                secondsToWaitBeforeDocumentCapture = json.optInt("secondsToWaitBeforeDocumentCapture", 2)
            )

            view.setObjectDetectionConfiguration(detectionConfig)

            // Apply selected template
            if (json.has("selectedTemplateId")) {
                val selectedTemplateId = json.optString("selectedTemplateId", "")

                if (selectedTemplateId.isNotEmpty()) {
                    val templateManager = TemplateManager()
                    val templates = templateManager.getAllBarcodeTemplates()
                    val templateToApply = templates.firstOrNull { it.name == selectedTemplateId }

                    if (templateToApply != null) {
                        view.applyBarcodeTemplate(templateToApply)
                    } else {
                        Log.w(TAG, "Template not found: $selectedTemplateId")
                    }
                } else {
                    view.removeBarcodeTemplate()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying object detection settings: ${e.message}", e)
        }
    }

    private fun setCameraSettings(view: VisionCameraView, args: ReadableArray?) {
        val settingsJson = args?.getString(0) ?: "{}"
        Log.d(TAG, "setCameraSettings: $settingsJson")

        try {
            val json = JSONObject(settingsJson)

            val cameraSettings = CameraSettings(
                nthFrameToProcess = json.optInt("nthFrameToProcess", 10)
            )

            view.setCameraSettings(cameraSettings)
            Log.d(TAG, "Camera settings applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying camera settings: ${e.message}")
        }
    }

    private fun createTemplate(view: VisionCameraView) {
        view.stopCamera()

        val activity = appContext.currentActivity as? FragmentActivity
        if (activity == null) {
            Log.e(TAG, "Activity is not a FragmentActivity")
            return
        }

        val onTemplateCreated: (String) -> Unit = { newCreatedTemplateId: String ->
            val event = Arguments.createMap().apply {
                putString("data", newCreatedTemplateId)
            }
            sendEvent("onCreateTemplate", event)
            view.startCamera()
        }

        startCreateTemplateScreen(
            fragmentManager = activity.supportFragmentManager,
            onTemplateCreated = onTemplateCreated,
            onCancelled = {
                view.startCamera()
            }
        )
    }

    private fun getAllTemplates(view: VisionCameraView) {
        val templateManager = TemplateManager()
        val templates = templateManager.getAllBarcodeTemplates()

        // Convert to JSON array string (Fabric requires JSON string, not array)
        val templateNames = templates.map { it.name }
        val dataJson = JSONArray(templateNames).toString()

        val event = Arguments.createMap().apply {
            putString("dataJson", dataJson)
        }
        sendEvent("onGetTemplates", event)
    }

    private fun deleteTemplateWithId(args: ReadableArray?) {
        val templateManager = TemplateManager()
        val id = args?.getString(0)

        val allTemplates = templateManager.getAllBarcodeTemplates()
        val templateToDelete = allTemplates.firstOrNull { template -> template.name == id }

        if (templateToDelete != null) {
            templateManager.deleteBarcodeTemplate(templateToDelete)
            val event = Arguments.createMap().apply {
                putString("data", id)
            }
            sendEvent("onDeleteTemplateById", event)
        }
    }

    private fun deleteAllTemplates() {
        val templateManager = TemplateManager()
        val allTemplates = templateManager.getAllBarcodeTemplates()

        allTemplates.forEach { template ->
            templateManager.deleteBarcodeTemplate(template)
        }

        val event = Arguments.createMap().apply {
            putBoolean("success", true)
        }
        sendEvent("onDeleteTemplates", event)
    }

    // MARK: - Metadata/Recipient/Sender Commands

    private fun setMetaData(args: ReadableArray?) {
        val metaDataMap = args?.getMap(0)
        Log.d(TAG, "setMetaData: $metaDataMap")
        this.metaData = metaDataMap?.toHashMap() as? Map<String, Any>
    }

    private fun setRecipient(args: ReadableArray?) {
        val recipientMap = args?.getMap(0)
        Log.d(TAG, "setRecipient: $recipientMap")
        this.recipient = if (recipientMap == null) {
            emptyMap()
        } else {
            recipientMap.toHashMap() as? Map<String, Any>
        }
    }

    private fun setSender(args: ReadableArray?) {
        val senderMap = args?.getMap(0)
        Log.d(TAG, "setSender: $senderMap")
        this.sender = if (senderMap == null) {
            emptyMap()
        } else {
            senderMap.toHashMap() as? Map<String, Any>
        }
    }

    // MARK: - Manual Prediction Commands

    private fun getPrediction(args: ReadableArray?) {
        val image = args?.getString(0)
        this.imagePath = image ?: ""
        val barcodeArray = args?.getArray(1)
        val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()

        uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
            bitmap?.let {
                performOnDevicePrediction(it, barcodeList)
            }
        }
    }

    private fun performOnDevicePrediction(bitmap: Bitmap, barcodes: List<String>) {
        lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
            try {
                val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context!!, modelType)
                val result = onDeviceOCRManager.getPredictions(bitmap, barcodes)
                withContextMain {
                    onOCRResponse(result)
                }
            } catch (e: VisionSDKException) {
                withContextMain {
                    onOCRResponseFailed(e)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                withContextMain {
                    onOCRResponseFailed(VisionSDKException.UnknownException(e))
                }
            }
        }
    }

    private fun getPredictionWithCloudTransformations(args: ReadableArray?) {
        val image = args?.getString(0)
        this.imagePath = image ?: ""
        val barcodeArray = args?.getArray(1)
        val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
        val token = args?.getString(2)
        val apiKey = args?.getString(3)
        val locationId = args?.getString(4)
        val options = args?.getMap(5)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
        val metadata = args?.getMap(6)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
        val recipient = args?.getMap(7)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
        val sender = args?.getMap(8)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
        val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
            args.getBoolean(9)
        } else {
            true
        }

        uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
            bitmap?.let {
                performPredictionWithCloudTransformations(
                    it, barcodeList, token, apiKey, locationId,
                    options, metadata, recipient, sender, shouldResizeImage
                )
            }
        }
    }

    private fun performPredictionWithCloudTransformations(
        bitmap: Bitmap,
        list: List<String>,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: Map<String, Any>?,
        metadata: Map<String, Any>?,
        recipient: Map<String, Any>?,
        sender: Map<String, Any>?,
        shouldResizeImage: Boolean
    ) {
        val resolvedToken = token ?: this.token
        val resolvedApiKey = apiKey ?: this.apiKey
        val resolvedLocationId = locationId ?: this.locationId ?: ""
        val resolvedOptions = options ?: emptyMap()
        val resolvedMetadata = metadata ?: this.metaData ?: emptyMap()
        val resolvedRecipient = recipient ?: this.recipient ?: emptyMap()
        val resolvedSender = sender ?: this.sender ?: emptyMap()

        lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
            try {
                val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context!!, modelType)
                val onDeviceResponse = onDeviceOCRManager.getPredictions(bitmap, list)

                val result = ApiManager().shippingLabelMatchingApiSync(
                    apiKey = resolvedApiKey,
                    token = resolvedToken,
                    bitmap = bitmap,
                    shouldResizeImage = shouldResizeImage,
                    barcodeList = list,
                    onDeviceResponse = onDeviceResponse,
                    locationId = resolvedLocationId.takeIf { it.isNotEmpty() },
                    recipient = resolvedRecipient,
                    sender = resolvedSender,
                    options = resolvedOptions,
                    metadata = resolvedMetadata
                )
                withContextMain {
                    onOCRResponse(result)
                }
            } catch (e: VisionSDKException) {
                Log.e(TAG, "Error in matching api: ", e)
                withContextMain {
                    onOCRResponseFailed(e)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Error in matching api: ", e)
                withContextMain {
                    onOCRResponseFailed(VisionSDKException.UnknownException(e))
                }
            }
        }
    }

    private fun getPredictionShippingLabelCloud(args: ReadableArray?) {
        try {
            val image = args?.getString(0)
            this.imagePath = image ?: ""
            val barcodeArray = args?.getArray(1)
            val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
            val token = args?.getString(2)
            val apiKey = args?.getString(3)
            val locationId = args?.getString(4)
            val options = args?.getMap(5)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
            val metadata = args?.getMap(6)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
            val recipient = args?.getMap(7)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
            val sender = args?.getMap(8)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
            val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
                args.getBoolean(9)
            } else {
                true
            }

            uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
                bitmap?.let {
                    performShippingLabelCloudPrediction(
                        it, barcodeList, token, apiKey, locationId,
                        options, metadata, recipient, sender, shouldResizeImage
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getPredictionShippingLabelCloud: ${e.message}", e)
        }
    }

    private fun performShippingLabelCloudPrediction(
        bitmap: Bitmap,
        list: List<String>,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: Map<String, Any>?,
        metadata: Map<String, Any>?,
        recipient: Map<String, Any>?,
        sender: Map<String, Any>?,
        shouldResizeImage: Boolean
    ) {
        val resolvedToken = token ?: this.token
        val resolvedApiKey = apiKey ?: this.apiKey
        val resolvedLocationId = locationId ?: this.locationId ?: ""
        val resolvedOptions = options ?: emptyMap()
        val resolvedMetadata = metadata ?: this.metaData ?: emptyMap()
        val resolvedRecipient = recipient ?: this.recipient ?: emptyMap()
        val resolvedSender = sender ?: this.sender ?: emptyMap()

        ApiManager().shippingLabelApiCallAsync(
            apiKey = resolvedApiKey,
            token = resolvedToken,
            bitmap = bitmap,
            barcodeList = list,
            locationId = resolvedLocationId,
            options = resolvedOptions,
            metadata = resolvedMetadata,
            recipient = resolvedRecipient,
            sender = resolvedSender,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    private fun getPredictionBillOfLadingCloud(args: ReadableArray?) {
        try {
            val image = args?.getString(0)
            this.imagePath = image ?: ""
            val barcodeArray = args?.getArray(1)
            val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
            val token = args?.getString(2)
            val apiKey = args?.getString(3)
            val locationId = args?.getString(4)
            val options = args?.getMap(5)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
            val shouldResizeImage = if (args != null && args.size() > 6 && !args.isNull(6)) {
                args.getBoolean(6)
            } else {
                true
            }

            uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
                bitmap?.let {
                    performBillOfLadingCloudPrediction(it, barcodeList, token, apiKey, locationId, options, shouldResizeImage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getPredictionBillOfLadingCloud: ${e.message}", e)
        }
    }

    private fun performBillOfLadingCloudPrediction(
        bitmap: Bitmap,
        list: List<String>,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: Map<String, Any>?,
        shouldResizeImage: Boolean
    ) {
        val resolvedToken = token ?: this.token
        val resolvedApiKey = apiKey ?: this.apiKey
        val resolvedLocationId = locationId ?: this.locationId ?: ""
        val resolvedOptions = options ?: emptyMap()

        ApiManager().billOfLadingApiCallAsync(
            apiKey = resolvedApiKey,
            token = resolvedToken,
            locationId = resolvedLocationId.takeIf { it.isNotEmpty() },
            options = resolvedOptions,
            bitmap = bitmap,
            barcodeList = list,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    private fun getPredictionItemLabelCloud(args: ReadableArray?) {
        try {
            val image = args?.getString(0)
            this.imagePath = image ?: ""
            val token = args?.getString(1)
            val apiKey = args?.getString(2)
            val shouldResizeImage = if (args != null && args.size() > 3 && !args.isNull(3)) {
                args.getBoolean(3)
            } else {
                true
            }

            uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
                bitmap?.let {
                    performItemLabelCloudPrediction(it, token, apiKey, shouldResizeImage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getPredictionItemLabelCloud: ${e.message}", e)
        }
    }

    private fun performItemLabelCloudPrediction(
        bitmap: Bitmap,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Boolean
    ) {
        val resolvedToken = token ?: this.token
        val resolvedApiKey = apiKey ?: this.apiKey

        ApiManager().itemLabelApiCallAsync(
            apiKey = resolvedApiKey,
            token = resolvedToken,
            bitmap = bitmap,
            shouldResizeImage = shouldResizeImage,
            onScanResult = this
        )
    }

    private fun getPredictionDocumentClassificationCloud(args: ReadableArray?) {
        try {
            val image = args?.getString(0)
            this.imagePath = image ?: ""
            val token = args?.getString(1)
            val apiKey = args?.getString(2)
            val shouldResizeImage = if (args != null && args.size() > 3 && !args.isNull(3)) {
                args.getBoolean(3)
            } else {
                true
            }

            uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
                bitmap?.let {
                    performDocumentClassificationCloudPrediction(it, token, apiKey, shouldResizeImage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getPredictionDocumentClassificationCloud: ${e.message}", e)
        }
    }

    private fun performDocumentClassificationCloudPrediction(
        bitmap: Bitmap,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Boolean
    ) {
        val resolvedToken = token ?: this.token
        val resolvedApiKey = apiKey ?: this.apiKey

        ApiManager().documentClassificationApiCallAsync(
            apiKey = resolvedApiKey,
            token = resolvedToken,
            bitmap = bitmap,
            onScanResult = this,
            shouldResizeImage = shouldResizeImage
        )
    }

    // MARK: - Report Error Command

    private fun reportError(args: ReadableArray?) {
        try {
            val data = args?.getMap(0)
            val token = args?.getString(1)
            val apiKey = args?.getString(2)

            val resolvedToken = token ?: this.token
            val resolvedApiKey = apiKey ?: this.apiKey

            if (data == null) {
                Log.e(TAG, "reportError: Data is null")
                return
            }

            // Extension function to safely convert Any to Map<String, Any?>
            fun Any?.asStringMap(): Map<String, Any?>? {
                return if (this is Map<*, *>) {
                    this.entries
                        .filter { it.key is String }
                        .associate { it.key as String to it.value }
                } else {
                    null
                }
            }

            // Convert ReadableMap to a Kotlin Map
            val parsedData = data.toHashMap()

            // Extract properties with default values
            val reportText = parsedData["reportText"] as? String ?: "No Report Text"
            val modelType = parsedData["type"] as? String ?: "shipping_label"
            val modelSizeStr = parsedData["size"] as? String ?: "large"
            val imagePath = parsedData["image"] as? String

            // Safely handle 'response' as Map<String, Any?> or null
            val response = parsedData["response"]?.asStringMap()

            val errorFlags: Map<String, Boolean> = parsedData["errorFlags"]?.asStringMap()?.mapValues {
                it.value as? Boolean ?: false
            } ?: emptyMap()

            val modelToReport = when (modelType.lowercase().replace("-", "_")) {
                "shipping_label" -> {
                    SLModelToReport(
                        this.modelSize,
                        trackingNo = errorFlags["trackingNo"] ?: false,
                        courierName = errorFlags["courierName"] ?: false,
                        weight = errorFlags["weight"] ?: false,
                        dimensions = errorFlags["dimensions"] ?: false,
                        receiverName = errorFlags["receiverName"] ?: false,
                        receiverAddress = errorFlags["receiverAddress"] ?: false,
                        senderName = errorFlags["senderName"] ?: false,
                        senderAddress = errorFlags["senderAddress"] ?: false
                    )
                }
                "item_label" -> {
                    ILModelToReport(
                        this.modelSize,
                        supplierName = errorFlags["supplierName"] ?: false,
                        itemName = errorFlags["itemName"] ?: false,
                        itemSKU = errorFlags["itemSKU"] ?: false,
                        weight = errorFlags["weight"] ?: false,
                        dimensions = errorFlags["dimensions"] ?: false
                    )
                }
                "bill_of_lading" -> {
                    BOLModelToReport(
                        this.modelSize,
                        referenceNo = errorFlags["referenceNo"] ?: false,
                        loadNumber = errorFlags["loadNumber"] ?: false,
                        purchaseOrderNumber = errorFlags["purchaseOrderNumber"] ?: false,
                        invoiceNumber = errorFlags["invoiceNumber"] ?: false,
                        customerPurchaseOrderNumber = errorFlags["customerPurchaseOrderNumber"] ?: false,
                        orderNumber = errorFlags["orderNumber"] ?: false,
                        billOfLading = errorFlags["billOfLading"] ?: false,
                        masterBillOfLading = errorFlags["masterBillOfLading"] ?: false,
                        lineBillOfLading = errorFlags["lineBillOfLading"] ?: false,
                        houseBillOfLading = errorFlags["houseBillOfLading"] ?: false,
                        shippingId = errorFlags["shippingId"] ?: false,
                        shippingDate = errorFlags["shippingDate"] ?: false,
                        date = errorFlags["date"] ?: false
                    )
                }
                "document_classification" -> {
                    DCModelToReport(
                        this.modelSize,
                        documentClass = errorFlags["documentClass"] ?: false
                    )
                }
                else -> {
                    Log.e(TAG, "reportError: Unknown model type")
                    return
                }
            }

            Log.d(TAG, """
                Report Error:
                - Report Text: $reportText
                - Model Type: $modelType
                - Model Size: $modelSizeStr
                - Image Path: $imagePath
                - Response: $response
            """.trimIndent())

            // Convert image path to Base64 if available
            val base64Image = imagePath?.takeIf { it.isNotBlank() }?.let { convertImageToBase64(it) }

            // API call
            ApiManager().reportAnIssueAsync(
                context = appContext,
                apiKey = resolvedApiKey,
                token = resolvedToken,
                platformType = PlatformType.ReactNative,
                modelToReport = modelToReport,
                report = reportText,
                customData = response,
                base64ImageToReportOn = base64Image,
                onComplete = { result ->
                    Log.d(TAG, "Report completed with result: $result")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "ERROR REPORTING ERROR: ", e)
        }
    }

    private fun convertImageToBase64(inputPath: String): String? {
        return try {
            Log.d(TAG, "Input path: $inputPath")

            // Check if the input path is a URL
            val uri = Uri.parse(inputPath)
            val bitmap = if (uri.scheme == "http" || uri.scheme == "https") {
                // Handle remote URL
                val url = java.net.URL(inputPath)
                android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream())
            } else {
                // Handle local file path
                val file = File(uri.path ?: inputPath)
                if (!file.exists()) {
                    Log.e(TAG, "File does not exist: ${file.absolutePath}")
                    return null
                }
                val inputStream = FileInputStream(file)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap")
                return null
            }

            // Convert bitmap to Base64
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to Base64: ${e.message}", e)
            null
        }
    }

    private fun setModelSize(size: String) {
        modelSize = when (size.lowercase()) {
            "nano" -> ModelSize.Nano
            "micro" -> ModelSize.Micro
            "small" -> ModelSize.Small
            "medium" -> ModelSize.Medium
            "large" -> ModelSize.Large
            "xlarge" -> ModelSize.XLarge
            else -> ModelSize.Large
        }
        Log.d(TAG, "Model size set to: $modelSize")
    }

    private fun setModelType(type: String) {
        // Normalize type string: replace hyphens with underscores
        val normalizedType = type.lowercase().replace("-", "_")
        modelType = when (normalizedType) {
            "shipping_label" -> OCRModule.ShippingLabel(modelSize)
            "bill_of_lading" -> OCRModule.BillOfLading(modelSize)
            "item_label" -> OCRModule.ItemLabel(modelSize)
            "document_classification" -> OCRModule.DocumentClassification(modelSize)
            else -> OCRModule.ShippingLabel(modelSize)
        }
        Log.d(TAG, "Model type set to: $modelType")
    }
}
