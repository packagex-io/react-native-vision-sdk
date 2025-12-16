import Foundation
import VisionSDK

@objc(VisionSdkModule)
class VisionSdkModule: RCTEventEmitter {
   // Callback for sending events (used for TurboModule)
   private var eventCallback: ((_ eventName: String, _ body: [String: Any]) -> Void)?

   override static func moduleName() -> String {
        return "VisionSdkModule"
    }

  override func supportedEvents() -> [String]! {
      return ["onModelDownloadProgress"]
  }

  @objc override func constantsToExport() -> [AnyHashable: Any]! {
    return [:]
  }

  // Expose callback setter to Objective-C
  @objc func setEventCallback(_ callback: @escaping (_ eventName: String, _ body: NSDictionary) -> Void) {
    self.eventCallback = { eventName, body in
      callback(eventName, body as NSDictionary)
    }
  }

  // Override sendEvent to use callback if available
  override func sendEvent(withName name: String, body: Any?) {
    if let callback = eventCallback, let bodyDict = body as? [String: Any] {
      callback(name, bodyDict)
    } else {
      // Only call super if we have a bridge (old architecture)
      if self.bridge != nil {
        super.sendEvent(withName: name, body: body)
      }
    }
  }

  @objc func setEnvironment(_ environment: String) {

      switch environment {

      case "dev":
          VSDKConstants.apiEnvironment = .dev
          break
      case "qa":
          VSDKConstants.apiEnvironment = .qa
          break
      case "sandbox":
          VSDKConstants.apiEnvironment = .sandbox
          break
      case "prod":
          VSDKConstants.apiEnvironment = .production
          break
      case "staging":
          VSDKConstants.apiEnvironment = .staging
          break
      default:
          VSDKConstants.apiEnvironment = .production
      }
  }

  // Converts a model type string to the corresponding VisionSDK model type
  private func getModelType(_ modelType: String) -> VSDKModelExternalClass {
      switch modelType {
      case "shipping_label", "shipping-label":
          return .shippingLabel
      case "item_label", "item-label":
          return .itemLabel
      case "bill_of_lading", "bill-of-lading":
          return .billOfLading
      case "document_classification", "document-classification":
          return .documentClassification
      default:
          return .shippingLabel // Default to shipping label if unknown
      }
  }

  // Converts a model size string to the corresponding VisionSDK model size
  private func getModelSize(_ modelSize: String?) -> VSDKModelExternalSize? {
      guard let modelSize = modelSize else { return nil }
      switch modelSize {
      case "nano": return .nano
      case "micro": return .micro
      case "small": return .small
      case "medium": return .medium
      case "large": return .large
      case "xlarge": return .xlarge
      default: return .large // Default to large if unknown
      }
  }


