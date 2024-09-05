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
            component.codeScannerView?.rescan()
        }
    }
    
    @objc func setFocusSettings(_ node: NSNumber, focusSettings: NSDictionary) {
        
        DispatchQueue.main.async {
            guard let component = self.bridge.uiManager.view(forReactTag: node) as? RNCodeScannerView else {
                return
            }
            let updatedFocusSettings = VisionSDK.CodeScannerView.FocusSettings()
            
            // Extract and assign each value to the corresponding property
            if let focusImageString = focusSettings["focusImage"] as? String,
               let imageData = Data(base64Encoded: focusImageString),
               let image = UIImage(data: imageData) {
                updatedFocusSettings.focusImage = image
            }

            if let focusImageRectDict = focusSettings["focusImageRect"] as? NSDictionary,
               let x = focusImageRectDict["x"] as? CGFloat,
               let y = focusImageRectDict["y"] as? CGFloat,
               let width = focusImageRectDict["width"] as? CGFloat,
               let height = focusImageRectDict["height"] as? CGFloat {
                updatedFocusSettings.focusImageRect = CGRect(x: x, y: y, width: width, height: height)
            }

            if let shouldDisplayFocusImage = focusSettings["shouldDisplayFocusImage"] as? Bool {
                updatedFocusSettings.shouldDisplayFocusImage = shouldDisplayFocusImage
            }

            if let shouldScanInFocusImageRect = focusSettings["shouldScanInFocusImageRect"] as? Bool {
                updatedFocusSettings.shouldScanInFocusImageRect = shouldScanInFocusImageRect
            }

            if let showCodeBoundariesInMultipleScan = focusSettings["showCodeBoundariesInMultipleScan"] as? Bool {
                updatedFocusSettings.showCodeBoundariesInMultipleScan = showCodeBoundariesInMultipleScan
            }

            if let showDocumentBoundaries = focusSettings["showDocumentBoundaries"] as? Bool {
                updatedFocusSettings.showDocumentBoundries = showDocumentBoundaries
            }

            if let _ = focusSettings["validCodeBoundaryBorderColor"] as? String {
                if let color = UIColor(hex: focusSettings["validCodeBoundaryBorderColor"] as! String) {
                    updatedFocusSettings.validCodeBoundryBorderColor = color
                }
            }

            if let validCodeBoundaryBorderWidth = focusSettings["validCodeBoundaryBorderWidth"] as? NSNumber {
                updatedFocusSettings.validCodeBoundryBorderWidth = CGFloat(validCodeBoundaryBorderWidth.floatValue)
            }

            if let _ = focusSettings["validCodeBoundaryFillColor"] as? String {
                if let color = UIColor(hex: focusSettings["validCodeBoundaryFillColor"] as! String) {
                    updatedFocusSettings.validCodeBoundryFillColor = color
                }
            }

            if let _ = focusSettings["inValidCodeBoundaryBorderColor"] as? String {
                if let color = UIColor(hex: focusSettings["inValidCodeBoundaryBorderColor"] as! String) {
                    updatedFocusSettings.inValidCodeBoundryBorderColor = color
                }
            }

            if let inValidCodeBoundaryBorderWidth = focusSettings["inValidCodeBoundaryBorderWidth"] as? NSNumber {
                updatedFocusSettings.inValidCodeBoundryBorderWidth = CGFloat(inValidCodeBoundaryBorderWidth.floatValue)
            }

            if let _ = focusSettings["inValidCodeBoundaryFillColor"] as? String {
                if let color = UIColor(hex: focusSettings["inValidCodeBoundaryFillColor"] as! String) {
                    updatedFocusSettings.inValidCodeBoundryFillColor = color
                }
            }

            if let _ = focusSettings["documentBoundaryBorderColor"] as? String {
                if let color = UIColor(hex: focusSettings["documentBoundaryBorderColor"] as! String) {
                    updatedFocusSettings.documentBoundryBorderColor = color
                }
            }

            if let _ = focusSettings["documentBoundaryFillColor"] as? String {
                let color = UIColor(hex: focusSettings["documentBoundaryFillColor"] as! String, alpha: 0.4)
                updatedFocusSettings.documentBoundryFillColor = color
            }

            if let _ = focusSettings["focusImageTintColor"] as? String {
                if let color = UIColor(hex: focusSettings["focusImageTintColor"] as! String) {
                    updatedFocusSettings.focusImageTintColor = color
                }
            }

            if let _ = focusSettings["focusImageHighlightedColor"] as? String {
                if let color = UIColor(hex: focusSettings["focusImageHighlightedColor"] as! String) {
                    updatedFocusSettings.focusImageHighlightedColor = color
                }
            }
            
            component.codeScannerView?.focusSettings = updatedFocusSettings
        }
    }
    
    @objc func setObjectDetectionSettings(_ node: NSNumber, objectDetectionSettings: NSDictionary) {
        
        DispatchQueue.main.async {
            guard let component = self.bridge.uiManager.view(forReactTag: node) as? RNCodeScannerView else {
                return
            }
            
            let detectionSettings = VisionSDK.CodeScannerView.ObjectDetectionConfiguration()
            
            // Update the camera settings in the component
            if let isTextIndicationOn = objectDetectionSettings["isTextIndicationOn"] as? Bool {
                detectionSettings.isTextIndicationOn = isTextIndicationOn
            }

            if let isBarCodeOrQRCodeIndicationOn = objectDetectionSettings["isBarCodeOrQRCodeIndicationOn"] as? Bool {
                detectionSettings.isBarCodeOrQRCodeIndicationOn = isBarCodeOrQRCodeIndicationOn
            }

            if let isDocumentIndicationOn = objectDetectionSettings["isDocumentIndicationOn"] as? Bool {
                detectionSettings.isDocumentIndicationOn = isDocumentIndicationOn
            }

            if let codeDetectionConfidence = objectDetectionSettings["codeDetectionConfidence"] as? Float {
                detectionSettings.codeDetectionConfidence = codeDetectionConfidence
            }

            if let documentDetectionConfidence = objectDetectionSettings["documentDetectionConfidence"] as? Float {
                detectionSettings.documentDetectionConfidence = documentDetectionConfidence
            }

            if let secondsToWaitBeforeDocumentCapture = objectDetectionSettings["secondsToWaitBeforeDocumentCapture"] as? Double {
                detectionSettings.secondsToWaitBeforeDocumentCapture = secondsToWaitBeforeDocumentCapture
            }

            if let selectedTemplateId = objectDetectionSettings["selectedTemplateId"] as? String {
                detectionSettings.selectedTemplateId = selectedTemplateId
            }

            component.codeScannerView?.objectDetectionConfiguration = detectionSettings
        }
    }
    
    @objc func setCameraSettings(_ node: NSNumber, cameraSettings: NSDictionary) {
       
        DispatchQueue.main.async {
            guard let component = self.bridge.uiManager.view(forReactTag: node) as? RNCodeScannerView else {
                return
            }
            
            let updatedCameraSettings = VisionSDK.CodeScannerView.CameraSettings()

            // Update and print each setting
            if let nthFrameToProcess = cameraSettings["nthFrameToProcess"] as? Int {
                updatedCameraSettings.nthFrameToProcess = Int64(nthFrameToProcess)
            }

//            if let shouldAutoSaveCapturedImage = cameraSettings["shouldAutoSaveCapturedImage"] as? Bool {
//                updatedCameraSettings.shouldAutoSaveCapturedImage = shouldAutoSaveCapturedImage
//            }
//
//            updatedCameraSettings.sessionPreset = .high
            
            component.codeScannerView?.cameraSettings = updatedCameraSettings
        }
    }

    
    
    override func view() -> UIView! {
        
        return RNCodeScannerView()
        
    }
    
    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}


