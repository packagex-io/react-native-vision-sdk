package io.packagex.visionsdk.ocr.ml.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.ocr.ml.core.onnx.VisionOrtSession
import io.packagex.visionsdk.ocr.ml.enums.ExecutionProvider
import io.packagex.visionsdk.ocr.ml.process.LocationProcessor
import io.packagex.visionsdk.ocr.ml.process.sl.large.SLLargeModel
import io.packagex.visionsdk.ocr.ml.process.sl.micro.SLMicroModel
import io.packagex.visionsdk.preferences.VisionSDKSettings
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimedValue

class OnDeviceOCRManager(
    private val context: Context,
    private val platformType: PlatformType = PlatformType.Native,
    private val modelClass: ModelClass,
    private val modelSize: ModelSize = ModelSize.Micro,
) {

    private val locationProcessor by lazy { LocationProcessor(context) }

    private val slModel by lazy {
        when (modelClass) {
            ModelClass.ShippingLabel -> {
                when (modelSize) {
                    ModelSize.Nano -> TODO()
                    ModelSize.Micro -> SLMicroModel(context, locationProcessor)
                    ModelSize.Small -> TODO()
                    ModelSize.Medium -> TODO()
                    ModelSize.Large -> SLLargeModel(context, locationProcessor)
                    ModelSize.XLarge -> TODO()
                }
            }
            ModelClass.BillOfLading -> TODO()
            ModelClass.PriceTag -> TODO()
        }
    }

    fun isConfigured() = VisionOrtSession.isConfigured()

    fun isModelAlreadyDownloaded() = slModel.isModelAlreadyDownloaded()

    suspend fun configure(
        apiKey: String? = null,
        token: String? = null,
        executionProvider: ExecutionProvider = ExecutionProvider.NNAPI,
        progressListener: ((Float) -> Unit)? = null
    ) {
        try {
            locationProcessor.init()
            slModel.configure(context, apiKey, token, platformType, executionProvider, progressListener)
        } catch (e: Exception) {
            addTelemetryData(
                action = "shipping_label_extraction",
                extractionDuration = 0L,
                e = e
            )
            throw VisionSDKException.UnknownException(e)
        }
    }

    suspend fun getPredictions(
        bitmap: Bitmap,
        barcodes: List<String>
    ): String {

        if (isConfigured().not()) throw VisionSDKException.OnDeviceOCRManagerNotConfigured

        return try {

            val (result, duration) = measureTimedValueWithException {
                slModel.getPredictions(bitmap, barcodes)
            }
            VisionSDKSettings.onDeviceModelUsageIncrement(modelClass, modelSize, duration.inWholeMilliseconds)
            result
        } catch (e: TimedException) {
            addTelemetryData(
                action = "shipping_label_extraction",
                extractionDuration = e.exceptionDuration.inWholeMilliseconds,
                e = e
            )
            throw VisionSDKException.UnknownException(e.cause)
        }
    }

    fun permanentlyDeleteGivenModel() {
        slModel.permanentlyDeleteGivenModel()
    }

    fun permanentlyDeleteAllModels() {
        slModel.permanentlyDeleteAllModels()
    }

    fun destroy() {
        slModel.invalidateModel()
        locationProcessor.destroy()
    }

    private fun addTelemetryData(action: String, extractionDuration: Long, e: Exception) {

        /*val modelId = VisionSdkSettings.getModelId(modelClass, modelSize) ?: return
        val modelVersionId = VisionSdkSettings.getModelVersionId(modelClass, modelSize) ?: return

        VisionSdkSettings.addTelemetryData(
            TelemetryData(
                action = action,
                actionPerformedAt = Date().isoFormat(),
                modelInfo = ModelInfo(
                    modelId = modelId,
                    ModelVersion(modelVersionId)
                ),
                extractionTimeInMillis = extractionDuration,
                error = e.stackTraceToString(),
            )
        )*/
    }
}

internal inline fun <T> measureTimedValueWithException(block: () -> T): TimedValue<T> {
    val startTime = System.currentTimeMillis()
    try {
        val result = block()
        val totalTime = System.currentTimeMillis() - startTime
        return TimedValue(result, totalTime.milliseconds)
    } catch (e: Exception) {
        val totalTime = System.currentTimeMillis() - startTime
        throw TimedException(totalTime.milliseconds, e)
    }
}

data class TimedException(val exceptionDuration: Duration, override val cause: Throwable) : Exception(cause)