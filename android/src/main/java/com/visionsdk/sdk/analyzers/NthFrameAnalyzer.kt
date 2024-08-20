package io.packagex.visionsdk.analyzers

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

internal abstract class NthFrameAnalyzer : ImageAnalysis.Analyzer {

    var nthFrame: Int = -1

    private var count = 0

    /**
     * Function to reset the nth frame condition and start analyzing every frame.
     */
    fun analyzeEveryFrame() {
        nthFrame = -1
    }

    override fun analyze(image: ImageProxy) {
        if (nthFrame <= 0 || count % nthFrame == 0) {
            analyzeImageProxy(image)
        } else {
            image.close()
            skipImageProxy()
        }
        count++
        if (count == 100_000) {
            count = 0
        }
    }

    fun analyze(bitmap: Bitmap) {
        if (nthFrame <= 0 || count % nthFrame == 0) {
            analyzeBitmap(bitmap)
        } else {
            skipBitmap(bitmap)
        }
        count++
        if (count == 100_000) {
            count = 0
        }
    }

    protected open fun skipImageProxy() {}
    protected open fun skipBitmap(bitmap: Bitmap) {}

    protected open fun analyzeImageProxy(imageProxy: ImageProxy) {}
    protected open fun analyzeBitmap(bitmap: Bitmap) {}
}