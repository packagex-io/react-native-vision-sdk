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
}
