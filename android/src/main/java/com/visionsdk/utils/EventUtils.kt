package com.visionsdk.utils

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule

object EventUtils {
    fun sendModelDownloadProgressEvent(
      reactContext: ReactApplicationContext,
      progress: Double = 0.5,
      downloadStatus: Boolean = false,
      isReady: Boolean = false
    ) {
      val event = Arguments.createMap().apply {
        putDouble("progress", progress)
        putBoolean("downloadStatus", downloadStatus)
        putBoolean("isReady", isReady)
      }
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onModelDownloadProgress", event)
    }
}
