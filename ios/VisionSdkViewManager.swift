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

 @objc func toggleTorch(_ node: NSNumber, isEnabled: Bool) {
      DispatchQueue.main.async {
        let component =
          self.bridge.uiManager.view(
            forReactTag: node
          ) as! RNCodeScannerView

          component.setTorchActive(isOn: isEnabled)
      }

    }

  @objc func setZoomTo(_ node: NSNumber,zoomValue: NSNumber) {
      let zoomFloatValue = zoomValue.floatValue
      DispatchQueue.main.async {
        let component =
          self.bridge.uiManager.view(
            forReactTag: node
          ) as! RNCodeScannerView

          component.setZoomTo(zoomFloatValue as NSNumber)
      }

    }

@objc func setHeight(_ node: NSNumber, height: NSNumber) {
    DispatchQueue.main.async {
      let component =
        self.bridge.uiManager.view(
          forReactTag: node
        ) as! RNCodeScannerView
        component.setHeight(height as NSNumber)
    }
   }

@objc func setMetaData(_ node: NSNumber, metaData: NSString) {
    DispatchQueue.main.async {
      let component =
        self.bridge.uiManager.view(
          forReactTag: node
        ) as! RNCodeScannerView
        component.setMetaData(metaData as NSString)
    }
   }

@objc func setRecipient(_ node: NSNumber, recipient: NSString) {
    DispatchQueue.main.async {
      let component =
        self.bridge.uiManager.view(
          forReactTag: node
        ) as! RNCodeScannerView
        component.setRecipient(recipient as NSString)
    }
}
    
@objc func setSender(_ node: NSNumber, sender: NSString) {
    DispatchQueue.main.async {
        let component =
        self.bridge.uiManager.view(
            forReactTag: node
        ) as! RNCodeScannerView
        component.setSender(sender as NSString)
    }
}
 

  override func view() -> UIView! {

    return RNCodeScannerView()

  }

  override static func requiresMainQueueSetup() -> Bool {
    return true
  }
}
