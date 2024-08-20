package io.packagex.visionsdk.ocr.ml.core.onnx

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import io.packagex.visionsdk.ocr.ml.enums.ExecutionProvider
import java.io.File

/**
 * This class is basically a wrapper for OrtEnvironment and OrtSession objects.
 * We want to make sure that, at a given time, only one of the models is loaded
 * in memory. That is why this class is made a Kotlin singleton object.
 */
internal object VisionOrtSession {
    var ortEnv: OrtEnvironment? = null
    var ortSession: OrtSession? = null

    fun initiate(modelFile: File, executionProvider: ExecutionProvider) {
        val sessionOptions = OrtSession.SessionOptions()
        when (executionProvider) {
            ExecutionProvider.CPU -> {}
            ExecutionProvider.NNAPI -> {
                sessionOptions.addNnapi()
            }

            ExecutionProvider.XNNPACK -> {
                sessionOptions.addXnnpack(emptyMap())
            }
        }
        ortEnv = OrtEnvironment.getEnvironment()
        ortSession = ortEnv!!.createSession(modelFile.absolutePath, sessionOptions)
    }

    fun isConfigured() = ortEnv != null && ortSession != null

    fun close() {
        ortEnv?.close()
        ortEnv = null
        ortSession?.close()
        ortSession = null
    }
}