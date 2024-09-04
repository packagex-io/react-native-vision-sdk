package io.packagex.visionsdk.ui.views

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import com.asadullah.handyutils.boundingBox
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.toDp
import com.asadullah.handyutils.toNullIfEmptyOrBlank
import com.google.mlkit.vision.text.Text
import com.scottyab.rootbeer.RootBeer
import io.packagex.visionsdk.analyzers.BarcodeResult
import io.packagex.visionsdk.analyzers.FullScreenImageAnalyzer
import io.packagex.visionsdk.analyzers.ImageScanner
import io.packagex.visionsdk.config.CameraSettings
import io.packagex.visionsdk.config.FocusSettings
import io.packagex.visionsdk.config.ObjectDetectionConfiguration
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.CameraXBarcodeCallback
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.preferences.VisionSDKSettings
import io.packagex.visionsdk.preferences.dto.BarcodeTemplate
import io.packagex.visionsdk.utils.BitmapUtils.bitmapCompression
import io.packagex.visionsdk.utils.LinearConversion
import io.packagex.visionsdk.utils.TAG
import io.packagex.visionsdk.utils.isEmulator
import io.packagex.visionsdk.utils.isOneDimensional
import io.packagex.visionsdk.utils.isQRCode
import java.io.File
import java.util.Timer
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class VisionCameraView(
  context: Context, attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

  private val imagesCacheDirectory = File(context.filesDir, "VisionCamera/Images")

  // Views to be added programmatically
  private val previewView = PreviewView(context).apply {
    addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(view: View) {
        startCameraOnAttach()
      }

      override fun onViewDetachedFromWindow(view: View) {}
    })
  }

  private val barcodeAndQRCodeBoundingBoxesMap = mutableMapOf<String, View>()
  private var documentBoundsView: View? = null

  private var cameraProvider: ProcessCameraProvider? = null
  private var cameraExecutor: ExecutorService? = null

  private var scannerCallback: ScannerCallback? = null
  private var cameraLifecycleCallback: CameraLifecycleCallback? = null

  private var autoCaptureDocumentTimer: Timer? = null

  // This variable is to prevent the analyzer code to execute anymore, once stop scanning is called.
  private var isScanning = false

  private var camera: Camera? = null

  private var imageViewForCapturedImage: ImageView? = null

  private var linearZoom = Float.MIN_VALUE
  private var zoomRatio = Float.MIN_VALUE

  private var textDetected = false
  private var barcodeDetected = false
  private var qrCodeDetected = false
  private var documentDetected = false

  private var isCaptureButtonClicked = false
    set(value) {
      field = value
      imageScanner.setButtonPressed(value)
    }

  private var appliedBarcodeTemplate: BarcodeTemplate? = null

  private var imageCompressionFactorInPercentage = 100

  private val barcodesMapForRegex = mutableMapOf<String, BarcodeResult>()

  private val imageScanner by lazy {
    ImageScanner(
      allInitialBarcodes = { initialBarcodes ->
        getBarcodesWithinFocusFrame(initialBarcodes)
      },
      onScanResult = cameraXBarcodeCallback
    )
  }

  private val imageAnalyzer by lazy {
    FullScreenImageAnalyzer(imageScanner)
  }

  private var imageAnalysisUseCase: ImageAnalysis? = null
  private var imageCaptureUseCase: ImageCapture? = null

  /** CAMERA STATES */
  private var isFlashTurnedOn = false
  private lateinit var detectionMode: DetectionMode
  private lateinit var scanningMode: ScanningMode
  private var isMultipleScanEnabled = false

  /** CAMERA SETTINGS */
  private var cameraSettings = CameraSettings()

  /** OBJECT DETECTION CONFIGURATIONS */
  private var objectDetectionConfiguration = ObjectDetectionConfiguration()

  /** FOCUS SETTINGS */
  private var focusSettings = FocusSettings(context)
  private var focusRegionManager: FocusRegionManager? = null
  private var focusImageView: ImageView? = null
  internal fun setFocusSettings(focusSettings: FocusSettings) {
    this.focusSettings = focusSettings
    applyFocusSettings()
  }
  fun getFocusRegionManager(): FocusRegionManager {
    if (focusRegionManager == null) throw VisionSDKException.FocusRegionManagerNotAvailable
    return focusRegionManager!!
  }

  fun configure(detectionMode: DetectionMode, scanningMode: ScanningMode, isMultipleScanEnabled: Boolean) {
    this.detectionMode = detectionMode
    this.scanningMode = scanningMode
    this.isMultipleScanEnabled = isMultipleScanEnabled
  }

  fun setDetectionMode(detectionMode: DetectionMode) {
    this.detectionMode = detectionMode
    updateUIWithCurrentViewState()
  }

  fun setScanningMode(scanningMode: ScanningMode) {
    this.scanningMode = scanningMode
    updateUIWithCurrentViewState()
  }

  fun setMultipleScanEnabled(enabled: Boolean) {
    this.isMultipleScanEnabled = enabled
    updateUIWithCurrentViewState()
  }

  fun setFlashTurnedOn(enabled: Boolean) {
    this.isFlashTurnedOn = enabled
    updateUIWithCurrentViewState()
  }

  fun setCameraSettings(cameraSettings: CameraSettings) {
    this.cameraSettings = cameraSettings
    applyCameraSettings()
  }

  fun setObjectDetectionConfiguration(objectDetectionConfiguration: ObjectDetectionConfiguration) {
    this.objectDetectionConfiguration = objectDetectionConfiguration
    updateUIWithCurrentViewState()
  }

  /**
   * The following function will delete all the images that are saved when
   * `CameraSettings.shouldAutoSaveCapturedImage` is set to true and a picture
   * is captured. Use this function frequently if you want to keep
   * `CameraSettings.shouldAutoSaveCapturedImage` to true.
   */
  fun clearImagesFromCache() {
    imagesCacheDirectory.listFiles()?.forEach { it.delete() }
  }

  /**
   * The following variable and overridden function are added to update the children
   * views in ReactNative.
   */
  private val measureAndLayout = Runnable {
    measure(
      MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(left, top, right, bottom)
  }

  override fun requestLayout() {
    super.requestLayout()
    post(measureAndLayout)
  }

  /**
   * This function will compress the captured bitmaps before sending those back to client
   * application.
   * @param compressPercentage: Input value can range from 10 - 100. 100 means no compression
   * at all. Less than 10 will be converted to 10.
   */
  fun enableImageCompression(compressPercentage: Int) {
    this.imageCompressionFactorInPercentage = compressPercentage
  }

  /** TEMPLATES */

  fun getAllBarcodeTemplates(): List<BarcodeTemplate> {
    return VisionSDKSettings.getAllBarcodeTemplates()
  }

  fun applyBarcodeTemplate(barcodeTemplate: BarcodeTemplate) {
    this.appliedBarcodeTemplate = barcodeTemplate
  }

  fun getAppliedBarcodeTemplate(): BarcodeTemplate? {
    return this.appliedBarcodeTemplate
  }

  fun removeBarcodeTemplate() {
    this.appliedBarcodeTemplate = null
  }

  fun setScannerCallback(scannerCallback: ScannerCallback) {
    this.scannerCallback = scannerCallback
  }

  fun setCameraLifecycleCallback(callback: CameraLifecycleCallback) {
    cameraLifecycleCallback = callback
  }

  fun startCamera() {
    removeAllViewsFun()
    addViewFun(
      previewView,
      LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    )
    updateUIWithCurrentViewState()
  }

  // AI: This function will be used to make sure that if camera is started,
  // further calls to startCamera() are ignored.
  fun isCameraStarted() = camera != null

  private fun startCameraOnAttach() {

    if (isCameraStarted()) {
      return
    }

    val rootBeer = RootBeer(context)
    if (rootBeer.isRooted) {
      throwFailure(VisionSDKException.RootDeviceDetected)
      return
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        try {
          cameraProvider = cameraProviderFuture.get()
          startScanning()
        } catch (e: ExecutionException) {
          scannerCallback?.onFailure(
            VisionSDKException.UnknownException(e)
          )
        } catch (e: InterruptedException) {
          scannerCallback?.onFailure(
            VisionSDKException.UnknownException(e)
          )
        } catch (e: Exception) {
          scannerCallback?.onFailure(
            VisionSDKException.UnknownException(e)
          )
        }
      },
      ContextCompat.getMainExecutor(context)
    )
  }

  fun stopCamera() {

    if (isCameraStarted().not()) return

    cameraProvider?.unbindAll()
    cameraExecutor?.shutdown()
    removeAllViewsFun()
    camera = null
    focusRegionManager = null
    cameraLifecycleCallback?.onCameraStopped()
  }

  fun rescan() {
    cameraProvider?.unbindAll()
    cameraExecutor?.shutdown()
    imageViewForCapturedImage?.let { removeViewFun(it) }
    try {
      startScanning()
    } catch (e: Exception) {
      scannerCallback?.onFailure(
        VisionSDKException.UnknownException(e)
      )
    }
  }

  private fun startScanning() {

    if (cameraProvider == null) {
      throwFailure(VisionSDKException.CallStartCameraOrRescanBeforeCapture)
      return
    }

    cameraExecutor = Executors.newSingleThreadExecutor()

    val cameraSelector = CameraSelector
      .Builder()
      .requireLensFacing(CameraSelector.LENS_FACING_BACK)
      .build()

    val previewUseCase = Preview
      .Builder()
      .build()
      .apply { setSurfaceProvider(previewView.surfaceProvider) }

    imageScanner.rescan()

    imageAnalysisUseCase = ImageAnalysis
      .Builder()
      .setTargetResolution(Size(width, height))
      .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
      .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
      .build()
      .apply {
        if (isEmulator().not()) {
          setAnalyzer(cameraExecutor!!, imageAnalyzer)
        }
      }

    imageCaptureUseCase = ImageCapture
      .Builder()
      .build()

    val useCaseGroup = UseCaseGroup.Builder()
      .addUseCase(previewUseCase)
      .addUseCase(imageAnalysisUseCase!!)
      .addUseCase(imageCaptureUseCase!!)
      .setViewPort(previewView.viewPort!!)
      .build()

    val camera = cameraProvider?.bindToLifecycle(context as LifecycleOwner, cameraSelector, useCaseGroup)

    isScanning = true

    this.camera = camera

    this.focusRegionManager = FocusRegionManager()
    this.focusRegionManager?.setFocusSettings(focusSettings)

    if (linearZoom != Float.MIN_VALUE) {
      setLinearZoom(linearZoom)
    }

    if (zoomRatio != Float.MIN_VALUE) {
      setZoomRatio(zoomRatio)
    }

    cameraLifecycleCallback?.onCameraStarted()
  }

  private fun stopScanning() {
    cameraProvider?.unbind(imageAnalysisUseCase)
    isScanning = false

    // For some stupid Android reason, onTextDetected was called even after
    // imageAnalysisUseCase was unbound, which was causing to add bounding
    // box views after processing was stopped. To prevent that from happening,
    // we're adding the following delay. We will remove these bounding box views
    // after that initial delay, giving time for detectors to completely
    // shutdown. Then we will remove the bounding box views.
    Handler(Looper.getMainLooper()).postDelayed(
      {
        // Remove all bounding boxes
        barcodeAndQRCodeBoundingBoxesMap.forEach { (_, boundingBoxView) ->
          removeViewFun(boundingBoxView)
        }
        barcodeAndQRCodeBoundingBoxesMap.clear()

        // Remove document bounding box
        removeViewFun(documentBoundsView)
        documentBoundsView = null
      },
      1500
    )
  }

  private fun updateUIWithCurrentViewState() {

    // SETTING FLASH STATUS
    if (isFlashTurnedOn) {
      enableTorch()
    } else {
      disableTorch()
    }

    // SETTING TEXT DETECTOR
    if (detectionMode == DetectionMode.OCR) {
      imageScanner.enableTextDetection(true)
    } else {
      imageScanner.enableTextDetection(objectDetectionConfiguration.isTextIndicationOn)
    }

    // SETTING BARCODE/QRCODE DETECTOR
    imageScanner.enableBarcodeDetection(objectDetectionConfiguration.isBarcodeOrQRCodeIndicationOn)

    // SETTING SCANNING MODE
    imageScanner.scanningModeManual(scanningMode == ScanningMode.Manual)

    // SETTING MULTIPLE SCAN MODE
    imageScanner.multipleScanEnabled(isMultipleScanEnabled)

    applyFocusSettings()
  }

  private fun applyCameraSettings() {

    if (cameraSettings.nthFrameToProcess <= 0) {
      imageAnalyzer.analyzeEveryFrame()
    } else {
      imageAnalyzer.nthFrame = cameraSettings.nthFrameToProcess
    }
  }

  private fun applyFocusSettings() {

    val rectF = if (focusSettings.focusImageRect.isEmpty) {

      if (this@VisionCameraView.width <= 0 || this@VisionCameraView.height <= 0) return

      val (widthOfFocusImage, heightOfFocusImage) = when (detectionMode) {
        DetectionMode.Barcode -> {
          this@VisionCameraView.width * 0.78F to this@VisionCameraView.height * 0.24F
        }

        DetectionMode.QRCode -> {
          this@VisionCameraView.width * 0.60F to this@VisionCameraView.width * 0.60F
        }

        else -> {
          this@VisionCameraView.width * 0.80F to this@VisionCameraView.height * 0.40F
        }
      }

      val x = (this@VisionCameraView.width / 2.0F) - (widthOfFocusImage / 2)
      val y = (this@VisionCameraView.height / 2.0F) - (heightOfFocusImage / 2)

      RectF(
        x,
        y,
        x + widthOfFocusImage,
        y + heightOfFocusImage
      )

//            focusSettings = focusSettings.copy(
//                focusImageRect = RectF(
//                    x,
//                    y,
//                    x + widthOfFocusImage,
//                    y + heightOfFocusImage
//                )
//            )
    } else focusSettings.focusImageRect

    // SETTING FOCUS VIEW
    if (focusSettings.shouldDisplayFocusImage && (detectionMode == DetectionMode.Barcode || detectionMode == DetectionMode.QRCode)) {

      // AI: If it is added previously, then remove the previous one before adding the new one.
      removeViewFun(focusImageView)

      focusImageView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY

        this.x = rectF.left
        this.y = rectF.top

        setImageBitmap(focusSettings.focusImage)

        addViewFun(
          this,
          LayoutParams(
            rectF.width().roundToInt(),
            rectF.height().roundToInt()
          )
        )
      }
    } else {
      removeViewFun(focusImageView)
    }
  }

  private fun barcodeAndQrCodeCallbacks(barcodes: List<BarcodeResult>, qrCodes: List<BarcodeResult>) {

    val barcodeDetected = barcodes.isNotEmpty()
    val qrCodeDetected = qrCodes.isNotEmpty()

    focusImageView?.let {
      val tintColor = when (detectionMode) {
        DetectionMode.Barcode -> {
          if (barcodeDetected) {
            focusSettings.focusImageHighlightedColor
          } else {
            focusSettings.focusImageTintColor
          }
        }

        DetectionMode.QRCode -> {
          if (qrCodeDetected) {
            focusSettings.focusImageHighlightedColor
          } else {
            focusSettings.focusImageTintColor
          }
        }

        else -> null
      }
      ImageViewCompat.setImageTintList(it, if (tintColor != null) ColorStateList.valueOf(tintColor) else null)
    }

    this.barcodeDetected = barcodeDetected
    this.qrCodeDetected = qrCodeDetected

    scannerCallback?.onIndications(
      barcodeDetected = objectDetectionConfiguration.isBarcodeOrQRCodeIndicationOn && barcodeDetected,
      qrCodeDetected = objectDetectionConfiguration.isBarcodeOrQRCodeIndicationOn && qrCodeDetected,
      textDetected = objectDetectionConfiguration.isTextIndicationOn && textDetected,
      documentDetected = objectDetectionConfiguration.isDocumentIndicationOn && documentDetected
    )
  }

  private val cameraXBarcodeCallback = object : CameraXBarcodeCallback {

    override fun barcodeIndicator(barcodes: List<BarcodeResult>) {

      if (isScanning.not() || barcodes.isEmpty()) {
        barcodeAndQrCodeCallbacks(emptyList(), emptyList())
        barcodeAndQRCodeBoundingBoxesMap.forEach {
          removeViewFun(it.value)
        }
        barcodeAndQRCodeBoundingBoxesMap.clear()
        return
      }

      val onlyBarcodes = barcodes
        .filter { it.barcode.isOneDimensional() }

      val onlyQRCodes = barcodes.filter { it.barcode.isQRCode() }

      barcodeAndQrCodeCallbacks(onlyBarcodes, onlyQRCodes)
    }

    override fun aggregatedBarcodeResults(barcodes: List<BarcodeResult>) {

      // This piece of code is vital for OCR processes. Here we collect the barcodes that
      // were present on camera feed and send them in onImageCaptured callback.
      // In order to improve performance, we will only collect them when VisionCameraView
      // is in OCR mode.
      if (detectionMode == DetectionMode.OCR) {
        if (barcodes.isEmpty()) {
          barcodesMapForRegex.clear()
        } else {
          barcodes
            .filter { it.barcode.displayValue.isNeitherNullNorEmptyNorBlank() }
            .forEach { barcodesMapForRegex[it.barcode.displayValue!!] = it }
        }
      }

      // Following code is for showing bounding boxes around the scanned barcodes/QRCodes.
      // We will only show these bounding boxes when detection mode is set to either
      // Barcode or QRCode and client app has requested to show these bounding boxes.
      if (focusSettings.showCodeBoundariesInMultipleScan.not() || isMultipleScanEnabled.not() || (detectionMode != DetectionMode.Barcode && detectionMode != DetectionMode.QRCode)) {
        barcodeAndQRCodeBoundingBoxesMap.forEach {
          removeViewFun(it.value)
        }
        barcodeAndQRCodeBoundingBoxesMap.clear()

        return
      }

      val barcodesToDrawBoxOn = when (detectionMode) {
        DetectionMode.Barcode -> {
          barcodes
            .filter { item -> item.barcode.isOneDimensional() }
            // If a template is applied, filter barcode with that template too.
            .run { filterByAppliedTemplate(this) }
        }
        DetectionMode.QRCode -> {
          barcodes.filter { item -> item.barcode.isQRCode() }
        }
        else -> {
          throw VisionSDKException.IllegalDetectionMode(detectionMode)
        }
      }

      drawBoundingBoxes(context, barcodesToDrawBoxOn, barcodes)
    }

    override fun onBarcodesScanned(barcodes: List<BarcodeResult>) {

      if (detectionMode != DetectionMode.Barcode && detectionMode != DetectionMode.QRCode) return

      if (barcodes.isEmpty()) {
        if (scanningMode == ScanningMode.Manual) {
          stopScanning()
          isCaptureButtonClicked = false
          throwFailure(
            when (detectionMode) {
              DetectionMode.Barcode -> VisionSDKException.NoBarcodeDetected
              DetectionMode.QRCode -> VisionSDKException.NoQRCodeDetected

              // Following else should never be invoked. Because, other than Barcode
              // and QR code state, onBarcodesScanned() function should never be called.
              // So if following else is being invoked, that means there is some issue
              // with our SDK and should be fixed immediately.
              else -> VisionSDKException.IllegalDetectionMode(detectionMode)
            }
          )
        }
        return
      }

      val barcodesToReturn = when (detectionMode) {
        DetectionMode.Barcode -> {
          barcodes
            .filter { item -> item.barcode.isOneDimensional() }
            // If a template is applied, filter barcode with that template too.
            .run { filterByAppliedTemplate(this) }
        }
        DetectionMode.QRCode -> {
          barcodes.filter { item -> item.barcode.isQRCode() }
        }
        else -> {
          throwFailure(VisionSDKException.IllegalDetectionMode(detectionMode))
          return
        }
      }

      if (barcodesToReturn.isNotEmpty()) {
        if (scanningMode == ScanningMode.Auto || (scanningMode == ScanningMode.Manual && isCaptureButtonClicked)) {
          stopScanning()
          isCaptureButtonClicked = false
          scannerCallback?.onCodesScanned(barcodesToReturn.mapNotNull { it.barcode.displayValue })
        }
      } else {
        if (scanningMode == ScanningMode.Manual) {
          stopScanning()
          isCaptureButtonClicked = false
          throwFailure(
            when (detectionMode) {
              DetectionMode.Barcode -> VisionSDKException.NoBarcodeDetected
              DetectionMode.QRCode -> VisionSDKException.NoQRCodeDetected

              // Following else should never be invoked. Because, other than Barcode
              // and QR code state, onBarcodesScanned() function should never be called.
              // So if following else is being invoked, that means there is some issue
              // with our SDK and should be fixed immediately.
              else -> VisionSDKException.IllegalDetectionMode(detectionMode)
            }
          )
        }
      }
    }

    override fun onTextDetected(text: Text, width: Int, height: Int) {

      if (isScanning.not()) return

      textDetected = text.textBlocks.isNotEmpty()

      // We only need to draw bounds for documents in OCR mode. In all other modes, we will
      // not draw it.
      if (detectionMode == DetectionMode.OCR) {

        // AI: For all the text blocks, we will figure out a rectangle that encompasses
        // all the text blocks. This will help us determine if given text can be considered
        // as a document or not.
        val potentialDocumentBounds = run {
          var left = Int.MIN_VALUE
          var top = Int.MIN_VALUE
          var right = Int.MAX_VALUE
          var bottom = Int.MAX_VALUE

          // Calculating the document bounds
          text.textBlocks.forEach { block ->

            block.boundingBox ?: return@forEach

            left = if (left == Int.MIN_VALUE) block.boundingBox!!.left else min(left, block.boundingBox!!.left)

            top = if (top == Int.MIN_VALUE) block.boundingBox!!.top else min(top, block.boundingBox!!.top)

            right = if (right == Int.MAX_VALUE) block.boundingBox!!.right else max(right, block.boundingBox!!.right)

            bottom = if (bottom == Int.MAX_VALUE) block.boundingBox!!.bottom else max(bottom, block.boundingBox!!.bottom)
          }

          return@run if (left == Int.MIN_VALUE || top == Int.MIN_VALUE) null else Rect(left, top, right, bottom)
        }

        potentialDocumentBounds?.let {
          val widthConversion = LinearConversion(
            0,
            height,
            0,
            previewView.width
          )
          val newLeft = widthConversion.getValueAgainst(it.left)
          val newRight = widthConversion.getValueAgainst(it.right)

          val heightConversion = LinearConversion(
            0,
            width,
            0,
            previewView.height
          )
          val newTop = heightConversion.getValueAgainst(it.top)
          val newBottom = heightConversion.getValueAgainst(it.bottom)

          val translatedRect = Rect(newLeft, newTop, newRight, newBottom)
          val previewViewRect = previewView.boundingBox

          if (translatedRect.width() > (previewViewRect.width() / 2.0F) && translatedRect.height() > (previewViewRect.height() / 2.0F)) {

            if (scanningMode == ScanningMode.Auto && focusSettings.showDocumentBoundaries) {

              documentDetected = true

              // Add a check here to prevent adding view after stopScanning
              if (documentBoundsView == null) {
                documentBoundsView = View(context)
                addViewFun(
                  documentBoundsView,
                  LayoutParams(
                    it.width(),
                    it.height()
                  )
                )
              }

              documentBoundsView!!.x = translatedRect.left.toFloat()
              documentBoundsView!!.y = translatedRect.top.toFloat()

              val params = documentBoundsView!!.layoutParams as LayoutParams
              params.width = translatedRect.width()
              params.height = translatedRect.height()

              documentBoundsView!!.layoutParams = params

              documentBoundsView!!.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(focusSettings.documentBoundaryFillColor)
                setStroke(focusSettings.documentBoundaryBorderColor, focusSettings.validCodeBoundaryBorderColor)
              }

              initiateDocumentAutoCapture()
            } else {
              documentDetected = false
              removeViewFun(documentBoundsView)
              documentBoundsView = null
              terminateDocumentAutoCapture()
            }
          } else {
            documentDetected = false
            removeViewFun(documentBoundsView)
            documentBoundsView = null
            terminateDocumentAutoCapture()
          }
        } ?: run {
          documentDetected = false
          removeViewFun(documentBoundsView)
          documentBoundsView = null
          terminateDocumentAutoCapture()
        }
      } else {
        documentDetected = false
        removeViewFun(documentBoundsView)
        documentBoundsView = null
        terminateDocumentAutoCapture()
      }

      // The following if condition has a trick to it.
      // So, we're only supposed to send the indication callback if client app has
      // set the isTextIndicationOn to true. Ideally, if isTextIndicationOn is set
      // to false, our SDK should not process the frames for text detection. But,
      // since our OCR mode is dependent on text being detected, that's why, in
      // case of OCR mode, we have to keep our text detector on, even when
      // isTextIndicationOn is false. But if user sets isTextIndicationOn to false,
      // and we keep detecting text for OCR to work, then our SDK will start to send
      // text detection indication to client app, which is not the expected behavior
      // because user has set isTextIndicationOn to false. So we have to explicitly
      // add a check here to only send text detection indication when user has set
      // isTextIndicationOn to true.
      // Four things will happen now:
      // 1 - If user is in Barcode or QRCode mode, and text indication is false, our
      // text detector will stop processing the frames and will not send text detection
      // indication to client.
      // 2 - If user is in Barcode or QRCode mode, and text indication is true, our
      // text detector will keep processing the frames and will send text detection
      // indication to client.
      // 3 - If user is in OCR mode, and text indication is false, our text detector
      // will keep processing the frames but will not send text indication to client.
      // 4 - If user is in OCR mode, and text indication is true, our text detector
      // will keep processing the frames and will send text indication to client.
      scannerCallback?.onIndications(
        barcodeDetected = objectDetectionConfiguration.isBarcodeOrQRCodeIndicationOn && barcodeDetected,
        qrCodeDetected = objectDetectionConfiguration.isBarcodeOrQRCodeIndicationOn && qrCodeDetected,
        textDetected = objectDetectionConfiguration.isTextIndicationOn && textDetected,
        documentDetected = objectDetectionConfiguration.isDocumentIndicationOn && documentDetected,
      )
    }
  }

  private fun drawBoundingBoxes(context: Context, barcodesToDrawBoxOn: List<BarcodeResult>, barcodes: List<BarcodeResult>) {
    // Firstly, we need to add bounding box around each barcode that has been
    // received in this batch. In order to do that, we'll loop over all the
    // barcodesWithinFocusFrame and see if a bounding box view is added for
    // that specific barcode or not.
    //
    // If not, then we will create a new bounding box View, add it in this
    // layout and add it in the boundingBoxesMap.
    //
    // But if it is already added, then we will get its reference from
    // boundingBoxesMap and only modify its coordinates.

    // Loop over every barcode that has been received in this batch.
    barcodesToDrawBoxOn.forEach { barcodeResult ->

      // If, for some reason, its displayValue is null, or empty, or blank
      // then ignore this barcode and move to next one.
      barcodeResult.barcode.displayValue.toNullIfEmptyOrBlank() ?: return@forEach

      // If boundingBoxesMap doesn't contain the current barcode, then add
      // it and create a new bounding box View against it. Add that View in
      // this layout too.
      if (barcodeAndQRCodeBoundingBoxesMap.containsKey(barcodeResult.barcode.displayValue).not()) {
        barcodeAndQRCodeBoundingBoxesMap[barcodeResult.barcode.displayValue!!] = View(context).also { addViewFun(it) }
      }

      if (barcodeAndQRCodeBoundingBoxesMap.containsKey(barcodeResult.barcode.displayValue).not())
        return@forEach

      val boundingBoxView = barcodeAndQRCodeBoundingBoxesMap[barcodeResult.barcode.displayValue] ?: return@forEach
      val rectF = barcodeResult.barcode.boundingBox?.toRectF() ?: return@forEach

      val widthConversion = LinearConversion(
        0.0F,
        barcodeResult.imageHeight.toFloat(),
        0.0F,
        previewView.width.toFloat()
      )
      val newLeft = widthConversion.getValueAgainst(rectF.left)
      val newRight = widthConversion.getValueAgainst(rectF.right)

      val heightConversion = LinearConversion(
        0.0F,
        barcodeResult.imageWidth.toFloat(),
        0.0F,
        previewView.height.toFloat()
      )
      val newTop = heightConversion.getValueAgainst(rectF.top)
      val newBottom = heightConversion.getValueAgainst(rectF.bottom)

      val translatedRectF = RectF(newLeft, newTop, newRight, newBottom)

      boundingBoxView.x = translatedRectF.left
      boundingBoxView.y = translatedRectF.top

      val params = boundingBoxView.layoutParams as LayoutParams
      params.width = translatedRectF.width().roundToInt()
      params.height = max(translatedRectF.height().roundToInt(), context.toDp(200))

      boundingBoxView.layoutParams = params

      boundingBoxView.background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(focusSettings.validCodeBoundaryFillColor)
        setStroke(focusSettings.validCodeBoundaryBorderWidth, focusSettings.validCodeBoundaryBorderColor)
      }
    }

    // Now we need to remove any keys that are in boundingBoxesMap but are
    // not detected in the current batch of barcodes.

    // Now check if boundingBoxesMap has keys that are not present in
    // displayValuesOfBarcodes.
    val stringsOfBarcodes = barcodes.map { it.barcode.displayValue }
    val keysToBeRemoved = barcodeAndQRCodeBoundingBoxesMap.keys.filter { stringsOfBarcodes.contains(it).not() }

    // Now remove all the keys that should be removed from boundingBoxesMap,
    // as well as from this layout.
    keysToBeRemoved.forEach {
      val viewToBeRemoved = barcodeAndQRCodeBoundingBoxesMap.remove(it)
      removeViewFun(viewToBeRemoved)
    }
  }

  private fun initiateDocumentAutoCapture() {

    if (scanningMode == ScanningMode.Manual) return

    if (autoCaptureDocumentTimer != null) return

    autoCaptureDocumentTimer = Timer()
    autoCaptureDocumentTimer?.schedule(
      timerTask {
        Log.d(TAG, "Auto capturing image")
        captureImage {
          // We should set autoCaptureDocumentTimer to null after captureImage() function completion.
          // i.e. in this callback. Otherwise, it will be set to null immediately and new timer will
          // be set, which will capture a new image again, even when the first captureImage() function
          // call was not finished.
          autoCaptureDocumentTimer = null
          Log.d(TAG, "Capture image completed")
        }
      },
      objectDetectionConfiguration.secondsToWaitBeforeDocumentCapture * 1000L
    )
    Log.d(TAG, "Document auto capture started")
  }

  private fun terminateDocumentAutoCapture() {
    autoCaptureDocumentTimer?.let {
      it.cancel()
      Log.d(TAG, "Document auto capture cancelled")
    } ?: return
    autoCaptureDocumentTimer = null
  }

  private fun getBarcodesWithinFocusFrame(barcodes: List<BarcodeResult>) = if ((detectionMode == DetectionMode.Barcode || detectionMode == DetectionMode.QRCode) && focusSettings.isFrameAddedAndApplicable()) {
    barcodes.filter {

      val widthConversion = LinearConversion(
        0,
        it.imageHeight,
        0,
        previewView.width
      )
      val newLeft = widthConversion.getValueAgainst(it.barcode.boundingBox?.left ?: return@filter false)
      val newRight = widthConversion.getValueAgainst(it.barcode.boundingBox?.right ?: return@filter false)

      val heightConversion = LinearConversion(
        0,
        it.imageWidth,
        0,
        previewView.height
      )
      val newTop = heightConversion.getValueAgainst(it.barcode.boundingBox?.top ?: return@filter false)
      val newBottom = heightConversion.getValueAgainst(it.barcode.boundingBox?.bottom ?: return@filter false)

      val translatedRect = Rect(newLeft, newTop, newRight, newBottom)

      focusSettings.focusImageRect.contains(translatedRect.toRectF())
    }
  } else {
    barcodes
  }

  private fun filterByAppliedTemplate(barcodes: List<BarcodeResult>): List<BarcodeResult> {
    return appliedBarcodeTemplate?.let { template ->
      barcodes.filter { barcode ->
        template.barcodeTemplateData.any { templateData ->
          barcode.barcode.displayValue?.length == templateData.barcodeLength
            && barcode.barcode.format == templateData.barcodeFormat
        }
      }
    } ?: barcodes
  }

  private fun enableTorch() {
    camera?.cameraControl?.enableTorch(true)
  }

  private fun disableTorch() {
    camera?.cameraControl?.enableTorch(false)
  }

  fun setLinearZoom(zoomLevel: Float) {
    linearZoom = zoomLevel
    zoomRatio = Float.MIN_VALUE
    camera?.cameraControl?.setLinearZoom(linearZoom)
  }

  fun currentLinearZoom() = camera?.cameraInfo?.zoomState?.value?.linearZoom

  fun setZoomRatio(zoomRatio: Float) {
    linearZoom = Float.MIN_VALUE
    this.zoomRatio = zoomRatio
    camera?.cameraControl?.setZoomRatio(this.zoomRatio)
  }

  fun getCurrentZoomRatio() = camera?.cameraInfo?.zoomState?.value?.zoomRatio

  fun getMaxZoomRatioAvailable() = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio

  fun getMinZoomRatioAvailable() = camera?.cameraInfo?.zoomState?.value?.minZoomRatio

  fun capture() {

    if (isScanning.not()) {
      throwFailure(VisionSDKException.CallStartCameraOrRescanBeforeCapture)
      return
    }

    when (detectionMode) {

      DetectionMode.Barcode -> {
        isCaptureButtonClicked = true
      }

      DetectionMode.QRCode -> {
        isCaptureButtonClicked = true
      }

      DetectionMode.OCR, DetectionMode.PriceTag, DetectionMode.Photo -> {
        if (autoCaptureDocumentTimer == null) {
          captureImage()
        }
      }
    }
  }

  private fun captureImage(onCaptureCompletion: (() -> Unit)? = null) {
    imageCaptureUseCase?.takePicture(
      cameraExecutor!!,
      object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(imageProxy: ImageProxy) {

          if (detectionMode == DetectionMode.OCR && textDetected.not()) {
            (context as Activity).runOnUiThread {
              scannerCallback?.onFailure(
                VisionSDKException.NoTextDetected
              )
            }
            imageProxy.close()
            onCaptureCompletion?.invoke()
            return
          }

          var bitmap = imageProxy.toBitmap()

          // Cropping bitmap to achieve WYSIWYG.
          bitmap = Bitmap.createBitmap(bitmap, imageProxy.cropRect.left, imageProxy.cropRect.top, imageProxy.cropRect.width(), imageProxy.cropRect.height())

          if (imageProxy.imageInfo.rotationDegrees != 0) {
            bitmap = fixOrientation(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
          }

          bitmap = bitmapCompression(bitmap, imageCompressionFactorInPercentage)

          (context as Activity).runOnUiThread {
            stopScanning()
            imageViewForCapturedImage = ImageView(context)
            imageViewForCapturedImage?.setImageBitmap(bitmap)
            addViewFun(imageViewForCapturedImage, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            scannerCallback?.onImageCaptured(bitmap, barcodesMapForRegex.values.toList().mapNotNull { it.barcode.displayValue })
          }
          imageProxy.close()
          onCaptureCompletion?.invoke()
        }

        override fun onError(exception: ImageCaptureException) {
          (context as Activity).runOnUiThread {
            throwFailure(VisionSDKException.UnknownException(exception))
          }
          onCaptureCompletion?.invoke()
        }
      }) ?: throw VisionSDKException.CallStartCameraOrRescanBeforeCapture
  }

  private fun addViewFun(view: View?, layoutParams: LayoutParams? = null) {
    view?.let {
      if (layoutParams != null) {
        addView(it, layoutParams)
      } else {
        addView(it)
      }
      printChildCount()
    }
  }

  private fun removeViewFun(view: View?) {
    view?.let {
      removeView(view)
      printChildCount()
    }
  }

  private fun removeAllViewsFun() {
    for (i in 0 until childCount) {
      removeViewFun(getChildAt(i))
    }
  }

  private fun printChildCount() {
//        Log.d(TAG, "Child Count: ${this.childCount}")
  }

  private fun fixOrientation(mBitmap: Bitmap, rotationAngle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotationAngle)
    return Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.width, mBitmap.height, matrix, true)
  }

  private fun throwFailure(visionSDKException: VisionSDKException) {
    visionSDKException.printStackTrace()
    isCaptureButtonClicked = false
    scannerCallback?.onFailure(visionSDKException)
  }

  inner class FocusRegionManager internal constructor() {
    fun setFocusSettings(focusSettings: FocusSettings) {
      this@VisionCameraView.setFocusSettings(focusSettings)
    }
  }
}
