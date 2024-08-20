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
import io.packagex.visionsdk.interfaces.CameraXBarcodeCallback

internal class ImageScanner(
  private val allInitialBarcodes: (barcodeResults: List<BarcodeResult>) -> List<BarcodeResult>,
  private val onScanResult: CameraXBarcodeCallback
) {

  private val maxFramesForConsistency = 7

  private var aggregateCounter = 0
  private var consistencyCounter = 0

  private val aggregatedBarcodesMap = mutableMapOf<String, BarcodeResult>()
  private val consistencyBarcodesMap = mutableMapOf<String, BarcodeResult>()

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
    set(value) {
      field = value
      consistencyBarcodesMap.clear()
      aggregatedBarcodesMap.clear()
    }

  fun enableBarcodeDetection(enable: Boolean) {
    enableBarcodeDetection = enable
  }

  private var isScanningModeManual = false
    set(value) {
      field = value
      consistencyBarcodesMap.clear()
      aggregatedBarcodesMap.clear()
    }

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
      barcodeScanner.process(finalInputImage).addOnCompleteListener { barcodesTask ->

        aggregateCounter++

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

          sendContinuousAggregatedResult(barcodeResults)

          sendOneTimeResultIfCriteriaIsMet(barcodeResults)
        } else {
          onScanResult.barcodeIndicator(emptyList())
        }

        closeImageProxyIfCriteriaIsMet()
      }
    }
  }

  private fun sendOneTimeResultIfCriteriaIsMet(barcodeResults: List<BarcodeResult>) {

    if (isScanningModeManual) {
      if (isButtonPressed) {
        makeResultConsistent(barcodeResults)
      }
    } else {
      if (barcodeResults.isNotEmpty() || consistencyBarcodesMap.isNotEmpty()) {
        makeResultConsistent(barcodeResults)
      }
    }
  }

  private fun makeResultConsistent(barcodeResults: List<BarcodeResult>) {

    consistencyCounter++

    barcodeResults.forEach { addToMap(consistencyBarcodesMap, it) }
    imageScannerLifecycleListener?.scanning()

    if (consistencyCounter >= maxFramesForConsistency) {
      consistencyCounter = 0
      imageScannerLifecycleListener?.noScanning()
      onScanResult.onBarcodesScanned(consistencyBarcodesMap.values.toList())
      consistencyBarcodesMap.clear()
    }
  }

  private fun sendContinuousAggregatedResult(barcodeResults: List<BarcodeResult>) {
    barcodeResults.forEach { addToMap(aggregatedBarcodesMap, it) }

    if (aggregateCounter >= maxFramesForConsistency) {
      aggregateCounter = 0
      onScanResult.aggregatedBarcodeResults(aggregatedBarcodesMap.values.toList())
      aggregatedBarcodesMap.clear()
    }
  }

  private fun addToMap(map: MutableMap<String, BarcodeResult>, barcodeResult: BarcodeResult) {
    // If multiple scan is disabled, then we should only aggregate one barcode. The code above
    // will give us one barcode if multiple scan is disabled, but there's no guarantee that it
    // will detect the same barcode it detected in the previous frame. That's why, when multiple
    // scan is disabled, we are aggregating the received barcode only if it is already present
    // in the map, or the map is completely empty.
    if (isMultipleScanEnabled.not()) {
      if (map.isEmpty() || map.containsKey(barcodeResult.barcode.displayValue!!)) {
        map[barcodeResult.barcode.displayValue!!] = barcodeResult
      }
    } else {
      map[barcodeResult.barcode.displayValue!!] = barcodeResult
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