// UIColors Extension for converting hex color to iOS format color.
extension UIColor {
    
    convenience init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if hexSanitized.hasPrefix("#") {
            hexSanitized.remove(at: hexSanitized.startIndex)
        }

        guard hexSanitized.count == 6 else { return nil }
        
        if hexSanitized.count != 6 {
            self.init(red: 255, green: 255, blue: 255, alpha: 1.0)
        }
        else {
            var rgb: UInt64 = 0
            Scanner(string: hexSanitized).scanHexInt64(&rgb)
            
            let red = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
            let green = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
            let blue = CGFloat(rgb & 0x0000FF) / 255.0
            
            self.init(red: red, green: green, blue: blue, alpha: 1.0)
        }
    }
    
    convenience init(hex: String, alpha: CGFloat = 1.0) {
           var hexFormatted: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
           
           if hexFormatted.hasPrefix("#") {
               hexFormatted.remove(at: hexFormatted.startIndex)
           }
           
           var rgbValue: UInt64 = 0
           Scanner(string: hexFormatted).scanHexInt64(&rgbValue)
           
           let red = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
           let green = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
           let blue = CGFloat(rgbValue & 0x0000FF) / 255.0
           
           self.init(red: red, green: green, blue: blue, alpha: alpha)
       }
}


//extension UIColor {
//    convenience init(hex: String) {
//        var hexFormatted: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
//
//        if hexFormatted.hasPrefix("#") {
//            hexFormatted.remove(at: hexFormatted.startIndex)
//        }
//
//        var alpha: CGFloat = 1.0
//        var rgbValue: UInt64 = 0
//        Scanner(string: hexFormatted).scanHexInt64(&rgbValue)
//
//        if hexFormatted.count == 8 {
//            alpha = CGFloat((rgbValue & 0xFF000000) >> 24) / 255.0
//            let red = CGFloat((rgbValue & 0x00FF0000) >> 16) / 255.0
//            let green = CGFloat((rgbValue & 0x0000FF00) >> 8) / 255.0
//            let blue = CGFloat(rgbValue & 0x000000FF) / 255.0
//            self.init(red: red, green: green, blue: blue, alpha: alpha)
//        } else if hexFormatted.count == 6 {
//            let red = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
//            let green = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
//            let blue = CGFloat(rgbValue & 0x0000FF) / 255.0
//            self.init(red: red, green: green, blue: blue, alpha: alpha)
//        } else {
//            // Invalid hex string length
//            self.init(red: 0, green: 0, blue: 0, alpha: 1)
//        }
//    }
//}
