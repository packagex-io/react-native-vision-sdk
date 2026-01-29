package com.visionsdk

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.visionsdk.NativeVisionSdkModuleSpec
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.ApiManager
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.interfaces.ResponseCallback
import kotlinx.coroutines.*
import com.visionsdk.utils.EventUtils
import com.visionsdk.utils.uriToBitmap
import com.visionsdk.utils.getModelType
import org.json.JSONArray
import org.json.JSONObject
import io.packagex.visionsdk.modelmanagement.api.ModelManager
import io.packagex.visionsdk.modelmanagement.api.ModelLifecycleListener
import io.packagex.visionsdk.modelmanagement.error.ModelException
import io.packagex.visionsdk.modelmanagement.model.DownloadProgress
import io.packagex.visionsdk.modelmanagement.model.ModelInfo
import io.packagex.visionsdk.modelmanagement.model.ModelUpdateInfo
import io.packagex.visionsdk.ocr.ml.core.enums.ExecutionProvider
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import io.packagex.visionsdk.ocr.ml.core.enums.PlatformType
import io.packagex.visionsdk.ocr.ml.core.OnDeviceOCRManager
import io.packagex.visionsdk.dto.ScannedCodeResult
import io.packagex.visionsdk.dto.BarcodeSymbology
import io.packagex.visionsdk.ocr.ml.core.model_options.BillOfLadingOptions
import io.packagex.visionsdk.ocr.ml.core.model_options.ItemLabelOptions

/**
 * TurboModule implementation for VisionCore (New Architecture)
 * This class implements the spec defined in NativeVisionSdkModule.ts
 */
