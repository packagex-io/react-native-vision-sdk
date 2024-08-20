package io.packagex.visionsdk.ocr.ml.core

import android.content.Context
import android.graphics.Bitmap
import io.packagex.visionsdk.ocr.ml.core.onnx.VisionOrtSession
import io.packagex.visionsdk.ocr.ml.enums.ExecutionProvider
import io.packagex.visionsdk.ocr.ml.process.sl.large.SLLargeModel
import io.packagex.visionsdk.ocr.ml.process.sl.micro.SLMicroModel

class OnDeviceOCRManager(
    private val context: Context,
    private val platformType: PlatformType = PlatformType.Native,
    private val modelClass: ModelClass,
    private val modelSize: ModelSize = ModelSize.Micro,
) {

    private val slModel by lazy {
        when (modelClass) {
            ModelClass.ShippingLabel -> {
                when (modelSize) {
                    ModelSize.Nano -> TODO()
                    ModelSize.Micro -> SLMicroModel(context)
                    ModelSize.Small -> TODO()
                    ModelSize.Medium -> TODO()
                    ModelSize.Large -> SLLargeModel(context)
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
        executionProvider: ExecutionProvider = ExecutionProvider.NNAPI,
        progressListener: ((Float) -> Unit)? = null
    ) {
        slModel.configure(context, platformType, executionProvider, progressListener)
    }

    suspend fun getPredictions(
        bitmap: Bitmap,
        barcodes: List<String>
    ): String {

        assert(isConfigured()) { "You need to call function configure first." }

        return slModel.getPredictions(context, bitmap, barcodes)
    }

    fun permanentlyDeleteGivenModel() {
        slModel.permanentlyDeleteGivenModel()
    }

    fun permanentlyDeleteAllModels() {
        slModel.permanentlyDeleteAllModels()
    }

    fun destroy() {
        slModel.invalidateModel()
    }
}