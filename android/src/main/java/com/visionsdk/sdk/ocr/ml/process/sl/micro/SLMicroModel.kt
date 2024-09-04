package io.packagex.visionsdk.ocr.ml.process.sl.micro

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
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
import io.packagex.visionsdk.exceptions.VisionSDKException
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.measureTimedValue

internal class SLMicroModel(context: Context, locationProcessor: LocationProcessor) : SLModel(context, locationProcessor) {

    private val vocabularyDictionary = mutableMapOf<String, Long>()

    override fun getModelClass() = ModelClass.ShippingLabel

    override fun getModelSize() = ModelSize.Micro

    override fun loadRequiredData(context: Context) {

        Log.d(TAG, "Loading vocabulary in memory...")
        val vocabFile = context.assets.open("sl/micro/vocab.txt")
        vocabFile.use {
            val bufferReader = BufferedReader(InputStreamReader(it))
            var index = 0L
            while (bufferReader.ready()) {
                val word = bufferReader.readLine()
                vocabularyDictionary[word] = index++
            }
        }
        Log.d(TAG, "Loading vocabulary in memory successful.")
    }

    override suspend fun predict(ortEnvironment: OrtEnvironment, ortSession: OrtSession, locationProcessor: LocationProcessor, bitmap: Bitmap, barcodes: List<String>): PredictionResult {

        if (vocabularyDictionary.isEmpty()) throw VisionSDKException.UnknownException(IllegalStateException("Vocabulary was not loaded."))

        val (result, localOCRTime) = measureTimedValue { LocalOCR().performLocalOCR(bitmap) }
        Log.d(TAG, "Local OCR took ${localOCRTime.toReadableDuration()}.")

        val (ocrExtractedText, wordsWithLineNumbers) = result

        val (regexResult, regexDuration) = measureTimedValue {
            regexProcessing(barcodes, ocrExtractedText)
        }
        Log.d(TAG, "Regex processing took ${regexDuration.toReadableDuration()}.")

        val textArray = mutableListOf<String>()
        val lineNumbers = mutableListOf<Int>()

        wordsWithLineNumbers.forEach { item ->
            textArray.add(item.word)
            lineNumbers.add(item.lineNumber)
        }

        val (mlResults, mlProcessDuration) = measureTimedValue {
            ocrExtractedText?.let {
                MicroClassifierClient().predict(
                    vocabulary = vocabularyDictionary,
                    ortEnvironment = ortEnvironment,
                    ortSession = ortSession,
                    locationProcessor = locationProcessor,
                    textArray = textArray,
                    lineNumbers = lineNumbers
                )
            }
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
}