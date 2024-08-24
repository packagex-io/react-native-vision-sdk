package io.packagex.visionsdk.ocr.ml.process.sl.large

import android.graphics.Bitmap
import io.packagex.visionsdk.ocr.ml.process.sl.large.tokenization.Tokenizer
import java.nio.FloatBuffer
import kotlin.math.min

internal class FeatureConverter(
    encoder: Map<String, Long>,
    bpeRanks: List<SLLargeModel.MergesData>
) {

    companion object {
        const val MAX_TOKENS = 256
        const val DIM_BATCH_SIZE = 1
        const val DIM_PIXEL_SIZE = 3
        const val IMAGE_SIZE_X = 224
        const val IMAGE_SIZE_Y = 224
    }

    private val tokenizer: Tokenizer = Tokenizer(encoder, bpeRanks)

    fun convert(textArray: List<String>, boundingBoxes: List<List<Int>>, image: Bitmap): Feature {

        val tokenizerOutput = tokenizer.encodeWithBoundingBoxes(textArray, boundingBoxes)

        val inputIds = mutableListOf<Long>(0)
        val origTokens = mutableListOf<String>()
        val inputBboxs = mutableListOf<Long>(0, 0, 0, 0)

        for (i in 0 until min(tokenizerOutput.size, MAX_TOKENS - 2)) {
            inputIds.add(tokenizerOutput[i].tokenId)
            origTokens.add(tokenizerOutput[i].token)
            for (j in 0..3) {
                inputBboxs.add(tokenizerOutput[i].boundingBox[j].toLong())
            }
        }

        inputIds.add(2)
        inputBboxs.add(0)
        inputBboxs.add(0)
        inputBboxs.add(0)
        inputBboxs.add(0)

        val inputMask = LongArray(inputIds.size) { 1 }.toMutableList()

        while (inputIds.size < MAX_TOKENS) {
            inputIds.add(1)
            inputMask.add(0)
            inputBboxs.add(0)
            inputBboxs.add(0)
            inputBboxs.add(0)
            inputBboxs.add(0)
        }

        val inputPixelValues = preprocessImage(image)

        return Feature(
            inputIds = inputIds.toLongArray(),
            inputMask = inputMask.toLongArray(),
            inputBboxs = inputBboxs.toLongArray(),
            inputPixelValues = inputPixelValues,
            origTokens = origTokens
        )
    }

    private fun preProcess(bitmap: Bitmap): FloatBuffer {
        val imgData = FloatBuffer.allocate(
            DIM_BATCH_SIZE
                    * DIM_PIXEL_SIZE
                    * IMAGE_SIZE_X
                    * IMAGE_SIZE_Y
        )
        imgData.rewind()
        val stride = IMAGE_SIZE_X * IMAGE_SIZE_Y
        val bmpData = IntArray(stride)
        bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in 0..<IMAGE_SIZE_X) {
            for (j in 0..<IMAGE_SIZE_Y) {
                val idx = IMAGE_SIZE_Y * i + j
                val pixelValue = bmpData[idx]
                imgData.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - 0.5f) / 0.5f))
                imgData.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - 0.5f) / 0.5f))
                imgData.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - 0.5f) / 0.5f))
            }
        }

        imgData.rewind()
        return imgData
    }

    private fun preprocessImage(image: Bitmap): FloatBuffer {
        val scaledImage = Bitmap.createScaledBitmap(image, 224, 224, false)
        val imgData = preProcess(scaledImage)

        return imgData
    }
}