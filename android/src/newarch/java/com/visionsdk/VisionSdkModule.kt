package com.visionsdk

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
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

    @ReactMethod
    override fun setEnvironment(environment: String) {
        Log.d(TAG, "🔄 Setting Environment to: $environment")
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
        Log.d(TAG, "🔹 Loading On-Device Model: $modelType with size: $modelSize")

        // Validate Model Type & Size
        val modelSizeEnum = getModelSize(modelSize)
        val resolvedModelType = getModelType(modelType, modelSizeEnum)

        // Use Singleton to get OnDeviceOCRManager instance
        val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(reactApplicationContext, resolvedModelType)
        val isModelAlreadyDownloaded = onDeviceOCRManager.isModelAlreadyDownloaded()

        if (OnDeviceOCRManagerSingleton.isModelConfigured(resolvedModelType)) {
            Log.d(TAG, "✅ Model '$modelType' (Size: $modelSize) is already configured, skipping setup")
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

    @ReactMethod
    override fun unLoadOnDeviceModels(
        modelType: String?,
        shouldDeleteFromDisk: Boolean,
        promise: Promise
    ) {
        Log.d(TAG, "🗑️ Unloading model: $modelType, deleteFromDisk: $shouldDeleteFromDisk")

        try {
            if (modelType == null) {
                // Unload all models
                OnDeviceOCRManagerSingleton.destroy()
                promise.resolve("All models unloaded successfully")
            } else {
                // Check if requested model matches current model
                val modelSizeEnum = ModelSize.Large // Default size for comparison
                val requestedModelType = getModelType(modelType, modelSizeEnum)

                if (OnDeviceOCRManagerSingleton.isModelConfigured(requestedModelType)) {
                    OnDeviceOCRManagerSingleton.destroy()
                    promise.resolve("Model '$modelType' unloaded successfully")
                } else {
                    promise.resolve("Model '$modelType' is not currently loaded")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unloading model", e)
            promise.reject("MODEL_UNLOAD_ERROR", "Failed to unload model: ${e.message}", e)
        }
    }

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
        Log.d(TAG, "logDocumentClassificationDataToPx called")

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
        Log.d(TAG, "🔹 Standalone Cloud Item Label Prediction for: $imagePath")

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
        Log.d(TAG, "🔹 Standalone Cloud Document Classification Prediction for: $imagePath")

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
