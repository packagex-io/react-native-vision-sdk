package com.visionsdk

import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import kotlinx.coroutines.*

class VisionSdkModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  companion object {
    const val TAG = "IJS" // Companion object to hold constants, like the TAG for logging
  }


  override fun getName(): String {
    return "VisionSdkModule"
  }

  private fun getModelSize(modelSizeStr: String?): ModelSize {
    return when (modelSizeStr?.lowercase()) {
      "nano" -> ModelSize.Nano
      "micro" -> ModelSize.Micro
      "small" -> ModelSize.Small
      "medium" -> ModelSize.Medium
      "large" -> ModelSize.Large
      "xlarge" -> ModelSize.XLarge
      else -> ModelSize.Large // Default to Large
    }
  }
  private fun getModelType(modelTypeStr: String?, modelSize: ModelSize): OCRModule {
    Log.d(TAG, "Setting Model Type: $modelTypeStr")

    return when (modelTypeStr?.lowercase()) {
      "shipping_label", "shipping-label" -> OCRModule.ShippingLabel(modelSize)
      "bill_of_lading", "bill-of-lading" -> OCRModule.BillOfLading(modelSize)
      "item_label", "item-label" -> OCRModule.ItemLabel(modelSize)
      "document_classification", "document-classification" -> OCRModule.DocumentClassification(modelSize)
      else -> {
        Log.w(TAG, "⚠️ Unknown model type, defaulting to ShippingLabel")
        OCRModule.ShippingLabel(modelSize)  // Default to Shipping Label
      }
    }
  }

  @ReactMethod
  fun loadOnDeviceModels(token: String?, apiKey: String?, modelTypeStr: String?, modelSizeStr: String?, promise: Promise) {
    Log.d("VisionSdkModule", "🔹 Loading On-Device Model: $modelTypeStr with size: $modelSizeStr")

    val context = reactContext

    VisionSDK.getInstance().initialize(
      context ?: return, // Ensure context is not null before initializing
      Environment.STAGING // Pass the environment configuration
    )

    // Validate Model Type & Size
    val modelSize = getModelSize(modelSizeStr)
    val resolvedModelType = getModelType(modelTypeStr, modelSize)

    // Use Singleton to get OnDeviceOCRManager instance
    val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context, resolvedModelType)



    if(OnDeviceOCRManagerSingleton.isModelConfigured(resolvedModelType)){
      Log.d(TAG, "✅ Model '$modelTypeStr' (Size: $modelSizeStr) is already configured, skipping setup")
      val evnt = Arguments.createMap().apply {
        putDouble("progress", 1.0)
        putBoolean("downloadStatus", true)
        putBoolean("isReady", true)
      }

      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onModelDownloadProgress", evnt)

      promise.resolve("Model is already configured and ready")
      return
    }

    // Launch Coroutine
    CoroutineScope(Dispatchers.IO).launch {
      try {
        onDeviceOCRManager?.configure(apiKey, token) { progress ->
          val event = Arguments.createMap().apply {
            putDouble("progress", progress.toDouble())
            putBoolean("downloadStatus", progress >= 1.0)
            putBoolean("isReady", progress >= 1.0)
          }
          reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onModelDownloadProgress", event)
        }

        val evt = Arguments.createMap().apply {
          putDouble("progress", 1.0)
          putBoolean("downloadStatus", true)
          putBoolean("isReady", true)
        }

        reactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit("onModelDownloadProgress", evt)
        promise.resolve("Model loaded successfully")
      } catch (e: Exception) {
        Log.e("VisionSdkModule", "❌ Error loading model", e)
        promise.reject("MODEL_LOAD_FAILED", "Failed to load on-device model: ${e.message}")
      }
    }
  }
}
