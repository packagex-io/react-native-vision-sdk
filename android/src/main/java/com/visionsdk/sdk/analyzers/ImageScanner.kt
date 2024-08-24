package io.packagex.visionsdk.analyzers

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.asadullah.handyutils.isNeitherNullNorEmptyNorBlank
import com.asadullah.handyutils.runOnListIf
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.packagex.visionsdk.FixedLengthQueue
import io.packagex.visionsdk.interfaces.CameraXBarcodeCallback

internal class ImageScanner(
    private val allInitialBarcodes: (barcodeResults: List<BarcodeResult>) -> List<BarcodeResult>,
    private val onScanResult: CameraXBarcodeCallback
) {

    private val imageFramesQueue = FixedLengthQueue<List<BarcodeResult>>(9)

    private val barcodeScanner: BarcodeScanner by lazy { BarcodeScanning.getClient() }

    private val textDetector by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private var imageScannerLifecycleListener: ImageScannerLifecycleListener? = null

    fun setImageScannerLifecycleListener(listener: ImageScannerLifecycleListener?) {
        imageScannerLifecycleListener = listener
    }

    private var enableTextDetection = true

    fun enableTextDetection(enable: Boolean) {
        enableTextDetection = enable
    }

    private var enableBarcodeDetection = true

    fun enableBarcodeDetection(enable: Boolean) {
        enableBarcodeDetection = enable
    }

    private var isScanningModeManual = false

    fun scanningModeManual(isManual: Boolean) {
        isScanningModeManual = isManual
    }

    private var isMultipleScanEnabled = false

    fun multipleScanEnabled(enabled: Boolean) {
        isMultipleScanEnabled = enabled
    }

    private var isButtonPressed = false

    fun setButtonPressed(pressed: Boolean) {
        isButtonPressed = pressed
    }

    private var isAggregatedResultSentBack = false

    fun rescan() { isAggregatedResultSentBack = false }

    @SuppressLint("UnsafeOptInUsageError")
    fun scanFrame(imageProxy: ImageProxy, inputImage: InputImage?) {

        // AI: We must only close imageProxy when all detectors have responded back. In order
        // to achieve that, we're using the following integer and closeImageProxyIfCriteriaIsMet.
        var totalDetectors = (if (enableTextDetection) 1 else 0) + (if (enableBarcodeDetection) 1 else 0)

        fun closeImageProxyIfCriteriaIsMet() {
            if (totalDetectors == 1) {
                imageProxy.close()
            } else {
                totalDetectors--
            }
        }

        val finalInputImage = inputImage ?: InputImage.fromMediaImage(imageProxy.image!!, 0)

        if (enableTextDetection) {
            textDetector.process(finalInputImage).addOnCompleteListener {
                if (it.isSuccessful) {
                    onScanResult.onTextDetected(it.result, finalInputImage.width, finalInputImage.height)
                }

                closeImageProxyIfCriteriaIsMet()
            }
        }

        if (enableBarcodeDetection) {

//            val startTime = System.currentTimeMillis()

            barcodeScanner.process(finalInputImage).addOnCompleteListener { barcodesTask ->

//                val totalTime = System.currentTimeMillis() - startTime
//                println(totalTime)

                if (barcodesTask.isSuccessful) {

                    val barcodes = barcodesTask.result

                    val barcodeResults = barcodes
                        .filter { it.displayValue.isNeitherNullNorEmptyNorBlank() }
                        .map { BarcodeResult(it, finalInputImage.width, finalInputImage.height) }
                        .run { allInitialBarcodes.invoke(this) }
                        .runOnListIf(
                            condition = isMultipleScanEnabled,
                            runIf = {
                                it
                            },
                            runElse = {
                                it.firstOrNull()?.let { firstElement ->
                                    listOf(firstElement)
                                } ?: emptyList()
                            }
                        )

                    onScanResult.barcodeIndicator(barcodeResults)

                    addToImageFramesQueue(barcodeResults)

//                    val oneTimeDuration = measureTime { sendOneTimeResultIfCriteriaIsMet(barcodeResults) }

//                    println(
//                        "Indication: ${indicationDuration.toReadableDuration()}\t\tAggregated: ${aggregatedDuration.toReadableDuration()}\t\tOneTime: ${oneTimeDuration.toReadableDuration()}"
//                    )
                } else {
                    onScanResult.barcodeIndicator(emptyList())
                    addToImageFramesQueue(emptyList())
                }

                closeImageProxyIfCriteriaIsMet()
            }
        }
    }

    private fun addToImageFramesQueue(barcodeResults: List<BarcodeResult>) {

        if (isAggregatedResultSentBack) return

        imageFramesQueue.push(barcodeResults)

        if (imageFramesQueue.isFull()) {

            val distinctResult = imageFramesQueue.toList().flatten().distinctBy { it.barcode.displayValue }

            onScanResult.aggregatedBarcodeResults(distinctResult)

            if (isScanningModeManual) {
                // When imageFramesQueue is full and isScanningModeManual is true, that means
                // manual mode was selected. Check if the button is pressed, then send the
                // data in queue back, empty the queue and stop collecting more, until rescan.
                if (isButtonPressed) {
                    isAggregatedResultSentBack = true
                    onScanResult.onBarcodesScanned(distinctResult)
                    imageFramesQueue.clear()
                }
            } else {

                // When imageFramesQueue is full and isScanningModeManual is false, that means
                // auto mode was selected. So, in auto mode, we should only respond back when
                // the last element (the one added earliest) of the queue has barcodes in it.
                if (imageFramesQueue.firstOrNull()?.isNotEmpty() == true) {
                    isAggregatedResultSentBack = true
                    onScanResult.onBarcodesScanned(distinctResult)
                    imageFramesQueue.clear()
                }
            }
        }
    }

    internal abstract class ImageScannerLifecycleListener {

        private var isScanningStarted = false
        private var isScanningEnded = false

        internal fun scanning() {
            if (isScanningStarted) return
            isScanningStarted = true
            isScanningEnded = false
            onScanningStarted()
        }

        internal fun noScanning() {
            if (isScanningEnded) return
            isScanningEnded = true
            isScanningStarted = false
            onScanningEnded()
        }

        protected abstract fun onScanningStarted()
        protected abstract fun onScanningEnded()
    }
}