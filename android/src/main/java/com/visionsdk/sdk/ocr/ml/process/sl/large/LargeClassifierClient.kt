package io.packagex.visionsdk.ocr.ml.process.sl.large

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import io.packagex.visionsdk.ocr.ml.dto.MLResults
import io.packagex.visionsdk.ocr.ml.process.sl.micro.FeatureConverter.Companion.MAX_TOKENS
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.ocr.ml.process.sl.ClassifierClient
import java.nio.LongBuffer

internal class LargeClassifierClient: ClassifierClient() {

    suspend fun predict(
        vocabulary: Map<String, Long>,
        mergesList: List<SLLargeModel.MergesData>,
        ortEnvironment: OrtEnvironment,
        ortSession: OrtSession,
        locationProcessor: LocationProcessor,
        image: Bitmap,
        textArray: List<String>,
        boundingBoxes: List<List<Int>>,
        lineNumbers: List<Int>
    ): MLResults {

        val features = FeatureConverter(vocabulary, mergesList).convert(textArray, boundingBoxes, image)

        val inputIdsBuffer = LongBuffer.wrap(features.inputIds)
        val attentionMaskBuffer = LongBuffer.wrap(features.inputMask)
        val boundingBoxesBuffer = LongBuffer.wrap(features.inputBboxs)
        val pixelValuesBuffer = features.inputPixelValues

        val inputIdsTensor = OnnxTensor.createTensor(
            ortEnvironment,
            inputIdsBuffer,
            longArrayOf(1, MAX_TOKENS.toLong())
        )
        val attentionMaskTensor = OnnxTensor.createTensor(
            ortEnvironment,
            attentionMaskBuffer,
            longArrayOf(1, MAX_TOKENS.toLong())
        )
        val boundingBoxesTensor = OnnxTensor.createTensor(
            ortEnvironment,
            boundingBoxesBuffer,
            longArrayOf(1, MAX_TOKENS.toLong(), 4)
        )
        val pixelValuesTensor = OnnxTensor.createTensor(
            ortEnvironment,
            pixelValuesBuffer,
            longArrayOf(1, 3, 224, 224)
        )

        val outputArray: Array<Array<FloatArray>>

        inputIdsTensor.use {
            attentionMaskTensor.use {
                boundingBoxesTensor.use {
                    pixelValuesTensor.use {
                        val inputMap = mapOf(
                            "input_ids" to inputIdsTensor,
                            "attention_mask" to attentionMaskTensor,
                            "bbox" to boundingBoxesTensor,
                            "pixel_values" to pixelValuesTensor
                        )
                        val output = ortSession.run(inputMap)
                        output.use {
                            outputArray = (output?.get(0)?.value as Array<Array<FloatArray>>)
                        }
                    }
                }
            }
        }

        val predictions = mutableListOf<Int>()
        val predictionsInString = mutableListOf<String>()

        for (i in 0 until  features.origTokens.size) {

            // We are adding 1 to the index as we want to skip the prediction for <s> token at the start.
            // For details, consult Salman Maqbool.
            val predictionArrayForToken = outputArray[0][i + 1]

            val highestIndex = getIndexOfHighestValue(predictionArrayForToken)
            predictions.add(highestIndex)
            predictionsInString.add(indexLabels[highestIndex])
        }

        val wordsWithPredictions = getWordsWithPredictions(features.origTokens, predictions)

        return convertDataIntoMLResult(wordsWithPredictions, locationProcessor, lineNumbers)
    }

    private fun getWordsWithPredictions(
        tokens: List<String>, predictions: List<Int>
    ): List<Pair<String, Int>> {

        val wordsWithPredictions = mutableListOf<Pair<String, Int>>()

        var firstCursor = 0
        var secondCursor = 1

        while (firstCursor < tokens.size) {

            while (true) {
                if (secondCursor < tokens.size) {
                    val token = tokens[secondCursor]
                    if (token.startsWith("Ġ").not()) {
                        secondCursor++
                        continue
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            val prediction = predictions[firstCursor]

            val wordBuilder = StringBuilder()
            while (firstCursor < secondCursor) {
                wordBuilder.append(tokens[firstCursor].replace("Ġ", ""))
                firstCursor++
            }

            val word = wordBuilder.toString()

            wordsWithPredictions.add(Pair(word, prediction))

            secondCursor++
        }

        return wordsWithPredictions
    }
}
