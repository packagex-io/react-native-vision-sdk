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


  @objc func unLoadOnDeviceModels(
      _ modelType: String?,
      shouldDeleteFromDisk: Bool,
      resolver: @escaping RCTPromiseResolveBlock,
      rejecter: @escaping RCTPromiseRejectBlock
  ) {
    if let modelType = modelType {
      let modelClass = getModelType(modelType)
      
      do {
        try OnDeviceOCRManager.shared.deconfigureOfflineOCR(for: modelClass, shouldDeleteFromDisk: shouldDeleteFromDisk)
      }
      catch(let error) {
        rejecter("MODEL_UNLOAD_ERROR", error.localizedDescription, nil)
      }
      
      resolver("Model unloaded successfully")
      
    }
    else {
      do {
        try OnDeviceOCRManager.shared.deconfigureOfflineOCR(shouldDeleteFromDisk: shouldDeleteFromDisk)
      }
      catch(let error) {
        
        rejecter("MODEL_UNLOAD_ERROR", error.localizedDescription, nil)
      }
      
      resolver("Model unloaded successfully")
    }
    
  }
  
  
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

      OnDeviceOCRManager.shared.prepareOfflineOCR(
        withApiKey: apiKey,
        andToken: token,
        forModelClass: modelClass,
        withModelSize: modelSizeEnum,
        withProgressTracking: { currentProgress, totalSize, isModelAlreadyDownloaded in
            let progressData: [String: Any] = [
                "progress": isModelAlreadyDownloaded ? 1.0 : (currentProgress / totalSize),
                "downloadStatus": isModelAlreadyDownloaded,
                "isReady": false
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
          print(error)
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
            print(error)
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
          print("Image path is empty.")
          completion(nil)
          return
      }

      if imagePath.hasPrefix("http") {
          // It's a remote URL
          guard let remoteUrl = URL(string: imagePath) else {
              print("Invalid remote URL.")
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
              print("Invalid local URL.")
              completion(nil)
              return
          }

          if !FileManager.default.fileExists(atPath: localUrl.path) {
              print("File does not exist at path: \(localUrl.path)")
              completion(nil)
              return
          }

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
    // Bill of Lading uses the same scan API as shipping labels
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
        andMetaData: [:],
        andRecipient: [:],
        andSender: [:],
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

}
