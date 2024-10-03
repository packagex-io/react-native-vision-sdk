package com.visionsdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.launchOnIO
import com.asadullah.handyutils.save
import com.asadullah.handyutils.toDecimalPoints
import com.asadullah.handyutils.withContextMain
import com.facebook.infer.annotation.Assertions
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import io.packagex.visionsdk.ApiManager
import io.packagex.visionsdk.Authentication
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.R
import io.packagex.visionsdk.VisionSDK
import io.packagex.visionsdk.config.CameraSettings
import io.packagex.visionsdk.config.FocusSettings
import io.packagex.visionsdk.config.ObjectDetectionConfiguration
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.OCRResult
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.OnDeviceOCRManager
import io.packagex.visionsdk.ocr.ml.core.PlatformType
import io.packagex.visionsdk.ui.views.VisionCameraView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date


class VisionSdkViewManager(private val appContext: ReactApplicationContext) :
  ViewGroupManager<VisionCameraView>(), ScannerCallback, CameraLifecycleCallback, OCRResult {

  private var context: Context? = null
  override fun getName() = "VisionSdkView"
  private var apiKey: String? = ""
  private var token: String? = ""
  private var ocrMode: String = "cloud"
  private var locationId: String? = ""
  private var options: Map<String, Any>? = emptyMap()
  private var metaData: Map<String, Any>? = emptyMap()
  private var recipient: Map<String, Any>? = emptyMap()
  private var sender: Map<String, Any>? = emptyMap()
  private var environment: Environment = Environment.PRODUCTION
  private var visionCameraView: VisionCameraView? = null
  private var detectionMode: DetectionMode = DetectionMode.Barcode
  private var scanningMode: ScanningMode = ScanningMode.Manual
  private var flash: Boolean = false
  private var lifecycleOwner: LifecycleOwner? = null
  private var shouldStartScanning = true
  private var authentication: Authentication? = null

  //on device OCR
  private var onDeviceOCRManager: OnDeviceOCRManager? = null
  private var modelSize: ModelSize = ModelSize.Large
  private var modelType: ModelClass = ModelClass.ShippingLabel

  //camera configurations
  private var cameraSettings: CameraSettings = CameraSettings()

  //focus configurations
  private var focusSettings: FocusSettings = FocusSettings(appContext)

  //object detection configurations
  private var objectDetectionConfiguration: ObjectDetectionConfiguration =
    ObjectDetectionConfiguration()


  companion object {
    val TAG = "VisionCameraView"
  }

  override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {
    Log.d(TAG, "createViewInstance: ")
    context = appContext.currentActivity!!
    lifecycleOwner = context as LifecycleOwner
    visionCameraView = VisionCameraView(context!!, null)
    configureCamera()
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
  }

  override fun onDropViewInstance(view: VisionCameraView) {
    super.onDropViewInstance(view)
    Log.d(TAG, "onDropViewInstance: ")
    shouldStartScanning = true
    visionCameraView?.stopCamera()
  }


  private fun initializeSdk() {
    Log.d(TAG, "initializeSdk: ")
    VisionSDK.getInstance().initialize(
      context ?: return,
      environment,
    )
  }

  private fun configureCamera() {
    visionCameraView?.configure(
      isMultipleScanEnabled = true,
      detectionMode = detectionMode,
      scanningMode = scanningMode,
    )
  }

  private fun configureFocusSettings(focus: String? = null) {
    Log.d(TAG, "configureFocusSettings: $focus")

    if (focus != null) {
      JSONObject(focus).apply {
        focusSettings = FocusSettings(
          context = context!!,
          shouldDisplayFocusImage = optBoolean("shouldDisplayFocusImage", false),
          shouldScanInFocusImageRect = optBoolean("shouldScanInFocusImageRect", true),
          showDocumentBoundaries = optBoolean("showDocumentBoundaries", true),
          documentBoundaryBorderColor = optString("documentBoundaryBorderColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.WHITE,
          documentBoundaryFillColor = optString("documentBoundaryFillColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.argb(76, 255, 255, 0),
          focusImage = optString("focusImage").ifNeitherNullNorEmptyNorBlank {
            convertBase64ToBitmap(
              it
            )
          } ?: BitmapFactory.decodeResource(
            context?.resources,
            R.drawable.default_focus_frame
          ),
          focusImageRect = optJSONObject("focusImageRect")?.let {
            RectF(
              it.optDouble("x", 0.0).toFloat(),
              it.optDouble("y", 0.0).toFloat(),
              it.optDouble("x", 0.0).toFloat() + it.optDouble("width", 0.0).toFloat(),
              it.optDouble("y", 0.0).toFloat() + it.optDouble("height", 0.0).toFloat()
            )
          } ?: RectF(0.0F, 0.0F, 0.0F, 0.0F),
          focusImageTintColor = optString("focusImageTintColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.WHITE,
          focusImageHighlightedColor = optString("focusImageHighlightedColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.WHITE,
          showCodeBoundariesInMultipleScan = optBoolean(
            "showCodeBoundariesInMultipleScan",
            true
          ),
          validCodeBoundaryBorderColor = optString("validCodeBoundaryBorderColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.GREEN,
          validCodeBoundaryBorderWidth = optInt("validCodeBoundaryBorderWidth", 1),
          validCodeBoundaryFillColor = optString("validCodeBoundaryFillColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.argb(76, 0, 255, 0),
          invalidCodeBoundaryBorderColor = optString("invalidCodeBoundaryBorderColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.RED,
          invalidCodeBoundaryBorderWidth = optInt("invalidCodeBoundaryBorderWidth", 1),
          invalidCodeBoundaryFillColor = optString("invalidCodeBoundaryFillColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(
              it
            )
          } ?: Color.argb(76, 255, 0, 0),
        )
      }
    }
  }

  private fun setObjectDetectionSettings(objectDetectionSettings: String? = null) {
    Log.d(TAG, "configureObjectDetectionSetting: $objectDetectionSettings")

    if (objectDetectionSettings != null) {
      JSONObject(objectDetectionSettings).apply {
        objectDetectionConfiguration = ObjectDetectionConfiguration(
          isDocumentIndicationOn = optBoolean("isDocumentIndicationOn", true),
          secondsToWaitBeforeDocumentCapture = optInt(
            "secondsToWaitBeforeDocumentCapture",
            3
          ),
          isTextIndicationOn = optBoolean("isTextIndicationOn", true),
          isBarcodeOrQRCodeIndicationOn = optBoolean(
            "isBarcodeOrQRCodeIndicationOn",
            true
          ),
        )
      }
    }
    visionCameraView?.setObjectDetectionConfiguration(objectDetectionConfiguration)
  }

  private fun setCameraSettings(cameraSettingsObject: String? = null) {
    Log.d(TAG, "configureCameraSettings: $cameraSettingsObject")

    if (cameraSettingsObject != null) {
      JSONObject(cameraSettingsObject).apply {
        cameraSettings = CameraSettings(
          nthFrameToProcess = optInt("nthFrameToProcess", 10),
        )
      }
      visionCameraView?.setCameraSettings(cameraSettings)
    }
  }


  private fun setFocusSettings() {
    Log.d(TAG, "setFocusSettings: ")
    if (visionCameraView?.isCameraStarted()?.not() == true) return
    visionCameraView?.getFocusRegionManager()?.setFocusSettings(focusSettings)
  }

  override fun onIndications(
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

  override fun onCodesScanned(barcodeList: List<String>) {
    Log.d(TAG, "onBarcodeDetected: ")
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


  override fun onFailure(exception: VisionSDKException) {
    onOCRResponseFailed(exception)
  }

  override fun onImageCaptured(bitmap: Bitmap, value: List<String>) {
    Log.d(TAG, "onImageCaptured: ")

    saveBitmapAndSendEvent(bitmap)

    if (detectionMode == DetectionMode.OCR) {
      when (ocrMode) {
        "cloud" -> {
          cloudOcr(bitmap, value)
        }

        "on-device" -> {
          onDeviceOCR(bitmap, value)
        }

        "on-device-with-translation" -> {
          onDeviceWithMatchingApi(bitmap, value)
        }
      }
    }
  }

  private fun saveBitmapAndSendEvent(bitmap: Bitmap) {
    val parentDir = File(context?.filesDir, "VisionSdkSavedBitmaps")
    parentDir.mkdirs()

    val savedBitmapFile = File(parentDir, Date().time.toString() + ".jpg")

    bitmap.save(
      fileToSaveBitmapTo = savedBitmapFile,
      compressFormat = Bitmap.CompressFormat.JPEG
    )

    val filesList = parentDir.list() ?: emptyArray()
    if (filesList.size > 10) {
      filesList.sortBy { it }
      val fileToDelete = File(parentDir, filesList[0])
      fileToDelete.delete()
    }

    val event = Arguments.createMap().apply {
      putString("image", savedBitmapFile.toUri().toString())
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onImageCaptured", event)
  }

  private fun cloudOcr(bitmap: Bitmap, list: List<String>) {
    val apiManager = ApiManager()
    apiManager.shippingLabelApiCallAsync(
      apiKey = apiKey,
      token = token,
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


  private fun onDeviceOCR(bitmap: Bitmap, list: List<String>) {

    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        val result = getOndeviceOCRResponse(bitmap, list) ?: return@launchOnIO

        withContextMain {
          onOCRResponse(result)
        }
      } catch (e: VisionSDKException) {
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        if (e is CancellationException) throw e
        withContextMain {
          onOCRResponseFailed(VisionSDKException.UnknownException(e))
        }
      }
    }
  }

  private fun onDeviceWithMatchingApi(
    bitmap: Bitmap,
    list: List<String>
  ) {

    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        val onDeviceResponse = getOndeviceOCRResponse(bitmap, list) ?: return@launchOnIO

        val result = ApiManager().shippingLabelWithOnDeviceResponseApiCallSync(
          apiKey = apiKey,
          token = token,
          bitmap = bitmap,
          barcodeList = list,
          locationId = locationId ?: "",
          options = options ?: emptyMap(),
          metadata = metaData ?: emptyMap(),
          recipient = recipient ?: emptyMap(),
          sender = sender ?: emptyMap(),
          onDeviceResponse = onDeviceResponse
        )
        withContextMain {
          onOCRResponse(result)
        }
      } catch (e: VisionSDKException) {
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        if (e is CancellationException) throw e
        withContextMain {
          onOCRResponseFailed(VisionSDKException.UnknownException(e))
        }
      }
    }
  }

  private suspend fun getOndeviceOCRResponse(bitmap: Bitmap, list: List<String>): String? {
    return try {
      // Use withContext(Dispatchers.IO) to execute in the IO thread
      val result = withContext(Dispatchers.IO) {
        onDeviceOCRManager?.getPredictions(bitmap, list)
      }
      result
    } catch (e: VisionSDKException) {
      e.printStackTrace()
      withContextMain {
        onOCRResponseFailed(e)
      }
      null
    } catch (e: Exception) {
      e.printStackTrace()
      if (e is CancellationException) throw e
      withContextMain {
        onOCRResponseFailed(VisionSDKException.UnknownException(e))
      }
      null
    }
  }

  override fun getCommandsMap(): Map<String?, Int?> {
//    Log.d(TAG, "getCommandsMap: ")
    return mapOf(
      "captureImage" to
        0,
      "stopRunning" to
        1,
      "startRunning" to
        2,
      "setMetaData" to
        3,
      "setRecipient" to
        4,
      "setSender" to
        5,
      "configureOnDeviceModel" to
        6,
      "restartScanning" to
        7,
      "setFocusSettings" to
        8,
      "setObjectDetectionSettings" to
        9,
      "setCameraSettings" to
        10,
    )
  }

  override fun receiveCommand(
    view: VisionCameraView,
    commandType: Int,
    args: ReadableArray?
  ) {
//    Log.d(TAG, "receiveCommand: $commandType")
    Assertions.assertNotNull(view)
    Assertions.assertNotNull(args)
    when (commandType?.toInt()) {
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
        setMetaData(args?.getMap(0))
        return
      }

      4 -> {
        setRecipient(args?.getMap(0))
        return
      }

      5 -> {
        setSender(args?.getMap(0))
        return
      }

      6 -> {
        configureOnDeviceModel(args?.getMap(0).toString())
        return
      }

      7 -> {
        restartScanning()
        return
      }

      8 -> {
        configureFocusSettings(args?.getMap(0).toString())
        return
      }

      9 -> {
        setObjectDetectionSettings(args?.getMap(0).toString())
        return
      }

      10 -> {
        setCameraSettings(args?.getMap(0).toString())
        return
      }

//      else -> throw IllegalArgumentException(
//        String.format(
//          "Unsupported command %d received by %s.",
//          commandType,
//          javaClass.simpleName
//        )
//      )
    }

  }


  private fun captureImage() {
    Log.d(TAG, "captureImage: ")
    visionCameraView?.capture()
  }

  private fun stopScanning() {
    Log.d(TAG, "stopScanning: ")
//        if (visionCameraView?.isCameraStarted()?.not() == true) return
    visionCameraView?.stopCamera()
  }

  private fun startCamera() {
    Log.d(TAG, "startCamera: ")
    if (visionCameraView?.isCameraStarted() == true)
      visionCameraView?.stopCamera()
    visionCameraView?.startCamera()
  }

  private fun setMetaData(metaData: ReadableMap?) {
    Log.d(TAG, "metaData: $metaData")
    this.metaData = metaData?.toHashMap()
  }

  private fun setRecipient(recipient: ReadableMap?) {
    Log.d(TAG, "recipient: $recipient")
    if (recipient == null) {
      this.recipient = emptyMap()
    } else {
      this.recipient = recipient.toHashMap()
    }
  }

  private fun setSender(sender: ReadableMap?) {
    Log.d(TAG, "sender: $sender")
    if (sender == null) {
      this.sender = emptyMap()
    } else {
      this.sender = sender.toHashMap()
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

    if (onDeviceConfigs != "null")
      onDeviceConfigs.ifNeitherNullNorEmptyNorBlank { onDeviceConfigs ->
        JSONObject(onDeviceConfigs).apply {
          setModelSize(optString("size", "large"))
          setModelType(optString("type", "shipping_label"))
        }
      }


    onDeviceOCRManager?.destroy()
    onDeviceOCRManager = OnDeviceOCRManager(
      context = context!!,
      platformType = PlatformType.ReactNative,
      modelClass = modelType,
      modelSize = modelSize
    )
    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        if (onDeviceOCRManager?.isConfigured()?.not() == true) {
          var lastProgress = 0.00
          onDeviceOCRManager?.configure(apiKey, token) {
            val progressInt = (it).toDecimalPoints(2).toDouble()
            if (progressInt != lastProgress) {
              lastProgress = progressInt

              Log.d(TAG, "Install Progress: ${(it * 100).toInt()}")
              if (onDeviceOCRManager?.isModelAlreadyDownloaded() == false) {
                val event = Arguments.createMap().apply {
                  putDouble("progress", lastProgress)
                  putBoolean("downloadStatus", false)
                }
                appContext
                  .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                  .emit("onModelDownloadProgress", event)
              }
            }
          }
        }

        val event = Arguments.createMap().apply {
          putDouble("progress", 1.00)
          putBoolean("downloadStatus", true)
        }
        appContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
          .emit("onModelDownloadProgress", event)

      } catch (e: VisionSDKException) {
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        if (e is CancellationException) throw e
        withContextMain {
          onOCRResponseFailed(VisionSDKException.UnknownException(e))
        }
      }
    }
  }

  private fun restartScanning() {
    Log.d(TAG, "restartScanning: ")
    visionCameraView?.rescan()
  }

  //  React Props
  @ReactProp(name = "flash")
  fun flash(view: View, flash: Boolean = false) {
    Log.d(TAG, "flash: $flash")
    this.flash = flash
    this.visionCameraView?.setFlashTurnedOn(flash)
  }

  @ReactProp(name = "zoomLevel")
  fun setZoomLevel(view: VisionCameraView, zoomLevel: Float) {
    Log.d(TAG, "setZoomLevel: $zoomLevel")
    visionCameraView?.setZoomRatio(zoomLevel)
  }

  @ReactProp(name = "apiKey")
  fun setApiKey(view: View, apiKey: String = "") {
    Log.d(TAG, "apiKey: $apiKey")
    this.apiKey = apiKey
  }

  @ReactProp(name = "token")
  fun setToken(view: View, token: String = "") {
    Log.d(TAG, "token: $token")
    this.token = token
  }

  @ReactProp(name = "environment")
  fun setEnvironment(view: View, env: String = "") {
    Log.d(TAG, "environment: $env")
    environment = when (env.lowercase()) {
      "dev" -> Environment.DEV
      "staging" -> Environment.STAGING
      "sandbox" -> Environment.SANDBOX
      "prod" -> Environment.PRODUCTION
      else -> Environment.PRODUCTION
    }
    initializeSdk()
  }

  @ReactProp(name = "captureMode")
  fun setCaptureMode(view: View, captureMode: String = "") {
    Log.d(TAG, "captureMode: $captureMode")
    scanningMode = when (captureMode.lowercase()) {
      "auto" -> ScanningMode.Auto
      "manual" -> ScanningMode.Manual
      else -> ScanningMode.Auto
    }
    visionCameraView?.setScanningMode(scanningMode)

  }

  @ReactProp(name = "mode")
  fun setMode(view: View, mode: String = "") {
    Log.d(TAG, "mode: $mode")
    detectionMode = when (mode.lowercase()) {
      "ocr" -> DetectionMode.OCR
      "barcode" -> DetectionMode.Barcode
      "qrcode" -> DetectionMode.QRCode
      "photo" -> DetectionMode.Photo
      "barcodeorqrcode" -> DetectionMode.BarcodeOrQRCode
      else -> DetectionMode.Barcode
    }
    visionCameraView?.setDetectionMode(detectionMode)
  }

  @ReactProp(name = "ocrMode")
  fun ocrMode(view: View, ocrMode: String = "cloud") {
    Log.d(TAG, "ocrMode: $ocrMode")
    this.ocrMode = ocrMode
  }

  @ReactProp(name = "locationId")
  fun setLocationId(view: View, locationId: String = "") {
    Log.d(TAG, "locationId: $locationId")
    this.locationId = locationId
  }

  @ReactProp(name = "options")
  fun setOptions(view: View, options: ReadableMap) {
    Log.d(TAG, "options: $options")
    this.options = options.toHashMap()
  }

  override fun onCameraStarted() {
    Log.d(TAG, "onCameraStarted: ")
    Log.d(TAG, "maxZoomLevel: ${visionCameraView?.getMaxZoomRatioAvailable()}")
    setFocusSettings()
  }

  override fun onCameraStopped() {
    Log.d(TAG, "onCameraStopped: ")
  }

  override fun onOCRResponse(response: String?) {

    Log.d(TAG, "api responded with  ${response}")
    val event = Arguments.createMap().apply {
      putString("data", JSONObject(response).toString())
    }
//          val reactContext = context as ReactContext
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onOCRScan", event)
  }

  override fun onOCRResponseFailed(exception: VisionSDKException) {
    exception.printStackTrace()
    val event = Arguments.createMap().apply {
      putString("message", exception.errorMessage)
      putInt("code", exception.errorCode)
      putString("detailedMessage", exception.detailedMessage)
    }
    Log.d(TAG, "${event}")
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onError", event)

  }

  private fun hexColorToInt(color: String): Int {
    return Color.parseColor(convertRRGGBBAAToAARRGGBB(color))
  }

//  #50ffbf80

  private fun intColorToHex(color: Int): String {
    return String.format("#%06X", 0xFFFFFF and color)
  }

  private fun convertRRGGBBAAToAARRGGBB(color: String): String {
    // Ensure the input color is in the expected format
    if (!(color.length == 7 || color.length == 9) || !color.startsWith("#")) {
      onOCRResponseFailed(VisionSDKException.UnknownException(IllegalArgumentException("Invalid color format. Expected format: #RRGGBBAA or #RRGGBB")))
    }

    // Extract the color components based on the length of the input
    val r = color.substring(1, 3)
    val g = color.substring(3, 5)
    val b = color.substring(5, 7)

    // Check if there is an alpha component
    val a = if (color.length == 9) {
      color.substring(7, 9)
    } else {
      "FF" // Default alpha value for full opacity
    }

    // Return the color in AARRGGBB format
    return "#$a$r$g$b"
  }

  private fun convertBase64ToBitmap(b64: String): Bitmap {
    val imageAsBytes: ByteArray = Base64.decode(b64.toByteArray(), Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
  }

  private fun convertDrawableToBase64(drawable: Int): String {
    val bitmap = BitmapFactory.decodeResource(context?.getResources(), drawable)
    val byteStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream)
    val byteArray = byteStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
  }
}





