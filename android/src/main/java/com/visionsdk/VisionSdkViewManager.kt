package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.annotation.Nullable
import androidx.lifecycle.LifecycleOwner
import com.facebook.infer.annotation.Assertions
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.google.mlkit.vision.barcode.common.Barcode
import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.core.ScreenState
import io.packagex.visionsdk.core.ScreenViewType
import io.packagex.visionsdk.exceptions.ScannerException
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.views.VisionCameraView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class VisionSdkViewManager(val appContext: ReactApplicationContext) :
  ViewGroupManager<VisionCameraView>(), ScannerCallback {

  var context: Context? = null
  override fun getName() = "VisionSdkView"
  var apiKey: String? = ""
  var token: String? = ""
  var locationId: String? = ""
  var options: Map<String, Any>? = emptyMap()
  var environment: Environment = Environment.DEV
  lateinit var authentication: Authentication

  var customScannerView: VisionCameraView? = null
  var detectionMode: DetectionMode = DetectionMode.Barcode
  var scanningMode: ScanningMode = ScanningMode.Manual
  private var lifecycleOwner: LifecycleOwner? = null
  private var shouldStartScanning = true

  companion object {
    val TAG = "CustomScannerView"
  }

  override fun onAfterUpdateTransaction(view: VisionCameraView) {
    super.onAfterUpdateTransaction(view)
    customScannerView = view
    Log.d(TAG, "onAfterUpdateTransaction: ")
    if (token!!.isNotEmpty()) {
      if (shouldStartScanning) {
        shouldStartScanning = false
        startScanning()
      } else {
        restartScanning()
      }
    }

//    Handler(Looper.myLooper()!!).postDelayed({
//      if (detectionMode == DetectionMode.OCR)
//        captureImage()
//    }, 5000)
  }

  override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {


    Log.d(TAG, "createViewInstance: ")

    context = appContext.currentActivity!!
    lifecycleOwner = context as LifecycleOwner

//    val inflater: LayoutInflater =
//      LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//    val view = inflater.inflate(R.layout.custom_view, null)

//    customScannerView = view.findViewById<CustomScannerView>(R.id.customScannerView)
    customScannerView = VisionCameraView(context!!, null)
//    customScannerView = CustomView(reactContext)
    return customScannerView!!
  }

  private fun initializeSdk() {

    if (apiKey?.isNotEmpty() == true)
      authentication = Authentication.API(apiKey!!)
    else if (token?.isNotEmpty() == true)
      authentication = Authentication.BearerToken(token!!)
    else return

    VisionSDK.getInstance().initialize(
      environment,
      authentication,
      ""
    )
  }

  override fun onDropViewInstance(view: VisionCameraView) {
    super.onDropViewInstance(view)
    Log.d(TAG, "onDropViewInstance: ")
    shouldStartScanning = true
    customScannerView?.recycle()
  }

  private fun startScanning() {

    Log.d(TAG, "startScanning: ")
//    Log.d(VisionSdkViewManager.TAG, "scanningMode: $scanningMode")
//    Log.d(VisionSdkViewManager.TAG, "detectionMode: $detectionMode")
    customScannerView?.setState(
      ScreenState(
        ScreenViewType.FullScreen,
        detectionMode,
        scanningMode
      )
    )
    customScannerView?.setScannerCallback(this)
    customScannerView?.startScanning()
//    if (shouldAddGlobalListner){
//      shouldAddGlobalListner = false
    viewTreeObserver()
//    }
  }

  override fun detectionCallbacks(
    barcodeDetected: Boolean,
    qrCodeDetected: Boolean,
    textDetected: Boolean
  ) {
    val event = Arguments.createMap().apply {
      putBoolean("barcode", barcodeDetected)
      putBoolean("qrcode", qrCodeDetected)
      putBoolean("text", textDetected)
    }
//          val reactContext = context as ReactContext
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onDetected", event)
  }

  override fun onBarcodesDetected(barcodeList: List<Barcode>) {
    Log.d(TAG, "onBarcodeDetected: ")
    val event = Arguments.createMap().apply {
      putArray("code", Arguments.fromList(barcodeList))
    }
//          val reactContext = context as ReactContext
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onBarcodeScanSuccess", event)
  }

  override fun onFailure(exception: ScannerException) {
    exception.printStackTrace()
  }
  override fun onImageCaptured(bitmap: Bitmap, imageFile: File?, value: List<Barcode>) {
    Log.d(TAG, "onImageCaptured: ")

//        Toast.makeText(context!!,"onImageCaptured",Toast.LENGTH_LONG).show()
    triggerOCRCalls(bitmap, value ?: mutableListOf())
  }

//  override fun onMultipleBarcodesDetected(barcodeList: List<Barcode>) {
//    Log.d(TAG, "onMultipleBarcodesDetected: ")
//  }

  private fun viewTreeObserver() {
    customScannerView!!.viewTreeObserver.addOnGlobalLayoutListener {
      for (i in 0 until customScannerView!!.childCount) {
        val child: View = customScannerView!!.getChildAt(i)
        child.measure(
          View.MeasureSpec.makeMeasureSpec(child.measuredWidth, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(child.measuredHeight, View.MeasureSpec.EXACTLY)
        )
        child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight())
      }
    }
  }

  override fun getCommandsMap(): Map<String?, Int?>? {
    Log.d("React", " View manager getCommandsMap:")
    return MapBuilder.of(
      "captureImage",
      0,
      "stopRunning",
      1,
      "startRunning",
      2,
      "toggleTorch",
      3
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
        restartScanning()
        return
      }

      3 -> {
        toggleTorch()
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

  //  @ReactMethod
  private fun captureImage() {
    Log.d(TAG, "captureImage: ")
    customScannerView!!.capture()
  }

  private fun stopScanning() {
    Log.d(TAG, "stopScanning: ")
    customScannerView!!.recycle()
  }

  private fun restartScanning() {
    Log.d(TAG, "restartScanning: ")
    customScannerView!!.rescan()
  }

  private fun toggleTorch() {
    Log.d(TAG, "enableTorch: ")
    customScannerView!!.enableTorch()
  }

  private fun triggerOCRCalls(bitmap: Bitmap, list: List<Barcode>) {
    customScannerView!!.makeOCRApiCall(bitmap = bitmap,
      barcodeList = list,
      locationId = locationId ?: "",
      options = options ?: emptyMap(),
      onScanResult = object : OCRResult {
        override fun onOCRResponse(ocrResponse: String?) {

          Log.d(TAG, "api responded with  ${ocrResponse}")
          val event = Arguments.createMap().apply {
            putString("data", ocrResponse)
          }
//          val reactContext = context as ReactContext
          appContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onOCRDataReceived", event)
        }

        override fun onOCRResponseFailed(throwable: Throwable?) {
//          progressBar.visibility = View.GONE
          Log.d(VisionSdkViewManager.TAG, "Something went wrong ${throwable?.message}")
        }
      })
  }


  @ReactProp(name = "apiKey")
  fun setApiKey(view: View, apiKey: String = "") {
    Log.d(TAG, "apiKey: " + apiKey)
    this.apiKey = apiKey
    initializeSdk()
  }

  @ReactProp(name = "token")
  fun setToken(view: View, token: String = "") {
    Log.d(TAG, "token: " + token)
    this.token = token
    initializeSdk()
  }

  @ReactProp(name = "environment")
  fun setEnvironment(view: View, env: String = "") {
    Log.d(TAG, "environment: " + env)
    environment = when (env.lowercase()) {
      "dev" -> Environment.DEV
      "staging" -> Environment.STAGING
      else -> Environment.DEV
    }
    initializeSdk()
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
      "qrcode" -> DetectionMode.QR
      else -> DetectionMode.OCR
    }
  }

  @ReactProp(name = "locationId")
  fun setLocationId(view: View, locationId: String = "") {
    Log.d(TAG, "locationId: " + locationId)
    this.locationId = locationId
  }

  @ReactProp(name = "options")
  fun setOptions(view: View, options: String) {
    Log.d(TAG, "options: " + options)
    val map = options.split(",").associate {
      val (left, right) = it.split(":")
      left to right
    }
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
}
