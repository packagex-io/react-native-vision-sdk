import VisionSDK

@available(iOS 15.0, *)
@objc(VisionSdkViewManager)

class VisionSdkViewManager: RCTViewManager {

    @objc func getComponent(_ node: NSNumber, completion: @escaping (RNCodeScannerView?) -> Void) {
        DispatchQueue.main.async {
            if let component = self.bridge.uiManager.view(forReactTag: node) as? RNCodeScannerView {
                completion(component)
            } else {
                completion(nil)
            }
        }
    }

       // MARK: - Method to open the template controller for scanning
       @objc func createTemplate(_ node: NSNumber) {
           getComponent(node) { component in
               component?.createTemplate()
           }
       }



    // MARK: - Method to fetch saved templates from the RNCodeScannerView
       @objc func getAllTemplates(_ node: NSNumber) {
           getComponent(node) { component in
               component?.getAllTemplates()
           }
       }

       // MARK: - Method to delete template with a given ID
       @objc func deleteTemplateWithId(_ node: NSNumber, id: NSString) {
           getComponent(node) { component in
               component?.deleteTemplate(withId: id as String)
           }
       }

       // MARK: - Method to delete all templates
       @objc func deleteAllTemplates(_ node: NSNumber) {
           getComponent(node) { component in
               component?.deleteAllTemplates()
           }
       }


    @objc func captureImage(_ node: NSNumber) {
        getComponent(node) { component in
            component?.codeScannerView!.capturePhoto()
        }
    }

    @objc func stopRunning(_ node: NSNumber) {
        getComponent(node) { component in
            component?.codeScannerView!.stopRunning()
        }
    }

    @objc func startRunning(_ node: NSNumber) {
        getComponent(node) { component in
            component?.codeScannerView?.startRunning()
        }
    }

    @objc func setMetaData(_ node: NSNumber, metaData: NSDictionary) {
        getComponent(node) { component in
            component?.setMetaData(metaData as NSDictionary)
        }
    }

    @objc func setRecipient(_ node: NSNumber, recipient: NSDictionary) {
        getComponent(node) { component in
            component?.setRecipient(recipient as NSDictionary)
        }
    }

    @objc func setSender(_ node: NSNumber, sender: NSDictionary) {
        getComponent(node) { component in
            component?.setSender(sender as NSDictionary)
        }
    }

    @objc func configureOnDeviceModel(_ node: NSNumber, onDeviceConfigs: NSDictionary) {
        getComponent(node) { component in
            guard let modelType = (onDeviceConfigs["type"] as? String) else { return }
            component?.configureOnDeviceModel(modelType: modelType , modelSize: onDeviceConfigs["size"] as? String)
        }
    }


