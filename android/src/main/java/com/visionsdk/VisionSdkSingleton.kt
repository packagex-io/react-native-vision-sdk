package com.visionsdk

import android.content.Context
import android.util.Log
import io.packagex.visionsdk.Environment
import io.packagex.visionsdk.VisionSDK

object VisionSdkSingleton{
  private var isInitialized = false
  private var currentEnvironment: Environment? = null

  fun initializeSdk(context: Context, environment: Environment){
    if(!isInitialized || currentEnvironment != environment){
      VisionSDK.getInstance().initialize(context, environment)
      isInitialized = true
      currentEnvironment = environment
      Log.d("IJS", "\uD83C\uDF0D VisionSDK Initialized with Environment: $environment")
    } else {
      Log.d("IJS", "✅ VisionSDK already initialized with Environment: $environment - Skipping reinitialization")
    }
  }
}
