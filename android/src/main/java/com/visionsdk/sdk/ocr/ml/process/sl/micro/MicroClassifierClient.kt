package io.packagex.visionsdk.ocr.ml.process.sl.micro

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import io.packagex.visionsdk.ocr.ml.dto.MLResults
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.ocr.ml.process.sl.ClassifierClient
import io.packagex.visionsdk.ocr.ml.process.sl.micro.FeatureConverter.Companion.MAX_TOKENS
import java.nio.LongBuffer

internal class MicroClassifierClient: ClassifierClient() {

    suspend fun predict(
        vocabulary: Map<String, Long>,
        ortEnvironment: OrtEnvironment,
        ortSession: OrtSession,
        locationProcessor: LocationProcessor,
        textArray: List<String>,
        lineNumbers: List<Int>
    ): MLResults {

        val features = FeatureConverter(vocabulary).convert(textArray)

        val inputIdsBuffer = LongBuffer.wrap(features.inputIds)
        val attentionMaskBuffer = LongBuffer.wrap(features.inputMask)

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

        val outputArray: Array<Array<FloatArray>>

        inputIdsTensor.use {
            attentionMaskTensor.use {
                val inputMap = mapOf(
                    "input_ids" to inputIdsTensor,
                    "attention_mask" to attentionMaskTensor
                )
                val output = ortSession.run(inputMap)
                output.use {
                    outputArray = (output?.get(0)?.value as Array<Array<FloatArray>>)
                }
            }
        }

        val predictions = mutableListOf<Int>()
        val predictionsInString = mutableListOf<String>()
        outputArray[0].forEach {
            val index = getIndexOfHighestValue(it)
            predictions.add(index)
            predictionsInString.add(indexLabels[index])
        }

        val allPredictionsWithoutThePredictionsOfCLSAndSEP = predictions.subList(1, features.origTokens.size - 1)

        val wordsWithPredictions = getWordsWithPredictions(allPredictionsWithoutThePredictionsOfCLSAndSEP, textArray, features.subWordMap)

        return convertDataIntoMLResult(wordsWithPredictions, locationProcessor, lineNumbers)
    }

    private fun getWordsWithPredictions(
        predictions: List<Int>,
        textArray: List<String>,
        subWordMap: List<Int>
    ): List<Pair<String, Int>> {
        val wordsWithPredictions = mutableListOf<Pair<String, Int>>()

        predictions.withIndex()
            .zip(subWordMap)
            .forEach { (pair, subWordIndex) ->
                val (index, prediction) = pair
                if (index > 0 && subWordIndex == subWordMap[index - 1]) {
                    return@forEach
                } else {
                    wordsWithPredictions.add(Pair(textArray[subWordIndex], prediction))
                }
            }
        return  wordsWithPredictions
    }
}