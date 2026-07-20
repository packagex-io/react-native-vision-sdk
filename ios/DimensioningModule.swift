import Foundation
import VisionSDK
import VisionSDKDimensioning

// MARK: - DimensioningModule
//
// Swift implementation of the DimensioningModule TurboModule.
// Instantiated lazily by DimensioningModuleTurboModule.mm via NSClassFromString.
// iOS-only: both methods delegate to VSDKDimensioning static helpers.

@objc(DimensioningModule)
class DimensioningModule: NSObject {

  // MARK: - deviceCapabilities
  @objc func deviceCapabilities(
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    let caps = VSDKDimensioning.deviceCapabilities()
    let dict: [String: Any] = [
      "lidar": caps.lidar,
      "arWorldTracking": caps.arWorldTracking,
      "sceneReconstruction": caps.sceneReconstruction
    ]
    guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
          let jsonString = String(data: jsonData, encoding: .utf8) else {
      rejecter("SERIALIZATION_ERROR", "Failed to serialise capabilities", nil)
      return
    }
    resolver(jsonString)
  }

  // MARK: - prefetchModels
  @objc func prefetchModels(
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      await VSDKDimensioning.prefetchModels()
      resolver(nil)
    }
  }
}
