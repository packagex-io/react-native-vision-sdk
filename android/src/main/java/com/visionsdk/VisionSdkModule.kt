package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.*
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import io.packagex.visionsdk.ApiManager
import kotlinx.coroutines.*
import com.visionsdk.utils.EventUtils
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.interfaces.ResponseCallback
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
  fun logItemLabelDataToPx(
    imageUri: String,
    barcodes: ReadableArray,
    responseData: ReadableMap,
    token: String?,
    apiKey: String?,
    shouldResizeImage: Boolean,
    promise: Promise
  ){
   Log.d(TAG, "log item label data to px called")
    val uri = Uri.parse(imageUri)
    uriToBitmap(reactContext, uri) { bitmap ->
      if (bitmap == null) {
        promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imageUri")
        return@uriToBitmap
      }

      try {
        val barcodeList = mutableListOf<String>()
        for (i in 0 until barcodes.size()) {
          barcodeList.add(barcodes.getString(i) ?: "")
        }

        val dataMap = responseData.toHashMap()
        val onDeviceResponse = (dataMap as Map<*, *>?)?.let { JSONObject(it).toString() } ?: ""

        Log.d(TAG, "ondeviceresponse:\n $onDeviceResponse")

        ApiManager().itemLabelMatchingApiCallAsync(
          reactContext,
          apiKey,
          token,
          bitmap,
          shouldResizeImage,
          barcodeList,
          onDeviceResponse,
          onResponseCallback =  object : ResponseCallback {
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
  fun logShippingLabelDataToPx(
    imageUri: String,
    barcodes: ReadableArray,
    responseData: ReadableMap,
    token: String?,
    apiKey: String?,
    locationId: String?,
    options: ReadableMap?,
    metadata: ReadableMap?,
    recipient: ReadableMap?,
    sender: ReadableMap?,
    shouldResizeImage: Boolean,
    promise: Promise
  ){
    Log.d(TAG, "logShippingLabelDataToPx called with imageUri: $imageUri")

    val uri = Uri.parse(imageUri)

    uriToBitmap(reactContext, uri) { bitmap ->
      if (bitmap == null) {
        promise.reject("BITMAP_ERROR", "Failed to decode image from URI: $imageUri")
        return@uriToBitmap
      }

      try {
        val barcodeList = mutableListOf<String>()
        for (i in 0 until barcodes.size()) {
          barcodeList.add(barcodes.getString(i) ?: "")
        }

        val dataMap = responseData.toHashMap()
        val onDeviceResponse = (dataMap as Map<*, *>?)?.let { JSONObject(it).toString() } ?: ""

        Log.d(TAG, "ondeviceresponse:\n $onDeviceResponse")

        val optionsMap = options?.toHashMap() ?: emptyMap()
        val metadataMap = metadata?.toHashMap() ?: emptyMap()
        val recipientMap = recipient?.toHashMap()
        val senderMap = sender?.toHashMap()

        ApiManager().shippingLabelMatchingApiAsync(
          apiKey = apiKey,
          token = token,
          bitmap = bitmap,
          shouldResizeImage = shouldResizeImage,
          barcodeList = barcodeList,
          onDeviceResponse = onDeviceResponse,
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

  /**
   * Converts a URI to a Bitmap.
   * This function supports both HTTP/HTTPS URIs (for remote images) and local URIs.
   * The result is posted back to the main UI thread through a callback.
   *
   * @param context - The application context, used to access content resolver for local URIs.
   * @param uri - The URI to be converted to a Bitmap.
   * @param onComplete - A callback function invoked with the Bitmap result on the main UI thread, or null if conversion fails.
   */
  private fun uriToBitmap(context: Context, uri: Uri, onComplete: (Bitmap?) -> Unit) {
    Thread {
      try {
        val bitmap: Bitmap? = if (uri.scheme == "http" || uri.scheme == "https") {
          // Handle HTTP/HTTPS URIs for remote images
          val url = URL(uri.toString())
          val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
          connection.doInput = true
          connection.connect()

          connection.inputStream.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
          }
        } else {
          // Handle local URIs (file, content, etc.)
          context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
          } ?: null
        }

        // Post the result back to the main UI thread via the callback
        Handler(Looper.getMainLooper()).post {
          onComplete(bitmap)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        // Post null to the callback on the main thread if there's an error
        Handler(Looper.getMainLooper()).post {
          onComplete(null)
        }
      }
    }.start()  // Start the background thread
  }

}
