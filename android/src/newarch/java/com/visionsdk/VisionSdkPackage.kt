package com.visionsdk

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

/**
 * New Architecture Package
 *
 * In Fabric (New Architecture), TurboModules are auto-registered by Codegen,
 * but ViewManagers still need to be manually registered.
 */
class VisionSdkPackage : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    // TurboModules are auto-registered by Codegen in New Architecture
    // We still need to return the module here for backward compatibility during transition
    return listOf(VisionSdkModule(reactContext))
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    // Fabric ViewManagers
    return listOf(
      VisionSdkViewManager(reactContext),
      VisionCameraViewManager(reactContext)
    )
  }
}
