package io.packagex.visionsdk.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.packagex.visionsdk.ocr.ml.dto.WordWithBoundingBox
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class LocalOCR {

    suspend fun performLocalOCR(bitmap: Bitmap): String? {

        return suspendCoroutine { suspendCoroutine ->

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { recognizedText ->
                    /*val resultText = recognizedText.text

                    val stringBuilder = StringBuilder()

                    for (block in recognizedText.textBlocks) {

                        stringBuilder.append('\n')

                        val blockText = block.text
                        val blockCornerPoints = block.cornerPoints
                        val blockFrame = block.boundingBox

                        stringBuilder.append(blockText).append('\n')
                        stringBuilder.append(blockFrame.toString()).append('\n')

                        for (line in block.lines) {

                            stringBuilder.append("    ").append('\n')

                            val lineText = line.text
                            val lineCornerPoints = line.cornerPoints
                            val lineFrame = line.boundingBox

                            stringBuilder.append("    ").append(lineText).append('\n')
                            stringBuilder.append("    ").append(lineFrame.toString()).append('\n')

                            for (element in line.elements) {

                                stringBuilder.append("        ").append('\n')

                                val elementText = element.text
                                val elementCornerPoints = element.cornerPoints
                                val elementFrame = element.boundingBox

                                stringBuilder.append("        ").append(elementText).append(" ").append(elementFrame.toString()).append('\n')

                                for (symbol in element.symbols) {

                                    stringBuilder.append("            ").append('\n')

                                    val symbolText = symbol.text
                                    val symbolCornerPoints = symbol.cornerPoints
                                    val symbolFrame = symbol.boundingBox

                                    stringBuilder.append("            ").append(symbolText).append(" ").append(symbolFrame.toString()).append('\n')

                                }
                            }
                        }
                    }*/

                    suspendCoroutine.resume(recognizedText.text)
                }
                .addOnFailureListener {
                    suspendCoroutine.resumeWithException(it)
                }

        }
    }

    suspend fun performLocalOCRWithBoundingBoxes(bitmap: Bitmap): Pair<String?, List<WordWithBoundingBox>> {
        val imageHeight: Int = bitmap.height
        val imageWidth: Int = bitmap.width

        return suspendCoroutine { suspendCoroutine ->

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { recognizedText ->
                    val results = mutableListOf<WordWithBoundingBox>()

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
                                results.add(WordWithBoundingBox(elementText, boundingBox))
                            }
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