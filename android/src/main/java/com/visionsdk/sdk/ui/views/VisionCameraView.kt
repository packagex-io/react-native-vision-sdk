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
import androidx.core.graphics.toRectF
import androidx.core.view.doOnAttach
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.LifecycleOwner
import com.asadullah.handyutils.boundingBox
import com.asadullah.handyutils.format
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.save
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
import io.packagex.visionsdk.core.VisionViewState
import io.packagex.visionsdk.exceptions.RootedDeviceException
import io.packagex.visionsdk.exceptions.ScannerException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.CameraXBarcodeCallback
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.preferences.VisionSdkSettings
import io.packagex.visionsdk.preferences.dto.BarcodeTemplate
import io.packagex.visionsdk.utils.BitmapUtils.bitmapCompression
import io.packagex.visionsdk.utils.BitmapUtils.imageToBitmap
import io.packagex.visionsdk.utils.LinearConversion
import io.packagex.visionsdk.utils.TAG
import io.packagex.visionsdk.utils.isEmulator
import io.packagex.visionsdk.utils.isOneDimensional
import io.packagex.visionsdk.utils.isQRCode
import java.io.File
import java.util.Date
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

    private val barcodeAndQRCodeBoundingBoxesMap = mutableMapOf<BarcodeResult, View>()
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

    /** SCREEN STATE SETTINGS */
    private var visionViewState = VisionViewState()

    /** CAMERA SETTINGS STARTED */
    private var cameraSettings = CameraSettings()

    /**
     * If you don't want to process every frame to save resources, you can set an nth frame
     * using this function. So let say you pass 10 in this function. This will mean that every
     * 10th frame will be processed and the rest of the frame will be skipped from processing.
     *
     * Its default value is 10.
     *
     * If you want to process every frame, you should pass -1 in this function, or call the
     * convenient function analyzeEveryFrame().
     */
    fun shouldOnlyProcessNthFrame(nthFrameToProcess: Int) {
        cameraSettings = cameraSettings.copy(nthFrameToProcess = nthFrameToProcess)
        imageAnalyzer.nthFrame = cameraSettings.nthFrameToProcess
    }

    /**
     * Convenient function for `shouldOnlyProcessNthFrame(-1)`.
     */
    fun analyzeEveryFrame() {
        cameraSettings = cameraSettings.copy(nthFrameToProcess = -1)
        imageAnalyzer.analyzeEveryFrame()
    }

    fun shouldAutoSaveCapturedImage(save: Boolean) {
        cameraSettings = cameraSettings.copy(shouldAutoSaveCapturedImage = save)
    }

    fun clearImagesFromCache() {
        imagesCacheDirectory.listFiles()?.forEach { it.delete() }
    }

    fun setVisionViewState(visionViewState: VisionViewState? = null) {
        visionViewState?.let { this.visionViewState = it }
        updateUIWithCurrentViewState()
    }

    /** FOCUS SETTINGS */
    private var focusSettings = FocusSettings()
    private var focusRegionManager: FocusRegionManager? = null
    private var focusImageView: ImageView? = null
    internal fun setFocusSettings(focusSettings: FocusSettings? = null) {
        focusSettings?.let { this.focusSettings = it }
        applyFocusSettings()
    }
    fun getFocusRegionManager(): FocusRegionManager {
        assert(focusRegionManager != null) { "You will get FocusRegionManager after camera is started." }
        return focusRegionManager!!
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

    /** OBJECT DETECTION CONFIGURATIONS */
    private var objectDetectionConfiguration = ObjectDetectionConfiguration()

    fun setObjectDetectionConfiguration(objectDetectionConfiguration: ObjectDetectionConfiguration) {
        this.objectDetectionConfiguration = objectDetectionConfiguration
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
        return VisionSdkSettings.getAllBarcodeTemplates()
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
            throw RootedDeviceException()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    cameraProvider = cameraProviderFuture.get()
                    startScanning()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
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
        startScanning()
    }

    private fun startScanning() {

        cameraProvider ?: throw IllegalStateException("You need to call startCameraAndScanning() first.")

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
        if (visionViewState.isFlashTurnedOn) {
            enableTorch()
        } else {
            disableTorch()
        }

        // SETTING TEXT DETECTOR
        if (visionViewState.detectionMode == DetectionMode.OCR) {
            imageScanner.enableTextDetection(true)
        } else {
            imageScanner.enableTextDetection(objectDetectionConfiguration.isTextIndicationOn)
        }

        // SETTING BARCODE/QRCODE DETECTOR
        imageScanner.enableBarcodeDetection(objectDetectionConfiguration.isBarcodeOrQRCodeIndicationOn)

        // SETTING SCANNING MODE
        imageScanner.scanningModeManual(visionViewState.scanningMode == ScanningMode.Manual)

        // SETTING MULTIPLE SCAN MODE
        imageScanner.multipleScanEnabled(visionViewState.isMultipleScanEnabled)
    }

    private fun applyFocusSettings() {

        // SETTING FOCUS VIEW
        if (focusSettings.shouldDisplayFocusImage && (visionViewState.detectionMode == DetectionMode.Barcode || visionViewState.detectionMode == DetectionMode.QRCode)) {

            // AI: If it is added previously, then remove the previous one before adding the new one.
            removeViewFun(focusImageView)

            focusImageView = ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY

                if (focusSettings.focusImageRect.isEmpty) {
                    val (widthOfFocusImage, heightOfFocusImage) = when (visionViewState.detectionMode) {
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

                    focusSettings = focusSettings.copy(
                        focusImageRect = RectF(
                            x,
                            y,
                            x + widthOfFocusImage,
                            y + heightOfFocusImage
                        )
                    )
                }

                this.x = focusSettings.focusImageRect.left
                this.y = focusSettings.focusImageRect.top

                setImageResource(focusSettings.focusImage)

                addViewFun(
                    this,
                    LayoutParams(
                        focusSettings.focusImageRect.width().roundToInt(),
                        focusSettings.focusImageRect.height().roundToInt()
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

        if (focusSettings.isFrameAddedAndApplicable()) {
            focusImageView?.let {
                val tintColor = when (visionViewState.detectionMode) {
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
        }

        this.barcodeDetected = barcodeDetected
        this.qrCodeDetected = qrCodeDetected

        scannerCallback?.detectionCallbacks(
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
            if (visionViewState.detectionMode == DetectionMode.OCR) {
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
            if (focusSettings.showCodeBoundariesInMultipleScan.not() || visionViewState.isMultipleScanEnabled.not() || (visionViewState.detectionMode != DetectionMode.Barcode && visionViewState.detectionMode != DetectionMode.QRCode)) {
                barcodeAndQRCodeBoundingBoxesMap.forEach {
                    removeViewFun(it.value)
                }
                barcodeAndQRCodeBoundingBoxesMap.clear()

                return
            }

            val barcodesToDrawBoxOn = when (visionViewState.detectionMode) {
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
                    throw IllegalStateException("If detectionMode is set to OCR or Photo, then this whole section should have been skipped. This is an issue in SDK and needs fixing immediately.")
                }
            }

            drawBoundingBoxes(context, barcodesToDrawBoxOn, barcodes)
        }

        override fun onBarcodesScanned(barcodes: List<BarcodeResult>) {

            if (visionViewState.detectionMode != DetectionMode.Barcode && visionViewState.detectionMode != DetectionMode.QRCode) return

            if (barcodes.isEmpty()) {
                if (visionViewState.scanningMode == ScanningMode.Manual) {
                    stopScanning()
                    isCaptureButtonClicked = false
                    throwFailure()
                }
                return
            }

            val barcodesToReturn = when (visionViewState.detectionMode) {
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
                    throw IllegalStateException("If detectionMode is set to OCR or Photo, then this whole section should have been skipped. This is an issue in SDK and needs fixing immediately.")
                }
            }

            if (barcodesToReturn.isNotEmpty()) {
                if (visionViewState.scanningMode == ScanningMode.Auto || (visionViewState.scanningMode == ScanningMode.Manual && isCaptureButtonClicked)) {
                    stopScanning()
                    isCaptureButtonClicked = false
                    scannerCallback?.onBarcodesDetected(barcodesToReturn.mapNotNull { it.barcode.displayValue })
                }
            } else {
                if (visionViewState.scanningMode == ScanningMode.Manual) {
                    stopScanning()
                    isCaptureButtonClicked = false
                    throwFailure()
                }
            }
        }

        override fun onTextDetected(text: Text, width: Int, height: Int) {

            if (isScanning.not()) return

            textDetected = text.textBlocks.isNotEmpty()

            // We only need to draw bounds for documents in OCR mode. In all other modes, we will
            // not draw it.
            if (visionViewState.detectionMode == DetectionMode.OCR && focusSettings.showDocumentBoundaries) {

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

                if (potentialDocumentBounds != null) {

                    val widthConversion = LinearConversion(
                        0,
                        height,
                        0,
                        previewView.width
                    )
                    val newLeft = widthConversion.getValueAgainst(potentialDocumentBounds.left)
                    val newRight = widthConversion.getValueAgainst(potentialDocumentBounds.right)

                    val heightConversion = LinearConversion(
                        0,
                        width,
                        0,
                        previewView.height
                    )
                    val newTop = heightConversion.getValueAgainst(potentialDocumentBounds.top)
                    val newBottom = heightConversion.getValueAgainst(potentialDocumentBounds.bottom)

                    val translatedRect = Rect(newLeft, newTop, newRight, newBottom)
                    val previewViewRect = previewView.boundingBox

                    if (translatedRect.width() > (previewViewRect.width() / 2.0F) && translatedRect.height() > (previewViewRect.height() / 2.0F)) {

                        documentDetected = true

                        // Add a check here to prevent adding view after stopScanning
                        if (documentBoundsView == null) {
                            documentBoundsView = View(context)
                            addViewFun(
                                documentBoundsView,
                                LayoutParams(
                                    potentialDocumentBounds.width(),
                                    potentialDocumentBounds.height()
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
            scannerCallback?.detectionCallbacks(
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
        // boundingBoxesMap and only modify it coordinates.

        // Loop over every barcode that has been received in this batch.
        barcodesToDrawBoxOn.forEach { barcodeResult ->

            // If, for some reason, its displayValue is null, or empty, or blank
            // then ignore this barcode and move to next one.
            barcodeResult.barcode.displayValue.toNullIfEmptyOrBlank() ?: return@forEach

            // If boundingBoxesMap doesn't contain the current barcode, then add
            // it and create a new bounding box View against it. Add that View in
            // this layout too.
            if (barcodeAndQRCodeBoundingBoxesMap.containsKey(barcodeResult).not()) {
                barcodeAndQRCodeBoundingBoxesMap[barcodeResult] = View(context).also { addViewFun(it) }
            }

            if (barcodeAndQRCodeBoundingBoxesMap.containsKey(barcodeResult).not())
                return@forEach

            val boundingBoxView = barcodeAndQRCodeBoundingBoxesMap[barcodeResult] ?: return@forEach
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
        val keysToBeRemoved = barcodeAndQRCodeBoundingBoxesMap.keys.filter { barcodes.contains(it).not() }

        // Now remove all the keys that should be removed from boundingBoxesMap,
        // as well as from this layout.
        keysToBeRemoved.forEach {
            val viewToBeRemoved = barcodeAndQRCodeBoundingBoxesMap.remove(it)
            removeViewFun(viewToBeRemoved)
        }
    }

    private fun initiateDocumentAutoCapture() {

        if (visionViewState.scanningMode == ScanningMode.Manual) return

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

    private fun getBarcodesWithinFocusFrame(barcodes: List<BarcodeResult>) = if ((visionViewState.detectionMode == DetectionMode.Barcode || visionViewState.detectionMode == DetectionMode.QRCode) && focusSettings.isFrameAddedAndApplicable()) {
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
        when (visionViewState.detectionMode) {

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

                    (context as Activity).runOnUiThread {
                        stopScanning()
                    }

                    if (visionViewState.detectionMode == DetectionMode.OCR && textDetected.not()) {
                        (context as Activity).runOnUiThread {
                            scannerCallback?.onFailure(ScannerException.TextNotDetected())
                        }
                        imageProxy.close()
                        onCaptureCompletion?.invoke()
                        return
                    }

                    var bitmap = imageToBitmap(imageProxy)

                    // Cropping bitmap to achieve WYSIWYG.
                    bitmap = Bitmap.createBitmap(bitmap, imageProxy.cropRect.left, imageProxy.cropRect.top, imageProxy.cropRect.width(), imageProxy.cropRect.height())

                    if (imageProxy.imageInfo.rotationDegrees != 0) {
                        bitmap = fixOrientation(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
                    }

                    // AI: After having discussion with the iOS guy, we will only
                    // compress the bitmap that we return and won't compress the bitmap that
                    // we need to save in a file. That's why, saving bitmap into a file is
                    // happening before the bitmap compression.
                    val newImageFile = if (cameraSettings.shouldAutoSaveCapturedImage) {
                        saveBitmapToFile(bitmap)
                    } else null

                    bitmap = bitmapCompression(bitmap, imageCompressionFactorInPercentage)

                    (context as Activity).runOnUiThread {
                        imageViewForCapturedImage = ImageView(context)
                        imageViewForCapturedImage?.setImageBitmap(bitmap)
                        addViewFun(imageViewForCapturedImage, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                        scannerCallback?.onImageCaptured(bitmap, newImageFile, barcodesMapForRegex.values.toList().mapNotNull { it.barcode.displayValue })
                    }
                    imageProxy.close()
                    onCaptureCompletion?.invoke()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    (context as Activity).runOnUiThread {
                        stopScanning()
                        scannerCallback?.onFailure(ScannerException.UnknownErrorDetected(exception.message ?: exception.toString()))
                    }
                    onCaptureCompletion?.invoke()
                }

                private fun saveBitmapToFile(bitmap: Bitmap): File {
                    if (imagesCacheDirectory.exists().not()) {
                        imagesCacheDirectory.mkdirs()
                    }
                    val newImageFileName = generateNewImageFileName()
                    val newImageFile = File(imagesCacheDirectory, newImageFileName)
                    bitmap.save(newImageFile, Bitmap.CompressFormat.JPEG)
                    return newImageFile
                }

                private fun generateNewImageFileName(): String {
                    return "${Date().format("yyyyMMdd_HHmmssSSS")}.jpg"
                }
            }) ?: throw IllegalStateException("You need to call startCamera() first.")
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

    private fun throwFailure() {
        stopScanning()
        isCaptureButtonClicked = false
        when (visionViewState.detectionMode) {
            DetectionMode.Barcode -> scannerCallback?.onFailure(ScannerException.BarcodeNotDetected())
            DetectionMode.QRCode -> scannerCallback?.onFailure(ScannerException.QRCodeNotDetected())
            else -> scannerCallback?.onFailure(ScannerException.UnknownErrorDetected())
        }
    }

    inner class FocusRegionManager internal constructor() {
        fun setFocusSettings(focusSettings: FocusSettings?) {
            this@VisionCameraView.setFocusSettings(focusSettings)
        }
    }
}
