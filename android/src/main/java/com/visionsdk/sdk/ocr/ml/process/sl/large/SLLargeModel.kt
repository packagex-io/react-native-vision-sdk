package io.packagex.visionsdk.ocr.ml.process.sl.large

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.JsonReader
import android.util.Log
import com.asadullah.handyutils.ifNeitherNullNorEmptyNorBlank
import io.packagex.visionsdk.ocr.LocalOCR
import io.packagex.visionsdk.ocr.courier.LabelsExtraction
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.dto.PredictionResult
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.ocr.ml.process.sl.SLModel
import io.packagex.visionsdk.utils.TAG
import com.asadullah.handyutils.toReadableDuration
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.measureTimedValue

internal class SLLargeModel(context: Context) : SLModel(context) {

    private val vocabularyDictionary = mutableMapOf<String, Long>()
    private val mergesList = mutableListOf<MergesData>()

    override fun getModelClass() = ModelClass.ShippingLabel

    override fun getModelSize() = ModelSize.Large

    override fun loadRequiredData(context: Context) {

        Log.d(TAG, "Loading vocabulary in memory...")
        val vocabStream = context.assets.open("sl/large/vocab.json")
        vocabStream.use {
            val vocabReader = JsonReader(InputStreamReader(it, "UTF-8"))
            vocabReader.beginObject()
            while (vocabReader.hasNext()) {
                val key = vocabReader.nextName()
                val value = vocabReader.nextLong()
                vocabularyDictionary[key] = value
            }
            vocabReader.close()
        }
        Log.d(TAG, "Loading vocabulary in memory successful.")

        Log.d(TAG, "Loading merges in memory...")
        val mergesStream = context.assets.open("sl/large/merges.txt")
        mergesStream.use { stream ->
            val mergesReader = BufferedReader(InputStreamReader(stream))
            mergesReader.useLines { seq ->
                seq.drop(1).forEachIndexed { i, s ->
                    val list = s.split(" ")
                    mergesList.add(
                        MergesData(
                            first = list[0],
                            second = list[1],
                            index = i
                        )
                    )
                }
            }
        }
        Log.d(TAG, "Loading merges in memory successful.")
    }

    override suspend fun predict(ortEnvironment: OrtEnvironment, ortSession: OrtSession, locationProcessor: LocationProcessor, bitmap: Bitmap, barcodes: List<String>): PredictionResult {

        check(vocabularyDictionary.isNotEmpty()) { "Vocabulary was not loaded." }

        check(mergesList.isNotEmpty()) { "Merges was not loaded." }

        val (result, localOCRTime) = measureTimedValue { LocalOCR().performLocalOCRWithBoundingBoxes(bitmap) }
        Log.d(TAG, "Local OCR took ${localOCRTime.toReadableDuration()}.")

        val (ocrExtractedText, wordsWithBoundingBoxesAndLineNumbers) = result

        val (regexResult, regexDuration) = measureTimedValue {
            regexProcessing(barcodes, ocrExtractedText)
        }
        Log.d(TAG, "Regex processing took ${regexDuration.toReadableDuration()}.")

        val textArray = mutableListOf<String>()
        val boundingBoxes = mutableListOf<List<Int>>()
        val lineNumbers = mutableListOf<Int>()

        wordsWithBoundingBoxesAndLineNumbers.forEach { item ->
            textArray.add(item.word)
            boundingBoxes.add(item.boundingBox)
            lineNumbers.add(item.lineNumber)
        }

        val (mlResults, mlProcessDuration) = measureTimedValue {
            LargeClassifierClient().predict(
                vocabulary = vocabularyDictionary,
                mergesList = mergesList,
                ortEnvironment = ortEnvironment,
                ortSession = ortSession,
                locationProcessor = locationProcessor,
                image = bitmap,
                textArray = textArray,
                boundingBoxes = boundingBoxes,
                lineNumbers = lineNumbers
            )
        }

        Log.d(TAG, "ML processing took ${mlProcessDuration.toReadableDuration()}.")

        return PredictionResult(
            ocrExtractedText = ocrExtractedText,
            regexResult = regexResult,
            mlResults = mlResults
        )
    }

    override fun cleanUp() {
        vocabularyDictionary.clear()
    }

    data class MergesData(
        val first: String,
        val second: String,
        val index: Int
    )
}