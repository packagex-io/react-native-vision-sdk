import Foundation
import VisionSDK

@objc(VisionSdkModule)
class VisionSdkModule: RCTEventEmitter {
   override static func moduleName() -> String {
        return "VisionSdkModule"
    }

  override func supportedEvents() -> [String]! {
      return ["onModelDownloadProgress"]
  }

  @objc override func constantsToExport() -> [AnyHashable: Any]! {
    return [:]
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

  
  
}
