package io.packagex.visionsdk.config

data class CameraSettings(

    /**
     * If you don't want to process every frame to save resources, you can set an nth frame
     * using this function. So let say you pass 10 in this function. This will mean that every
     * 10th frame will be processed and the rest of the frame will be skipped from processing.
     *
     * Its default value is 10.
     *
     * If you want to process every frame, you should set it to -1.
     */
    val nthFrameToProcess: Int = 10,
)