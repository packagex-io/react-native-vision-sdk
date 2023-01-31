import VisionSDK

@objc(VisionSdkViewManager)
class VisionSdkViewManager: RCTViewManager {

  @objc func captureImage(_ node: NSNumber) {

    DispatchQueue.main.async {
      let component =
        self.bridge.uiManager.view(
          forReactTag: node
        ) as! RNCodeScannerView

      component.codeScannerView!.capturePhoto()

    }

  }

  override func view() -> UIView! {

    return RNCodeScannerView()

  }

  override static func requiresMainQueueSetup() -> Bool {
    return true
  }
}