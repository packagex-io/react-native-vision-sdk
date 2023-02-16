package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.lifecycle.LifecycleOwner
import com.example.customscannerview.mlkit.Authentication
import com.example.customscannerview.mlkit.Environment
import com.example.customscannerview.mlkit.VisionSDK
import com.example.customscannerview.mlkit.enums.ViewType
import com.example.customscannerview.mlkit.interfaces.OCRResult
import com.example.customscannerview.mlkit.views.*
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

class VisionSdkViewManager(val appContext: ReactApplicationContext) :
  ViewGroupManager<CustomScannerView>() {

  var context: Context? = null
  override fun getName() = "VisionSdkView"
  var apiKey: String? = ""
  var token: String? = ""
  var locationId: String? = ""
  var options: Map<String,String>? =  mapOf()
  var environment: Environment = Environment.DEV
  lateinit var authentication: Authentication

  var customScannerView: CustomScannerView? = null
  var detectionMode: DetectionMode = DetectionMode.Barcode
  var scanningMode: ScanningMode = ScanningMode.Manual
  private var lifecycleOwner: LifecycleOwner? = null

  companion object {
    val TAG = "CustomScannerView"
  }

  override fun onAfterUpdateTransaction(view: CustomScannerView) {
    super.onAfterUpdateTransaction(view)
    Log.d(TAG, "onAfterUpdateTransaction: ")
    startScanning()

//    Handler(Looper.myLooper()!!).postDelayed({
//      if (detectionMode == DetectionMode.OCR)
//        captureImage()
//    }, 5000)
  }

  override fun createViewInstance(reactContext: ThemedReactContext): CustomScannerView {


    Log.d(TAG, "createViewInstance: ")

    context = appContext.currentActivity!!
    lifecycleOwner = context as LifecycleOwner

//    val inflater: LayoutInflater =
//      LayoutInflater.from(context).context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//    val view = inflater.inflate(R.layout.custom_view, null)

//    customScannerView = view.findViewById<CustomScannerView>(R.id.customScannerView)
    customScannerView = CustomScannerView(context!!, null)
//    customScannerView = CustomView(reactContext)

    return customScannerView!!
  }

  private fun initializeSdk() {

    if (apiKey?.isNotEmpty() == true)
      authentication = Authentication.API(apiKey!!)
    else if (token?.isNotEmpty() == true)
      authentication = Authentication.BearerToken(token!!)
    else return

    VisionSDK.getInstance().initialise(
      authentication,
      environment
    )
  }

//  val scope = CoroutineScope(Dispatchers.Default)

  private fun startScanning() {

    Log.d(VisionSdkViewManager.TAG, "startScanning: ")
    Log.d(VisionSdkViewManager.TAG, "scanningMode: $scanningMode")
    Log.d(VisionSdkViewManager.TAG, "detectionMode: $detectionMode")


//    val barcodeIndicatorFlow = callbackFlow<MutableList<Barcode>> {
//      customScannerView?.barcodeIndicators?.observe(lifecycleOwner!!) { list ->
//        scope.launch { send(list) }
//      }
//    }
//
//    val textIndicatorFlow = callbackFlow<Text> {
//      customScannerView?.textIndicator?.observe(lifecycleOwner!!) { text ->
//        scope.launch { send(text) }
//      }
//    }

//    barcodeIndicatorFlow.combine(textIndicatorFlow) { barcodeIndicator, textIndicator ->
//      Log.d(TAG, "textIndicator: " +textIndicator)
//      Log.d(TAG, "barcodeIndicators: "+barcodeIndicator)
//
//    }

//    val textIndicatorFlow = customScannerView?.textIndicator?.asFlow()
//    val barcodeIndicatorsFlow = customScannerView?.barcodeIndicators?.asFlow()
//    val thirdFlow = textIndicatorFlow?.combine(barcodeIndicatorsFlow!!) { textIndicator, barcodeIndicator ->
//      Pair(textIndicator, barcodeIndicator)
//    }
////    customScannerView?.textIndicator?.observe(lifecycleOwner!!){
////      Log.d(TAG, "textIndicator: ")
////
////    }
//    scope.launch {
////      Log.d(TAG, "scope: ")
//      thirdFlow?.collect {
//
//
////      Log.d(TAG, "thirdFlow: " +it.first.text)
//    } }

    customScannerView?.startScanning(
      ViewType.WINDOW,
      scanningMode,
      detectionMode,
      object : ScannerCallbacks {
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

        override fun onBarcodeDetected(barcode: Barcode) {
          Log.d(VisionSdkViewManager.TAG, "onBarcodeDetected: ")
          Toast.makeText(context!!, barcode.displayValue, Toast.LENGTH_LONG).show()

          val event = Arguments.createMap().apply {
            putArray("code", Arguments.fromArray(arrayOf(barcode.displayValue)))
          }
//          val reactContext = context as ReactContext
          appContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onBarcodeScanSuccess", event)
        }

        override fun onFailure(exception: ScannerException) {
          exception.printStackTrace()
        }

        override fun onImageCaptured(bitmap: Bitmap, value: MutableList<Barcode>?) {
          Log.d(VisionSdkViewManager.TAG, "onImageCaptured: ")

//        Toast.makeText(context!!,"onImageCaptured",Toast.LENGTH_LONG).show()
          triggerOCRCalls(bitmap, value ?: mutableListOf())
        }

        override fun onMultipleBarcodesDetected(barcodeList: List<Barcode>) {
          Log.d(VisionSdkViewManager.TAG, "onMultipleBarcodesDetected: ")
        }
      })

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
      "saveImage",
      0
    )
  }

  override fun receiveCommand(
    view: CustomScannerView,
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

  private fun triggerOCRCalls(bitmap: Bitmap, list: MutableList<Barcode>) {

    customScannerView!!.makeOCRApiCall(bitmap = bitmap,
      barcodeList = list,
      locationId = locationId ?: "",
      options = options?: mapOf() ,
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
    Log.d(TAG ,"apiKey: "+ apiKey)
    this.apiKey = apiKey
    initializeSdk()
  }

  @ReactProp(name = "token")
  fun setToken(view: View, token: String = "") {
    Log.d(TAG,"token: "+ token)
    this.token = token
    initializeSdk()
  }

  @ReactProp(name = "environment")
  fun setEnvironment(view: View, env: String = "") {
    Log.d(TAG , "environment: "+env)
    environment = when (env.lowercase()) {
      "dev" -> Environment.DEV
      "staging" -> Environment.STAGING
      else -> Environment.DEV
    }
    initializeSdk()
  }

  @ReactProp(name = "captureMode")
  fun setCaptureMode(view: View, captureMode: String = "") {
    Log.d(TAG , "captureMode: "+ captureMode)
    scanningMode = when (captureMode.lowercase()) {
      "auto" -> ScanningMode.Auto
      "manual" -> ScanningMode.Manual
      else -> ScanningMode.Auto
    }
  }

  @ReactProp(name = "mode")
  fun setMode(view: View, mode: String = "") {
    Log.d(TAG ,"mode: "+ mode)
    detectionMode = when (mode.lowercase()) {
      "ocr" -> DetectionMode.OCR
      "barcode" -> DetectionMode.Barcode
      "qrcode" -> DetectionMode.QR
      else -> DetectionMode.OCR
    }
  }

  @ReactProp(name = "locationId")
  fun setLocationId(view: View, locationId: String = "") {
    Log.d(TAG ,"locationId: "+ locationId)
    this.locationId = locationId
  }

  @ReactProp(name = "options")
  fun setOptions(view: View, options: String) {
    Log.d(TAG ,"options: "+ options)
    val map = options.split(",").associate {
      val (left, right) = it.split(":")
      left to right
    }
    this.options = map
  }


  @ReactProp(name = "color")
  fun setColor(view: View, color: String) {
    view.setBackgroundColor(Color.parseColor(color))


  }

  @ReactMethod
  fun startCamera() {
    Log.d(TAG, "startCamera: ")
//    this.startScanning()
  }

}
