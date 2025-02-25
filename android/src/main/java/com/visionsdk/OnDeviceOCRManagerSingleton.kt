package com.visionsdk

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import io.packagex.visionsdk.ocr.ml.core.OnDeviceOCRManager
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import io.packagex.visionsdk.ocr.ml.core.enums.PlatformType

object OnDeviceOCRManagerSingleton {
  @SuppressLint("StaticFieldLeak")
  private var onDeviceOCRManager: OnDeviceOCRManager? = null
  private var currentModelType: OCRModule? = null
  private var currentModelSize: ModelSize? = null
  private const val TAG = "IJS"

  /**
   * Returns the shared instance of OnDeviceOCRManager, creating a new one only if needed.
   */
  fun getInstance(context: Context, modelType: OCRModule): OnDeviceOCRManager {
    val existingManager = onDeviceOCRManager
    Log.d(TAG, "existing manager: $existingManager")

    // ✅ If model is already configured, return it without reconfiguring
    if (existingManager != null && isModelConfigured(modelType)) {
      Log.d(TAG, "✅ Returning already configured model: $modelType")
      return existingManager
    }

    // 🚨 Do NOT destroy if the same model is already configured
    if (!isModelConfigured(modelType)) {
      Log.d(TAG, "🛑 Destroying previous OnDeviceOCRManager instance")
      destroy() // Only destroy if we are switching models
    }

    Log.d(TAG, "♻️ Creating new instance for model: $modelType")

    onDeviceOCRManager = OnDeviceOCRManager(
      context = context.applicationContext,
      platformType = PlatformType.ReactNative,
      ocrModule = modelType
    )

    currentModelType = modelType
    currentModelSize = modelType.modelSize
    return onDeviceOCRManager!!
  }

  /**
   * Checks if the currently configured model matches the requested one.
   */
  fun isModelConfigured(modelType: OCRModule): Boolean {
    val isSameModel = (currentModelType == modelType)
    val isConfigured = onDeviceOCRManager?.isConfigured() == true

    Log.d(TAG, "🛠 Checking model configuration: ModelType=$isSameModel, isConfigured=$isConfigured")

    return isSameModel && isConfigured
  }

  /**
   * Destroys the current instance of OnDeviceOCRManager only if needed.
   */
  fun destroy() {
    Log.d(TAG, "🛑 Destroying OnDeviceOCRManager instance")
    onDeviceOCRManager?.destroy()
    onDeviceOCRManager = null
    currentModelType = null
    currentModelSize = null
  }

}
