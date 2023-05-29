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

  @objc func stopRunning(_ node: NSNumber) {

    DispatchQueue.main.async {
      let component =
        self.bridge.uiManager.view(
          forReactTag: node
        ) as! RNCodeScannerView

       component.codeScannerView?.stopRunning()
    }

  }



  @objc func startRunning(_ node: NSNumber) {

    DispatchQueue.main.async {
      let component =
        self.bridge.uiManager.view(
          forReactTag: node
        ) as! RNCodeScannerView

       component.codeScannerView?.startRunning()
    }

  }

  @objc func toggleTorch(_ node: NSNumber) {

      DispatchQueue.main.async {
        let component =
          self.bridge.uiManager.view(
            forReactTag: node
          ) as! RNCodeScannerView

          component.setTorchActive()
      }

    }

 


  override func view() -> UIView! {

    return RNCodeScannerView()

  }

  override static func requiresMainQueueSetup() -> Bool {
    return true
  }
}