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
import io.packagex.visionsdk.ui.views.VisionCameraView
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

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
            val event = Arguments.createMap().apply {
                val barcodeRectsArray = Arguments.createArray()
                val qrCodeRectsArray = Arguments.createArray()
                val documentsRect = Arguments.createMap()

                // Convert barcode bounding boxes with full metadata
                if (barcodeBoundingBoxes.isNotEmpty()) {
                    barcodeBoundingBoxes.forEach { code ->
                        barcodeRectsArray.pushMap(Arguments.createMap().apply {
                            putString("scannedCode", code.scannedCode)
                            putString("symbology", code.symbology.toString())

                            // Add GS1 extracted info if available
                            if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                                val gs1Map = Arguments.createMap()
                                code.gs1ExtractedInfo?.forEach { (key, value) ->
                                    gs1Map.putString(key, value)
                                }
                                putMap("gs1ExtractedInfo", gs1Map)
                            } else {
                                putMap("gs1ExtractedInfo", Arguments.createMap())
                            }

                            putMap("boundingBox", Arguments.createMap().apply {
                                code.boundingBox?.let { box ->
                                    putInt("x", box.left.toDp(density))
                                    putInt("y", box.top.toDp(density))
                                    putInt("width", box.width().toDp(density))
                                    putInt("height", box.height().toDp(density))
                                }
                            })
                        })
                    }
                }

                // Convert QR code bounding boxes with full metadata
                if (qrCodeBoundingBoxes.isNotEmpty()) {
                    qrCodeBoundingBoxes.forEach { code ->
                        qrCodeRectsArray.pushMap(Arguments.createMap().apply {
                            putString("scannedCode", code.scannedCode)
                            putString("symbology", code.symbology.toString())

                            // Add GS1 extracted info if available
                            if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                                val gs1Map = Arguments.createMap()
                                code.gs1ExtractedInfo?.forEach { (key, value) ->
                                    gs1Map.putString(key, value)
                                }
                                putMap("gs1ExtractedInfo", gs1Map)
                            } else {
                                putMap("gs1ExtractedInfo", Arguments.createMap())
                            }

                            putMap("boundingBox", Arguments.createMap().apply {
                                code.boundingBox?.let { box ->
                                    putInt("x", box.left.toDp(density))
                                    putInt("y", box.top.toDp(density))
                                    putInt("width", box.width().toDp(density))
                                    putInt("height", box.height().toDp(density))
                                }
                            })
                        })
                    }
                }

                // Convert document bounding box
                if (documentBoundingBox != null) {
                    documentsRect.putInt("x", documentBoundingBox.left.toDp(density))
                    documentsRect.putInt("y", documentBoundingBox.top.toDp(density))
                    documentsRect.putInt("width", documentBoundingBox.width().toDp(density))
                    documentsRect.putInt("height", documentBoundingBox.height().toDp(density))
                }

                putArray("barcodeBoundingBoxes", barcodeRectsArray)
                putArray("qrCodeBoundingBoxes", qrCodeRectsArray)
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
            val event = Arguments.createMap().apply {
                val codesArray = Arguments.createArray()

                barcodeList.forEach { code ->
                    val codeMap = Arguments.createMap().apply {
                        putString("scannedCode", code.scannedCode)
                        putString("symbology", code.symbology.toString())

                        val boundingBox = Arguments.createMap()
                        code.boundingBox?.let { box ->
                            boundingBox.putInt("x", box.left)
                            boundingBox.putInt("y", box.top)
                            boundingBox.putInt("width", box.width())
                            boundingBox.putInt("height", box.height())
                        }
                        putMap("boundingBox", boundingBox)

                        code.gs1ExtractedInfo?.let { gs1Info ->
                            val gs1Map = Arguments.createMap()
                            gs1Info.forEach { (key, value) ->
                                gs1Map.putString(key, value)
                            }
                            putMap("gs1ExtractedInfo", gs1Map)
                        }
                    }
                    codesArray.pushMap(codeMap)
                }

                putArray("codes", codesArray)
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

        ApiManager().shippingLabelApiCallAsync(
            apiKey = apiKey,
            token = token,
            bitmap = bitmap,
            barcodeList = barcodes,
            locationId = locationId,
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

        ApiManager().billOfLadingApiCallAsync(
            apiKey = apiKey,
            token = token,
            bitmap = bitmap,
            barcodeList = barcodes,
            locationId = locationId,
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
            putString("data", response)
            putString("imagePath", imagePath)
        }
        sendEvent("onOCRScan", event)
    }

    override fun onOCRResponseFailed(exception: VisionSDKException) {
        Log.e(TAG, "onOCRResponseFailed: ${exception.message}")
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
            "captureImage" to 1,
            "stopRunning" to 2,
            "startRunning" to 3,
            "restartScanning" to 4,
            "configureOnDeviceModel" to 6,
            "setFocusSettings" to 8,
            "setObjectDetectionSettings" to 9,
            "setCameraSettings" to 10,
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
            "captureImage", "1" -> captureImage(root)
            "stopRunning", "2" -> stopRunning(root)
            "startRunning", "3" -> startRunning(root)
            "restartScanning", "4" -> restartScanning(root)
            "configureOnDeviceModel", "6" -> configureOnDeviceModel(args)
            "setFocusSettings", "8" -> setFocusSettings(root, args)
            "setObjectDetectionSettings", "9" -> setObjectDetectionSettings(root, args)
            "setCameraSettings", "10" -> setCameraSettings(root, args)
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
        Log.d(TAG, "setObjectDetectionSettings: $settingsJson")

        try {
            val json = JSONObject(settingsJson)

            val detectionConfig = ObjectDetectionConfiguration(
                isTextIndicationOn = json.optBoolean("isTextIndicationOn", true),
                isBarcodeOrQRCodeIndicationOn = json.optBoolean("isBarcodeOrQRCodeIndicationOn", true),
                isDocumentIndicationOn = json.optBoolean("isDocumentIndicationOn", true),
                secondsToWaitBeforeDocumentCapture = json.optInt("secondsToWaitBeforeDocumentCapture", 2)
            )

            view.setObjectDetectionConfiguration(detectionConfig)
            Log.d(TAG, "Object detection settings applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying object detection settings: ${e.message}")
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
        Log.d(TAG, "createTemplate called")
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
        Log.d(TAG, "getAllTemplates called")
        val templateManager = TemplateManager()
        val templates = templateManager.getAllBarcodeTemplates()

        val arr = Arguments.createArray().apply {
            templates.forEach { pushString(it.name) }
        }

        val event = Arguments.createMap().apply {
            putArray("data", arr)
        }
        sendEvent("onGetTemplates", event)
    }

    private fun deleteTemplateWithId(args: ReadableArray?) {
        val templateManager = TemplateManager()
        val id = args?.getString(0)
        Log.d(TAG, "deleteTemplateWithId called: $id")

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
        Log.d(TAG, "deleteAllTemplates called")
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
