package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import com.facebook.infer.annotation.Assertions
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.google.mlkit.vision.barcode.common.Barcode
import io.packagex.visionsdk.ApiManager
//import io.packagex.visionsdk.ApiManager
import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.analyzers.BarcodeResult
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
  private var locationId: String? = ""
  private var options: Map<String, Any>? = emptyMap()
  private var metaData: Map<String, Any>? = emptyMap()
  private var recipient: Map<String, Any>? = emptyMap()
  private var sender: Map<String, Any>? = emptyMap()
  private var environment: Environment = Environment.DEV
  private var visionCameraView: VisionCameraView? = null
  private var screenState:VisionViewState? = null
  private var focusSettings: FocusSettings? = null
  private var detectionMode: DetectionMode = DetectionMode.Barcode
  private var scanningMode: ScanningMode = ScanningMode.Manual
  private var shouldDisplayFocusImage:Boolean = false
  private var shouldScanInFocusImageRect: Boolean = false
  private var lifecycleOwner: LifecycleOwner? = null
  private var shouldStartScanning = true
  private lateinit var authentication: Authentication

  companion object {
    val TAG = "VisionSDK"
  }

  override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {
    Log.d(TAG, "createViewInstance: ")
    context = appContext.currentActivity!!
    lifecycleOwner = context as LifecycleOwner
    visionCameraView = VisionCameraView(context!!, null)
//    visionCameraView?.layoutParams = ViewGroup.LayoutParams(100,100)
    return visionCameraView!!
  }


  /*this will update the camera state after changing
  any property from react native side*/
  override fun onAfterUpdateTransaction(view: VisionCameraView) {
    super.onAfterUpdateTransaction(view)
    visionCameraView = view
    Log.d(TAG, "onAfterUpdateTransaction: ")
    configureCamera()

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
    else return

    VisionSDK.getInstance().initialize(
      environment,
      authentication,
      Authentication.API(""),
    )
  }

  private fun configureCamera() {

    Log.d(TAG, "configureCamera: ")
    setStateAndFocusSettings()
    visionCameraView?.setObjectDetectionConfiguration(ObjectDetectionConfiguration(isDocumentIndicationOn = false))
    visionCameraView?.shouldAutoSaveCapturedImage(true)
    visionCameraView?.setCameraLifecycleCallback(this)
    visionCameraView?.setScannerCallback(this)


//    viewTreeObserver()
  }

  private fun setStateAndFocusSettings() {


    screenState = VisionViewState(detectionMode = detectionMode, scanningMode = ScanningMode.Manual)
    visionCameraView?.setVisionViewState(screenState)
  }

  override fun detectionCallbacks(
    barcodeDetected: Boolean,
    qrCodeDetected: Boolean,
    textDetected: Boolean,
    documentDetected: Boolean
  ) {
    Log.d(TAG, "detectionCallbacks: "+barcodeDetected)
    val event = Arguments.createMap().apply {
      putBoolean("barcode", barcodeDetected)
      putBoolean("qrcode", qrCodeDetected)
      putBoolean("text", textDetected)
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onDetected", event)
  }

  override fun onBarcodesDetected(barcodeList: List<BarcodeResult>) {
    Log.d(TAG, "onBarcodeDetected: ")
    visionCameraView?.rescan()
    val event = Arguments.createMap().apply {
      putArray(
        "code",
        Arguments.fromArray(barcodeList.map { it.barcode.displayValue }.toTypedArray())
      )
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onBarcodeScanSuccess", event)
  }


  override fun onFailure(exception: ScannerException) {
    exception.printStackTrace()
  }

  override fun onImageCaptured(bitmap: Bitmap, imageFile: File?, value: List<BarcodeResult>) {
    Log.d(TAG, "onImageCaptured: " + imageFile?.toUri().toString())

    visionCameraView?.rescan()

    if (screenState?.detectionMode == DetectionMode.OCR) {
      initializeSdk()
      triggerOCRCalls(bitmap, value.map { it.barcode.displayValue }.filterNotNull() )
    }
    val event = Arguments.createMap().apply {
      putString("image", imageFile?.toUri().toString())
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onImageCaptured", event)
  }

  private fun triggerOCRCalls(bitmap: Bitmap, list: List<String>) {
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
    visionCameraView!!.capture()
  }

  private fun stopScanning() {
    Log.d(TAG, "stopScanning: ")
    visionCameraView!!.stopCamera()
  }

  private fun startCamera() {
//    configureCamera()
    Log.d(TAG, "startCamera: ")
    visionCameraView?.startCamera()
    //focusImage = R.drawable.default_focus_frame,
    //  focusImageRect = RectF(100f,100f,200f,200f),
    visionCameraView?.requestLayout();

  }

  private fun toggleTorch(boolean: Boolean?) {
    Log.d(TAG, "enableTorch: ")
    screenState = screenState?.copy(isFlashTurnedOn = boolean!!)
    setStateAndFocusSettings()
  }

  private fun setZoomTo(zoom: Float? = 1f) {
    Log.d(TAG, "setZoomTo: " + zoom)
    visionCameraView?.setZoomRatio(zoom ?: 1f)
  }

  private fun setHeight(height: Int?) {
    Log.d(TAG, "setHeight: ")
  }

  fun setMetaData(metaData: String?) {
    Log.d(TAG, "metaData: " + metaData)
    this.metaData = JSONObject(metaData).toMap()
  }

  fun setRecipient(recipient: String?) {
    Log.d(TAG, "recipient: " + recipient)
    if (recipient?.isEmpty() == true){
      this.recipient = emptyMap()
    }else{
      this.recipient = JSONObject(recipient).toMap()
    }
  }

  fun setSender(sender: String?) {
    Log.d(TAG, "sender: " + sender)
    if (sender?.isEmpty() == true){
      this.sender = emptyMap()
    }else{
      this.sender = JSONObject(sender).toMap()
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
      else -> Environment.STAGING
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

    focusSettings = FocusSettings( shouldDisplayFocusImage =  shouldDisplayFocusImage, shouldScanInFocusImageRect = shouldScanInFocusImageRect)
    visionCameraView?.getFocusRegionManager()?.setFocusSettings(focusSettings)
    visionCameraView?.requestLayout();

//    viewTreeObserver()
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
      .emit("onOCRDataReceived", event)
  }

  override fun onOCRResponseFailed(throwable: Throwable?) {
    val message = if (throwable is APIErrorResponse){
      (throwable as APIErrorResponse).errorModel.message
    }else{
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