    @objc func  getPrediction (_ node: NSNumber, image: String, barcode barcodeArray: [String]) {
        getComponent(node) { component in
            // Load the image (either from a local URI or URL)
            print("Image Path: \(image)")
            self.loadImage(from: image) { loadedImage in
                // Ensure we have a valid UIImage to send
                guard let finalImage = loadedImage else {
                    print("Failed to load image.")
                    return
                }
                print("Loaded Image: \(finalImage)")
                // Call onDeviceFlow with the UIImage and barcodes
                component?.getPrediction(withImage: finalImage, andBarcodes: barcodeArray, imagePath:image)
            }
        }
    }
    @objc func  getPredictionWithCloudTransformations (_ node: NSNumber, image: String, barcode: [String]) {
        getComponent(node) { component in
            // Load the image (either from a local URI or URL)
            print("Image Path: \(image)")
            self.loadImage(from: image) { loadedImage in
                // Ensure we have a valid UIImage to send
                guard let finalImage = loadedImage else {
                    print("Failed to load image.")
                    return
                }
                print("Loaded Image: \(finalImage)")
                // Call onDeviceFlow with the UIImage and barcodes
                component?.getPredictionWithCloudTransformations(withImage: finalImage, andBarcodes: barcode, imagePath:image)
            }
        }
    }
    @objc func getPredictionShippingLabelCloud(
      _ node: NSNumber,
      image: String,
      barcode: [String],
      token: NSString?,
      apiKey: NSString?,
      locationId: NSString?,
      options: NSDictionary?,
      metadata: NSDictionary?,
      recipient: NSDictionary?,
      sender: NSDictionary?,
      shouldResizeImage: NSNumber?

      ) {
        getComponent(node) { component in
            // Load the image (either from a local URI or URL)
            print("Image Path: \(image)")
            self.loadImage(from: image) { loadedImage in
                // Ensure we have a valid UIImage to send
                guard let finalImage = loadedImage else {
                    print("Failed to load image.")
                    return
                }
                print("Loaded Image: \(finalImage)")
                // Call onDeviceFlow with the UIImage and barcodes
                            // If a parameter is not provided via dispatchCommand, fallback to the component's existing values
                let tokenValue = token as String? ?? component?.token
                let apiKeyValue = apiKey as String? ?? VSDKConstants.apiKey
                let locationIdValue = locationId as String? ?? component?.locationId
                let optionsDict = options as? [String: Any] ?? component?.options ?? [:]
                let metadataDict = metadata as? [String: Any] ?? component?.metaData ?? [:]
                let recipientDict = recipient as? [String: Any] ?? component?.recipient ?? [:]
                let senderDict = sender as? [String: Any] ?? component?.sender ?? [:]
                let shouldResize = shouldResizeImage?.boolValue ?? component?.shouldResizeImage ?? true

                component?.getPredictionShippingLabelCloud(
                  withImage: finalImage,
                  andBarcodes: barcode,
                  imagePath:image,
                  token: tokenValue,
                  apiKey: apiKeyValue,
                  locationId: locationIdValue,
                  options: optionsDict,
                  metadata: metadataDict,
                  recipient: recipientDict,
                  sender: senderDict,
                  shouldResizeImage: shouldResize
                )
            }
        }
    }
    @objc func getPredictionBillOfLadingCloud(
      _ node: NSNumber,
      image: String,
      barcode: [String],
      token: NSString?,
      apiKey: NSString?,
      locationId: NSString?,
      options: NSDictionary?,
      shouldResizeImage: NSNumber?
      ) {
        getComponent(node) { component in
            // Load the image (either from a local URI or URL)
            print("Image Path: \(image)")
            self.loadImage(from: image) { loadedImage in
                // Ensure we have a valid UIImage to send
                guard let finalImage = loadedImage else {
                    print("Failed to load image.")
                    return
                }
                print("Loaded Image: \(finalImage)")
                // Call onDeviceFlow with the UIImage and barcodes
                let tokenValue = token as String? ?? component?.token
                let apiKeyValue = apiKey as String? ?? VSDKConstants.apiKey
                let locationIdValue = locationId as String? ?? component?.locationId
                let optionsDict = options as? [String: Any] ?? component?.options ?? [:]
                let shouldResize = shouldResizeImage?.boolValue ?? component?.shouldResizeImage ?? true



                component?.getPredictionBillOfLadingCloud(
                  withImage: finalImage,
                  andBarcodes: barcode,
                  imagePath:image,
                  token: tokenValue,
                  apiKey: apiKeyValue,
                  locationId: locationIdValue,
                  options: optionsDict,
                  shouldResizeImage: shouldResize
                )
            }
        }
    }
    @objc func getPredictionItemLabelCloud(
      _ node: NSNumber,
      image: String,
      token: NSString?,
      apiKey: NSString?,
      shouldResizeImage: NSNumber?
      ) {
        getComponent(node) { component in
            // Load the image (either from a local URI or URL)
            print("Image Path: \(image)")
            self.loadImage(from: image) { loadedImage in
                // Ensure we have a valid UIImage to send
                guard let finalImage = loadedImage else {
                    print("Failed to load image.")
                    return
                }
                print("Loaded Image: \(finalImage)")
                // Call onDeviceFlow with the UIImage and barcodes

                let tokenValue = token as String? ?? component?.token
                let apiKeyValue = apiKey as String? ?? VSDKConstants.apiKey
                let shouldResize = shouldResizeImage?.boolValue ?? component?.shouldResizeImage ?? true

                component?.getPredictionItemLabelCloud(
                  withImage: finalImage,
                  imagePath:image,
                  token: tokenValue,
                  apiKey: apiKeyValue,
                  shouldResizeImage: shouldResize
                )
            }
        }
    }

    @objc func getPredictionDocumentClassificationCloud(
        _ node: NSNumber,
        image: String,
        token: NSString?,
        apiKey: NSString?,
        shouldResizeImage: NSNumber?
      ) {
        getComponent(node) { component in
            // Load the image (either from a local URI or URL)
            print("Image Path: \(image)")
            self.loadImage(from: image) { loadedImage in
                // Ensure we have a valid UIImage to send
                guard let finalImage = loadedImage else {
                    print("Failed to load image.")
                    return
                }
                print("Loaded Image: \(finalImage)")
                // Call onDeviceFlow with the UIImage and barcodes

                let tokenValue = token as String? ?? component?.token
                let apiKeyValue = apiKey as String? ?? VSDKConstants.apiKey
                let shouldResize = shouldResizeImage?.boolValue ?? component?.shouldResizeImage ?? true

                component?.getPredictionDocumentClassificationCloud(
                  withImage: finalImage,
                  imagePath:image,
                  token: tokenValue,
                  apiKey: apiKeyValue,
                  shouldResizeImage: shouldResize
                )
            }
        }
    }

