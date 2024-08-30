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
    
    @objc func configureOnDeviceModel(_ node: NSNumber, onDeviceConfigs: NSDictionary) {
        
        DispatchQueue.main.async {
            let component =
            self.bridge.uiManager.view(
                forReactTag: node
            ) as! RNCodeScannerView
            
            if onDeviceConfigs["size"] != nil {
                component.setModelSize(onDeviceConfigs["size"] as! NSString)
            }
            
            if onDeviceConfigs["type"] != nil {
                component.setModelType(onDeviceConfigs["type"] as! NSString)
            }
            
            component.configureOnDeviceModel()
        }
    }
    
        @objc func restartScanning(_ node: NSNumber) {
            DispatchQueue.main.async {
                let component =
                self.bridge.uiManager.view(
                    forReactTag: node
                ) as! RNCodeScannerView
                component.restartScanning()
            }
        }
    
    @objc func setFocusSettings(_ node: NSNumber, focusSettings: NSDictionary) {
        DispatchQueue.main.async {
            guard let component = self.bridge.uiManager.view(forReactTag: node) as? RNCodeScannerView else { return }
            
            print("focusSettings --------------- >", focusSettings)
            
            let fs = VisionSDK.CodeScannerView.FocusSettings()
            
            // Extract and assign each value to the corresponding property
            if let focusImageString = focusSettings["focusImage"] as? String,
               let imageData = Data(base64Encoded: focusImageString),
               let image = UIImage(data: imageData) {
                fs.focusImage = image
            }

            if let focusImageRectDict = focusSettings["focusImageRect"] as? NSDictionary,
               let x = focusImageRectDict["x"] as? CGFloat,
               let y = focusImageRectDict["y"] as? CGFloat,
               let width = focusImageRectDict["width"] as? CGFloat,
               let height = focusImageRectDict["height"] as? CGFloat {
                fs.focusImageRect = CGRect(x: x, y: y, width: width, height: height)
            }

            if let shouldDisplayFocusImage = focusSettings["shouldDisplayFocusImage"] as? NSNumber {
                fs.shouldDisplayFocusImage = shouldDisplayFocusImage.boolValue
            }

            if let shouldScanInFocusImageRect = focusSettings["shouldScanInFocusImageRect"] as? NSNumber {
                fs.shouldScanInFocusImageRect = shouldScanInFocusImageRect.boolValue
            }

            if let showCodeBoundariesInMultipleScan = focusSettings["showCodeBoundariesInMultipleScan"] as? NSNumber {
                fs.showCodeBoundariesInMultipleScan = showCodeBoundariesInMultipleScan.boolValue
            }

            if let showDocumentBoundaries = focusSettings["showDocumentBoundaries"] as? NSNumber {
                fs.showDocumentBoundries = showDocumentBoundaries.boolValue
            }

            if let validCodeBoundaryBorderColorHex = focusSettings["validCodeBoundaryBorderColor"] as? String {
                fs.validCodeBoundryBorderColor = UIColor(hex: validCodeBoundaryBorderColorHex)!
            }

            if let validCodeBoundaryBorderWidth = focusSettings["validCodeBoundaryBorderWidth"] as? NSNumber {
                fs.validCodeBoundryBorderWidth = CGFloat(validCodeBoundaryBorderWidth.floatValue)
            }

            if let validCodeBoundaryFillColorHex = focusSettings["validCodeBoundaryFillColor"] as? String {
                fs.validCodeBoundryFillColor = UIColor(hex: validCodeBoundaryFillColorHex)!
            }

            if let inValidCodeBoundaryBorderColorHex = focusSettings["inValidCodeBoundaryBorderColor"] as? String {
                fs.inValidCodeBoundryBorderColor = UIColor(hex: inValidCodeBoundaryBorderColorHex)!
            }

            if let inValidCodeBoundaryBorderWidth = focusSettings["inValidCodeBoundaryBorderWidth"] as? NSNumber {
                fs.inValidCodeBoundryBorderWidth = CGFloat(inValidCodeBoundaryBorderWidth.floatValue)
            }

            if let inValidCodeBoundaryFillColorHex = focusSettings["inValidCodeBoundaryFillColor"] as? String {
                fs.inValidCodeBoundryFillColor = UIColor(hex: inValidCodeBoundaryFillColorHex)!
            }

            if let documentBoundaryBorderColorHex = focusSettings["documentBoundaryBorderColor"] as? String {
                fs.documentBoundryBorderColor = UIColor(hex: documentBoundaryBorderColorHex)!
            }

            if let documentBoundaryFillColorHex = focusSettings["documentBoundaryFillColor"] as? String {
                fs.documentBoundryFillColor = UIColor(hex: documentBoundaryFillColorHex)!
            }

            if let focusImageTintColorHex = focusSettings["focusImageTintColor"] as? String {
                fs.focusImageTintColor = UIColor(hex: focusImageTintColorHex)!
            }

            if let focusImageHighlightedColorHex = focusSettings["focusImageHighlightedColor"] as? String {
                fs.focusImageHighlightedColor = UIColor(hex: focusImageHighlightedColorHex)!
            }
            
            // Update the component's settings with the new focus settings
//            component.focusSettings = fs
            
            print("-------------------------> Updated focus settings:")
            print("focusImage: \(fs.focusImage?.description ?? "nil")")
            print("focusImageRect: \(fs.focusImageRect)")
            print("shouldDisplayFocusImage: \(fs.shouldDisplayFocusImage)")
            print("shouldScanInFocusImageRect: \(fs.shouldScanInFocusImageRect)")
            print("showCodeBoundariesInMultipleScan: \(fs.showCodeBoundariesInMultipleScan)")
            print("validCodeBoundryBorderColor: \(fs.validCodeBoundryBorderColor)")
            print("validCodeBoundryBorderWidth: \(fs.validCodeBoundryBorderWidth)")
            print("validCodeBoundryFillColor: \(fs.validCodeBoundryFillColor)")
            print("inValidCodeBoundryBorderColor: \(fs.inValidCodeBoundryBorderColor)")
            print("inValidCodeBoundryBorderWidth: \(fs.inValidCodeBoundryBorderWidth)")
            print("inValidCodeBoundryFillColor: \(fs.inValidCodeBoundryFillColor)")
            print("showDocumentBoundries: \(fs.showDocumentBoundries)")
            print("documentBoundryBorderColor: \(fs.documentBoundryBorderColor)")
            print("documentBoundryBorderWidth: \(fs.documentBoundryBorderWidth)")
            print("documentBoundryFillColor: \(fs.documentBoundryFillColor)")
            print("focusImageTintColor: \(fs.focusImageTintColor)")
            print("focusImageHighlightedColor: \(fs.focusImageHighlightedColor)")

        }
    }

    
    
    override func view() -> UIView! {
        
        return RNCodeScannerView()
        
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}