  // DEPRECATED - Use unloadModel() or deleteModel() instead
  // @objc func unLoadOnDeviceModels(
  //     _ modelType: String?,
  //     shouldDeleteFromDisk: Bool,
  //     resolver: @escaping RCTPromiseResolveBlock,
  //     rejecter: @escaping RCTPromiseRejectBlock
  // ) {
  //   // Convert empty string to nil
  //   let modelTypeValue = (modelType?.isEmpty ?? true) ? nil : modelType
  //
  //   if let modelType = modelTypeValue {
  //     let modelClass = getModelType(modelType)
  //
  //     do {
  //       try OnDeviceOCRManager.shared.deconfigureOfflineOCR(for: modelClass, shouldDeleteFromDisk: shouldDeleteFromDisk)
  //     }
  //     catch(let error) {
  //       rejecter("MODEL_UNLOAD_ERROR", error.localizedDescription, nil)
  //     }
  //
  //     resolver("Model unloaded successfully")
  //
  //   }
  //   else {
  //     do {
  //       try OnDeviceOCRManager.shared.deconfigureOfflineOCR(shouldDeleteFromDisk: shouldDeleteFromDisk)
  //     }
  //     catch(let error) {
  //
  //       rejecter("MODEL_UNLOAD_ERROR", error.localizedDescription, nil)
  //     }
  //
  //     resolver("Model unloaded successfully")
  //   }
  //
  // }
  
  
  @objc func loadOnDeviceModels(
    _ token: String?,
      apiKey: String?,
      modelType: String,
      modelSize: String?,
      resolver: @escaping RCTPromiseResolveBlock,
      rejecter: @escaping RCTPromiseRejectBlock
  ) {
      let modelClass = getModelType(modelType)
      let modelSizeEnum = getModelSize(modelSize) ?? VSDKModelExternalSize.large

      // Convert empty strings to nil
      let tokenValue = (token?.isEmpty ?? true) ? nil : token
      let apiKeyValue = (apiKey?.isEmpty ?? true) ? nil : apiKey

      // Dispatch to background queue to prevent blocking UI thread
      // VisionSDK's prepareOfflineOCR does heavy initialization work synchronously
      DispatchQueue.global(qos: .userInitiated).async {
          OnDeviceOCRManager.shared.prepareOfflineOCR(
            withApiKey: apiKeyValue,
            andToken: tokenValue,
            forModelClass: modelClass,
            withModelSize: modelSizeEnum,
            withProgressTracking: { currentProgress, totalSize, isModelAlreadyDownloaded in
                let progressData: [String: Any] = [
                    "progress": isModelAlreadyDownloaded ? 1.0 : (currentProgress / totalSize),
                    "downloadStatus": isModelAlreadyDownloaded,
                    "isReady": false  // Will be set to true in completion callback
                ]
                self.sendEvent(withName: "onModelDownloadProgress", body: progressData)
            },
            withCompletion: { error in
                if let error = error {
                    rejecter("MODEL_LOAD_ERROR", error.localizedDescription, nil)
                } else {
                    let completionData: [String: Any] = [
                        "progress": 1.0,
                        "downloadStatus": true,
                        "isReady": true
                    ]
                    self.sendEvent(withName: "onModelDownloadProgress", body: completionData)
                    resolver("Model configured successfully")
                }
            }
          )
      }
  }
  
  
  
  
  @objc func logShippingLabelDataToPx(
    _ imageUri: String,
    barcodes: [String]?,
    responseData: [String: Any],
    token: String?,
    apiKey: String?,
    locationId: String?,
    options: [String : Any]? = nil,
    metadata: [String : Any]? = nil,
    recipient: [String : Any]? = nil,
    sender: [String : Any]? = nil,
    shouldResizeImage: NSNumber,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
    
  ){
    self.loadImage(from: imageUri){image in
      guard let image = image else {
        rejecter("image_load_failed", "Failed to load image from URI", nil)
        return
      }
      
      guard JSONSerialization.isValidJSONObject(responseData),
            let responseDataJson = try? JSONSerialization.data(withJSONObject: responseData) else {
        rejecter("invalid_response_data", "Could not serialize responseData to JSON", nil)
        return
      }
      
      let barcodeList = barcodes ?? []
      let shouldResize = shouldResizeImage.boolValue
      
      VisionAPIManager.shared.callMatchingAPIWith(
        image,
        andBarcodes: barcodeList,
        andApiKey: apiKey,
        andToken: token,
        withResponseData: responseDataJson,
        andLocationId: (locationId ?? "").isEmpty ? nil : locationId,
        andOptions: options ?? [:],
        andMetaData: metadata ?? [:],
        andRecipient: recipient ?? [:],
        andSender: sender ?? [:],
        withImageResizing: shouldResize
      ) {data, error in
        if let error = error {
          rejecter("sdk_error", "SDK call failed", error)
        } else {
          resolver("Logged successfully")
        }
      }
      
    }
  }
  
