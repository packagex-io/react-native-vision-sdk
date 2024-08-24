package io.packagex.visionsdk.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.packagex.visionsdk.ocr.ml.dto.WordWithBoundingBoxAndLineNumber
import io.packagex.visionsdk.ocr.ml.dto.WordWithLineNumber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class LocalOCR {

    suspend fun performLocalOCR(bitmap: Bitmap): Pair<String?, List<WordWithLineNumber>> {

        return suspendCoroutine { suspendCoroutine ->

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { recognizedText ->
                    val results = mutableListOf<WordWithLineNumber>()

                    var lineNumber = 0
                    for (block in recognizedText.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val elementText = element.text
                                results.add(WordWithLineNumber(elementText, lineNumber))
                            }
                            lineNumber++
                        }
                    }
                    suspendCoroutine.resume(recognizedText.text to results)
                }
                .addOnFailureListener {
                    suspendCoroutine.resumeWithException(it)
                }
        }
    }

    suspend fun performLocalOCRWithBoundingBoxes(bitmap: Bitmap): Pair<String?, List<WordWithBoundingBoxAndLineNumber>> {
        val imageHeight: Int = bitmap.height
        val imageWidth: Int = bitmap.width

        return suspendCoroutine { suspendCoroutine ->

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { recognizedText ->
                    val results = mutableListOf<WordWithBoundingBoxAndLineNumber>()

                    var lineNumber = 0
                    for (block in recognizedText.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementBoundingBox = element.boundingBox
                                val normalizedBoundingBox = elementBoundingBox?.let {
                                    listOf(
                                        it.left * 1000 / imageWidth,
                                        it.top  * 1000 / imageHeight,
                                        it.right * 1000 / imageWidth,
                                        it.bottom * 1000 / imageHeight
                                    )
                                }
                                val boundingBox = intArrayOf(
                                    normalizedBoundingBox?.get(0) ?: 0,
                                    normalizedBoundingBox?.get(1) ?: 0,
                                    normalizedBoundingBox?.get(2) ?: 0,
                                    normalizedBoundingBox?.get(3) ?: 0
                                ).toList()
                                results.add(WordWithBoundingBoxAndLineNumber(elementText, boundingBox, lineNumber))
                            }
                            lineNumber++
                        }
                    }
                    suspendCoroutine.resume(recognizedText.text to results)
                }
                .addOnFailureListener {
                    suspendCoroutine.resumeWithException(it)
                }
        }
    }
}