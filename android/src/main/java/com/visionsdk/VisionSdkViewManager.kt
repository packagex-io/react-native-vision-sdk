package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.annotation.Nullable
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.asadullah.handyutils.launchOnIO
import com.asadullah.handyutils.withContextMain
import com.facebook.infer.annotation.Assertions
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import io.packagex.visionsdk.ApiManager
import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.config.FocusSettings
import io.packagex.visionsdk.config.ObjectDetectionConfiguration
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.core.VisionViewState
import io.packagex.visionsdk.exceptions.APIErrorResponse
import io.packagex.visionsdk.exceptions.ScannerException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.OnDeviceOCRManager
import io.packagex.visionsdk.ocr.ml.core.PlatformType
import io.packagex.visionsdk.ui.views.VisionCameraView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class VisionSdkViewManager(val appContext: ReactApplicationContext) :
  ViewGroupManager<VisionCameraView>(), ScannerCallback, CameraLifecycleCallback, OCRResult {

  private var context: Context? = null
  override fun getName() = "VisionSdkView"
  private var apiKey: String? = ""
  private var token: String? = ""
  private var isOnDeviceOCR: Boolean = false
  private var locationId: String? = ""
  private var options: Map<String, Any>? = emptyMap()
  private var metaData: Map<String, Any>? = emptyMap()
  private var recipient: Map<String, Any>? = emptyMap()
  private var sender: Map<String, Any>? = emptyMap()
  private var environment: Environment = Environment.PRODUCTION
  private var visionCameraView: VisionCameraView? = null
  private var visionViewState: VisionViewState? = null
  private var focusSettings: FocusSettings? = null
  private var detectionMode: DetectionMode = DetectionMode.Barcode
  private var scanningMode: ScanningMode = ScanningMode.Manual
  private var shouldDisplayFocusImage: Boolean = true
  private var shouldScanInFocusImageRect: Boolean = true
  private var showDocumentBoundaries: Boolean = false
  private var lifecycleOwner: LifecycleOwner? = null
  private var shouldStartScanning = true
  private var authentication: Authentication? = null
  private var onDeviceOCRManager: OnDeviceOCRManager? = null
  private var modelSize: ModelSize = ModelSize.Large
  private var modelType: ModelClass = ModelClass.ShippingLabel


  companion object {
    val TAG = "VisionCameraView"
  }

  override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {
    Log.d(TAG, "createViewInstance: ")
    context = appContext.currentActivity!!
    lifecycleOwner = context as LifecycleOwner
    visionCameraView = VisionCameraView(context!!, null)

    visionCameraView?.setObjectDetectionConfiguration(
      ObjectDetectionConfiguration(
//        isDocumentIndicationOn = false,
      )
    )
    visionCameraView?.shouldAutoSaveCapturedImage(true)
    visionCameraView?.setCameraLifecycleCallback(this)
    visionCameraView?.setScannerCallback(this)
//    visionCameraView?.layoutParams = ViewGroup.LayoutParams(100,100)
    return visionCameraView!!
  }


  /*this will update the camera state after changing
  any property from react native side*/
  override fun onAfterUpdateTransaction(view: VisionCameraView) {
    super.onAfterUpdateTransaction(view)
    visionCameraView = view
    Log.d(TAG, "onAfterUpdateTransaction: ")
    configureViewState()
    configureFocusSettings()
    initializeSdk()
  }

  override fun onDropViewInstance(view: VisionCameraView) {
    super.onDropViewInstance(view)
    Log.d(TAG, "onDropViewInstance: ")
    shouldStartScanning = true
    visionCameraView?.stopCamera()
  }


  private fun initializeSdk() {
    Log.d(TAG, "initializeSdk: ")

    if (apiKey?.isNotEmpty() == true)
      authentication = Authentication.API(apiKey!!)
    else if (token?.isNotEmpty() == true)
      authentication = Authentication.BearerToken(token!!)

    VisionSDK.getInstance().initialize(
      context ?: return,
      environment,
      authentication ?: Authentication.API(""),
      Authentication.API(""),
    )
  }

  private fun configureViewState() {
    visionViewState =
      VisionViewState(detectionMode = detectionMode, scanningMode = scanningMode)
    setVisionViewState()
  }

  private fun configureFocusSettings() {
    focusSettings = FocusSettings(
      shouldDisplayFocusImage = true,
      shouldScanInFocusImageRect = false,
      showDocumentBoundaries = showDocumentBoundaries
    )
    setFocusSettings()
  }

  private fun setVisionViewState() {
    visionCameraView?.setVisionViewState(visionViewState)
  }

  private fun setFocusSettings() {
    if (visionCameraView?.isCameraStarted()?.not() == true) return
    visionCameraView?.getFocusRegionManager()?.setFocusSettings(focusSettings)
  }

  override fun detectionCallbacks(
    barcodeDetected: Boolean,
    qrCodeDetected: Boolean,
    textDetected: Boolean,
    documentDetected: Boolean
  ) {
//    Log.d(TAG, "detectionCallbacks: "+barcodeDetected)
    val event = Arguments.createMap().apply {
      putBoolean("barcode", barcodeDetected)
      putBoolean("qrcode", qrCodeDetected)
      putBoolean("text", textDetected)
      putBoolean("document", documentDetected)
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onDetected", event)
  }

  override fun onBarcodesDetected(barcodeList: List<String>) {
    Log.d(TAG, "onBarcodeDetected: ")
    visionCameraView?.rescan()
    val event = Arguments.createMap().apply {
      putArray(
        "code",
        Arguments.fromArray(barcodeList.map { it }.toTypedArray())
      )
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onBarcodeScan", event)
  }


  override fun onFailure(exception: ScannerException) {
//    exception.printStackTrace()
//    Log.d(TAG, "onFailure: " + exception.message)
    onOCRResponseFailed(exception)
  }

  override fun onImageCaptured(bitmap: Bitmap, imageFile: File?, value: List<String>) {
    Log.d(TAG, "onImageCaptured: " + imageFile?.toUri().toString())

    val event = Arguments.createMap().apply {
      putString("image", imageFile?.toUri().toString())
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onImageCaptured", event)

    visionCameraView?.rescan()

    if (visionViewState?.detectionMode == DetectionMode.OCR) {
      initializeSdk()
      if (isOnDeviceOCR) {
        performLocalOCR(bitmap, value)
      } else {
        makeOCRApiCall(bitmap, value)
      }
    }
  }

  private fun makeOCRApiCall(bitmap: Bitmap, list: List<String>) {
    val apiManager = ApiManager()
    apiManager.shippingLabelApiCallAsync(
      bitmap = bitmap,
      barcodeList = list,
      locationId = locationId ?: "",
      options = options ?: emptyMap(),
      metadata = metaData ?: emptyMap(),
      recipient = recipient ?: emptyMap(),
      sender = sender ?: emptyMap(),
      onScanResult = this
    )
  }

  private fun performLocalOCR(bitmap: Bitmap, list: List<String>) {

    if (onDeviceOCRManager?.isModelAlreadyDownloaded()?.not() == true) {
      onOCRResponseFailed(Exception("Please download the required files before proceeding with on-device AI capabilities."))
      return
    }

    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        val result = onDeviceOCRManager?.getPredictions(bitmap, list)
        withContextMain {
          onOCRResponse(result)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e)
        }
      }
    }
  }

  override fun getCommandsMap(): Map<String?, Int?>? {
    Log.d("React", " View manager getCommandsMap:")
    return mapOf(
      "captureImage" to
        0,
      "stopRunning" to
        1,
      "startRunning" to
        2,
      "toggleTorch" to
        3,
      "setZoomTo" to
        4,
      "setHeight" to
        5,
      "setMetaData" to
        6,
      "setRecipient" to
        7,
      "setSender" to
        8,
      "configureOnDeviceModel" to
        9,
    )
  }

  override fun receiveCommand(
    view: VisionCameraView,
    commandType: Int,
    @Nullable args: ReadableArray?
  ) {
    Assertions.assertNotNull(view)
    Assertions.assertNotNull(args)
    when (commandType) {
      0 -> {
        captureImage()
        return
      }

      1 -> {
        stopScanning()
        return
      }

      2 -> {
        startCamera()
        return
      }

      3 -> {
        toggleTorch(args?.getBoolean(0))
        return
      }

      4 -> {
        setZoomTo(args?.getDouble(0)?.toFloat())
        return
      }

      5 -> {
        setHeight(args?.getInt(0))
        return
      }

      6 -> {
        setMetaData(args?.getString(0))
        return
      }

      7 -> {
        setRecipient(args?.getString(0))
        return
      }

      8 -> {
        setSender(args?.getString(0))
        return
      }

      9 -> {
        configureOnDeviceModel(args?.getMap(0).toString())
        return
      }

      else -> throw IllegalArgumentException(
        String.format(
          "Unsupported command %d received by %s.",
          commandType,
          javaClass.simpleName
        )
      )
    }

  }

  private fun captureImage() {
    Log.d(TAG, "captureImage: ")
    visionCameraView?.capture()
//    val bitmap = BitmapFactory.decodeResource(context?.resources, R.drawable.sample_ocr)
//    onImageCaptured(bitmap, null, emptyList())
  }

  private fun stopScanning() {
    Log.d(TAG, "stopScanning: ")
    if (visionCameraView?.isCameraStarted()?.not() == true) return
    visionCameraView?.stopCamera()
  }

  private fun startCamera() {
//    configureCamera()
    Log.d(TAG, "startCamera: ")
    visionCameraView?.startCamera()
    //focusImage = R.drawable.default_focus_frame,
    //  focusImageRect = RectF(100f,100f,200f,200f),
//    visionCameraView?.requestLayout();

  }

  private fun toggleTorch(boolean: Boolean?) {
    Log.d(TAG, "enableTorch: ")
    visionViewState = visionViewState?.copy(isFlashTurnedOn = boolean!!)
    setVisionViewState()
  }

  private fun setZoomTo(zoom: Float? = 1f) {
    Log.d(TAG, "setZoomTo: " + zoom)
    visionCameraView?.setZoomRatio(zoom ?: 1f)
  }

  private fun setHeight(height: Int?) {
    Log.d(TAG, "setHeight: ")
  }

  private fun setMetaData(metaData: String?) {
    Log.d(TAG, "metaData: " + metaData)
    this.metaData = JSONObject(metaData).toMap()
  }

  private fun setRecipient(recipient: String?) {
    Log.d(TAG, "recipient: " + recipient)
    if (recipient?.isEmpty() == true) {
      this.recipient = emptyMap()
    } else {
      this.recipient = JSONObject(recipient).toMap()
    }
  }

  private fun setSender(sender: String?) {
    Log.d(TAG, "sender: " + sender)
    if (sender?.isEmpty() == true) {
      this.sender = emptyMap()
    } else {
      this.sender = JSONObject(sender).toMap()
    }
  }

  private fun setModelSize(modelSize: String?) {
    Log.d(TAG, "modelSize: " + modelSize)
    this.modelSize = when (modelSize?.lowercase()) {
      "nano" -> ModelSize.Nano
      "micro" -> ModelSize.Micro
      "small" -> ModelSize.Small
      "medium" -> ModelSize.Medium
      "large" -> ModelSize.Large
      "xlarge" -> ModelSize.XLarge
      else -> ModelSize.Nano
    }
  }

  fun setModelType(modelType: String?) {
    Log.d(TAG, "modelType: " + modelType)
    this.modelType = when (modelType?.lowercase()) {
      "shipping_label" -> ModelClass.ShippingLabel
      "bill_of_lading" -> ModelClass.BillOfLading
      "item_label" -> ModelClass.PriceTag
      else -> ModelClass.ShippingLabel
    }

  }

  private fun configureOnDeviceModel(onDeviceConfigs: String?) {
    Log.d(TAG, "configureOnDeviceModel: $onDeviceConfigs")

    if (visionViewState?.detectionMode == DetectionMode.OCR) return

    if (JSONObject(onDeviceConfigs).has("size"))
      setModelSize(JSONObject(onDeviceConfigs).getString("size"))
    if (JSONObject(onDeviceConfigs).has("type"))
      setModelType(JSONObject(onDeviceConfigs).getString("type"))

    onDeviceOCRManager?.destroy()
    onDeviceOCRManager = OnDeviceOCRManager(
      context = context!!,
      platformType = PlatformType.Native,
      modelClass = modelType,
      modelSize = modelSize
    )
    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        if (onDeviceOCRManager?.isConfigured()?.not() == true) {
          onDeviceOCRManager?.configure {
//                Log.d(TAG, "Install Progress: $it")
            if (onDeviceOCRManager?.isModelAlreadyDownloaded() == false) {
              val event = Arguments.createMap().apply {
                putDouble("progress", it.toDouble())
                putBoolean("downloadStatus", false)
              }
              appContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("onModelDownloadProgress", event)
            }
          }
        }
        val event = Arguments.createMap().apply {
          putDouble("progress", 1.0)
          putBoolean("downloadStatus", true)
        }
        appContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit("onModelDownloadProgress", event)

      } catch (e: Exception) {
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e)
        }
      }
    }
  }

  @ReactProp(name = "apiKey")
  fun setApiKey(view: View, apiKey: String = "") {
    Log.d(TAG, "apiKey: " + apiKey)
    this.apiKey = apiKey
  }

  @ReactProp(name = "token")
  fun setToken(view: View, token: String = "") {
    Log.d(TAG, "token: " + token)
    this.token = token
  }

  @ReactProp(name = "environment")
  fun setEnvironment(view: View, env: String = "") {
    Log.d(TAG, "environment: " + env)
    environment = when (env.lowercase()) {
      "dev" -> Environment.DEV
      "staging" -> Environment.STAGING
      "sandbox" -> Environment.SANDBOX
      "prod" -> Environment.PRODUCTION
      else -> Environment.PRODUCTION
    }
  }

  @ReactProp(name = "captureMode")
  fun setCaptureMode(view: View, captureMode: String = "") {
    Log.d(TAG, "captureMode: " + captureMode)
    scanningMode = when (captureMode.lowercase()) {
      "auto" -> ScanningMode.Auto
      "manual" -> ScanningMode.Manual
      else -> ScanningMode.Auto
    }
  }

  @ReactProp(name = "mode")
  fun setMode(view: View, mode: String = "") {
    Log.d(TAG, "mode: " + mode)
    detectionMode = when (mode.lowercase()) {
      "ocr" -> DetectionMode.OCR
      "barcode" -> DetectionMode.Barcode
      "qrcode" -> DetectionMode.QRCode
      "photo" -> DetectionMode.Photo
      else -> DetectionMode.OCR
    }
  }

  @ReactProp(name = "isOnDeviceOCR")
  fun isOnDeviceOCR(view: View, isOnDeviceOCR: Boolean = false) {
    Log.d(TAG, "isOnDevice: " + isOnDeviceOCR)
    this.isOnDeviceOCR = isOnDeviceOCR
  }

  @ReactProp(name = "showScanFrame")
  fun showScanFrame(view: View, showScanFrame: Boolean = false) {
    Log.d(TAG, "showScanFrame: " + showScanFrame)
    shouldDisplayFocusImage = showScanFrame
  }

  @ReactProp(name = "captureWithScanFrame")
  fun captureWithScanFrame(view: View, captureWithScanFrame: Boolean = false) {
    Log.d(TAG, "captureWithScanFrame: " + captureWithScanFrame)
    shouldScanInFocusImageRect = captureWithScanFrame
  }

  @ReactProp(name = "showDocumentBoundaries")
  fun showDocumentBoundaries(view: View, showDocumentBoundaries: Boolean = false) {
    Log.d(TAG, "showDocumentBoundaries: " + showDocumentBoundaries)
    this.showDocumentBoundaries = showDocumentBoundaries
  }

  @ReactProp(name = "locationId")
  fun setLocationId(view: View, locationId: String = "") {
    Log.d(TAG, "locationId: " + locationId)
    this.locationId = locationId
  }

  @ReactProp(name = "options")
  fun setOptions(view: View, options: String) {
    Log.d(TAG, "options: " + options)
    this.options = JSONObject(options).toMap()
  }


  private fun JSONObject.toMap(): Map<String, Any> = keys().asSequence().associateWith {
    when (val value = this[it]) {
      is JSONArray -> {
        val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
        JSONObject(map).toMap().values.toList()
      }

      is JSONObject -> value.toMap()
      JSONObject.NULL -> ""
      else -> value
    }
  }

  override fun onCameraStarted() {
    Log.d(TAG, "onCameraStarted: ")

    setFocusSettings()
  }

  override fun onCameraStopped() {
    Log.d(TAG, "onCameraStopped: ")
  }

  private fun viewTreeObserver() {
    visionCameraView?.viewTreeObserver?.addOnGlobalLayoutListener {
      for (i in 0 until visionCameraView!!.childCount) {
        val child: View = visionCameraView!!.getChildAt(i)
        child.measure(
          View.MeasureSpec.makeMeasureSpec(child.measuredWidth, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(child.measuredHeight, View.MeasureSpec.EXACTLY)
        )
        child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight())
      }
    }
  }

  override fun onOCRResponse(response: String?) {

    Log.d(TAG, "api responded with  ${response}")
    val event = Arguments.createMap().apply {
      putString("data", response)
    }
//          val reactContext = context as ReactContext
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onOCRScan", event)
  }

  override fun onOCRResponseFailed(throwable: Throwable?) {
    val message = if (throwable is APIErrorResponse) {
      (throwable as APIErrorResponse).errorModel.message
    } else {
      throwable?.message ?: "Unknown error occurred"
    }
    val event = Arguments.createMap().apply {
      putString("message", message)
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onError", event)
    Log.d(TAG, "${message}")
  }
}