  @objc func logItemLabelDataToPx(
    _ imageUri: String,
    barcodes: [String]?,
    responseData: [String: Any],
    token: String?,
    apiKey: String?,
    shouldResizeImage: NSNumber,
    metadata: [String : Any]? = nil,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    self.loadImage(from: imageUri){image in
      guard let image = image else {
        rejecter("image_load_failed", "Could not load image from URI", nil)
        return
      }
      // Ensure barcodes is a non-nil array
      let barcodeList = barcodes ?? []

      // Convert responseData dictionary to JSON Data
      guard JSONSerialization.isValidJSONObject(responseData),
            let responseDataJson = try? JSONSerialization.data(withJSONObject: responseData) else {
        rejecter("invalid_response_data", "Failed to convert responseData to JSON", nil)
        return
      }

      let shouldResize = shouldResizeImage.boolValue


      VisionAPIManager.shared.callItemLabelsMatchingAPIWith(
        image,
        andBarcodes: barcodeList,
        andApiKey: apiKey,
        andToken: token,
        withResponseData: responseDataJson,
        withImageResizing: shouldResize,
        andMetaData: metadata ?? [:]
      ){data, error in
          if let error = error {
          rejecter("sdk_error", "SDK call failed", error)
          return
          } else {
            resolver("Logged successfully")
          }
      }

    }


  }

  
  
  private func loadImage(from imagePath: String, completion: @escaping (UIImage?) -> Void) {
      guard !imagePath.isEmpty else {
          completion(nil)
          return
      }

      if imagePath.hasPrefix("http") {
          // It's a remote URL
          guard let remoteUrl = URL(string: imagePath) else {
              completion(nil)
              return
          }

          // Load image from the remote URL
          URLSession.shared.dataTask(with: remoteUrl) { data, response, error in
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
          }.resume()

      } else {
          // Handle local file
          var adjustedImagePath = imagePath
          if !adjustedImagePath.hasPrefix("file://") {
              adjustedImagePath = "file://" + adjustedImagePath
          }

          guard let localUrl = URL(string: adjustedImagePath) else {
              completion(nil)
              return
          }

          if !FileManager.default.fileExists(atPath: localUrl.path) {
              completion(nil)
              return
          }

          DispatchQueue.global(qos: .default).async {
              if let data = try? Data(contentsOf: localUrl), let image = UIImage(data: data) {
                  DispatchQueue.main.async {
                      completion(image)
                  }
              } else {
                  DispatchQueue.main.async {
                      completion(nil)
                  }
              }
          }
      }
  }



