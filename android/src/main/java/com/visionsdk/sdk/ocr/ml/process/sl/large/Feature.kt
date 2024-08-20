package io.packagex.visionsdk.ocr.ml.process.sl.large

import java.nio.FloatBuffer

internal class Feature(
    val inputIds: LongArray,
    val inputMask: LongArray,
    val inputBboxs: LongArray,
    val inputPixelValues: FloatBuffer,
    val origTokens: List<String>,
)