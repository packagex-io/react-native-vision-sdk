package com.visionsdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.util.Base64
import android.util.Log
import android.view.View
import android.net.Uri
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
//import io.packagex.visionsdk.Authentication
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
import io.packagex.visionsdk.ocr.ml.core.enums.ModelSize
import io.packagex.visionsdk.ocr.ml.core.enums.PlatformType
import io.packagex.visionsdk.ui.views.VisionCameraView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.asadullah.handyutils.launchOnDefault
import java.net.HttpURLConnection
import java.net.URL
import com.visionsdk.utils.EventUtils
import io.packagex.visionsdk.core.TemplateManager
import io.packagex.visionsdk.core.pricetag.PriceTagData
import io.packagex.visionsdk.dto.ScannedCodeResult
import io.packagex.visionsdk.ocr.ml.core.enums.ExecutionProvider
import io.packagex.visionsdk.ocr.ml.core.enums.OCRModule
import io.packagex.visionsdk.service.dto.BOLModelToReport
import io.packagex.visionsdk.service.dto.DCModelToReport
import io.packagex.visionsdk.service.dto.ILModelToReport
import io.packagex.visionsdk.service.dto.SLModelToReport
import io.packagex.visionsdk.ui.startCreateTemplateScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

// VisionSdkViewManager is a class responsible for managing the Vision SDK view component within
// a React Native application. It connects native Vision SDK functionality with React Native through a bridge.
// This includes handling camera operations, configuring OCR settings, and processing scanned data.
class VisionSdkViewManager(private val appContext: ReactApplicationContext) :
  ViewGroupManager<VisionCameraView>(), ScannerCallback, CameraLifecycleCallback, OCRResult {
    private lateinit var coroutineScope: CoroutineScope

  private var context: Context? = null // The context of the current application

  override fun getName() = "VisionSdkView" // Override to provide the name of the custom view that will be used in React Native

  private var apiKey: String? = "" // API key for the Vision SDK authentication

  private var token: String? = "" // Token for the Vision SDK authentication

  private var ocrMode: String = "cloud" // Mode for OCR, default is "cloud" for server-side OCR

  private var ocrType: String = "shipping_label" //defines model type selected as string

  private var locationId: String? = null// Location identifier for the scanning process

  private var options: Map<String, Any>? = emptyMap() // Options for configuring the Vision SDK, passed from React Native

  private var metaData: Map<String, Any>? = emptyMap() // Metadata information related to the scanned items, passed from React Native

  private var recipient: Map<String, Any>? = emptyMap() // Recipient information for scanned data, passed from React Native

  private var sender: Map<String, Any>? = emptyMap() // Sender information for scanned data, passed from React Native

  private var environment: Environment = Environment.PRODUCTION // The environment (either production or testing) for the Vision SDK

  private var visionCameraView: VisionCameraView? = null // VisionCameraView instance that displays the camera view

  private var detectionMode: DetectionMode = DetectionMode.Barcode // Detection mode for scanning, default is Barcode detection

  private var scanningMode: ScanningMode = ScanningMode.Manual // Scanning mode, can be manual or automatic scanning

  private var flash: Boolean = false // Boolean flag to enable or disable the flash

  private var executionProvider: ExecutionProvider = ExecutionProvider.NNAPI

  private var isMultipleScanEnabled: Boolean = false //  Boolean isMultipleScanEnabled to enable or disable the isMultipleScanEnabled

  private var isEnableAutoOcrResponseWithImage : Boolean? = false //  Boolean isMultipleScanEnabled to enable or disable the isMultipleScanEnable

  private var lifecycleOwner: LifecycleOwner? = null // LifecycleOwner to manage the camera lifecycle

  private var shouldStartScanning = true // Flag to determine if scanning should start automatically

  private var shouldResizeImage = true
//  private var authentication: Authentication? = null // Authentication instance for Vision SDK login

  private var modelSize: ModelSize? = ModelSize.Large // Model size for on-device OCR (Large/Small)

  private var imagePath: String? = "" // Image Path

  private var modelType: OCRModule =  OCRModule.ShippingLabel(modelSize)// Model type for OCR processing (ShippingLabel, Invoice, etc.)

  private var cameraSettings: CameraSettings = CameraSettings() // Camera settings for configuring camera properties

  private var focusSettings: FocusSettings = FocusSettings(appContext) // Focus settings for controlling focus behavior in the camera view

  private var objectDetectionConfiguration: ObjectDetectionConfiguration = ObjectDetectionConfiguration() // Object detection settings for identifying objects within the camera view

  companion object {
    const val TAG = "VisionCameraView" // Companion object to hold constants, like the TAG for logging
  }


  /**
   * Creates a new instance of the VisionCameraView and sets it up for use.
   * This method also configures the camera settings, sets the camera lifecycle callback,
   * and the scanner callback.
   *
   * @param reactContext - The themed React Native context in which the view is being created.
   * @return VisionCameraView - The configured camera view instance.
   */
  override fun createViewInstance(reactContext: ThemedReactContext): VisionCameraView {
    Log.d(TAG, "createViewInstance: ")
    context = appContext.currentActivity!! // Get the current activity as context
    lifecycleOwner = context as LifecycleOwner // Set lifecycle owner to manage camera lifecycle
    visionCameraView = VisionCameraView(context!!, null) // Create a new VisionCameraView instance
    configureCamera() // Configure camera settings
    visionCameraView?.setCameraLifecycleCallback(this) // Set lifecycle callback
    visionCameraView?.setScannerCallback(this) // Set scanner callback

    coroutineScope = CoroutineScope(Dispatchers.Default)

    return visionCameraView!!
  }

  /**
   * Called after any property from React Native side is updated.
   * This method updates the VisionCameraView instance when React Native sends changes.
   *
   * @param view - The updated VisionCameraView instance that will be used.
   */
  override fun onAfterUpdateTransaction(view: VisionCameraView) {
    super.onAfterUpdateTransaction(view)
    visionCameraView = view // Assign the updated view to visionCameraView
    Log.d(TAG, "onAfterUpdateTransaction: ")
  }



  /**
   * Called when the view instance is removed from the view hierarchy.
   * This method is used to clean up resources associated with the camera view.
   *
   * @param view - The VisionCameraView instance that is being removed.
   */
  override fun onDropViewInstance(view: VisionCameraView) {
    super.onDropViewInstance(view)
    Log.d(TAG, "onDropViewInstance: ")
    coroutineScope.cancel()
    shouldStartScanning = true // Reset scanning flag
    visionCameraView?.stopCamera() // Stop the camera to release resources
  }

  /**
   * Initializes the Vision SDK with the provided environment configuration and context.
   * This method is required to set up the Vision SDK and should be called once during the app's initialization.
   *
   * @param environment - The environment settings (e.g., production, development) for the Vision SDK.
   */
  private fun initializeSdk() {
    Log.d(TAG, "initializeSdk: $environment")
    VisionSdkSingleton.initializeSdk(context ?: return, environment)
  }

  /**
   * Configures the camera settings, including enabling multiple scans and setting detection and scanning modes.
   *
   * @param isMultipleScanEnabled - Flag indicating whether multiple scans are allowed.
   * @param detectionMode - The mode of detection (e.g., barcode, document, etc.).
   * @param scanningMode - The mode of scanning (e.g., continuous, once per frame, etc.).
   */
  private fun configureCamera() {
    visionCameraView?.configure(
      isMultipleScanEnabled = isMultipleScanEnabled, // Enable multiple scans
      detectionMode = detectionMode, // Set detection mode
      scanningMode = scanningMode // Set scanning mode
    )
  }

  /**
   * Configures the focus settings for the VisionCameraView, allowing customization
   * of focus behavior, appearance, and scan region. It parses the provided JSON string
   * and sets the relevant focus options including focus image, boundaries, and other settings.
   *
   * @param focus - A JSON string that contains the focus settings. The settings include options
   *                for the focus image, document boundaries, focus image tint color, and
   *                focus rectangle. If null, default settings will be applied.
   */
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
            hexColorToInt(it)
          } ?: Color.WHITE,
          documentBoundaryFillColor = optString("documentBoundaryFillColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.argb(76, 255, 255, 0),
          focusImage = optString("focusImage").ifNeitherNullNorEmptyNorBlank {
            convertBase64ToBitmap(it)
          } ?: BitmapFactory.decodeResource(
            context?.resources,
            R.drawable.default_focus_frame
          ),
          focusImageRect = optJSONObject("focusImageRect")?.let {
            val density = context?.resources?.displayMetrics?.density ?: 1.0f
            // Extract dp values from JSON.
            val xDip = it.optDouble("x", 0.0)
            val yDip = it.optDouble("y", 0.0)
            val widthDip = it.optDouble("width", 0.0)
            val heightDip = it.optDouble("height", 0.0)

            // Convert dp to pixels using your working approach.
            val xPx = ((xDip * density.toDouble()).toFloat())
            val yPx = ((yDip * density.toDouble()).toFloat())
            val widthPx = ((widthDip * density.toDouble()).toFloat())
            val heightPx = ((heightDip * density.toDouble()).toFloat())

            RectF(xPx, yPx, xPx + widthPx, yPx + heightPx)
          } ?: RectF(0.0F, 0.0F, 0.0F, 0.0F),
          focusImageTintColor = optString("focusImageTintColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.WHITE,
          focusImageHighlightedColor = optString("focusImageHighlightedColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.WHITE,
          showCodeBoundariesInMultipleScan = optBoolean("showCodeBoundariesInMultipleScan", true),
          validCodeBoundaryBorderColor = optString("validCodeBoundaryBorderColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.GREEN,
          validCodeBoundaryBorderWidth = optInt("validCodeBoundaryBorderWidth", 1),
          validCodeBoundaryFillColor = optString("validCodeBoundaryFillColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.argb(76, 0, 255, 0),
          invalidCodeBoundaryBorderColor = optString("invalidCodeBoundaryBorderColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.RED,
          invalidCodeBoundaryBorderWidth = optInt("invalidCodeBoundaryBorderWidth", 1),
          invalidCodeBoundaryFillColor = optString("invalidCodeBoundaryFillColor").ifNeitherNullNorEmptyNorBlank {
            hexColorToInt(it)
          } ?: Color.argb(76, 255, 0, 0),
        )
      }
    }
  }

  /**
   * Configures the object detection settings, such as document, text, and barcode indications.
   * Accepts a JSON string defining the object detection settings.
   *
   * @param objectDetectionSettings - JSON string containing object detection settings (e.g., document, text, barcode detection).
   */
  private fun setObjectDetectionSettings(objectDetectionSettings: String? = null) {
    Log.d(TAG, "configureObjectDetectionSetting: $objectDetectionSettings")

    if (objectDetectionSettings != null) {
      JSONObject(objectDetectionSettings).apply {
        objectDetectionConfiguration = ObjectDetectionConfiguration(
          isDocumentIndicationOn = optBoolean("isDocumentIndicationOn", true), // Show document indication
          secondsToWaitBeforeDocumentCapture = optInt("secondsToWaitBeforeDocumentCapture", 3), // Delay for document capture
          isTextIndicationOn = optBoolean("isTextIndicationOn", true), // Show text indication
          isBarcodeOrQRCodeIndicationOn = optBoolean("isBarcodeOrQRCodeIndicationOn", true) // Show barcode indication
        )
      }


    }
    visionCameraView?.setObjectDetectionConfiguration(objectDetectionConfiguration) // Apply detection config to view

    if(objectDetectionSettings != null){
      var selectedTemplateId = ""
      JSONObject(objectDetectionSettings).apply {
        selectedTemplateId = optString("selectedTemplateId", "")
      }

      if(selectedTemplateId != ""){
        val templates = TemplateManager().getAllBarcodeTemplates()
        val templateToApply = templates.firstOrNull { template -> template.name == selectedTemplateId }
        if(templateToApply != null){
          visionCameraView?.applyBarcodeTemplate(templateToApply)
        }
      } else {
        visionCameraView?.removeBarcodeTemplate()
      }

    }
  }

  /**
   * Configures camera settings by parsing a JSON string for specific configurations, such as the frame processing rate.
   * If a configuration is provided, it sets the camera settings in the visionCameraView.
   * @param cameraSettingsObject - JSON string representing camera settings (optional)
   */
  private fun setCameraSettings(cameraSettingsObject: String? = null) {
    Log.d(TAG, "configureCameraSettings: $cameraSettingsObject")

    if (cameraSettingsObject != null) {
      JSONObject(cameraSettingsObject).apply {
        cameraSettings = CameraSettings(
          nthFrameToProcess = optInt("nthFrameToProcess", 10) // Default to processing every 10th frame if not specified
        )
      }
      visionCameraView?.setCameraSettings(cameraSettings)
    }
  }

  private fun reportError(args: ReadableArray?) {

    try {
      val data = args?.getMap(0)
      val token = args?.getString(1)  // Retrieve token from the third element of the array
      val apiKey = args?.getString(2) // Retrieve API key from the fourth element

      val resolvedToken = token ?: this.token
      val resolvedApiKey = apiKey ?: this.apiKey

      if (data == null) {
        Log.e(TAG, "reportError: Data is null")
        return
      }

      // Extension function to safely convert Any to Map<String, Any?>
      fun Any?.asStringMap(): Map<String, Any?>? {
        return if (this is Map<*, *>) {
          this.entries
            .filter { it.key is String }  // Only include entries with String keys
            .associate { it.key as String to it.value }  // Safe cast of key to String
        } else {
          null
        }
      }

      // Convert ReadableMap to a Kotlin Map
      val parsedData = data.toHashMap()

      // Extract properties with default values
      val reportText = parsedData["reportText"] as? String ?: "No Report Text"
      val modelType = parsedData["type"] as? String ?: "shipping_label"
      val modelSize = parsedData["size"] as? String ?: "large"
      val imagePath = parsedData["image"] as? String

      // Safely handle 'response' as Map<String, Any?> or null
      val response = parsedData["response"]?.asStringMap()

      val errorFlags: Map<String, Boolean> = parsedData["errorFlags"]?.asStringMap()?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()


      val modelToReport = when (modelType) {
        in listOf("shipping_label", "shipping-label")  -> {
          SLModelToReport(
            this.modelSize,
            trackingNo = errorFlags["trackingNo"] ?: false,
            courierName = errorFlags["courierName"] ?: false,
            weight = errorFlags["weight"] ?: false,
            dimensions = errorFlags["dimensions"] ?: false,
            receiverName = errorFlags["receiverName"] ?: false,
            receiverAddress = errorFlags["receiverAddress"] ?: false,
            senderName = errorFlags["senderName"] ?: false,
            senderAddress = errorFlags["senderAddress"] ?: false
          )
        }
        in listOf("item_label", "item-label") -> {
          ILModelToReport(
            this.modelSize,
            supplierName = errorFlags["supplierName"] ?: false,
            itemName = errorFlags["itemName"] ?: false,
            itemSKU = errorFlags["itemSKU"] ?: false,
            weight = errorFlags["weight"] ?: false,
            quantity = errorFlags["quantity"] ?: false,
            dimensions = errorFlags["dimensions"] ?: false,
            productionDate = errorFlags["productionDate"] ?: false,
            supplierAddress = errorFlags["supplierAddress"] ?: false
          )
        }
        in listOf("bill_of_lading", "bill-of-lading") -> {
          BOLModelToReport(
            this.modelSize,
            referenceNo = errorFlags["referenceNo"] ?: false,
            loadNumber = errorFlags["loadNumber"] ?: false,
            purchaseOrderNumber = errorFlags["purchaseOrderNumber"] ?: false,
            invoiceNumber = errorFlags["invoiceNumber"] ?: false,
            customerPurchaseOrderNumber = errorFlags["customerPurchaseOrderNumber"] ?: false,
            orderNumber = errorFlags["orderNumber"] ?: false,
            billOfLading = errorFlags["billOfLading"] ?: false,
            masterBillOfLading = errorFlags["masterBillOfLading"] ?: false,
            lineBillOfLading = errorFlags["lineBillOfLading"] ?: false,
            houseBillOfLading = errorFlags["houseBillOfLading"] ?: false,
            shippingId = errorFlags["shippingId"] ?: false,
            shippingDate = errorFlags["shippingDate"] ?: false,
            date = errorFlags["date"] ?: false
          )
        }
        in listOf("document-classification") -> {
          DCModelToReport(
            this.modelSize,
            documentClass = errorFlags["documentClass"] ?: false
          )
        }
        else -> {
          Log.e(TAG, "reportError: Unknown model type")
          return
        }
      }


      // Log all properties for debugging
      Log.d(TAG, """
        Report Error:
        - Report Text: $reportText
        - Model Type: $modelType
        - Model Size: $modelSize
        - Image Path: $imagePath
        - Response: $response
    """.trimIndent())

      // Convert image path to Base64 if available
      val base64Image = imagePath?.takeIf { it.isNotBlank() }?.let { convertImageToBase64(it) }

      // API call
      val apiManager = ApiManager()
      apiManager.reportAnIssueAsync(
        context = appContext, // Replace with your application context or required context
        apiKey = resolvedApiKey,      // Replace with the actual API key
        token = resolvedToken,        // Replace with the actual token
        platformType = PlatformType.ReactNative, // Assuming platform is Android, adjust as needed
        modelToReport = modelToReport,
        report = reportText,
        customData = response,
        base64ImageToReportOn = base64Image,
        onComplete = { result ->
          // Handle completion result here
          Log.d(TAG, "Report completed with result: $result")
        }
      )

    } catch(e: Exception){
        Log.e(TAG, "ERROR REPORTING ERROR: ", e)
    }




  }

  // Function to convert image to Base64
  private fun convertImageToBase64(inputPath: String): String? {
    return try {
      Log.d(TAG, "Input path: $inputPath")

      // Check if the input path is a URL
      if (inputPath.startsWith("http://") || inputPath.startsWith("https://")) {
        // Handle remote URL
        val url = URL(inputPath)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val inputStream = connection.inputStream
        val bytes = inputStream.readBytes()
        inputStream.close()
        Base64.encodeToString(bytes, Base64.DEFAULT)
      } else {
        // Handle local file path
        val correctedPath = if (inputPath.startsWith("file://")) {
          inputPath.removePrefix("file://")
        } else {
          inputPath
        }

        val file = File(correctedPath)
        if (!file.exists()) {
          Log.e(TAG, "File does not exist at the specified path: $correctedPath")
          null
        } else {
          Log.d(TAG, "File found: ${file.absolutePath}, Readable: ${file.canRead()}")
          val inputStream = FileInputStream(file)
          val bytes = inputStream.readBytes()
          inputStream.close()
          Base64.encodeToString(bytes, Base64.DEFAULT)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error converting image to Base64: ${e.message}", e)
      null
    }
  }

  private val density = appContext.resources.displayMetrics.density
  fun Int.toDp(): Int = (this / density + 0.5f).toInt()


  /**
   * Sets focus settings for the camera if the camera is started, using the focus region manager.
   */
  private fun setFocusSettings() {
    Log.d(TAG, "setFocusSettings: ")
    if (visionCameraView?.isCameraStarted()?.not() == true) return
    visionCameraView?.getFocusRegionManager()?.setFocusSettings(focusSettings)
  }

  /**
   * Callback function that is triggered when detection events occur.
   * Creates an event to indicate detection results (barcode, QR code, text, document) and sends it to JavaScript.
   * @param barcodeDetected - Whether a barcode was detected
   * @param qrCodeDetected - Whether a QR code was detected
   * @param textDetected - Whether text was detected
   * @param documentDetected - Whether a document was detected
   */
  override fun onIndications(
    barcodeDetected: Boolean,
    qrCodeDetected: Boolean,
    textDetected: Boolean,
    documentDetected: Boolean
  ) {
    val event = Arguments.createMap().apply {
      putBoolean("barcode", barcodeDetected)
      putBoolean("qrcode", qrCodeDetected)
      putBoolean("text", textDetected)
      putBoolean("document", documentDetected)
      putBoolean("test", false)
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onDetected", event)
  }

  override fun onIndicationsBoundingBoxes(
    barcodeBoundingBoxes: List<Rect>,
    qrCodeBoundingBoxes: List<Rect>,
    documentBoundingBox: Rect?
  ) {
    try {



      val event = Arguments.createMap().apply {
        val barcodeRectsArray = Arguments.createArray()
        val qrCodeRectsArray = Arguments.createArray()
        val documentsRect = Arguments.createMap()

        if(barcodeBoundingBoxes.isNotEmpty()){
          barcodeBoundingBoxes.forEach {
            boundingBox -> barcodeRectsArray.pushMap(Arguments.createMap().apply {
                putInt("x", boundingBox.left.toDp())
                putInt("y", boundingBox.top.toDp())
                putInt("width", boundingBox.width().toDp())
                putInt("height", boundingBox.height().toDp())
            })
          }
        }

        if(qrCodeBoundingBoxes.isNotEmpty()){
          qrCodeBoundingBoxes.forEach{
            boundingBox -> qrCodeRectsArray.pushMap(Arguments.createMap().apply {
                putInt("x", boundingBox.left.toDp())
                putInt("y", boundingBox.top.toDp())
                putInt("width", boundingBox.width().toDp())
                putInt("height", boundingBox.height().toDp())
            })
          }
        }

        if(documentBoundingBox != null){
          documentsRect.putInt("x", documentBoundingBox.left.toDp())
          documentsRect.putInt("y", documentBoundingBox.top.toDp())
          documentsRect.putInt("width", documentBoundingBox.width().toDp())
          documentsRect.putInt("height", documentBoundingBox.height().toDp())
        }

        putArray("barcodeBoundingBoxes", barcodeRectsArray)
        putArray("qrCodeBoundingBoxes", qrCodeRectsArray)
        putMap("documentBoundingBox", documentsRect)
      }

      appContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onBoundingBoxesDetected", event)

    } catch (e:Exception){
      Log.d(TAG, "error ${e.message}")
    }
  }

  override fun onItemRetrievalResult(scannedCodeResults: ScannedCodeResult) {
    Log.d("IJS", "on item retrieval results")
  }

  override fun onPriceTagResult(priceTagData: PriceTagData) {
    val boundingBoxMap = Arguments.createMap().apply {
      putInt("x", priceTagData.boundingBox.left.toDp())
      putInt("y", priceTagData.boundingBox.top.toDp())
      putInt("width", priceTagData.boundingBox.width().toDp())
      putInt("height", priceTagData.boundingBox.height().toDp())
    }

    val event = Arguments.createMap().apply {
      putString("sku", priceTagData.productSKU)
      putString("price", priceTagData.productPrice)
      putMap("boundingBox", boundingBoxMap)
    }

    Log.d(TAG, "Emitting event: onPriceTagDetected: $event")
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onPriceTagDetected", event)
  }

  /**
   * Callback function triggered upon failure, passing the exception to the OCR response failure handler.
   * @param exception - Exception representing the failure cause
   */
  override fun onFailure(exception: VisionSDKException) {
    onOCRResponseFailed(exception)
  }

  override fun onScanResult(barcodeList: List<ScannedCodeResult>) {
    Log.d(TAG, "onScanResult called with barcodeList: $barcodeList")

    if (barcodeList.isEmpty()) {
      Log.e(TAG, "barcodeList is empty, skipping event emission")

      return
    }

    try {
      val event = Arguments.createMap().apply {
        // Create a WritableArray to hold the scanned results
        val codesArray = Arguments.createArray()

        barcodeList.forEach { scannedCodeResult ->
          // Create a WritableMap for each ScannedCodeResult
          val codeMap = Arguments.createMap().apply {
            putString("scannedCode", scannedCodeResult.scannedCode)
            putString("symbology", scannedCodeResult.symbology.toString())

            // Add bounding box
            val boundingBoxMap = Arguments.createMap().apply {
              putInt("x", scannedCodeResult.boundingBox.left.toDp())
              putInt("y", scannedCodeResult.boundingBox.top.toDp())
              putInt("width", scannedCodeResult.boundingBox.width().toDp())
              putInt("height", scannedCodeResult.boundingBox.height().toDp())
            }
            putMap("boundingBox", boundingBoxMap)

            scannedCodeResult.gs1ExtractedInfo?.let { gs1Info ->
              val gs1Map = Arguments.createMap()
              for ((key, value) in gs1Info) {
                gs1Map.putString(key, value)
              }
              putMap("gs1ExtractedInfo", gs1Map)
            }
          }

          // Add the WritableMap to the WritableArray
          codesArray.pushMap(codeMap)
        }

        // Add the array to the event
        putArray("codes", codesArray)
        Log.d(TAG, "Event map created successfully")
      }

      Log.d(TAG, "Emitting event: $event")
      appContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onBarcodeScan", event)
    } catch (e: Exception) {
      Log.e(TAG, "Error in onScanResult: ${e.message}", e)
    }
  }


  override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>) {
    // Empty stub - only the version with sharpness score is used
  }

  override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, imageSharpnessScore: Float) {
    Log.d(TAG, "onImageCaptured (with sharpness): ")
    this.imagePath = ""
    val image = saveBitmapAndSendEvent(bitmap, scannedCodeResults, imageSharpnessScore)
    // Check if OCR mode is enabled and auto-response is required
    if (detectionMode == DetectionMode.OCR && isEnableAutoOcrResponseWithImage == true) {
      this.imagePath = image
      handleOcrMode(bitmap, scannedCodeResults.map { it.scannedCode })
    }
  }

  override fun onImageSharpnessScore(imageSharpnessScore: Float) {
    Log.d(TAG, "onImageSharpnessScore: $imageSharpnessScore")
    try {
      val event = Arguments.createMap().apply {
        putDouble("sharpnessScore", imageSharpnessScore.toDouble())
      }
      // Emit the event to JavaScript listeners for onSharpnessScore
      appContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onSharpnessScore", event)
    } catch (e: Exception) {
      Log.e(TAG, "Error in onImageSharpnessScore: ${e.message}", e)
    }
  }


  /**
   * Handles OCR processing based on the specified OCR mode.
   * Calls the appropriate prediction method for the given mode.
   *
   * @param bitmap Captured image as a Bitmap.
   * @param value List of detected values (e.g., barcodes, QR codes).
   */
  private fun handleOcrMode(bitmap: Bitmap, value: List<String>) {
    Log.d(TAG, "ocr mode and type in handle ocr mode are $ocrMode, $ocrType")

    when (ocrMode) {
      "cloud" -> when (ocrType) {
        "shipping_label", "shipping-label" -> getPredictionShippingLabelCloud(bitmap, value)
        "item_label", "item-label" -> getPredictionItemLabelCloud(bitmap)
        "bill_of_lading", "bill-of-lading" -> getPredictionBillOfLadingCloud(bitmap, value)
        "document_classification", "document-classification" -> getPredictionDocumentClassificationCloud(bitmap)

        else -> Log.w(TAG, "Unsupported OCR type for cloud mode: $ocrType")
      }
      "on-device", "on_device" -> when(ocrType){
        "on-device-with-translation", "on_device_with_translation" -> getPredictionWithCloudTransformations(bitmap, value)
        else -> getPrediction(bitmap, value) // Handle all ocrTypes for on-device
      }

      else -> Log.w(TAG, "Unsupported OCR mode: $ocrMode")
    }
  }

  /**
   * Saves the provided bitmap to the device and sends an event with the saved image's URI and barcode data.
   * If the directory contains more than 10 images, it deletes the oldest image.
   * @param bitmap - Bitmap to be saved
   * @param scannedCodeResults - List of detected ScannedCodeResult objects
   * @param sharpnessScore - Image sharpness score
   */
  private fun saveBitmapAndSendEvent(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, sharpnessScore: Float): String {
    val parentDir = File(context?.filesDir, "VisionSdkSavedBitmaps")
    parentDir.mkdirs()

    val savedBitmapFile = File(parentDir, "${Date().time}.jpg")
    bitmap.save(
      fileToSaveBitmapTo = savedBitmapFile,
      compressFormat = Bitmap.CompressFormat.JPEG
    )

    // Delete the oldest image if there are more than 10 in the directory
    val filesList = parentDir.list() ?: emptyArray()
    if (filesList.size > 10) {
      filesList.sortBy { it }
      val fileToDelete = File(parentDir, filesList[0])
      fileToDelete.delete()
    }


    /**
     * Prepares and emits an event containing the captured image URI and barcode data to JavaScript.
     * @param savedBitmapFile - The file where the image is saved
     * @param scannedCodeResults - List of detected ScannedCodeResult objects
     * @param sharpnessScore - Image sharpness score
     */
    val event = Arguments.createMap().apply {
      putString("image", savedBitmapFile.toUri().toString())
      putDouble("sharpnessScore", sharpnessScore.toDouble())

      // Create a detailed barcodes array matching iOS format
      val codesArray = Arguments.createArray()
      scannedCodeResults.forEach { scannedCodeResult ->
        val codeMap = Arguments.createMap().apply {
          putString("scannedCode", scannedCodeResult.scannedCode)
          putString("symbology", scannedCodeResult.symbology.toString())

          // Add bounding box
          val boundingBoxMap = Arguments.createMap().apply {
            putInt("x", scannedCodeResult.boundingBox.left.toDp())
            putInt("y", scannedCodeResult.boundingBox.top.toDp())
            putInt("width", scannedCodeResult.boundingBox.width().toDp())
            putInt("height", scannedCodeResult.boundingBox.height().toDp())
          }
          putMap("boundingBox", boundingBoxMap)

          // Add gs1ExtractedInfo if available
          scannedCodeResult.gs1ExtractedInfo?.let { gs1Info ->
            val gs1Map = Arguments.createMap()
            for ((key, value) in gs1Info) {
              gs1Map.putString(key, value)
            }
            putMap("gs1ExtractedInfo", gs1Map)
          }
        }
        codesArray.pushMap(codeMap)
      }
      putArray("barcodes", codesArray)
    }
    // Emit the event to JavaScript listeners for onImageCaptured
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onImageCaptured", event)

    // Return the image path as a string
    return savedBitmapFile.absolutePath
  }

  /**
   * Sends a shipping label prediction request to the cloud API using an image bitmap and barcode list.
   * @param bitmap - The image to be sent for prediction
   * @param list - List of barcode data to include in the API request
   */
  private fun getPredictionShippingLabelCloud(
    bitmap: Bitmap,
    list: List<String>,
    token: String? = null,
    apiKey: String? = null,
    locationId: String? = null,
    options: Map<String, Any>? = null,
    metadata: Map<String, Any>? = null,
    recipient: Map<String, Any>? = null,
    sender: Map<String, Any>? = null,
    shouldResizeImage: Boolean? = null
  ) {
    val apiManager = ApiManager()
    val resolvedToken = token ?: this.token
    val resolvedApiKey = apiKey ?: this.apiKey

    val resolvedLocationId = locationId ?: this.locationId ?: ""
    val resolvedOptions = options ?: this.options ?: emptyMap()
    val resolvedMetadata = metadata ?: this.metaData ?: emptyMap()
    val resolvedRecipient = recipient ?: this.recipient ?: emptyMap()
    val resolvedSender = sender ?: this.sender ?: emptyMap()
    val resolvedShouldResizeImage = shouldResizeImage ?: this.shouldResizeImage

    Log.d(TAG, "get prediction shipping label cloud $resolvedToken, $resolvedApiKey, $resolvedLocationId")
    Log.d(TAG, "$resolvedOptions, $resolvedMetadata, $resolvedRecipient, $resolvedSender, $resolvedShouldResizeImage")

    apiManager.shippingLabelApiCallAsync(
      apiKey = resolvedApiKey,
      token = resolvedToken,
      bitmap = bitmap,
      barcodeList = list,
      locationId = resolvedLocationId,
      options = resolvedOptions,
      metadata = resolvedMetadata,
      recipient = resolvedRecipient,
      sender = resolvedSender,
      onScanResult = this,
      shouldResizeImage = resolvedShouldResizeImage
    )
  }

  /**
   * Sends a bill of lading prediction request to the cloud API using an image bitmap and barcode list.
   * @param bitmap - The image to be sent for prediction
   * @param list - List of barcode data to include in the API request
   */
  private fun getPredictionBillOfLadingCloud(
    bitmap: Bitmap,
    list: List<String>,
    token: String? = null,
    apiKey: String? = null,
    locationId: String? = null,
    options: Map<String, Any>? = null,
    shouldResizeImage: Boolean? = null
    ) {
    val apiManager = ApiManager()
    val resolvedToken = token ?: this.token
    val resolvedApiKey = apiKey ?: this.apiKey
    val resolvedLocationId = locationId ?: this.locationId ?: ""
    val resolvedOptions = options ?: this.options ?: emptyMap()
    val resolvedShouldResizeImage = shouldResizeImage ?: this.shouldResizeImage

    apiManager.billOfLadingApiCallAsync(
      apiKey = resolvedApiKey,
      token = resolvedToken,
      locationId =  resolvedLocationId?.takeIf { it.isNotEmpty() },
      options = resolvedOptions,
      bitmap = bitmap,
      barcodeList = list,
      onScanResult = this,
      shouldResizeImage = resolvedShouldResizeImage
    )
  }

   /**
   * Sends a item label prediction request to the cloud API using an image bitmap and barcode list.
   * @param bitmap - The image to be sent for prediction
   */
  private fun getPredictionItemLabelCloud(
     bitmap: Bitmap,
     token: String? = null,
     apiKey: String? = null,
     shouldResizeImage: Boolean? = null
     ) {
    val apiManager = ApiManager()
     val resolvedToken = token ?: this.token
     val resolvedApiKey = apiKey ?: this.apiKey
     val resolvedShouldResizeImage = shouldResizeImage ?: this.shouldResizeImage

    apiManager.itemLabelApiCallAsync(
      apiKey = resolvedApiKey,
      token = resolvedToken,
      bitmap = bitmap,
      shouldResizeImage = resolvedShouldResizeImage,
      onScanResult = this
    )
  }

   /**
   * Sends a item label prediction request to the cloud API using an image bitmap and barcode list.
   * @param bitmap - The image to be sent for prediction
   */
  private fun getPredictionDocumentClassificationCloud(
     bitmap: Bitmap,
     token: String? = null,
     apiKey: String? = null,
     shouldResizeImage: Boolean? = null
     ) {
    val apiManager = ApiManager()
     val resolvedToken = token ?: this.token
     val resolvedApiKey = apiKey ?: this.apiKey
     val resolvedShouldResizeImage = shouldResizeImage ?: this.shouldResizeImage

    apiManager.documentClassificationApiCallAsync(
      apiKey = resolvedApiKey,
      token = resolvedToken,
      bitmap = bitmap,
      onScanResult = this,
      shouldResizeImage = resolvedShouldResizeImage
    )
  }

  /**
   * Makes an on-device OCR prediction using the provided bitmap and barcode list.
   * Results are posted back to the main thread.
   * @param bitmap - The image for on-device OCR processing
   * @param list - List of barcodes for OCR prediction
   */
  private fun getPrediction(bitmap: Bitmap, list: List<String>) {
    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        val result = getOnDeviceOCRResponse(bitmap, list) ?: return@launchOnIO
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

  /**
   * Performs cloud-based OCR with on-device transformations and sends the results to the cloud API.
   * @param bitmap - The image to be processed
   * @param list - List of barcodes to include in the API request
   */
  private fun getPredictionWithCloudTransformations(
    bitmap: Bitmap,
    list: List<String>,
    token: String? = null,
    apiKey: String? = null,
    locationId: String? = null,
    options: Map<String, Any>? = null,
    metadata: Map<String, Any>? = null,
    recipient: Map<String, Any>? = null,
    sender: Map<String, Any>? = null,
    shouldResizeImage: Boolean? = null
  ) {

    val resolvedToken = token ?: this.token
    val resolvedApiKey = apiKey ?: this.apiKey

    val resolvedLocationId = locationId ?: this.locationId ?: ""
    val resolvedOptions = options ?: this.options ?: emptyMap()
    val resolvedMetadata = metadata ?: this.metaData ?: emptyMap()
    val resolvedRecipient = recipient ?: this.recipient ?: emptyMap()
    val resolvedSender = sender ?: this.sender ?: emptyMap()
    val resolvedShouldResizeImage = shouldResizeImage ?: this.shouldResizeImage

    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        val onDeviceResponse = getOnDeviceOCRResponse(bitmap, list) ?: return@launchOnIO
        val result = ApiManager().shippingLabelMatchingApiSync(
          apiKey = resolvedApiKey,
          token = resolvedToken,
          bitmap = bitmap,
          shouldResizeImage = resolvedShouldResizeImage,
          barcodeList = list,
          onDeviceResponse = onDeviceResponse,
          locationId = resolvedLocationId?.takeIf { it.isNotEmpty() },
          recipient = resolvedRecipient,
          sender = resolvedSender,
          options = resolvedOptions,
          metadata = resolvedMetadata
        )
        withContextMain {
          onOCRResponse(result)
        }
      } catch (e: VisionSDKException) {
        Log.e(TAG, "X Error in matching api: ", e)
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e)
        }
      } catch (e: Exception) {
        Log.e(TAG, "X Error in matching api: ", e)
        e.printStackTrace()
        if (e is CancellationException) throw e
        withContextMain {
          onOCRResponseFailed(VisionSDKException.UnknownException(e))
        }
      }
    }
  }

  /**
   * Performs on-device OCR to extract predictions from a bitmap and barcode list.
   * @param bitmap - The image to be processed by on-device OCR
   * @param list - List of barcodes to assist OCR processing
   * @return A String representing the OCR result or null if failed
   */
  private suspend fun getOnDeviceOCRResponse(bitmap: Bitmap, list: List<String>): String? {

    return try {
      // Use withContext(Dispatchers.IO) to execute in the IO thread
      val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context!!, modelType)
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


  // The receiveCommand method handles different commands from React Native to native code.
  override fun receiveCommand(
    view: VisionCameraView,
    commandType: Int,
    args: ReadableArray?
  ) {
//    Log.d(TAG, "receiveCommand: $commandType")
//    Log.d(TAG, "🔄 receiveCommand called with type: $commandType and args: $args")

    Assertions.assertNotNull(view)
    Assertions.assertNotNull(args)
    // Handle each command type based on the provided `commandType`.
    when (commandType) {
      0 -> captureImage()  // Command to capture an image.
      1 -> stopScanning()  // Command to stop scanning.
      2 -> startCamera()   // Command to start the camera.
      3 -> setMetaData(args?.getMap(0))  // Command to set metadata.
      4 -> setRecipient(args?.getMap(0)) // Command to set recipient information.
      5 -> setSender(args?.getMap(0))    // Command to set sender information.
      6 -> configureOnDeviceModel(args) // Command to configure on-device model.
      7 -> restartScanning()  // Command to restart scanning.
      8 -> configureFocusSettings(args?.getMap(0).toString())  // Command to configure camera focus.
      9 -> setObjectDetectionSettings(args?.getMap(0).toString())  // Command to configure object detection.
      10 -> setCameraSettings(args?.getMap(0).toString())  // Command to configure camera settings.
      11 -> handleDevicePrediction(args)  // Command to handle image prediction.
      12 -> handleDevicePredictionWithCloudTransformations(args)  // Command to handle cloud-based prediction.
      13 -> handleCloudPrediction(args) // Command for shipping label prediction.
      14 -> handlePredictionBillOfLadingCloud(args)  // Command for bill of lading prediction.
      15 -> handlePredictionItemLabelCloud(args)  // Command for item label prediction.
      16 -> handlePredictionDocumentClassificationCloud(args)  // Command for document classification prediction.
      17 -> reportError(args)  // Command for Reports errors for on-device.
      18 -> createTemplate()  // Command for creates a new template.
      19 -> getAllTemplates()  // Command for get all saved templates.
      20 -> deleteTemplateWithId(args)  // Command for delete a specific template by its ID.
      21 -> deleteAllTemplates()  // Command for delete all templates from storage.
      else -> throw IllegalArgumentException("Unsupported command $commandType received.")  // Invalid command.
    }
  }

  // Capture image from the camera
  private fun captureImage() {
    Log.d(TAG, "captureImage: ")
    visionCameraView?.capture()
  }

  // stopScanning halts the camera and scanning process, freeing up camera resources.
  private fun stopScanning() {
    Log.d(TAG, "stopScanning: ")
//        if (visionCameraView?.isCameraStarted()?.not() == true) return
    visionCameraView?.stopCamera()
  }

  // startCamera triggers the camera and scanning process within VisionSdkView.
  private fun startCamera() {
    Log.d(TAG, "startCamera: ")
    if (visionCameraView?.isCameraStarted() == true)
      visionCameraView?.stopCamera()
    visionCameraView?.startCamera()

  }

  // Set metadata for scanning
  private fun setMetaData(metaData: ReadableMap?) {
    Log.d(TAG, "metaData: $metaData")
    this.metaData = metaData?.toHashMap() as Map<String, Any>? // do not remove this typecase, required for latest AGP
  }

  // Set recipient information for scanning
  private fun setRecipient(recipient: ReadableMap?) {
    Log.d(TAG, "recipient: $recipient")
    if (recipient == null) {
      this.recipient = emptyMap()
    } else {
      this.recipient = recipient.toHashMap() as Map<String, Any>? // do not remove this typecast, required for latest AGP
    }
  }

  // Set sender information for scanning
  private fun setSender(sender: ReadableMap?) {
    Log.d(TAG, "sender: $sender")
    if (sender == null) {
      this.sender = emptyMap()
    } else {
      this.sender = sender.toHashMap() as Map<String, Any>? //do not remove this typecast, required for latest AGP
    }
  }

  // setModelSize allows configuration of the OCR model size based on a string value passed from React Native.
  private fun setModelSize(modelSize: String?) {
    Log.d(TAG, "modelSize: " + modelSize)
    this.modelSize = when (modelSize?.lowercase()) {
      "nano" -> ModelSize.Nano
      "micro" -> ModelSize.Micro
      "small" -> ModelSize.Small
      "medium" -> ModelSize.Medium
      "large" -> ModelSize.Large
      "xlarge" -> ModelSize.XLarge
      else -> null
    }
  }

  fun getModelSize(modelSize: String?): ModelSize {
    Log.d(TAG, "modelSize: $modelSize")
    return when (modelSize?.lowercase()) {
        "nano" -> ModelSize.Nano
        "micro" -> ModelSize.Micro
        "small" -> ModelSize.Small
        "medium" -> ModelSize.Medium
        "large" -> ModelSize.Large
        "xlarge" -> ModelSize.XLarge
        else -> ModelSize.Large
    }
}

  // setModelType allows configuration of the OCR model type based on a string value passed from React Native.\
  fun setModelType(modelType: String?) {
    Log.d(TAG, "modelType: " + modelType)
    this.modelType = when (modelType?.lowercase()) {
      "shipping_label", "shipping-label" -> OCRModule.ShippingLabel(modelSize)
      "bill_of_lading", "bill-of-lading" -> OCRModule.BillOfLading(modelSize)
      "item_label", "item-label"   -> OCRModule.ItemLabel(modelSize)
      "document_classification", "document-classification" -> OCRModule.DocumentClassification(modelSize)
      else -> OCRModule.ShippingLabel(modelSize)
    }

  }

  // Configure the on-device model for OCR (Optical Character Recognition)
  private fun configureOnDeviceModel(args: ReadableArray?) {

   val onDeviceConfigs =  args?.getMap(0).toString()
    Log.d(TAG, "configureOnDeviceModel: $onDeviceConfigs")
    val argToken = args?.getString(1)  // Retrieve token from the third element of the array
    val argApiKey = args?.getString(2) // Retrieve API key from the fourth element

    val resolvedToken = argToken ?: this.token
    val resolvedApiKey = argApiKey ?: this.apiKey


    // If configurations are provided, parse them and set up the model size and type
    if (onDeviceConfigs != "null")
      onDeviceConfigs.ifNeitherNullNorEmptyNorBlank { onDeviceConfigs ->
        JSONObject(onDeviceConfigs).apply {
          setModelSize(optString("size", ""))
          setModelType(optString("type", "shipping_label"))
        }
      }

    val onDeviceOCRManager = OnDeviceOCRManagerSingleton.getInstance(context!!, modelType)

    val modelExecutionProvider = this.executionProvider
    Log.d(TAG, "model execution provider: ${modelExecutionProvider}")
    // Configure the OCR manager asynchronously, with download progress tracking
    lifecycleOwner?.lifecycle?.coroutineScope?.launchOnIO {
      try {
        if (!OnDeviceOCRManagerSingleton.isModelConfigured(modelType)) {
          var lastProgress = 0.00
          var isModelAlreadyDownloaded = onDeviceOCRManager.isModelAlreadyDownloaded()
          onDeviceOCRManager?.configure(resolvedApiKey, resolvedToken, modelExecutionProvider) {
            val progressInt = (it).toDecimalPoints(2).toDouble()
            if (progressInt != lastProgress) {
              lastProgress = progressInt
              Log.d(TAG, "Install Progress: ${(it * 100).toInt()}")

              EventUtils.sendModelDownloadProgressEvent(appContext, progress = lastProgress, downloadStatus = isModelAlreadyDownloaded, isReady = false)
            }
          }
        }
        // Emit success event when the model is downloaded
        EventUtils.sendModelDownloadProgressEvent(appContext, progress = 1.0, downloadStatus = true, isReady = true)

      } catch (e: VisionSDKException) {
        e.printStackTrace()
        withContextMain {
          onOCRResponseFailed(e) // Handle OCR configuration failure
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

  // Restart scanning after stopping
  private fun restartScanning() {
    Log.d(TAG, "restartScanning: ")
    visionCameraView?.rescan()
  }


  // This method handles shipping label prediction.
  private fun handleDevicePrediction(args: ReadableArray?) {
    val image = args?.getString(0)
    this.imagePath = image
    val barcodeArray = args?.getArray(1)
    val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
    uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
      bitmap?.let {
        getPrediction(it, barcodeList)
      }
    }
  }

  // This method handles Device based predictions.
  private fun handleDevicePredictionWithCloudTransformations(args: ReadableArray?) {
    val image = args?.getString(0)
    this.imagePath = image
    val barcodeArray = args?.getArray(1)
    val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
    val token = args?.getString(2)  // Retrieve token from the third element of the array
    val apiKey = args?.getString(3) // Retrieve API key from the fourth element
    val locationId = args?.getString(4) // Retrieve location ID from the fifth element
    val options = args?.getMap(5)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
    val metadata = args?.getMap(6)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
    val recipient = args?.getMap(7)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
    val sender = args?.getMap(8)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
    val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
      args.getBoolean(9)
    } else {
      true // Default value if nullÏ
    }

    uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
      bitmap?.let {
        getPredictionWithCloudTransformations(
          it,
          barcodeList,
          token,
          apiKey,
          locationId,
          options,
          metadata,
          recipient,
          sender,
          shouldResizeImage
          )  // Perform prediction on the image.
      }
     }
  }

  // This method handles cloud-based shipping label prediction.
  private fun handleCloudPrediction(args: ReadableArray?) {

    try {
      val image = args?.getString(0)
      this.imagePath = image
      val barcodeArray = args?.getArray(1)
      val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
      val token = args?.getString(2)  // Retrieve token from the third element of the array
      val apiKey = args?.getString(3) // Retrieve API key from the fourth element
      val locationId = args?.getString(4) // Retrieve location ID from the fifth element
      // Extract Maps safely
      val options = args?.getMap(5)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
      val metadata = args?.getMap(6)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
      val recipient = args?.getMap(7)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()
      val sender = args?.getMap(8)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()

      val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
        args.getBoolean(9)
      } else {
        true // Default value if null
      }



      uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
        bitmap?.let {
          getPredictionShippingLabelCloud(
            it,
            barcodeList,
            token,
            apiKey,
            locationId,
            options,
            metadata,
            recipient,
            sender,
            shouldResizeImage
          )  // Process shipping label prediction.
        }
      }
    } catch (e: Exception){
      Log.e(TAG, "❌ Exception in handleCloudPrediction: ${e.message}", e)
    }
  }





  // This method handles cloud-based bill of lading prediction.
  private fun handlePredictionBillOfLadingCloud(args: ReadableArray?) {
    try {
      val image = args?.getString(0)
      this.imagePath = image
      val barcodeArray = args?.getArray(1)
      val barcodeList = barcodeArray?.toArrayList()?.map { it.toString() } ?: emptyList()
      val token = args?.getString(2)  // Retrieve token from the third element of the array
      val apiKey = args?.getString(3) // Retrieve API key from the fourth element
      val locationId = args?.getString(4) // Retrieve location ID from the fifth element
      val options = args?.getMap(5)?.toHashMap()?.mapValues { it.value ?: "" } ?: emptyMap()


      val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
        args.getBoolean(6)
      } else {
        true // Default value if null
      }

      uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
        bitmap?.let {
          getPredictionBillOfLadingCloud(
            it,
            barcodeList,
            token,
            apiKey,
            locationId,
            options,
            shouldResizeImage
          )  // Process bill of lading prediction.
        }
      }
    } catch(e: Exception){
      Log.e(TAG, "❌ Exception in handlePredictionBillOfLadingCloud: ${e.message}", e)
    }
  }

  // This method handles cloud-based item label prediction.
  private fun handlePredictionItemLabelCloud(args: ReadableArray?) {
    try {
      val image = args?.getString(0)
      this.imagePath = image
      val token = args?.getString(1)  // Retrieve token from the third element of the array
      val apiKey = args?.getString(2) // Retrieve API key from the fourth element
      val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
        args.getBoolean(3)
      } else {
        true // Default value if null
      }
      uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
        bitmap?.let {
          getPredictionItemLabelCloud(
            it,
            token,
            apiKey,
            shouldResizeImage
          )  // Process item label prediction.
        }
      }
    } catch(e: Exception){
      Log.e(TAG, "❌ Exception in handlePredictionItemLabelCloud: ${e.message}", e)
    }

  }

  // This method handles cloud-based document classification prediction.
  private fun handlePredictionDocumentClassificationCloud(args: ReadableArray?) {

    try  {
      val image = args?.getString(0)
      this.imagePath = image
      val token = args?.getString(1)  // Retrieve token from the third element of the array
      val apiKey = args?.getString(2) // Retrieve API key from the fourth element
      val shouldResizeImage = if (args != null && args.size() > 9 && !args.isNull(9)) {
        args.getBoolean(3)
      } else {
        true // Default value if null
      }

      uriToBitmap(context!!, Uri.parse(image)) { bitmap ->
        bitmap?.let {
          getPredictionDocumentClassificationCloud(
            it,
            token,
            apiKey,
            shouldResizeImage
          )  // Process document classification prediction.
        }
      }
    } catch (e: Exception){
      Log.e(TAG, "❌ Exception in handlePredictionDocumentClassificationCloud: ${e.message}", e)
    }

  }

  // This method is used to create a new template for use in cloud predictions.

  private fun createTemplate() {
    stopScanning()
    val activity = appContext.currentActivity as? FragmentActivity ?: return

    val onTemplateCreated: (String) -> Unit = { newCreatedTemplateId: String ->

      val event = Arguments.createMap().apply {
        putString("data", newCreatedTemplateId)
      }
      // Emit the event to JavaScript listeners for onCreateTemplate
      appContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onCreateTemplate", event)

      startCamera()
    }



    startCreateTemplateScreen(
      fragmentManager = activity.supportFragmentManager,
      onTemplateCreated = onTemplateCreated,
      onCancelled = {
        startCamera()
      }
    )
  }
  // This method is used to get all saved templates.
  private fun getAllTemplates() {
    val templateManager = TemplateManager()
    // build a WritableArray of strings


    val templates =  templateManager.getAllBarcodeTemplates()

    val arr = Arguments.createArray().apply {
      templates.forEach { pushString(it.name) }
    }

    val event = Arguments.createMap().apply {
      putArray("data", arr)
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onGetTemplates", event)


  }

  // This method is used to delete a specific template by its ID.
  private fun deleteTemplateWithId(args: ReadableArray?) {
    val templateManager = TemplateManager()
    val id = args?.getString(0)
    val allTemplates = templateManager.getAllBarcodeTemplates()
    val templateToDelete = allTemplates.firstOrNull { template -> template.name == id }
    if(templateToDelete != null){
      templateManager.deleteBarcodeTemplate(templateToDelete)
      val event = Arguments.createMap().apply {
        putString("data", id)
      }
      appContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onDeleteTemplateById", event)
    }
  }


   // This method is used to delete all templates from storage.
  private fun deleteAllTemplates() {
    val templateManager = TemplateManager()
     val allTemplates = templateManager.getAllBarcodeTemplates()
     allTemplates.forEach{
       template -> templateManager.deleteBarcodeTemplate(template)
     }

     val event = Arguments.createMap().apply {
       putBoolean("success", true)
     }
     appContext
       .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
       .emit("onDeleteTemplates", event)


  }

  //  React Props
  // React Native property to enable or disable the flash on the camera
  @ReactProp(name = "flash")
  fun flash(view: View, flash: Boolean = false) {
    Log.d(TAG, "flash: $flash")
    this.flash = flash
    this.visionCameraView?.setFlashTurnedOn(flash)
  }

  @ReactProp(name = "modelExecutionProviderAndroid")
  fun setModelExecutionProviderAndroid(view: View, modelExecutionProviderAndroid: String = "NNAPI"){
    Log.d(TAG, "SETTING EXECTION PROVIFER TO: $modelExecutionProviderAndroid")
    if(modelExecutionProviderAndroid == "CPU"){
      this.executionProvider = ExecutionProvider.CPU
    } else if (modelExecutionProviderAndroid == "NNAPI") {
      this.executionProvider = ExecutionProvider.NNAPI
    } else if (modelExecutionProviderAndroid == "XNNPACK"){
      this.executionProvider = ExecutionProvider.XNNPACK
    } else {
      this.executionProvider = ExecutionProvider.NNAPI
    }

    Log.d(TAG, "SET EXECUTION PROVIDER TO : ${executionProvider}")
  }

  @ReactProp(name = "shouldResizeImage")
  fun setShouldResizeImage(view: View, shouldResizeImage: Boolean = true){
    Log.d(TAG, "should resize image: $shouldResizeImage")
    this.shouldResizeImage = shouldResizeImage
  }

  // React Native property to enable or disable the isMultipleScanEnabled
  @ReactProp(name = "isMultipleScanEnabled")
  fun seIsMultipleScanEnabled(view: View, isMultipleScanEnabled: Boolean = false) {
    Log.d(TAG, "isMultipleScanEnabled: $isMultipleScanEnabled")
    this.isMultipleScanEnabled = isMultipleScanEnabled
    visionCameraView?.setMultipleScanEnabled(isMultipleScanEnabled)
  }

  // React Native property to enable or disable the isEnableAutoOcrResponseWithImage
  @ReactProp(name = "isEnableAutoOcrResponseWithImage")
  fun seIsEnableAutoOcrResponseWithImage(view: View, isEnableAutoOcrResponseWithImage: Boolean = false) {
    Log.d(TAG, "isEnableAutoOcrResponseWithImage: $isEnableAutoOcrResponseWithImage")
    this.isEnableAutoOcrResponseWithImage = isEnableAutoOcrResponseWithImage
  }


  // React Native property to set the zoom level on the camera
  @ReactProp(name = "zoomLevel")
  fun setZoomLevel(view: VisionCameraView, zoomLevel: Float) {
    Log.d(TAG, "setZoomLevel: $zoomLevel")
    visionCameraView?.setZoomRatio(zoomLevel)
  }

  // React Native property to set the API key, used for authenticating API requests if necessary
  @ReactProp(name = "apiKey")
  fun setApiKey(view: View, apiKey: String = "") {
    Log.d(TAG, "apiKey: $apiKey")
    this.apiKey = apiKey
  }

  // React Native property to set the token, another method of authenticating requests if required
  @ReactProp(name = "token")
  fun setToken(view: View, token: String = "") {
    Log.d(TAG, "token: $token")
    this.token = token
  }

  // React Native property to set the environment type, e.g., SANDBOX or DEV
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

  // React Native property to set the capture mode on the camera, either automatic or manual
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

  // React Native property to define the detection mode, e.g., OCR, barcode, QR code, etc.
  @ReactProp(name = "mode")
  fun setMode(view: View, mode: String = "") {
    Log.d(TAG, "mode: $mode")
    if(mode.lowercase() != "pricetag") {
      detectionMode = when (mode.lowercase()) {
        "ocr" -> DetectionMode.OCR
        "barcode" -> DetectionMode.Barcode
        "qrcode" -> DetectionMode.QRCode
        "photo" -> DetectionMode.Photo
        "barcodeorqrcode" -> DetectionMode.BarcodeOrQRCode
        else -> DetectionMode.Barcode
      }

      visionCameraView?.setDetectionMode(detectionMode)
    } else {
      //mode = pricetag
//      visionCameraView?.enablePriceTagMode(apiKey, token)

      coroutineScope.launchOnDefault {
        try {
          visionCameraView?.enablePriceTagMode(apiKey, token)
        } catch (e: Exception) {
          if (e is CancellationException) throw e

          Log.d(TAG, "$e.message")

          val event = Arguments.createMap().apply {
            putString("message", e.message)
          }
          Log.d(TAG, "${event}")
          appContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onError", event)

        }
      }
    }
  }

  // React Native property to define the OCR mode, e.g., cloud or on-device
  @ReactProp(name = "ocrMode")
  fun ocrMode(view: View, ocrMode: String = "cloud") {
    Log.d(TAG, "ocrMode: $ocrMode")
    this.ocrMode = ocrMode
  }

  @ReactProp(name = "ocrType")
  fun ocrType(view: View, ocrType: String = "shipping_label"){
    Log.d(TAG,  "ocrType: $ocrType")
    this.ocrType = ocrType
    setModelType(ocrType)
  }

  // React Native property to set the location ID, used for location-specific functionalities
  @ReactProp(name = "locationId")
  fun setLocationId(view: View, locationId: String = "") {
    Log.d(TAG, "locationId: $locationId")
    this.locationId = locationId
  }

  // React Native property to set additional options via a ReadableMap, allowing custom configurations
  @ReactProp(name = "options")
  fun setOptions(view: View, options: ReadableMap) {
    Log.d(TAG, "options: $options")
    this.options = options.toHashMap() as Map<String, Any>? // do not remove this hashMap, required for latest AGP
  }

  // Called when the camera starts, logs the max zoom level and sets focus settings
  override fun onCameraStarted() {
    Log.d(TAG, "onCameraStarted: ")
    Log.d(TAG, "maxZoomLevel: ${visionCameraView?.getMaxZoomRatioAvailable()}")
    setFocusSettings()
  }

  // Called when the camera stops, logs the event
  override fun onCameraStopped() {
    Log.d(TAG, "onCameraStopped: ")
  }

  // Processes the OCR response, formats it, and sends it as a JavaScript event to React Native
  override fun onOCRResponse(response: String) {
    Log.d(TAG, "api responded with  ${response}")
    val event = Arguments.createMap().apply {
      putString("data", JSONObject(response).toString())
      putString("imagePath", imagePath)
    }
    appContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit("onOCRScan", event)
  }

  // Handles OCR response failure, formats error details, and sends them as a JavaScript event
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

  // Converts a hex color in RRGGBBAA format to an integer value in AARRGGBB format
  private fun hexColorToInt(color: String): Int {
    return Color.parseColor(convertRRGGBBAAToAARRGGBB(color))
  }

  // Converts an integer color value back to a hex string in AARRGGBB format
  private fun intColorToHex(color: Int): String {
    return String.format("#%06X", 0xFFFFFF and color)
  }

  // Converts a color string in RRGGBBAA format to AARRGGBB format for consistent parsing
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

  // Decodes a base64 string to a Bitmap
  private fun convertBase64ToBitmap(b64: String): Bitmap {
    val imageAsBytes: ByteArray = Base64.decode(b64.toByteArray(), Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.size)
  }

  // Converts a drawable resource to a base64-encoded string
  private fun convertDrawableToBase64(drawable: Int): String {
    val bitmap = BitmapFactory.decodeResource(context?.getResources(), drawable)
    val byteStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream)
    val byteArray = byteStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
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
