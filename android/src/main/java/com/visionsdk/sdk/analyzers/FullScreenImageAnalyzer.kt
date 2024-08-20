package io.packagex.visionsdk.analyzers

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import io.packagex.visionsdk.utils.TAG

internal class FullScreenImageAnalyzer(
    private val imageScanner: ImageScanner
) : NthFrameAnalyzer() {

    var lastNthFrameValue = -1

    init {
        imageScanner.setImageScannerLifecycleListener( object : ImageScanner.ImageScannerLifecycleListener() {
            override fun onScanningStarted() {
                lastNthFrameValue = nthFrame
                analyzeEveryFrame()
            }

            override fun onScanningEnded() {
                nthFrame = lastNthFrameValue
            }
        })
    }

    @ExperimentalGetImage
    override fun analyzeImageProxy(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image

        if (mediaImage != null) {

            /*val formatInString = when (mediaImage.format) {
                ImageFormat.NV21 -> "NV21"
                ImageFormat.JPEG -> "JPEG"
                ImageFormat.JPEG_R -> "JPEG_R"
                ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"
                ImageFormat.Y8 -> "Y8"
                ImageFormat.HEIC -> "HEIC"
                ImageFormat.NV16 -> "NV16"
                ImageFormat.DEPTH16 -> "DEPTH16"
                ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD"
                ImageFormat.FLEX_RGBA_8888 -> "FLEX_RGBA_8888"
                ImageFormat.FLEX_RGB_888 -> "FLEX_RGB_888"
                ImageFormat.PRIVATE -> "PRIVATE"
                ImageFormat.RAW10 -> "RAW10"
                ImageFormat.RAW12 -> "RAW12"
                ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
                ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
                ImageFormat.RGB_565 -> "RGB_565"
                ImageFormat.UNKNOWN -> "UNKNOWN"
                ImageFormat.YV12 -> "YV12"
                ImageFormat.YUY2 -> "YUY2"
                ImageFormat.YUV_444_888 -> "YUV_444_888"
                ImageFormat.YUV_422_888 -> "YUV_422_888"
                ImageFormat.YUV_420_888 -> "YUV_420_888"
                ImageFormat.YCBCR_P010 -> "YCBCR_P010"
                else -> throw IllegalStateException("No format was detected")
            }

            Log.d(TAG, "Frame format: $formatInString: ${mediaImage.format}")*/

            val croppedImageByteArray = cropYUV420Image(mediaImage, imageProxy.cropRect)

            val inputImage = InputImage.fromByteArray(
                croppedImageByteArray,
                imageProxy.cropRect.width(),
                imageProxy.cropRect.height(),
                imageProxy.imageInfo.rotationDegrees,
                InputImage.IMAGE_FORMAT_NV21
            )

            imageScanner.scanFrame(imageProxy, inputImage)
        }
    }

    private fun cropYUV420Image(image: Image, cropRect: Rect): ByteArray {
        val format = image.format
        if (format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("Unsupported image format: $format")
        }

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yStride = yPlane.rowStride
        val uStride = uPlane.rowStride
        val vStride = vPlane.rowStride

        val yPixelStride = yPlane.pixelStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride

        val cropWidth = cropRect.width()
        val cropHeight = cropRect.height()

        val croppedY = ByteArray(cropWidth * cropHeight)
        val croppedU = ByteArray(cropWidth * cropHeight / 4)
        val croppedV = ByteArray(cropWidth * cropHeight / 4)

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        for (row in 0 until cropHeight) {
            for (col in 0 until cropWidth) {
                croppedY[row * cropWidth + col] =
                    yBuffer.get((row + cropRect.top) * yStride + (col + cropRect.left) * yPixelStride)
            }
        }

        for (row in 0 until cropHeight / 2) {
            for (col in 0 until cropWidth / 2) {
                croppedU[row * (cropWidth / 2) + col] =
                    uBuffer.get((row + cropRect.top / 2) * uStride + (col + cropRect.left / 2) * uPixelStride)
                croppedV[row * (cropWidth / 2) + col] =
                    vBuffer.get((row + cropRect.top / 2) * vStride + (col + cropRect.left / 2) * vPixelStride)
            }
        }

        return createImageFromPlanes(croppedY, croppedU, croppedV, cropWidth, cropHeight)
    }

    private fun createImageFromPlanes(
        croppedY: ByteArray,
        croppedU: ByteArray,
        croppedV: ByteArray,
        cropWidth: Int,
        cropHeight: Int
    ): ByteArray {
        val ySize = cropWidth * cropHeight
        val uvSize = cropWidth * cropHeight / 4

        val yuvBytes = ByteArray(ySize + 2 * uvSize)

        System.arraycopy(croppedY, 0, yuvBytes, 0, ySize)
        System.arraycopy(croppedU, 0, yuvBytes, ySize, uvSize)
        System.arraycopy(croppedV, 0, yuvBytes, ySize + uvSize, uvSize)

        return yuvBytes
    }
}