    @objc func reportError (_ node: NSNumber, data: NSDictionary) {
        getComponent(node) { component in
            print("Parsed Data: \(data)")

            let response = data["response"] as? Data

            // Handle image if provided and not an empty string
                if let imagePath = data["image"] as? String, !imagePath.isEmpty {
                    self.loadImage(from: imagePath) { loadedImage in
                        guard let finalImage = loadedImage else {
                            print("Failed to load image.")
                            return
                        }
                        component?.reportError(
                            uiImage: finalImage,
                            reportText: data["reportText"] as? String ?? "",
                            response: response ?? nil,
                            modelType: data["type"] as? String ?? "shipping_label",
                            modelSize: data["size"] as? String ?? "large",
                            errorFlags: data["errorFlags"] as? [String:Bool]
                        )
                    }
                    return
                }

            // If no image, report the error without it
            component?.reportError(
                reportText: data["reportText"] as? String ?? "",
                response: response ?? nil,
                modelType: data["type"] as? String ?? "shipping_label",
                modelSize: data["size"] as? String ?? "large",
                errorFlags: data["errorFlags"] as? [String:Bool]
            )
        }
    }

    // Helper function to load UIImage from a local URI or URL
    private func loadImage(from imagePath: String, completion: @escaping (UIImage?) -> Void) {
        var adjustedImagePath = imagePath

//        // Check if the path starts with "file://" and adjust if not
        if !adjustedImagePath.hasPrefix("file://") {
            adjustedImagePath = "file://" + adjustedImagePath
        }

        // Check if the path starts with "http" for remote URLs or "file://" for local files
        if adjustedImagePath.hasPrefix("http") {
            // It's a remote URL
            guard let remoteUrl = URL(string: adjustedImagePath) else {
                print("Invalid remote URL.")
                completion(nil)
                return
            }

            // Load image from the remote URL
            let task = URLSession.shared.dataTask(with: remoteUrl) { data, response, error in
                if let data = data, error == nil, let image = UIImage(data: data) {
                    DispatchQueue.main.async {
                        completion(image)
                    }
                } else {
                    print("Error loading image from URL: \(error?.localizedDescription ?? "Unknown error")")
                    DispatchQueue.main.async {
                        completion(nil)
                    }
                }
            }
            task.resume()

        } else if !adjustedImagePath.isEmpty {
            // It's a local file
            guard let localUrl = URL(string: adjustedImagePath) else {
                print("Invalid local URL.")
                completion(nil)
                return
            }

            // Check if the file exists
            if !FileManager.default.fileExists(atPath: localUrl.path) {
                print("File does not exist at path: \(localUrl.path)")
                completion(nil)
                return
            }

            // Load image from the local URL
            DispatchQueue.global(qos: .default).async {
                if let data = try? Data(contentsOf: localUrl), let image = UIImage(data: data) {
                    DispatchQueue.main.async {
                        completion(image)
                    }
                } else {
                    print("Failed to load image from local URL.")
                    DispatchQueue.main.async {
                        completion(nil)
                    }
                }
            }
        } else {
            print("Invalid image path. Must start with http or file://.")
            completion(nil)
        }
    }

    @objc func restartScanning(_ node: NSNumber) {
        getComponent(node) { component in
            component?.codeScannerView?.rescan()
        }
    }

    @objc func setFocusSettings(_ node: NSNumber, focusSettings: NSDictionary) {

        getComponent(node) { component in
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

          component?.codeScannerView?.setFocusSettingsTo(updatedFocusSettings)
        }
    }

    @objc func setObjectDetectionSettings(_ node: NSNumber, objectDetectionSettings: NSDictionary) {

        getComponent(node) { component in
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

            //            if let selectedTemplateId = objectDetectionSettings["selectedTemplateId"] as? String {
            //                detectionSettings.selectedTemplateId = selectedTemplateId
            //            }

          component?.codeScannerView?.setObjectDetectionConfigurationTo(detectionSettings)
        }
    }

    @objc func setCameraSettings(_ node: NSNumber, cameraSettings: NSDictionary) {

        getComponent(node) { component in
            let updatedCameraSettings = VisionSDK.CodeScannerView.CameraSettings()

            // Update and print each setting
            if let nthFrameToProcess = cameraSettings["nthFrameToProcess"] as? Int {
                updatedCameraSettings.nthFrameToProcess = Int64(nthFrameToProcess)
            }

          component?.codeScannerView?.setCameraSettingsTo(updatedCameraSettings)
        }
    }



    override func view() -> UIView! {

        return RNCodeScannerView()

    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }
}


//MARK: - UIColors Extension for converting hex color to iOS format color.
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