@ReactModule(name = VisionSdkModule.NAME)
class VisionSdkModule(reactContext: ReactApplicationContext) :
    NativeVisionSdkModuleSpec(reactContext) {

    companion object {
        const val NAME = "VisionSdkModule"
        const val TAG = "VisionSDK TurboModule"
    }

    override fun getName(): String = NAME

    private fun getModelSize(modelSizeStr: String?): ModelSize {
        return when (modelSizeStr?.lowercase()) {
            "nano" -> ModelSize.Nano
            "micro" -> ModelSize.Micro
            "small" -> ModelSize.Small
            "medium" -> ModelSize.Medium
            "large" -> ModelSize.Large
            "xlarge" -> ModelSize.XLarge
            else -> ModelSize.Large
        }
    }

    private fun getEnvironment(environmentStr: String): Environment {
        return when (environmentStr.lowercase()) {
            "staging" -> Environment.STAGING
            "qa" -> Environment.QA
            "production" -> Environment.PRODUCTION
            "dev" -> Environment.DEV
            "sandbox" -> Environment.SANDBOX
            else -> Environment.STAGING
        }
    }

    // ============================================================================
    // MODEL MANAGEMENT HELPER FUNCTIONS
    // ============================================================================

    /**
     * Parses JSON string to OCRModule
     */
    private fun parseOCRModule(moduleJson: String): OCRModule {
        val json = JSONObject(moduleJson)
        val type = json.getString("type")
        val sizeStr = json.optString("size", "large")
        val modelSize = getModelSize(sizeStr)

        val options = json.optJSONObject("options")
        val enableAdditionalAttributes = options?.optBoolean("enableAdditionalAttributes", false) ?: false

        return when (type.lowercase()) {
            "shipping_label", "shipping-label" -> OCRModule.ShippingLabel(modelSize)
            "bill_of_lading", "bill-of-lading" -> {
                val bolOptions = BillOfLadingOptions(enableAdditionalAttributes)
                OCRModule.BillOfLading(modelSize, bolOptions)
            }
            "item_label", "item-label" -> {
                val ilOptions = ItemLabelOptions(enableAdditionalAttributes)
                OCRModule.ItemLabel(modelSize, ilOptions)
            }
            "document_classification", "document-classification" -> OCRModule.DocumentClassification(modelSize)
            else -> OCRModule.ShippingLabel(modelSize)
        }
    }

    /**
     * Parses string to PlatformType enum
     */
    private fun parsePlatformType(platformTypeStr: String): PlatformType {
        return when (platformTypeStr.lowercase()) {
            "native" -> PlatformType.Native
            "flutter" -> PlatformType.Flutter
            "react_native", "reactnative" -> PlatformType.ReactNative
            else -> PlatformType.ReactNative // Default for React Native
        }
    }

    /**
     * Parses string to ExecutionProvider enum
     */
    private fun parseExecutionProvider(providerStr: String?): ExecutionProvider {
        return when (providerStr?.uppercase()) {
            "CPU" -> ExecutionProvider.CPU
            "NNAPI" -> ExecutionProvider.NNAPI
            "XNNPACK" -> ExecutionProvider.XNNPACK
            else -> ExecutionProvider.CPU // Default to CPU for maximum compatibility
        }
    }

    /**
     * Serializes ModelInfo to JSON string
     */
    private fun modelInfoToJson(modelInfo: ModelInfo): String {
        val moduleJson = JSONObject().apply {
            // Serialize OCRModule
            val moduleType = when (modelInfo.module) {
                is OCRModule.ShippingLabel -> "shipping_label"
                is OCRModule.BillOfLading -> "bill_of_lading"
                is OCRModule.ItemLabel -> "item_label"
                is OCRModule.DocumentClassification -> "document_classification"
                else -> "shipping_label"
            }
            put("type", moduleType)

            modelInfo.module.modelSize?.let { size ->
                put("size", size.value)
            }

            // Add options if applicable
            when (modelInfo.module) {
                is OCRModule.BillOfLading -> {
                    val bolModule = modelInfo.module as OCRModule.BillOfLading
                    if (bolModule.billOfLadingOptions.enableAdditionalAttributes) {
                        put("options", JSONObject().apply {
                            put("enableAdditionalAttributes", true)
                        })
                    }
                }
                is OCRModule.ItemLabel -> {
                    val ilModule = modelInfo.module as OCRModule.ItemLabel
                    if (ilModule.itemLabelOptions.enableAdditionalAttributes) {
                        put("options", JSONObject().apply {
                            put("enableAdditionalAttributes", true)
                        })
                    }
                }
                else -> {}
            }
        }

        return JSONObject().apply {
            put("module", moduleJson)
            put("version", modelInfo.version)
            put("versionId", modelInfo.versionId)
            put("dateString", modelInfo.dateString)
            put("isLoaded", modelInfo.isLoaded)
        }.toString()
    }

    /**
     * Serializes list of ModelInfo to JSON array string
     */
    private fun modelInfoListToJson(modelInfoList: List<ModelInfo>): String {
        val jsonArray = JSONArray()
        modelInfoList.forEach { modelInfo ->
            jsonArray.put(JSONObject(modelInfoToJson(modelInfo)))
        }
        return jsonArray.toString()
    }

    /**
     * Converts ModelInfo to module JSON string (just the module part)
     */
    /**
     * Converts ModelInfo to JSONObject (for use in nested objects)
     */
    private fun modelInfoToModuleJsonObject(modelInfo: ModelInfo): JSONObject {
        val moduleType = when (modelInfo.module) {
            is OCRModule.ShippingLabel -> "shipping_label"
            is OCRModule.BillOfLading -> "bill_of_lading"
            is OCRModule.ItemLabel -> "item_label"
            is OCRModule.DocumentClassification -> "document_classification"
            else -> "shipping_label"
        }

        return JSONObject().apply {
            put("type", moduleType)

            modelInfo.module.modelSize?.let { size ->
                put("size", size.value)
            }

            // Add options if applicable
            when (modelInfo.module) {
                is OCRModule.BillOfLading -> {
                    val bolModule = modelInfo.module as OCRModule.BillOfLading
                    if (bolModule.billOfLadingOptions.enableAdditionalAttributes) {
                        put("options", JSONObject().apply {
                            put("enableAdditionalAttributes", true)
                        })
                    }
                }
                is OCRModule.ItemLabel -> {
                    val ilModule = modelInfo.module as OCRModule.ItemLabel
                    if (ilModule.itemLabelOptions.enableAdditionalAttributes) {
                        put("options", JSONObject().apply {
                            put("enableAdditionalAttributes", true)
                        })
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Converts ModelInfo to JSON string (for use as top-level response)
     */
    private fun modelInfoToModuleJson(modelInfo: ModelInfo): String {
        return modelInfoToModuleJsonObject(modelInfo).toString()
    }


    /**
     * Serializes ModelUpdateInfo to JSON string
     */
    private fun modelUpdateInfoToJson(updateInfo: ModelUpdateInfo): String {
        val moduleJson = JSONObject().apply {
            val moduleType = when (updateInfo.module) {
                is OCRModule.ShippingLabel -> "shipping_label"
                is OCRModule.BillOfLading -> "bill_of_lading"
                is OCRModule.ItemLabel -> "item_label"
                is OCRModule.DocumentClassification -> "document_classification"
                else -> "shipping_label"
            }
            put("type", moduleType)

            updateInfo.module.modelSize?.let { size ->
                put("size", size.value)
            }
        }

        return JSONObject().apply {
            put("module", moduleJson)
            put("currentVersion", updateInfo.currentVersion)
            put("latestVersion", updateInfo.latestVersion)
            put("updateAvailable", updateInfo.updateAvailable)
            put("message", updateInfo.message)
        }.toString()
    }

    /**
     * Sends model download progress event with requestId
     */
    private fun sendDownloadProgressEvent(
        requestId: String,
        progress: DownloadProgress
    ) {
        val event = Arguments.createMap().apply {
            putDouble("progress", progress.progress.toDouble())
            putString("requestId", requestId)

            // Serialize module
            val moduleMap = Arguments.createMap().apply {
                val moduleType = when (progress.module) {
                    is OCRModule.ShippingLabel -> "shipping_label"
                    is OCRModule.BillOfLading -> "bill_of_lading"
                    is OCRModule.ItemLabel -> "item_label"
                    is OCRModule.DocumentClassification -> "document_classification"
                    else -> "shipping_label"
                }
                putString("type", moduleType)

                progress.module.modelSize?.let { size ->
                    putString("size", size.value)
                }
            }
            putMap("module", moduleMap)
        }

        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onModelDownloadProgress", event)
    }

    // ============================================================================
    // MODEL MANAGEMENT LIFECYCLE LISTENER
    // ============================================================================
    // TODO: Uncomment when using unobfuscated SDK version
    /*
    private val modelLifecycleListener = object : ModelLifecycleListener {
        override fun onDownloadStarted(module: OCRModule) {
            Log.d(TAG, "⬇️ Download started: $module")
            sendModelLifecycleEvent("onDownloadStarted", module)
        }

        override fun onDownloadCompleted(module: OCRModule) {
            Log.d(TAG, "✅ Download completed: $module")
            sendModelLifecycleEvent("onDownloadCompleted", module)
        }

        override fun onDownloadFailed(module: OCRModule, exception: ModelException) {
            Log.e(TAG, "❌ Download failed: $module", exception)
            sendModelLifecycleEvent("onDownloadFailed", module, exception.message)
        }

        override fun onDownloadCancelled(module: OCRModule) {
            Log.d(TAG, "🚫 Download cancelled: $module")
            sendModelLifecycleEvent("onDownloadCancelled", module)
        }

        override fun onModelLoaded(module: OCRModule) {
            Log.d(TAG, "🟢 Model loaded: $module")
            sendModelLifecycleEvent("onModelLoaded", module)
        }

        override fun onModelUnloaded(module: OCRModule) {
            Log.d(TAG, "⚪ Model unloaded: $module")
            sendModelLifecycleEvent("onModelUnloaded", module)
        }

        override fun onModelDeleted(module: OCRModule) {
            Log.d(TAG, "🗑️ Model deleted: $module")
            sendModelLifecycleEvent("onModelDeleted", module)
        }
    }

    private fun sendModelLifecycleEvent(
        eventName: String,
        module: OCRModule,
        errorMessage: String? = null
    ) {
        val event = Arguments.createMap().apply {
            val moduleMap = Arguments.createMap().apply {
                val moduleType = when (module) {
                    is OCRModule.ShippingLabel -> "shipping_label"
                    is OCRModule.BillOfLading -> "bill_of_lading"
                    is OCRModule.ItemLabel -> "item_label"
                    is OCRModule.DocumentClassification -> "document_classification"
                    else -> "shipping_label"
                }
                putString("type", moduleType)

                module.modelSize?.let { size ->
                    putString("size", size.value)
                }
            }
            putMap("module", moduleMap)

            errorMessage?.let {
                putString("error", it)
            }
        }

        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, event)
    }
    */

    @ReactMethod
    override fun setEnvironment(environment: String) {
        val env = getEnvironment(environment)
        VisionSdkSingleton.initializeSdk(reactApplicationContext, env)
    }

    @ReactMethod
    override fun loadOnDeviceModels(
        token: String?,
        apiKey: String?,
        modelType: String,
        modelSize: String,
        promise: Promise
    ) {
        // Validate Model Type & Size
        val modelSizeEnum = getModelSize(modelSize)
        val resolvedModelType = getModelType(modelType, modelSizeEnum)

        // Use Singleton to get OnDeviceOCRManager instance
        val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(reactApplicationContext, resolvedModelType)
        val isModelAlreadyDownloaded = onDeviceOCRManager.isModelAlreadyDownloaded()

        if (OnDeviceOCRManagerSingleton.isModelConfigured(resolvedModelType)) {
            EventUtils.sendModelDownloadProgressEvent(reactApplicationContext, progress = 1.0, downloadStatus = true, isReady = true)
            promise.resolve("Model is already configured and ready")
            return
        }

        // Launch Coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onDeviceOCRManager?.configure(apiKey, token) { progress ->
                    EventUtils.sendModelDownloadProgressEvent(
                        reactApplicationContext,
                        progress = progress.toDouble(),
                        downloadStatus = progress >= 1.0,
                        isReady = progress >= 1.0
                    )
                }

                EventUtils.sendModelDownloadProgressEvent(reactApplicationContext, progress = 1.0, downloadStatus = isModelAlreadyDownloaded, isReady = true)
                promise.resolve("Model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading model", e)
                promise.reject("MODEL_LOAD_FAILED", "Failed to load on-device model: ${e.message}")
            }
        }
    }

    // DEPRECATED - Use unloadModel() or deleteModel() instead
    // @ReactMethod
    // override fun unLoadOnDeviceModels(
    //     modelType: String?,
    //     shouldDeleteFromDisk: Boolean,
    //     promise: Promise
    // ) {
    //     Log.d(TAG, "🗑️ Unloading model: $modelType, deleteFromDisk: $shouldDeleteFromDisk")
    //
    //     try {
    //         if (modelType == null) {
    //             // Unload all models
    //             OnDeviceOCRManagerSingleton.destroy()
    //             promise.resolve("All models unloaded successfully")
    //         } else {
    //             // Check if requested model matches current model
    //             val modelSizeEnum = ModelSize.Large // Default size for comparison
    //             val requestedModelType = getModelType(modelType, modelSizeEnum)
    //
    //             if (OnDeviceOCRManagerSingleton.isModelConfigured(requestedModelType)) {
    //                 OnDeviceOCRManagerSingleton.destroy()
    //                 promise.resolve("Model '$modelType' unloaded successfully")
    //             } else {
    //                 promise.resolve("Model '$modelType' is not currently loaded")
    //             }
    //         }
    //     } catch (e: Exception) {
    //         Log.e(TAG, "❌ Error unloading model", e)
    //         promise.reject("MODEL_UNLOAD_ERROR", "Failed to unload model: ${e.message}", e)
    //     }
    // }

    @ReactMethod
    override fun logItemLabelDataToPx(
        imageUri: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        responseData: String,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Boolean,
        metadata: String,
        promise: Promise
    ) {
        Log.d(TAG, "logItemLabelDataToPx called")

        val uri = Uri.parse(imageUri)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imageUri")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Parse metadata JSON string to Map
                val metadataMap = try {
                    val jsonObject = JSONObject(metadata)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata JSON, using empty map", e)
                    emptyMap()
                }

                Log.d(TAG, "ondeviceresponse:\n $responseData")

                ApiManager().itemLabelMatchingApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    shouldResizeImage = shouldResizeImage,
                    barcodeList = barcodeList,
                    metadata = metadataMap,
                    onDeviceResponse = responseData,
                    onResponseCallback = object : ResponseCallback {
                        override fun onError(visionException: VisionSDKException) {
                            Log.e(TAG, "Item label match failed", visionException)
                            promise.reject("MATCH_FAILED", "Item label match failed: ${visionException.message}", visionException)
                        }

                        override fun onResponse(response: String) {
                            Log.d(TAG, "Item label match success: $response")
                            promise.resolve("Item label match successful: $response")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in logItemLabelDataToPx", e)
                promise.reject("PROCESSING_FAILED", "Error during processing: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun logShippingLabelDataToPx(
        imageUri: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        responseData: String,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: String,
        metadata: String,
        recipient: String,
        sender: String,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {
        Log.d(TAG, "logShippingLabelDataToPx called with imageUri: $imageUri")

        val uri = Uri.parse(imageUri)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imageUri")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Parse JSON strings to Maps
                val optionsMap = try {
                    val jsonObject = JSONObject(options)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse options JSON", e)
                    emptyMap()
                }

                val metadataMap = try {
                    val jsonObject = JSONObject(metadata)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata JSON", e)
                    emptyMap()
                }

                val recipientMap = try {
                    val jsonObject = JSONObject(recipient)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse recipient JSON", e)
                    null
                }

                val senderMap = try {
                    val jsonObject = JSONObject(sender)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse sender JSON", e)
                    null
                }

                Log.d(TAG, "ondeviceresponse:\n $responseData")

                ApiManager().shippingLabelMatchingApiAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    shouldResizeImage = shouldResizeImage,
                    barcodeList = barcodeList,
                    onDeviceResponse = responseData,
                    locationId = locationId,
                    recipient = recipientMap,
                    sender = senderMap,
                    options = optionsMap,
                    metadata = metadataMap,
                    onResponseCallback = object : ResponseCallback {
                        override fun onError(visionException: VisionSDKException) {
                            Log.e(TAG, "Shipping label match failed", visionException)
                            promise.reject("MATCH_FAILED", "Shipping label match failed: ${visionException.message}", visionException)
                        }

                        override fun onResponse(response: String) {
                            Log.d(TAG, "Shipping label match success: $response")
                            promise.resolve("Shipping label match successful: $response")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in logShippingLabelDataToPx", e)
                promise.reject("PROCESSING_FAILED", "Error during processing: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun logBillOfLadingDataToPx(
        imageUri: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        responseData: String,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: String,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {
        Log.d(TAG, "logBillOfLadingDataToPx called")

        val uri = Uri.parse(imageUri)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imageUri")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Parse options JSON string to Map
                val optionsMap = try {
                    val jsonObject = JSONObject(options)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse options JSON", e)
                    emptyMap()
                }

                Log.d(TAG, "ondeviceresponse:\n $responseData")

                // Use the regular API call for Bill of Lading
                ApiManager().billOfLadingApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    barcodeList = barcodeList,
                    locationId = locationId,
                    options = optionsMap,
                    onScanResult = object : OCRResult {
                        override fun onOCRResponse(response: String) {
                            Log.d(TAG, "Bill of lading success: $response")
                            promise.resolve(response)
                        }

                        override fun onOCRResponseFailed(error: VisionSDKException) {
                            Log.e(TAG, "Bill of lading failed", error)
                            promise.reject("API_ERROR", "Bill of lading failed: ${error.message}", error)
                        }
                    },
                    shouldResizeImage = shouldResizeImage
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in logBillOfLadingDataToPx", e)
                promise.reject("PROCESSING_FAILED", "Error during processing: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun logDocumentClassificationDataToPx(
        imageUri: String,
        responseData: String,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {

        val uri = Uri.parse(imageUri)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imageUri")
                return@uriToBitmap
            }

            try {
                Log.d(TAG, "Processing document classification")

                // Use the regular API call for Document Classification
                ApiManager().documentClassificationApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    onScanResult = object : OCRResult {
                        override fun onOCRResponse(response: String) {
                            Log.d(TAG, "Document classification success: $response")
                            promise.resolve(response)
                        }

                        override fun onOCRResponseFailed(error: VisionSDKException) {
                            Log.e(TAG, "Document classification failed", error)
                            promise.reject("API_ERROR", "Document classification failed: ${error.message}", error)
                        }
                    },
                    shouldResizeImage = shouldResizeImage
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in logDocumentClassificationDataToPx", e)
                promise.reject("PROCESSING_FAILED", "Error during processing: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun predict(
        imagePath: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        promise: Promise
    ) {
        Log.d(TAG, "🔹 Standalone On-Device Prediction for: $imagePath")

        val uri = Uri.parse(imagePath)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imagePath")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Get the currently configured on-device OCR manager
                val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getCurrentInstance()

                if (onDeviceOCRManager == null) {
                    promise.reject("MODEL_NOT_CONFIGURED", "No on-device model is configured. Please load a model first.")
                    return@uriToBitmap
                }

                // Launch coroutine for on-device prediction
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = onDeviceOCRManager.getPredictions(bitmap, barcodeList)

                        Handler(Looper.getMainLooper()).post {
                            if (result != null) {
                                promise.resolve(result)
                            } else {
                                promise.reject("PREDICTION_ERROR", "On-device prediction returned null result")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "On-device prediction failed", e)
                        Handler(Looper.getMainLooper()).post {
                            promise.reject("PREDICTION_FAILED", "On-device prediction failed: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in predict", e)
                promise.reject("PROCESSING_FAILED", "Error during prediction: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun predictShippingLabelCloud(
        imagePath: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: String,
        metadata: String,
        recipient: String,
        sender: String,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {
        Log.d(TAG, "🔹 Standalone Cloud Shipping Label Prediction for: $imagePath")

        val uri = Uri.parse(imagePath)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imagePath")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Parse JSON strings to Maps
                val optionsMap = try {
                    val jsonObject = JSONObject(options)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse options JSON", e)
                    emptyMap()
                }

                val metadataMap = try {
                    val jsonObject = JSONObject(metadata)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata JSON", e)
                    emptyMap()
                }

                val recipientMap = try {
                    val jsonObject = JSONObject(recipient)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse recipient JSON", e)
                    null
                }

                val senderMap = try {
                    val jsonObject = JSONObject(sender)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse sender JSON", e)
                    null
                }

                ApiManager().shippingLabelApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    barcodeList = barcodeList,
                    locationId = locationId,
                    options = optionsMap,
                    metadata = metadataMap,
                    recipient = recipientMap,
                    sender = senderMap,
                    onScanResult = object : OCRResult {
                        override fun onOCRResponse(response: String) {
                            promise.resolve(response)
                        }
                        override fun onOCRResponseFailed(error: VisionSDKException) {
                            Log.e(TAG, "Shipping label cloud prediction failed", error)
                            promise.reject("API_ERROR", "Shipping label prediction failed: ${error.message}", error)
                        }
                    },
                    shouldResizeImage = shouldResizeImage
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in predictShippingLabelCloud", e)
                promise.reject("PROCESSING_FAILED", "Error during cloud prediction: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun predictItemLabelCloud(
        imagePath: String,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {

        val uri = Uri.parse(imagePath)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imagePath")
                return@uriToBitmap
            }

            try {
                ApiManager().itemLabelApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    onScanResult = object : OCRResult {
                        override fun onOCRResponse(response: String) {
                            promise.resolve(response)
                        }
                        override fun onOCRResponseFailed(error: VisionSDKException) {
                            Log.e(TAG, "Item label cloud prediction failed", error)
                            promise.reject("API_ERROR", "Item label prediction failed: ${error.message}", error)
                        }
                    },
                    shouldResizeImage = shouldResizeImage
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in predictItemLabelCloud", e)
                promise.reject("PROCESSING_FAILED", "Error during cloud prediction: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun predictBillOfLadingCloud(
        imagePath: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: String,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {
        Log.d(TAG, "🔹 Standalone Cloud Bill of Lading Prediction for: $imagePath")

        val uri = Uri.parse(imagePath)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imagePath")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Parse options JSON string to Map
                val optionsMap = try {
                    val jsonObject = JSONObject(options)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse options JSON", e)
                    emptyMap()
                }

                ApiManager().billOfLadingApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    barcodeList = barcodeList,
                    locationId = locationId,
                    options = optionsMap,
                    onScanResult = object : OCRResult {
                        override fun onOCRResponse(response: String) {
                            promise.resolve(response)
                        }
                        override fun onOCRResponseFailed(error: VisionSDKException) {
                            Log.e(TAG, "Bill of lading cloud prediction failed", error)
                            promise.reject("API_ERROR", "Bill of lading prediction failed: ${error.message}", error)
                        }
                    },
                    shouldResizeImage = shouldResizeImage
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in predictBillOfLadingCloud", e)
                promise.reject("PROCESSING_FAILED", "Error during cloud prediction: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun predictDocumentClassificationCloud(
        imagePath: String,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {

        val uri = Uri.parse(imagePath)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imagePath")
                return@uriToBitmap
            }

            try {
                ApiManager().documentClassificationApiCallAsync(
                    apiKey = apiKey,
                    token = token,
                    bitmap = bitmap,
                    onScanResult = object : OCRResult {
                        override fun onOCRResponse(response: String) {
                            promise.resolve(response)
                        }
                        override fun onOCRResponseFailed(error: VisionSDKException) {
                            Log.e(TAG, "Document classification cloud prediction failed", error)
                            promise.reject("API_ERROR", "Document classification prediction failed: ${error.message}", error)
                        }
                    },
                    shouldResizeImage = shouldResizeImage
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception in predictDocumentClassificationCloud", e)
                promise.reject("PROCESSING_FAILED", "Error during cloud prediction: ${e.message}", e)
            }
        }
    }

    @ReactMethod
    override fun predictWithCloudTransformations(
        imagePath: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: String,
        metadata: String,
        recipient: String,
        sender: String,
        shouldResizeImage: Boolean,
        promise: Promise
    ) {
        Log.d(TAG, "🔹 Standalone Hybrid Prediction (On-Device + Cloud) for: $imagePath")

        val uri = Uri.parse(imagePath)
        uriToBitmap(reactApplicationContext, uri) { bitmap ->
            if (bitmap == null) {
                promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imagePath")
                return@uriToBitmap
            }

            try {
                val barcodeList = mutableListOf<String>()
                for (i in 0 until barcodes.size()) {
                    barcodes.getString(i)?.let { barcodeList.add(it) }
                }

                // Get the currently configured on-device OCR manager
                val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getCurrentInstance()

                if (onDeviceOCRManager == null) {
                    promise.reject("MODEL_NOT_CONFIGURED", "No on-device model is configured. Please load a model first.")
                    return@uriToBitmap
                }

                // Parse JSON strings to Maps
                val optionsMap = try {
                    val jsonObject = JSONObject(options)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse options JSON", e)
                    emptyMap()
                }

                val metadataMap = try {
                    val jsonObject = JSONObject(metadata)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse metadata JSON", e)
                    emptyMap()
                }

                val recipientMap = try {
                    val jsonObject = JSONObject(recipient)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse recipient JSON", e)
                    null
                }

                val senderMap = try {
                    val jsonObject = JSONObject(sender)
                    jsonObject.keys().asSequence().associateWith { key ->
                        jsonObject.get(key)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse sender JSON", e)
                    null
                }

                // Launch coroutine for hybrid prediction
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Step 1: Get on-device prediction
                        val onDeviceResult = onDeviceOCRManager.getPredictions(bitmap, barcodeList)

                        if (onDeviceResult == null) {
                            Handler(Looper.getMainLooper()).post {
                                promise.reject("ON_DEVICE_ERROR", "On-device prediction returned null result")
                            }
                            return@launch
                        }

                        // Step 2: Send to cloud for transformation
                        Handler(Looper.getMainLooper()).post {
                            ApiManager().shippingLabelMatchingApiAsync(
                                apiKey = apiKey,
                                token = token,
                                bitmap = bitmap,
                                barcodeList = barcodeList,
                                locationId = locationId,
                                options = optionsMap,
                                metadata = metadataMap,
                                recipient = recipientMap,
                                sender = senderMap,
                                onDeviceResponse = onDeviceResult,
                                onResponseCallback = object : ResponseCallback {
                                    override fun onError(visionException: VisionSDKException) {
                                        Log.e(TAG, "Hybrid prediction cloud transformation failed", visionException)
                                        promise.reject("CLOUD_TRANSFORMATION_ERROR", "Cloud transformation failed: ${visionException.message}", visionException)
                                    }
                                    override fun onResponse(response: String) {
                                        promise.resolve(response)
                                    }
                                },
                                shouldResizeImage = shouldResizeImage
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Hybrid prediction failed", e)
                        Handler(Looper.getMainLooper()).post {
                            promise.reject("HYBRID_PREDICTION_FAILED", "Hybrid prediction failed: ${e.message}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in predictWithCloudTransformations", e)
                promise.reject("PROCESSING_FAILED", "Error during hybrid prediction: ${e.message}", e)
            }
        }
    }

    // ============================================================================
    // MODEL MANAGEMENT API METHODS
    // ============================================================================

    @ReactMethod
    override fun initializeModelManager(configJson: String) {
        try {
            val config = JSONObject(configJson)
            val maxConcurrentDownloads = config.optInt("maxConcurrentDownloads", 2)
            val enableLogging = config.optBoolean("enableLogging", true)

            Log.d(TAG, "🔹 Initializing ModelManager with maxConcurrentDownloads=$maxConcurrentDownloads, enableLogging=$enableLogging")

            ModelManager.initialize(reactApplicationContext) {
                maxConcurrentDownloads(maxConcurrentDownloads)
                enableLogging(enableLogging)
                // Lifecycle listener will be added in Phase 2
            }

            Log.d(TAG, "✅ ModelManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize ModelManager", e)
            throw e
        }
    }

    @ReactMethod
    override fun isModelManagerInitialized(): Boolean {
        val isInitialized = ModelManager.isInitialized()
        Log.d(TAG, "🔹 ModelManager.isInitialized() = $isInitialized")
        return isInitialized
    }

    @ReactMethod
    override fun downloadModel(
        moduleJson: String,
        apiKey: String?,
        token: String?,
        platformType: String,
        requestId: String,
        promise: Promise
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val module = parseOCRModule(moduleJson)
                val platform = parsePlatformType(platformType)
                val modelManager = ModelManager.getInstance()

                Log.d(TAG, "🔹 Starting download for module: $module")

                modelManager.downloadModel(
                    module = module,
                    apiKey = apiKey,
                    token = token,
                    platformType = platform,
                    progressListener = { progress ->
                        // Emit progress event to React Native
                        val event = Arguments.createMap().apply {
                            putString("module", moduleJson)
                            putDouble("progress", progress.progress.toDouble())
                            putString("requestId", requestId)
                        }
                        reactApplicationContext
                            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                            .emit("onModelDownloadProgress", event)
                    }
                )

                Log.d(TAG, "✅ Download completed for module: $module")
                promise.resolve(null)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Download failed", e)
                promise.reject("DOWNLOAD_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    override fun cancelDownload(moduleJson: String, promise: Promise) {
        try {
            val module = parseOCRModule(moduleJson)
            val modelManager = ModelManager.getInstance()

            val cancelled = modelManager.cancelDownload(module)
            Log.d(TAG, "🔹 cancelDownload for $module: $cancelled")

            promise.resolve(cancelled)
        } catch (e: Exception) {
            Log.e(TAG, "❌ cancelDownload failed", e)
            promise.reject("CANCEL_FAILED", e.message, e)
        }
    }

    // NOT AVAILABLE IN iOS SDK - COMMENTED OUT FOR API CONSISTENCY
    // @ReactMethod
    // override fun getActiveDownloadCount(): Double {
    //     return try {
    //         val modelManager = ModelManager.getInstance()
    //         val count = modelManager.activeDownloadCount()
    //         Log.d(TAG, "🔹 Active download count: $count")
    //         count.toDouble()
    //     } catch (e: Exception) {
    //         Log.e(TAG, "❌ getActiveDownloadCount failed", e)
    //         0.0
    //     }
    // }

    @ReactMethod
    override fun loadOCRModel(
        moduleJson: String,
        apiKey: String?,
        token: String?,
        platformType: String,
        executionProvider: String?,
        promise: Promise
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val module = parseOCRModule(moduleJson)
                val platform = parsePlatformType(platformType)
                val provider = parseExecutionProvider(executionProvider)
                val modelManager = ModelManager.getInstance()

                Log.d(TAG, "🔹 Loading model: $module with provider: $provider")

                modelManager.loadModel(
                    module = module,
                    apiKey = apiKey,
                    token = token,
                    platformType = platform,
                    executionProvider = provider
                )

                Log.d(TAG, "✅ Model loaded: $module")
                promise.resolve(null)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Load model failed", e)
                promise.reject("LOAD_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    override fun unloadModel(moduleJson: String): Boolean {
        return try {
            val module = parseOCRModule(moduleJson)
            val modelManager = ModelManager.getInstance()

            val unloaded = modelManager.unloadModel(module)
            Log.d(TAG, "🔹 unloadModel for $module: $unloaded")

            unloaded
        } catch (e: Exception) {
            Log.e(TAG, "❌ unloadModel failed", e)
            false
        }
    }

    @ReactMethod
    override fun isModelLoaded(moduleJson: String): Boolean {
        return try {
            val module = parseOCRModule(moduleJson)
            val modelManager = ModelManager.getInstance()

            val isLoaded = modelManager.isModelLoaded(module)
            Log.d(TAG, "🔹 isModelLoaded for $module: $isLoaded")

            isLoaded
        } catch (e: Exception) {
            Log.e(TAG, "❌ isModelLoaded failed", e)
            false
        }
    }

    @ReactMethod
    override fun getLoadedModelCount(): Double {
        return try {
            val modelManager = ModelManager.getInstance()
            val count = modelManager.loadedModelCount()
            Log.d(TAG, "🔹 Loaded model count: $count")
            count.toDouble()
        } catch (e: Exception) {
            Log.e(TAG, "❌ getLoadedModelCount failed", e)
            0.0
        }
    }

    @ReactMethod
    override fun findDownloadedModels(promise: Promise) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelManager = ModelManager.getInstance()
                val models = modelManager.findDownloadedModels()

                val jsonArray = JSONArray()
                models.forEach { modelInfo ->
                    val jsonObject = JSONObject().apply {
                        put("module", modelInfoToModuleJsonObject(modelInfo))
                        put("version", modelInfo.version)
                        put("versionId", modelInfo.versionId)
                        put("dateString", modelInfo.dateString)
                        put("isLoaded", modelInfo.isLoaded)
                    }
                    jsonArray.put(jsonObject)
                }

                Log.d(TAG, "🔹 findDownloadedModels: ${models.size} models")
                promise.resolve(jsonArray.toString())
            } catch (e: Exception) {
                Log.e(TAG, "❌ findDownloadedModels failed", e)
                promise.reject("QUERY_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    override fun findDownloadedModel(moduleJson: String, promise: Promise) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val module = parseOCRModule(moduleJson)
                val modelManager = ModelManager.getInstance()
                val modelInfo = modelManager.findDownloadedModel(module)

                if (modelInfo != null) {
                    val jsonObject = JSONObject().apply {
                        put("module", modelInfoToModuleJsonObject(modelInfo))
                        put("version", modelInfo.version)
                        put("versionId", modelInfo.versionId)
                        put("dateString", modelInfo.dateString)
                        put("isLoaded", modelInfo.isLoaded)
                    }
                    Log.d(TAG, "🔹 findDownloadedModel: found $module")
                    promise.resolve(jsonObject.toString())
                } else {
                    Log.d(TAG, "🔹 findDownloadedModel: not found $module")
                    promise.resolve("null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ findDownloadedModel failed", e)
                promise.reject("QUERY_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    override fun findLoadedModels(promise: Promise) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelManager = ModelManager.getInstance()
                val models = modelManager.findLoadedModels()

                val jsonArray = JSONArray()
                models.forEach { modelInfo ->
                    val jsonObject = JSONObject().apply {
                        put("module", modelInfoToModuleJsonObject(modelInfo))
                        put("version", modelInfo.version)
                        put("versionId", modelInfo.versionId)
                        put("dateString", modelInfo.dateString)
                        put("isLoaded", modelInfo.isLoaded)
                    }
                    jsonArray.put(jsonObject)
                }

                Log.d(TAG, "🔹 findLoadedModels: ${models.size} models")
                promise.resolve(jsonArray.toString())
            } catch (e: Exception) {
                Log.e(TAG, "❌ findLoadedModels failed", e)
                promise.reject("QUERY_FAILED", e.message, e)
            }
        }
    }

    // NOT AVAILABLE IN iOS SDK - COMMENTED OUT FOR API CONSISTENCY
    // @ReactMethod
    // override fun checkModelUpdates(
    //     moduleJson: String,
    //     apiKey: String?,
    //     token: String?,
    //     platformType: String,
    //     promise: Promise
    // ) {
    //     CoroutineScope(Dispatchers.IO).launch {
    //         try {
    //             val module = parseOCRModule(moduleJson)
    //             val platform = parsePlatformType(platformType)
    //             val modelManager = ModelManager.getInstance()
    //
    //             Log.d(TAG, "🔹 Checking model updates for: $module")
    //
    //             val updateInfo = modelManager.checkModelUpdates(
    //                 module = module,
    //                 apiKey = apiKey,
    //                 token = token,
    //                 platformType = platform
    //             )
    //
    //             val jsonObject = JSONObject().apply {
    //                 put("module", modelInfoToModuleJson(io.packagex.visionsdk.modelmanagement.model.ModelInfo(
    //                     module = updateInfo.module,
    //                     version = updateInfo.latestVersion,
    //                     versionId = "",
    //                     dateString = updateInfo.latestVersion,
    //                     isLoaded = false
    //                 )))
    //                 put("currentVersion", updateInfo.currentVersion ?: JSONObject.NULL)
    //                 put("latestVersion", updateInfo.latestVersion)
    //                 put("updateAvailable", updateInfo.updateAvailable)
    //                 put("message", updateInfo.message)
    //             }
    //
    //             Log.d(TAG, "✅ Update check complete: updateAvailable=${updateInfo.updateAvailable}")
    //             promise.resolve(jsonObject.toString())
    //         } catch (e: Exception) {
    //             Log.e(TAG, "❌ checkModelUpdates failed", e)
    //             promise.reject("UPDATE_CHECK_FAILED", e.message, e)
    //         }
    //     }
    // }

    @ReactMethod
    override fun deleteModel(moduleJson: String, promise: Promise) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val module = parseOCRModule(moduleJson)
                val modelManager = ModelManager.getInstance()

                Log.d(TAG, "🔹 Deleting model: $module")

                val deleted = modelManager.deleteModel(module)

                Log.d(TAG, if (deleted) "✅ Model deleted: $module" else "⚠️ Model not found: $module")
                promise.resolve(deleted)
            } catch (e: Exception) {
                Log.e(TAG, "❌ deleteModel failed", e)
                promise.reject("DELETE_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    override fun predictWithModule(
        moduleJson: String,
        imagePath: String,
        barcodes: com.facebook.react.bridge.ReadableArray,
        promise: Promise
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val module = parseOCRModule(moduleJson)
                val uri = Uri.parse(imagePath)

                Log.d(TAG, "🔹 predictWithModule: module=$module, loading image from: $imagePath")

                // Load bitmap from URI using uriToBitmap utility (synchronous version for suspend function)
                var bitmap: Bitmap? = null
                uriToBitmap(reactApplicationContext, uri) { loadedBitmap ->
                    bitmap = loadedBitmap
                }

                // Wait for bitmap to load (uriToBitmap is async for remote URLs)
                var attempts = 0
                while (bitmap == null && attempts < 50) { // Wait max 5 seconds
                    kotlinx.coroutines.delay(100)
                    attempts++
                }

                if (bitmap == null) {
                    throw IllegalArgumentException("Failed to load image from path: $imagePath")
                }

                // Parse barcodes to ScannedCodeResult list (using iOS format)
                val scannedCodes = mutableListOf<ScannedCodeResult>()
                for (i in 0 until barcodes.size()) {
                    val barcodeMap = barcodes.getMap(i)
                    if (barcodeMap != null) {
                        // Read iOS format: "scannedCode" instead of "value"
                        val value = barcodeMap.getString("scannedCode") ?: ""
                        val symbology = barcodeMap.getString("symbology") ?: "CODE_128"

                        // Read iOS format: "gs1ExtractedInfo" (optional)
                        val gs1ExtractedInfo = barcodeMap.getMap("gs1ExtractedInfo")?.let { gs1Map ->
                            val map = mutableMapOf<String, String>()
                            val iterator = gs1Map.keySetIterator()
                            while (iterator.hasNextKey()) {
                                val key = iterator.nextKey()
                                gs1Map.getString(key)?.let { value ->
                                    map[key] = value
                                }
                            }
                            map
                        }

                        // Read iOS format: "boundingBox" with {x, y, width, height}
                        val boundingBoxMap = barcodeMap.getMap("boundingBox")

                        val rect = if (boundingBoxMap != null) {
                            // Convert iOS format {x, y, width, height} to Android Rect {left, top, right, bottom}
                            val x = boundingBoxMap.getDouble("x").toInt()
                            val y = boundingBoxMap.getDouble("y").toInt()
                            val width = boundingBoxMap.getDouble("width").toInt()
                            val height = boundingBoxMap.getDouble("height").toInt()
                            Rect(
                                x,              // left
                                y,              // top
                                x + width,      // right
                                y + height      // bottom
                            )
                        } else {
                            Rect(0, 0, 0, 0)
                        }

                        // Convert symbology string to BarcodeSymbology enum
                        val barcodeSymbology = when (symbology.lowercase()) {
                            "code_128", "code128" -> BarcodeSymbology.code128
                            "qr_code", "qr" -> BarcodeSymbology.qr
                            "ean_13", "ean13" -> BarcodeSymbology.ean13
                            "ean_8", "ean8" -> BarcodeSymbology.ean8
                            "upc_a", "upca" -> BarcodeSymbology.upca
                            "upc_e", "upce" -> BarcodeSymbology.upce
                            "code_39", "code39" -> BarcodeSymbology.code39
                            "code_93", "code93" -> BarcodeSymbology.code93
                            "pdf417" -> BarcodeSymbology.pdf417
                            "aztec" -> BarcodeSymbology.aztec
                            "codabar" -> BarcodeSymbology.codabar
                            "data_matrix", "datamatrix" -> BarcodeSymbology.dataMatrix
                            "i2of5" -> BarcodeSymbology.I2of5
                            else -> BarcodeSymbology.unknown
                        }

                        scannedCodes.add(
                            ScannedCodeResult(
                                scannedCode = value,
                                symbology = barcodeSymbology,
                                boundingBox = rect,
                                gs1ExtractedInfo = gs1ExtractedInfo
                            )
                        )
                    }
                }

                // Get immutable reference to bitmap to avoid smart cast issues
                val loadedBitmap = bitmap ?: throw IllegalArgumentException("Bitmap is null after loading")

                // Create OnDeviceOCRManager and perform prediction
                val ocrManager = OnDeviceOCRManager(
                    context = reactApplicationContext,
                    ocrModule = module
                )

                // Use the explicit module overload
                val result = ocrManager.makePrediction(
                    ocrModule = module,
                    bitmap = loadedBitmap,
                    scannedCodeResults = scannedCodes
                )

                Log.d(TAG, "✅ predictWithModule completed successfully")
                promise.resolve(result)
            } catch (e: Exception) {
                Log.e(TAG, "❌ predictWithModule failed", e)
                promise.reject("PREDICTION_FAILED", e.message, e)
            }
        }
    }

    @ReactMethod
    override fun addListener(eventName: String) {
        // Required for TurboModule event emitters
        // Set up event listeners
    }

    @ReactMethod
    override fun removeListeners(count: Double) {
        // Required for TurboModule event emitters
        // Remove event listeners
    }
}