  @objc func predict(
    _ imagePath: String,
    barcodes: [[String: Any]]?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      // Convert UIImage to CIImage for OnDeviceOCRManager
      guard let ciImage = convertToCIImage(from: image) else {
        rejecter("IMAGE_CONVERSION_ERROR", "Failed to convert UIImage to CIImage", nil)
        return
      }

      let barcodeList = barcodes ?? []
      
      let allBarcodes: [DetectedCode] = barcodeList.map { barcodeDict in
        
        let stringValue: String = (barcodeDict["scannedCode"] as? String) ?? ""
        let symbology: VisionSDK.BarcodeSymbology = VisionSDK.BarcodeSymbology.value(VNStringValue: (barcodeDict["symbology"] as? String) ?? "")
        let extractedData: [String : String]? = (barcodeDict["gs1ExtractedInfo"] as? [String : String])
        
        var finalRect: CGRect = .zero
        
        if let boundingBoxRect = barcodeDict["boundingBox"] as? [String: CGFloat] {
          let x: CGFloat = boundingBoxRect["x"] ?? 0
          let y: CGFloat = boundingBoxRect["y"] ?? 0
          let width: CGFloat = boundingBoxRect["width"] ?? 0
          let height: CGFloat = boundingBoxRect["height"] ?? 0
          
          finalRect = CGRect(x: x, y: y, width: width, height: height)
        }
        
        return DetectedCode(stringValue: stringValue, symbology: symbology, extractedData: extractedData, boundingBox: finalRect)
        
      }
      
      

      // Use the correct OnDeviceOCRManager API
      
      OnDeviceOCRManager.shared.extractDataFromImageUsing(ciImage, withBarcodes: allBarcodes) { data, error in
        if let error = error {
          rejecter("PREDICTION_ERROR", "On-device prediction failed: \(error.localizedDescription)", error)
        } else if let data = data {
          let responseString = String(data: data, encoding: .utf8) ?? ""
          resolver(responseString)
        } else {
          rejecter("NO_DATA_ERROR", "No data returned from on-device prediction", nil)
        }
      }
    }
  }

  @objc func predictShippingLabelCloud(
    _ imagePath: String,
    barcodes: [String]?,
    token: String?,
    apiKey: String?,
    locationId: String?,
    options: [String: Any]?,
    metadata: [String: Any]?,
    recipient: [String: Any]?,
    sender: [String: Any]?,
    shouldResizeImage: NSNumber?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      let barcodeList = barcodes ?? []
      let shouldResize = shouldResizeImage?.boolValue ?? true

      VisionAPIManager.shared.callScanAPIWith(
        image,
        andBarcodes: barcodeList,
        andApiKey: apiKey,
        andToken: token,
        andLocationId: locationId ?? "",
        andOptions: options ?? [:],
        andMetaData: metadata ?? [:],
        andRecipient: recipient ?? [:],
        andSender: sender ?? [:],
        withImageResizing: shouldResize
      ) { data, error in
        if let error = error {
          rejecter("API_ERROR", "Shipping label prediction failed", error)
        } else if let data = data {
          resolver(String(data: data, encoding: .utf8) ?? "")
        } else {
          rejecter("UNKNOWN_ERROR", "Unknown error occurred", nil)
        }
      }
    }
  }

  @objc func predictItemLabelCloud(
    _ imagePath: String,
    token: String?,
    apiKey: String?,
    shouldResizeImage: NSNumber?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      let shouldResize = shouldResizeImage?.boolValue ?? true

      VisionAPIManager.shared.callItemLabelsAPIWith(
        image,
        andApiKey: apiKey,
        andToken: token,
        withImageResizing: shouldResize
      ) { data, error in
        if let error = error {
          rejecter("API_ERROR", "Item label prediction failed", error)
        } else if let data = data {
          resolver(String(data: data, encoding: .utf8) ?? "")
        } else {
          rejecter("UNKNOWN_ERROR", "Unknown error occurred", nil)
        }
      }
    }
  }

  @objc func predictBillOfLadingCloud(
    _ imagePath: String,
    barcodes: [String]?,
    token: String?,
    apiKey: String?,
    locationId: String?,
    options: [String: Any]?,
    shouldResizeImage: NSNumber?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      let barcodeList = barcodes ?? []
      let shouldResize = shouldResizeImage?.boolValue ?? true

      // Use the correct Bill of Lading API method
      if let validLocationId = locationId, !validLocationId.isEmpty {
        VisionAPIManager.shared.getPredictionBillOfLadingCloud(
          image,
          andBarcodes: barcodeList,
          andApiKey: apiKey,
          andToken: token,
          andLocationId: validLocationId,
          andOptions: options ?? [:],
          withImageResizing: shouldResize
        ) { data, error in
          if let error = error {
            rejecter("API_ERROR", "Bill of lading prediction failed", error)
          } else if let data = data {
            resolver(String(data: data, encoding: .utf8) ?? "")
          } else {
            rejecter("UNKNOWN_ERROR", "Unknown error occurred", nil)
          }
        }
      } else {
        VisionAPIManager.shared.getPredictionBillOfLadingCloud(
          image,
          andBarcodes: barcodeList,
          andApiKey: apiKey,
          andToken: token,
          andOptions: options ?? [:],
          withImageResizing: shouldResize
        ) { data, error in
          if let error = error {
            rejecter("API_ERROR", "Bill of lading prediction failed", error)
          } else if let data = data {
            resolver(String(data: data, encoding: .utf8) ?? "")
          } else {
            rejecter("UNKNOWN_ERROR", "Unknown error occurred", nil)
          }
        }
      }
    }
  }

  @objc func predictDocumentClassificationCloud(
    _ imagePath: String,
    token: String?,
    apiKey: String?,
    shouldResizeImage: NSNumber?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      let shouldResize = shouldResizeImage?.boolValue ?? true

      VisionAPIManager.shared.callDocumentClassificationAPIWith(
        image,
        andApiKey: apiKey,
        andToken: token,
        withImageResizing: shouldResize
      ) { data, error in
        if let error = error {
          rejecter("API_ERROR", "Document classification prediction failed", error)
        } else if let data = data {
          resolver(String(data: data, encoding: .utf8) ?? "")
        } else {
          rejecter("UNKNOWN_ERROR", "Unknown error occurred", nil)
        }
      }
    }
  }

  @objc func predictWithCloudTransformations(
    _ imagePath: String,
    barcodes: [String]?,
    token: String?,
    apiKey: String?,
    locationId: String?,
    options: [String: Any]?,
    metadata: [String: Any]?,
    recipient: [String: Any]?,
    sender: [String: Any]?,
    shouldResizeImage: NSNumber?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      let barcodeList = barcodes ?? []
      let shouldResize = shouldResizeImage?.boolValue ?? true

      // Convert UIImage to CIImage for OnDeviceOCRManager
      guard let ciImage = convertToCIImage(from: image) else {
        rejecter("IMAGE_CONVERSION_ERROR", "Failed to convert UIImage to CIImage", nil)
        return
      }

      // First get on-device prediction using correct API
      OnDeviceOCRManager.shared.extractDataFromImageUsing(ciImage, withBarcodes: []) { onDeviceData, onDeviceError in
        if let onDeviceError = onDeviceError {
          rejecter("ON_DEVICE_ERROR", "Failed to get on-device prediction: \(onDeviceError.localizedDescription)", onDeviceError)
          return
        }

        guard let onDeviceData = onDeviceData else {
          rejecter("NO_ON_DEVICE_DATA", "No data returned from on-device prediction", nil)
          return
        }

        // Send to cloud for transformation
        VisionAPIManager.shared.callMatchingAPIWith(
          image,
          andBarcodes: barcodeList,
          andApiKey: apiKey,
          andToken: token,
          withResponseData: onDeviceData,
          andLocationId: (locationId ?? "").isEmpty ? nil : locationId,
          andOptions: options ?? [:],
          andMetaData: metadata ?? [:],
          andRecipient: recipient ?? [:],
          andSender: sender ?? [:],
          withImageResizing: shouldResize
        ) { data, error in
          if let error = error {
            rejecter("CLOUD_TRANSFORMATION_ERROR", "Cloud transformation failed", error)
          } else if let data = data {
            resolver(String(data: data, encoding: .utf8) ?? "")
          } else {
            rejecter("UNKNOWN_ERROR", "Unknown error occurred", nil)
          }
        }
      }
    }
  }

  // ============================================================================
  // MODEL MANAGEMENT API - HELPER METHODS
  // ============================================================================

  /// Parse OCRModule JSON to iOS model class and size
  private func parseOCRModule(_ moduleJson: String) -> (modelClass: VSDKModelExternalClass, modelSize: VSDKModelExternalSize)? {
    guard let jsonData = moduleJson.data(using: .utf8),
          let json = try? JSONSerialization.jsonObject(with: jsonData) as? [String: Any],
          let typeString = json["type"] as? String else {
      return nil
    }

    let sizeString = json["size"] as? String ?? "large"

    let modelClass = stringToModelClass(typeString)
    let modelSize = stringToModelSize(sizeString)

    return (modelClass, modelSize)
  }

  /// Convert model class enum to string
  private func modelClassToString(_ modelClass: VSDKModelExternalClass) -> String {
    switch modelClass {
    case .shippingLabel:
      return "shipping_label"
    case .itemLabel:
      return "item_label"
    case .billOfLading:
      return "bill_of_lading"
    case .documentClassification:
      return "document_classification"
    @unknown default:
      return "shipping_label"
    }
  }

  /// Convert string to model class enum
  private func stringToModelClass(_ classString: String) -> VSDKModelExternalClass {
    switch classString.lowercased() {
    case "shipping_label", "shipping-label":
      return .shippingLabel
    case "item_label", "item-label":
      return .itemLabel
    case "bill_of_lading", "bill-of-lading":
      return .billOfLading
    case "document_classification", "document-classification":
      return .documentClassification
    default:
      return .shippingLabel
    }
  }

  /// Convert model size enum to string
  private func modelSizeToString(_ modelSize: VSDKModelExternalSize) -> String {
    switch modelSize {
    case .nano:
      return "nano"
    case .micro:
      return "micro"
    case .small:
      return "small"
    case .medium:
      return "medium"
    case .large:
      return "large"
    case .xlarge:
      return "xlarge"
    @unknown default:
      return "large"
    }
  }

  /// Convert string to model size enum
  private func stringToModelSize(_ sizeString: String) -> VSDKModelExternalSize {
    switch sizeString.lowercased() {
    case "nano":
      return .nano
    case "micro":
      return .micro
    case "small":
      return .small
    case "medium":
      return .medium
    case "large":
      return .large
    case "xlarge":
      return .xlarge
    default:
      return .large
    }
  }

  /// Convert iOS DownloadedModelData to ModelInfo JSON string
  private func downloadedModelDataToJson(_ modelData: DownloadedModelData, isLoaded: Bool) -> String {
    let moduleDict: [String: Any] = [
      "type": modelData.modelClass ?? "shipping_label",
      "size": modelData.modelSize ?? "large"
    ]

    let modelInfoDict: [String: Any] = [
      "module": moduleDict,
      "version": modelData.modelVersion ?? "",
      "versionId": modelData.modelVersionId ?? "",
      "dateString": modelData.modelVersion ?? "",
      "isLoaded": isLoaded
    ]

    guard let jsonData = try? JSONSerialization.data(withJSONObject: modelInfoDict),
          let jsonString = String(data: jsonData, encoding: .utf8) else {
      return "{}"
    }

    return jsonString
  }

  // ============================================================================
  // MODEL MANAGEMENT API METHODS
  // ============================================================================

  /// Initialize ModelManager - NO-OP on iOS (no initialization needed)
  @objc func initializeModelManager(_ configJson: String) {
    // NO-OP - iOS OnDeviceOCRManager doesn't require initialization
    // This method exists for cross-platform API compatibility
  }

  /// Check if ModelManager is initialized - Always true on iOS
  @objc func isModelManagerInitialized() -> Bool {
    // Always return true - iOS doesn't need initialization
    return true
  }

  /// Download a model from server to disk with progress tracking
  @objc func downloadModel(
    _ moduleJson: String,
    apiKey: String?,
    token: String?,
    platformType: String,
    requestId: String,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      rejecter("PARSE_ERROR", "Failed to parse module JSON", nil)
      return
    }

    let apiKeyValue = (apiKey?.isEmpty ?? true) ? nil : apiKey
    let tokenValue = (token?.isEmpty ?? true) ? nil : token

    OnDeviceOCRManager.shared.downloadModel(
      withApiKey: apiKeyValue,
      andToken: tokenValue,
      forModelClass: modelClass,
      withModelSize: modelSize,
      withProgressTracking: { currentProgress, totalSize in
        let normalizedProgress = totalSize > 0 ? currentProgress / totalSize : 0
        let progressData: [String: Any] = [
          "progress": Double(normalizedProgress),
          "module": moduleJson,
          "requestId": requestId
        ]
        self.sendEvent(withName: "onModelDownloadProgress", body: progressData)
      },
      withCompletion: { error in
        if let error = error {
          rejecter("DOWNLOAD_FAILED", error.localizedDescription, error)
        } else {
          resolver(nil)
        }
      }
    )
  }

  /// Cancel an active model download
  @objc func cancelDownload(
    _ moduleJson: String,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      rejecter("PARSE_ERROR", "Failed to parse module JSON", nil)
      return
    }

    OnDeviceOCRManager.shared.cancelDownload(modelClass, withModelSize: modelSize) { error in
      if let error = error {
        rejecter("CANCEL_FAILED", error.localizedDescription, error)
      } else {
        resolver(true)
      }
    }
  }

  /// Load a model into memory
  @objc func loadOCRModel(
    _ moduleJson: String,
    apiKey: String?,
    token: String?,
    platformType: String,
    executionProvider: String?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      rejecter("PARSE_ERROR", "Failed to parse module JSON", nil)
      return
    }

    let apiKeyValue = (apiKey?.isEmpty ?? true) ? nil : apiKey
    let tokenValue = (token?.isEmpty ?? true) ? nil : token
    // Note: executionProvider is Android-only, ignored on iOS

    OnDeviceOCRManager.shared.loadModel(
      withApiKey: apiKeyValue,
      andToken: tokenValue,
      forModelClass: modelClass,
      withModelSize: modelSize,
      withCompletion: { error in
        if let error = error {
          rejecter("LOAD_FAILED", error.localizedDescription, error)
        } else {
          resolver(nil)
        }
      }
    )
  }

  /// Unload a model from memory (file remains on disk)
  @objc func unloadModel(_ moduleJson: String) -> Bool {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      return false
    }

    OnDeviceOCRManager.shared.unloadModel(modelClass, withModelSize: modelSize)
    return true
  }

  /// Check if a model is currently loaded in memory
  @objc func isModelLoaded(_ moduleJson: String) -> Bool {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      return false
    }

    return OnDeviceOCRManager.shared.isModelLoaded(modelClass, withModelSize: modelSize)
  }

  /// Get the number of models currently loaded in memory
  @objc func getLoadedModelCount() -> Double {
    let loadedModels = OnDeviceOCRManager.shared.getLoadedModels()
    return Double(loadedModels.count)
  }

  /// Find all downloaded models by scanning the file system
  @objc func findDownloadedModels(
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    let downloadedModels = OnDeviceOCRManager.shared.getDownloadedModels()

    var modelInfoArray: [[String: Any]] = []

    for modelData in downloadedModels {
      guard let modelClassStr = modelData.modelClass,
            let modelSizeStr = modelData.modelSize else {
        continue
      }

      let modelClass = stringToModelClass(modelClassStr)
      let modelSize = stringToModelSize(modelSizeStr)
      let isLoaded = OnDeviceOCRManager.shared.isModelLoaded(modelClass, withModelSize: modelSize)

      let moduleDict: [String: Any] = [
        "type": modelClassStr,
        "size": modelSizeStr
      ]

      let modelInfoDict: [String: Any] = [
        "module": moduleDict,
        "version": modelData.modelVersion ?? "",
        "versionId": modelData.modelVersionId ?? "",
        "dateString": modelData.modelVersion ?? "",
        "isLoaded": isLoaded
      ]

      modelInfoArray.append(modelInfoDict)
    }

    guard let jsonData = try? JSONSerialization.data(withJSONObject: modelInfoArray),
          let jsonString = String(data: jsonData, encoding: .utf8) else {
      resolver("[]")
      return
    }

    resolver(jsonString)
  }

  /// Find a specific downloaded model
  @objc func findDownloadedModel(
    _ moduleJson: String,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      rejecter("PARSE_ERROR", "Failed to parse module JSON", nil)
      return
    }

    if let modelData = OnDeviceOCRManager.shared.getDownloadedModel(modelClass, withModelSize: modelSize) {
      let isLoaded = OnDeviceOCRManager.shared.isModelLoaded(modelClass, withModelSize: modelSize)
      let jsonString = downloadedModelDataToJson(modelData, isLoaded: isLoaded)
      resolver(jsonString)
    } else {
      resolver("null")
    }
  }

  /// Find all models currently loaded in memory
  @objc func findLoadedModels(
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    let loadedModels = OnDeviceOCRManager.shared.getLoadedModels()
    let downloadedModels = OnDeviceOCRManager.shared.getDownloadedModels()

    var modelInfoArray: [[String: Any]] = []

    for loadedModel in loadedModels {
      guard let modelClassStr = loadedModel["class"] as? String,
            let modelSizeStr = loadedModel["size"] as? String else {
        continue
      }

      // Find corresponding downloaded model data
      let modelClass = stringToModelClass(modelClassStr)
      let modelSize = stringToModelSize(modelSizeStr)

      if let modelData = downloadedModels.first(where: {
        $0.modelClass == modelClassStr && $0.modelSize == modelSizeStr
      }) {
        let moduleDict: [String: Any] = [
          "type": modelClassStr,
          "size": modelSizeStr
        ]

        let modelInfoDict: [String: Any] = [
          "module": moduleDict,
          "version": modelData.modelVersion ?? "",
          "versionId": modelData.modelVersionId ?? "",
          "dateString": modelData.modelVersion ?? "",
          "isLoaded": true
        ]

        modelInfoArray.append(modelInfoDict)
      }
    }

    guard let jsonData = try? JSONSerialization.data(withJSONObject: modelInfoArray),
          let jsonString = String(data: jsonData, encoding: .utf8) else {
      resolver("[]")
      return
    }

    resolver(jsonString)
  }

  /// Delete a model from disk (unloads from memory first if loaded)
  @objc func deleteModel(
    _ moduleJson: String,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      rejecter("PARSE_ERROR", "Failed to parse module JSON", nil)
      return
    }

    OnDeviceOCRManager.shared.deleteModel(modelClass, withModelSize: modelSize)
    resolver(true)
  }

  /// Perform on-device OCR prediction with explicit module selection
  @objc func predictWithModule(
    _ moduleJson: String,
    imagePath: String,
    barcodes: [[String: Any]]?,
    resolver: @escaping RCTPromiseResolveBlock,
    rejecter: @escaping RCTPromiseRejectBlock
  ) {
    guard let (modelClass, modelSize) = parseOCRModule(moduleJson) else {
      rejecter("PARSE_ERROR", "Failed to parse module JSON", nil)
      return
    }

    loadImage(from: imagePath) { image in
      guard let image = image else {
        rejecter("IMAGE_LOAD_ERROR", "Failed to load image from path: \(imagePath)", nil)
        return
      }

      // Convert UIImage to CIImage
      guard let ciImage = convertToCIImage(from: image) else {
        rejecter("IMAGE_CONVERSION_ERROR", "Failed to convert UIImage to CIImage", nil)
        return
      }

      let barcodeList = barcodes ?? []
      let allBarcodes: [DetectedCode] = barcodeList.map { barcodeDict in
        let stringValue: String = (barcodeDict["scannedCode"] as? String) ?? ""
        let symbology: VisionSDK.BarcodeSymbology = VisionSDK.BarcodeSymbology.value(VNStringValue: (barcodeDict["symbology"] as? String) ?? "")
        let extractedData: [String: String]? = (barcodeDict["gs1ExtractedInfo"] as? [String: String])

        var finalRect: CGRect = .zero
        if let boundingBoxRect = barcodeDict["boundingBox"] as? [String: CGFloat] {
          let x: CGFloat = boundingBoxRect["x"] ?? 0
          let y: CGFloat = boundingBoxRect["y"] ?? 0
          let width: CGFloat = boundingBoxRect["width"] ?? 0
          let height: CGFloat = boundingBoxRect["height"] ?? 0
          finalRect = CGRect(x: x, y: y, width: width, height: height)
        }

        return DetectedCode(stringValue: stringValue, symbology: symbology, extractedData: extractedData, boundingBox: finalRect)
      }

      // Call extractDataFromImageUsing with explicit modelClass and modelSize
      OnDeviceOCRManager.shared.extractDataFromImageUsing(
        ciImage,
        withBarcodes: allBarcodes,
        checkImageSharpness: false,
        modelClass: modelClass,
        withModelSize: modelSize
      ) { data, error in
        if let error = error {
          rejecter("PREDICTION_FAILED", error.localizedDescription, error)
        } else if let data = data {
          let responseString = String(data: data, encoding: .utf8) ?? ""
          resolver(responseString)
        } else {
          rejecter("NO_DATA_ERROR", "No data returned from prediction", nil)
        }
      }
    }
  }

}
