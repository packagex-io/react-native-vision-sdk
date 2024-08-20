package io.packagex.visionsdk

import android.app.Application
import io.packagex.visionsdk.preferences.VisionSdkSettings

internal class VisionSdkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        VisionSdkSettings.initialize(this)
    }
}