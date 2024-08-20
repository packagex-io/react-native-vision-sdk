package io.packagex.visionsdk.utils

import android.os.Build

internal const val TAG = "VisionSDK"

internal const val WORKER_PARAM_MODEL_CLASS = "MODEL_CLASS"
internal const val WORKER_PARAM_MODEL_SIZE = "MODEL_SIZE"

fun isEmulator() = (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.HARDWARE.contains("goldfish")
        || Build.HARDWARE.contains("ranchu")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || Build.PRODUCT.contains("sdk_google")
        || Build.PRODUCT.contains("google_sdk")
        || Build.PRODUCT.contains("sdk")
        || Build.PRODUCT.contains("sdk_x86")
        || Build.PRODUCT.contains("sdk_gphone64_arm64")
        || Build.PRODUCT.contains("vbox86p")
        || Build.PRODUCT.contains("emulator")
        || Build.PRODUCT.contains("simulator")