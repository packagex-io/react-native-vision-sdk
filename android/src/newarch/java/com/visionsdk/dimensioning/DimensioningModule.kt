package com.visionsdk.dimensioning

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule

/**
 * Android stub for the Dimensioning TurboModule.
 *
 * Android devices do not have LiDAR scanners, so:
 * - [deviceCapabilities] always resolves with all capability flags `false`.
 * - [prefetchModels] resolves immediately (no-op).
 * - The DimensioningView renders a static "not supported" placeholder via
 *   [DimensioningViewManager].
 */
@ReactModule(name = DimensioningModule.NAME)
class DimensioningModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "DimensioningModule"
    }

    override fun getName(): String = NAME

    /**
     * Returns device capability flags for dimensioning.
     * Android always returns `{ lidar: false, arWorldTracking: false, sceneReconstruction: false }`.
     */
    @ReactMethod
    fun deviceCapabilities(promise: Promise) {
        promise.resolve(
            """{"lidar":false,"arWorldTracking":false,"sceneReconstruction":false}"""
        )
    }

    /**
     * Pre-warms dimensioning ML models. No-op on Android.
     */
    @ReactMethod
    fun prefetchModels(promise: Promise) {
        promise.resolve(null)
    }

    // Required by RN event emitter protocol even though no events are emitted.
    @ReactMethod
    fun addListener(eventName: String) {
        // no-op
    }

    @ReactMethod
    fun removeListeners(count: Double) {
        // no-op
    }
}
