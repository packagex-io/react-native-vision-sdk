package com.visionsdk

import android.util.Log
import com.facebook.react.bridge.*
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import kotlinx.coroutines.*
import com.visionsdk.utils.EventUtils

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

  private fun getEnvironment(environmentStr: String): Environment {
    return when (environmentStr.lowercase()) {
      "staging" -> Environment.STAGING
      "qa" -> Environment.QA
      "production" -> Environment.PRODUCTION
      "dev" -> Environment.DEV
      "sandbox" -> Environment.SANDBOX
      else -> Environment.STAGING // Default to Large
    }
  }

  @ReactMethod
  fun setEnvironment(env: String){
    Log.d(TAG, "🔄 Setting Environment to: $env")
    val environment = getEnvironment(env)
    VisionSdkSingleton.initializeSdk(reactContext, environment)
  }

  @ReactMethod
  fun loadOnDeviceModels(
    token: String?,
    apiKey: String?,
    modelTypeStr: String?,
    modelSizeStr: String?,
    promise: Promise
  ) {
    Log.d(TAG, "🔹 Loading On-Device Model: $modelTypeStr with size: $modelSizeStr")

    // Validate Model Type & Size
    val modelSize = getModelSize(modelSizeStr)
    val resolvedModelType = getModelType(modelTypeStr, modelSize)

    // Use Singleton to get OnDeviceOCRManager instance
    val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(reactContext, resolvedModelType)



    if(OnDeviceOCRManagerSingleton.isModelConfigured(resolvedModelType)){
      Log.d(TAG, "✅ Model '$modelTypeStr' (Size: $modelSizeStr) is already configured, skipping setup")
      EventUtils.sendModelDownloadProgressEvent(reactContext, progress = 1.0, downloadStatus = true, isReady = true)
      promise.resolve("Model is already configured and ready")
      return
    }

    // Launch Coroutine
    CoroutineScope(Dispatchers.IO).launch {
      try {
        onDeviceOCRManager?.configure(apiKey, token) { progress ->
          EventUtils.sendModelDownloadProgressEvent(
              reactContext,
              progress = progress.toDouble(),
              downloadStatus = progress >= 1.0,
              isReady = progress >= 1.0
            )
        }

        EventUtils.sendModelDownloadProgressEvent(reactContext, progress = 1.0, downloadStatus = true, isReady = true)
        promise.resolve("Model loaded successfully")
      } catch (e: Exception) {
        Log.e("VisionSdkModule", "❌ Error loading model", e)
        promise.reject("MODEL_LOAD_FAILED", "Failed to load on-device model: ${e.message}")
      }
    }
  }
}
