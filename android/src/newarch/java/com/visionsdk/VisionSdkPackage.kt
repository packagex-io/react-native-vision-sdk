package com.visionsdk

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.visionsdk.dimensioning.DimensioningModule
import com.visionsdk.dimensioning.DimensioningViewManager

/**
 * New Architecture Package
 *
 * In Fabric (New Architecture), both TurboModules and ViewManagers
 * need to be manually registered via this Package class.
 */
class VisionSdkPackage : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    // TurboModules still need manual registration in Fabric
    return listOf(
      VisionSdkModule(reactContext),
      DimensioningModule(reactContext),
    )
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return listOf(
      VisionCameraViewManager(reactContext),
      DimensioningViewManager(reactContext),
    )
  }
}
