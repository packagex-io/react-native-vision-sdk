package io.packagex.visionsdk.ocr.security

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import io.packagex.visionsdk.ocr.ml.core.ModelClass
import io.packagex.visionsdk.ocr.ml.core.ModelSize
import io.packagex.visionsdk.ocr.ml.core.OnDeviceOCRManager
import io.packagex.visionsdk.utils.TAG
import io.packagex.visionsdk.utils.WORKER_PARAM_MODEL_CLASS
import io.packagex.visionsdk.utils.WORKER_PARAM_MODEL_SIZE

internal class SecurityWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        Log.d(TAG, "Executing SecurityWorker...")
        val modelClass = ModelClass.valueOf(inputData.getString(WORKER_PARAM_MODEL_CLASS) ?: return Result.failure())
        val modelSize = ModelSize.valueOf(inputData.getString(WORKER_PARAM_MODEL_SIZE) ?: return Result.failure())
        OnDeviceOCRManager(
            context = applicationContext,
            modelClass = modelClass,
            modelSize = modelSize
        ).permanentlyDeleteGivenModel()
        Log.d(TAG, "Executing SecurityWorker successful.")
        return Result.success()
    }
}