import Foundation
import CoreML
import VisionSDK
import VisionSDKDimensioning

// MARK: - DimensioningModule
//
// Swift implementation of the DimensioningModule TurboModule.
// Instantiated lazily by DimensioningModuleTurboModule.mm via NSClassFromString.
//
// iOS-only. As of VisionSDK 2.7.0 the VSDK-prefixed dimensioning wrapper is gone
// and `import VisionSDKDimensioning` re-exports MVDimensioning's own API, so
// these call the vendor types directly (`Dimensioning`, not `VSDKDimensioning`).

@objc(DimensioningModule)
class DimensioningModule: NSObject {

  // MARK: - deviceCapabilities
  @objc func deviceCapabilities(
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    let caps: DimensioningCapabilities = Dimensioning.deviceCapabilities()
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
  //
  // MVDimensioning exposes no prefetch entry point of its own, so warm the
  // bundled models here. They ship unencrypted inside MVDimensioningCore's
  // resource bundle as of 2.7.0 — no .mlmodelkey, no ModelKeyServerService
  // round-trip, no team-ID gate — so this is a directory walk plus a compile,
  // with no network involved. Cheap and idempotent after the first launch;
  // skipping it just means the first capture pays the Core ML compile cost.
  @objc func prefetchModels(
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    Task {
      let bundleURL = Bundle(for: DimensioningSession.self).bundleURL
        .appendingPathComponent("MVDimensioningCore_MVDimensioningCore.bundle")
      let modelURLs = (try? FileManager.default.contentsOfDirectory(
        at: bundleURL, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles]
      ))?.filter { $0.pathExtension == "mlmodelc" } ?? []

      await withTaskGroup(of: Void.self) { group in
        for url in modelURLs {
          group.addTask { _ = try? await MLModel.load(contentsOf: url) }
        }
      }
      resolver(nil)
    }
  }
}
