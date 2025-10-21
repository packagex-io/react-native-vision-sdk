
import UIKit
import VisionSDK
import AVFoundation

//MARK: - React Native CodeScannerView Wrapper
//MARK: -
class RNCodeScannerView: UIView {

    //MARK: - Events from Swift to Js, on any update to sent back
    @objc var onBarcodeScan: RCTDirectEventBlock?
    @objc var onModelDownloadProgress: RCTDirectEventBlock?
    @objc var onImageCaptured: RCTDirectEventBlock?
    @objc var onSharpnessScore: RCTDirectEventBlock?
    @objc var onOCRScan: RCTDirectEventBlock?
    @objc var onDetected: RCTDirectEventBlock?
    @objc var onError: RCTDirectEventBlock?
    @objc var onBoundingBoxesDetected: RCTDirectEventBlock?
    @objc var onPriceTagDetected: RCTDirectEventBlock?
    @objc var onCreateTemplate: RCTDirectEventBlock?
    @objc var onGetTemplates: RCTDirectEventBlock?
    @objc var onDeleteTemplateById: RCTDirectEventBlock?
    @objc var onDeleteTemplates: RCTDirectEventBlock?

    //MARK: - CodeScannerView Instance
    var codeScannerView: CodeScannerView?

    //MARK: - Variable Details
    // Dynamic Props are the one that are being passed by Client side, but if Client doesn't passed value then default values will be set.
    // Optional Props are the one that are not necessory to be sent from Client side, hence default values will be set.

    //MARK: - Props Received from React-Native
    var token: String?  // Dynamic Prop | Optional:
    var locationId: String? // Dynamic Prop | Optional:
    var options: [String: Any]? // Dynamic Prop | Optional:
    var metaData: [String: Any]? // Dynamic Prop | Optional:
    var recipient: [String: Any]? // Dynamic Prop | Optional:
    var sender: [String: Any]? // Dynamic Prop | Optional:
    var scanMode: CodeScannerMode = .barCode // Dynamic Prop | Optional:
    var isEnableAutoOcrResponseWithImage: Bool? = false // Dynamic Prop | Optional:
    var captureMode: CaptureMode = .manual // Dynamic Prop | Optional:
    var isMultipleScanEnabled: CaptureType = .single // Static Prop | Optional:
    var sessionPreset: AVCaptureSession.Preset = .high

    //MARK: - On-Device OCR Specific Variables
    var ocrMode: String = "cloud" // Dynamic Prop | Optional:
    var ocrType: String = "shipping-label" // Dynamic Prop | Optional:
    var shouldResizeImage: Bool = true // Dynamic Prop | Optional:
    var onDeviceModelType: VSDKModelExternalClass = VSDKModelExternalClass.shippingLabel // Dynamic Prop | Optional:
    var onDeviceModelSize: VSDKModelExternalSize = VSDKModelExternalSize.large // Dynamic Prop | Optional:

    private var previousSize: CGSize = .zero

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let codeScannerView = codeScannerView else { return }
        let newSize = bounds.size
        if previousSize != newSize {
            previousSize = newSize
          codeScannerView.frame = self.bounds
        }
    }

    //MARK: - Initializer
    init() {

        super.init(frame: UIScreen.main.bounds)
//        codeScannerView?.stopRunning()
        codeScannerView = CodeScannerView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height))
        self.addSubview(codeScannerView!)
        codeScannerView!.configure(delegate: self, sessionPreset: sessionPreset, captureMode: captureMode, captureType: isMultipleScanEnabled, scanMode: scanMode)

      codeScannerView?.stopRunning()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    // MARK: - Find Parent UIViewController
    // Helper function to find the parent view controller
    func findViewController() -> UIViewController? {
        var responder: UIResponder? = self
        while let r = responder {
            if let viewController = r as? UIViewController {
                return viewController
            }
            responder = r.next
        }
        return nil
    }

    // MARK: - Template Controller Functions
    // Open the template creation controller
    @available(iOS 15.0, *)
    @objc func createTemplate() {
      // Defer to the next run-loop on the main thread
      DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(25)) { [weak self] in
          guard let self = self else { return }

          let scanController = GenerateTemplateController.instantiate()
          scanController.delegate = self

          if let sheet = scanController.sheetPresentationController {
              sheet.prefersGrabberVisible = true
          }

          if let parent = self.findViewController() {
              parent.present(scanController, animated: true) {
                  print("Template controller is now presented.")
              }
          } else {
              print("❌ No UIViewController found to present from.")
          }
      }
    }

    // Fetch saved templates
    @objc func getAllTemplates() {
        let savedTemplates = CodeScannerView.getAllTemplates()
        print("Saved Template IDs: \(savedTemplates)")
        onGetTemplates!(["data": savedTemplates])
    }

    // Delete template with specific ID
    @objc func deleteTemplate(withId id: String) {
        CodeScannerView.deleteTemplateWithId(id)
        onDeleteTemplateById!(["data": id])
        print("Template with ID \(id) has been deleted.")
    }

    // Delete all templates
    @objc func deleteAllTemplates() {
        CodeScannerView.deleteAllTemplates()
        onDeleteTemplates!(["data": "All templates have been deleted."])
        print("All templates have been deleted.")
    }
}

// MARK: - VisionSDK GenerateTemplateControllerDelegate functions
//MARK: -
extension RNCodeScannerView: GenerateTemplateControllerDelegate {

    // Template creation success
    func templateScanController(_ controller: GenerateTemplateController, didFinishWith result: String) {
        print("Template created successfully with ID: \(result)")

        if !result.isEmpty {
            if let onCreateTemplate = onCreateTemplate {
                onCreateTemplate(["data": result]) // Safely call onCreateTemplate
            } else {
                print("Error: onCreateTemplate is nil.")
            }
        } else {
            if let onError = onError {
                onError(["message": "Result is empty or onCreateTemplate is nil.", "code": ""])
            } else {
                print("Error: onError is nil.")
            }
        }

        // Dismiss the controller
        controller.dismiss(animated: true)
    }

    // Template creation failure
    func templateScanController(_ controller: GenerateTemplateController, didFailWithError error: any Error) {
        print("Template creation failed with error: \(error.localizedDescription)")
        onError!(["message": error.localizedDescription, "code": error])
        controller.dismiss(animated: true)
    }

    // Template creation cancelled
    func templateScanControllerDidCancel(_ controller: GenerateTemplateController) {
        print("Template creation cancelled by the user.")
     // showInfoAlert(message: "Template creation cancelled.")
        controller.dismiss(animated: true)
    }
}

// MARK: - VisionSDK CodeScannerViewDelegate functions
//MARK: -
extension RNCodeScannerView: CodeScannerViewDelegate {
  func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didSuccess codes: [VisionSDK.DetectedCode]) {
    if onBarcodeScan != nil {

      var codesArray: [[String: Any]] = []

      for code in codes{
        var codeInfo: [String: Any] = [:]
        codeInfo["scannedCode"] = code.stringValue
        codeInfo["symbology"] = code.symbology.stringValue()
        codeInfo["boundingBox"] = dict(from: code.boundingBox)
        if let gs1Info = code.extractedData {
          codeInfo["gs1ExtractedInfo"] = gs1Info
        }
        codesArray.append(codeInfo)
      }

      onBarcodeScan!(["codes": codesArray])
    }

  }

    func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didFailure error: NSError) {
        if onError != nil {
          if error.code != 13 && error.code != 14 && error.code != 15 && error.code != 16 {
            onError!(["message": error.localizedDescription, "code": error.code])
          }
        }
    }

    func codeScannerViewDidDetect(_ text: Bool, barCode: Bool, qrCode: Bool, document: Bool) {

        if onDetected != nil {
            onDetected!(["text": text, "barcode": barCode, "qrcode": qrCode, "document": document])
        }
    }
  
    func codeScannerView(_ imageSharpnessScore: Float) {
      //
      if onSharpnessScore != nil {
        onSharpnessScore!(["sharpnessScore": imageSharpnessScore])
      }
    }
  
    func dict(from rect: CGRect) -> [String: CGFloat] {
      return [
        "x":      rect.origin.x,
        "y":      rect.origin.y,
        "width":  rect.size.width,
        "height": rect.size.height
      ]
    }

//    func codeScannerViewDidDetectBoxes(_ text: Bool, barCode: [CGRect], qrCode: [CGRect], document: CGRect) {
//      guard let callback = onBoundingBoxesDetected else { return }
//
//      // Helper to convert a single CGRect -> [String: CGFloat]
//
//
//      // Convert arrays of CGRects
//      let barcodeBoundingBoxes = barCode.map { dict(from: $0) }
//      let qrCodeBoundingBoxes  = qrCode.map { dict(from: $0) }
//      let documentBoundingBox  = dict(from: document)
//
//      callback([
//        "barcodeBoundingBoxes": barcodeBoundingBoxes,
//        "qrCodeBoundingBoxes":  qrCodeBoundingBoxes,
//        "documentBoundingBox":  documentBoundingBox
//      ])
//    }
  
  func codeScannerViewDidDetectBoxes(_ text: Bool, barCode: [DetectedCode], qrCode: [DetectedCode], document: CGRect) {
    
    guard let onBoundingBoxesUpdate = onBoundingBoxesDetected else { return }

    

    // Convert arrays of CGRects
    let barcodeBoundingBoxes = barCode.map { code in
      return [
        "scannedCode": code.stringValue,
        "symbology": code.symbology.stringValue(),
        "gs1ExtractedInfo": code.extractedData ?? [:],
        "boundingBox": dict(from: code.boundingBox)
      ]
    }
    
    let qrCodeBoundingBoxes = qrCode.map { code in
      return [
        "scannedCode": code.stringValue,
        "symbology": code.symbology.stringValue(),
        "gs1ExtractedInfo": code.extractedData ?? [:],
        "boundingBox": dict(from: code.boundingBox)
      ]
    }
    
    let documentBoundingBox = dict(from: document)

    onBoundingBoxesUpdate([
        "barcodeBoundingBoxes": barcodeBoundingBoxes,
        "qrCodeBoundingBoxes": qrCodeBoundingBoxes,
        "documentBoundingBox": documentBoundingBox
    ])
  }
  
  func codeScannerViewDidCapturePrice(_ price: String, withSKU sKU: String, withBoundingBox boundingBox: CGRect) -> Bool {
    print("Price: \(price), SKU: \(sKU), Bounding Box: \(boundingBox)")
    
    if(onPriceTagDetected != nil){
      onPriceTagDetected!([
        "price": price,
        "sku": sKU,
        "boundingBox": dict(from: boundingBox)
      ])
    }
    
    return true
  }

  func codeScannerView(_ scannerView: CodeScannerView, didCaptureOCRImage image: UIImage, withCroppedImge croppedImage: UIImage?, withBarcodes barcodes: [DetectedCode], imageSharpnessScore: Float) {
    
    guard let savedImageURL = saveImageToVisionSDK(image: image, withName: UUID().uuidString) else {
          print("❌ Image saving failed.")
          onError?(["message": "Error converting image: Could not save image to disk."])
          return
      }

      // Log the successful save
    print("✅ Image saved successfully at: \(savedImageURL.path)")
    handleCapturedImage(withImage: savedImageURL, barcodes: barcodes, nativeImage: image, sharpnessScore: imageSharpnessScore)
//        if scanMode != .ocr {
//            print("Scan mode is not OCR. Skipping OCR logic.")
//            return // Exit the function early
//        }
    print("isEnableAutoOcrResponseWithImage: ", isEnableAutoOcrResponseWithImage as Any, " scanMode: ", scanMode, "")
    guard isEnableAutoOcrResponseWithImage == true, scanMode == .ocr else {
          print("Auto OCR response with image is disabled or scan mode is not OCR. Exiting function.")
          return
    }
    handelAutoOcrResponseWithImage(image: image, barcodes: barcodes,  imagePath: savedImageURL.path)
    
  }
  

  func handelAutoOcrResponseWithImage(image: UIImage, barcodes: [VisionSDK.DetectedCode], imagePath:String?) {
        guard isEnableAutoOcrResponseWithImage == true, scanMode == .ocr else {
              print("Auto OCR response with image is disabled or scan mode is not OCR. Exiting function.")
              return
        }
        print("isEnableAutoOcrResponseWithImage: ", isEnableAutoOcrResponseWithImage ?? false ? "true": "false")

        switch ocrMode {
          case "cloud":
              switch ocrType {
              case "shipping_label", "shipping-label":
                  self.getPredictionShippingLabelCloud(
                    withImage: image,
                    andBarcodes: barcodes,
                    imagePath: imagePath,
                    token: self.token,
                    apiKey: VSDKConstants.apiKey,
                    locationId: self.locationId,
                    options: self.options ?? [:],
                    metadata: self.metaData ?? [:],
                    recipient: self.recipient ?? [:],
                    sender: self.sender ?? [:],
                    shouldResizeImage: self.shouldResizeImage
                    )
              case "item_label", "item-label":
                self.getPredictionItemLabelCloud(
                    withImage: image,
                    imagePath: imagePath,
                    token: self.token,
                    apiKey: VSDKConstants.apiKey,
                    shouldResizeImage: self.shouldResizeImage
                  )
              case "bill_of_lading", "bill-of-lading":
                self.getPredictionBillOfLadingCloud(
                    withImage: image,
                    andBarcodes: barcodes.map(\.stringValue),
                    imagePath:imagePath,
                    token: self.token,
                    apiKey: VSDKConstants.apiKey,
                    locationId: self.locationId,
                    options: self.options ?? [:],
                    shouldResizeImage: self.shouldResizeImage
                  )
              case "document_classification", "document-classification":
                  self.getPredictionDocumentClassificationCloud(
                    withImage: image,
                    imagePath: imagePath,
                    token: self.token,
                    apiKey: VSDKConstants.apiKey,
                    shouldResizeImage: self.shouldResizeImage
                  )
              default:
                  print("Unsupported OCR type for cloud mode: \(ocrType)")
              }

          case "on-device", "on_device":
              switch ocrType {
              case "on-device-with-translation", "on_device_with_translation":
                  self.getPredictionWithCloudTransformations(
                    withImage: image,
                    andBarcodes: barcodes,
                    imagePath: imagePath,
                    token: self.token,
                    apiKey: VSDKConstants.apiKey,
                    locationId: self.locationId,
                    options: self.options ?? [:],
                    metadata: self.metaData ?? [:],
                    recipient: self.recipient ?? [:],
                    sender: self.sender ?? [:],
                    shouldResizeImage: self.shouldResizeImage
                  )
              default:
                  self.getPrediction(withImage: image, andBarcodes: barcodes, imagePath: imagePath) // Handle all other ocrTypes for on-device
              }

          default:
              print("Unsupported OCR mode: \(ocrMode)")
          }
    }
}

// MARK: - VisionSDK On-Device call functions, onDeviceFlow
//MARK: -
extension RNCodeScannerView {

    /// This method initialises and setup On-Device OCR model to detect labels, can be called from client side, will download and prepare model only if scanMode == ocr
    func configureOnDeviceModel(
        modelType: String,
        modelSize: String?,
        token: String?,
        apiKey: String?
      ) {
        // Calling the setupDownloadOnDeviceOCR method
        setupDownloadOnDeviceOCR(modelType: modelType,
                                 modelSize: modelSize,
                                 token: token,
                                 apiKey: apiKey
        ) {
            // Completion block after OCR setup
            // Add any actions you want to take after the OCR is downloaded and prepared
            debugPrint("On-Device OCR setup completed")
        }
    }

    /// This method downloads and prepares offline / On Device OCR for use in the app.
    /// - Parameter completion: completionHandler
    func setupDownloadOnDeviceOCR(
      modelType: String,
      modelSize: String?,
      token: String?,
      apiKey: String?
      ,completion: @escaping () -> Void) {
        debugPrint("setupDownloadOnDevice", modelType)
        debugPrint(modelSize)

        var apiKeyValue: String? = nil
        if let apiKey = apiKey, !apiKey.isEmpty {
            apiKeyValue = apiKey
        }

        var tokenValue: String? = nil
        if let token = token, !token.isEmpty {
            tokenValue = token
        }




        // Calling the prepareOfflineOCR method with progress tracking
        if let modelSize = modelSize {
            OnDeviceOCRManager.shared.prepareOfflineOCR(withApiKey: apiKeyValue,
                                                        andToken: tokenValue,
                                                        forModelClass: getModelType(modelType),
                                                        withModelSize: getModelSize(modelSize) ?? VSDKModelExternalSize.micro) { currentProgress, totalSize, isModelAlreadyDownloaded in
                // If the model is already downloaded, set progress to 100% and download status to true
                if isModelAlreadyDownloaded {
                    self.onModelDownloadProgress!(["progress": (1),
                                                   "downloadStatus": true, // Indicate that the model is already downloaded
                                                   "isReady": false])
                } else {
                    // Progress tracking and debugging output
                    debugPrint(String(format: "Download progress: %.2f%%", (currentProgress / totalSize) * 100))

                    // Calling the download progress handler
                    self.onModelDownloadProgress!(["progress": ((currentProgress / totalSize)),
                                                   "downloadStatus": false,// Update download status to false during download
                                                   "isReady": false])
                }
            } withCompletion: { error in
                // Handling download completion
                if error == nil {
                    // If no error, set progress to 100% and download status to true
                    self.onModelDownloadProgress!(["progress": (1),
                                                   "downloadStatus": true, // Indicating successful download completion
                                                   "isReady": true])
                    completion() // Call completion to indicate success
                } else {
                    self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "We got On-Device OCR error!")
                }
            }
        }
        else {
            print("downloaded without size")
            OnDeviceOCRManager.shared.prepareOfflineOCR(withApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil,
                                                        andToken: tokenValue,
                                                        forModelClass: getModelType(modelType)) { currentProgress, totalSize, isModelAlreadyDownloaded in
                // If the model is already downloaded, set progress to 100% and download status to true
                if isModelAlreadyDownloaded {
                    self.onModelDownloadProgress!(["progress": (1),
                                                   "downloadStatus": true,// Indicate that the model is already downloaded
                                                   "isReady": false])
                } else {
                    // Progress tracking and debugging output
                    debugPrint(String(format: "Download progress: %.2f%%", (currentProgress / totalSize) * 100))

                    // Calling the download progress handler
                    self.onModelDownloadProgress!(["progress": ((currentProgress / totalSize)),
                                                   "downloadStatus": false,// Update download status to false during download
                                                   "isReady": false])
                }
            } withCompletion: { error in
                // Handling download completion
                if error == nil {
                    // If no error, set progress to 100% and download status to true
                    self.onModelDownloadProgress!(["progress": (1),
                                                   "downloadStatus": true, // Indicating successful download completion
                                                   "isReady": true]) // indicates that the model is successfully initialized after downloaidng
                    completion() // Call completion to indicate success
                } else {
                    self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "We got On-Device OCR error!")
                }
            }
        }

    }

    /// This method pass ciImage of label to downloaded on-device OCR model that extracts informations from label and returns the response
    /// - Parameters:
    ///   - uiImage: uiImage of label that is going to be detected
    ///   - barcodes: barcodes that are detected by SDK within the label
  func getPrediction(withImage uiImage: UIImage, andBarcodes barcodes: [VisionSDK.DetectedCode], imagePath: String?) {
      debugPrint("GET PREDICTION METHOD")
        guard let ciImage = convertToCIImage(from: uiImage) else {
            callForOCRWithImageFailedWithMessage(message: "Failed to convert UIImage to CIImage.")
            return
        }

        OnDeviceOCRManager.shared.extractDataFromImageUsing(ciImage, withBarcodes: barcodes) { [weak self] data, error in
            if let error = error {
                self?.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
            } else {
                guard let responseData = data else {
                    DispatchQueue.main.async {
                        self?.callForOCRWithImageFailedWithMessage(message: "Data of request was found nil")
                    }
                    return
                }
                // Directly call on-device API response
                self?.onDeviceAPIResponse(responseData: responseData, imagePath:imagePath)
            }
        }
    }

    /// This method pass ciImage of label to downloaded on-device OCR model that extracts informations from label and returns the response
    /// - Parameters:
    ///   - uiImage: uiImage of label that is going to be detected
    ///   - barcodes: barcodes that are detected by SDK within the label
    func getPredictionWithCloudTransformations(
        withImage uiImage: UIImage,
        andBarcodes barcodes: [VisionSDK.DetectedCode],
        imagePath:String?,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: [String: Any]?,
        metadata: [String: Any]?,
        recipient: [String: Any]?,
        sender: [String: Any]?,
        shouldResizeImage: Bool
    ) {
      guard let ciImage = convertToCIImage(from: uiImage) else {
        callForOCRWithImageFailedWithMessage(message: "Failed to convert UIImage to CIImage.")
        return
      }
      
      OnDeviceOCRManager.shared.extractDataFromImageUsing(ciImage, withBarcodes: barcodes) { [weak self] data, error in
        if let error = error {
          self?.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
        }
        else {
          guard let responseData = data else {
            DispatchQueue.main.async {
              self?.callForOCRWithImageFailedWithMessage(message: "Data of request was found nil")
            }
            return
          }
          
          if let ocrMode = self?.ocrMode.lowercased(),
             ["on-device-with-translation", "on_device_with_translation"].contains(ocrMode) {
            // on-device + matching API case
            self?.callMatchingAPIWithImage(
              usingImage: uiImage,
              withbarCodes: barcodes.map(\.stringValue),
              responseData: responseData,
              imagePath: imagePath,
              token: token,
              apiKey: apiKey,
              locationId: locationId,
              options: options,
              metadata: metadata,
              recipient: recipient,
              sender: sender,
              shouldResizeImage: shouldResizeImage
            )
          }
          else {
            self?.onDeviceAPIResponse(responseData: responseData, imagePath:imagePath)
          }
        }
      }
    }
    /// This Method is responsible for sending the on-device extracted data object to client side.
    /// - Parameter responseData: responseData that needs to be sent back to client
    func onDeviceAPIResponse(responseData: Data, imagePath:String?) {
        DispatchQueue.main.async {
            do {
                if let jsonResponse = try JSONSerialization.jsonObject(with: responseData) as? [String: Any] {
                    self.callForOCRWithImageCompletedWithData(data: jsonResponse, imagePath:imagePath)
                }
            }
            catch let error {
                self.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
            }
        }
    }
}
// MARK: - VisionSDK API call function, getPredictionShippingLabelCloud, getPredictionBillOfLadingCloud, callMatchingAPIWithImage, handleAPIResponse
//MARK: -
extension RNCodeScannerView {

    /// This Functions takes input params from VisionSDK and calls OCR API and fetch response from the server to pass it to react native app, function is called from the codeScannerView delegate method
    /// - Parameters:
    ///   - image: Captured image from VisionSDK to pass into API
    ///   - barcodes: Detected barcodes from VisionSDK to pass into API
    func getPredictionShippingLabelCloud(
      withImage image: UIImage,
      andBarcodes barcodes: [VisionSDK.DetectedCode],
      imagePath:String?,
      token: String?,
      apiKey: String?,
      locationId: String?,
      options: [String: Any]?,
      metadata: [String: Any]?,
      recipient: [String: Any]?,
      sender: [String: Any]?,
      shouldResizeImage: Bool
    ) {
      VisionAPIManager.shared.callScanAPIWith(
          image,
          andBarcodes: barcodes.map(\.stringValue),
          andApiKey: apiKey?.isEmpty == false ? apiKey : nil, //!VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil,
          andToken: token?.isEmpty == false ? token : nil,
          andLocationId: locationId ?? "",
          andOptions: options ?? [:],
          andMetaData: metaData ?? [:],
          andRecipient: recipient ?? [:],
          andSender: sender ?? [:],
          withImageResizing: shouldResizeImage
        ) {
            [weak self] data, error in
            guard let self = self else { return }
            handleAPIResponse(error: error, data: data, imagePath:imagePath)
        }
    }
    /// This Functions takes input params from VisionSDK and calls OCR API and fetch response from the server to pass it to react native app, function is called from the codeScannerView delegate method
    /// - Parameters:
    ///   - image: Captured image from VisionSDK to pass into API
    ///   - barcodes: Detected barcodes from VisionSDK to pass into API
    ///   - withImageResizing: Resizing


     func getPredictionBillOfLadingCloud(
        withImage image: UIImage,
        andBarcodes barcodes: [String],
        imagePath: String?,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: [String: Any]?,
        shouldResizeImage: Bool
    ) {
        // Construct the API call
        if let validLocationId = locationId, !validLocationId.isEmpty {
            VisionAPIManager.shared.getPredictionBillOfLadingCloud(
                image,
                andBarcodes: barcodes,
                andApiKey: apiKey?.isEmpty == false ? apiKey : nil,
                andToken:  token?.isEmpty == false ? token : nil,
                andLocationId: locationId ?? "",
                andOptions: options ?? [:],
                withImageResizing: shouldResizeImage
            ) { [weak self] data, error in
                guard let self = self else { return }
                self.handleAPIResponse(error: error, data: data, imagePath: imagePath)
            }
        } else {
            VisionAPIManager.shared.getPredictionBillOfLadingCloud(
                image,
                andBarcodes: barcodes,
                andApiKey: apiKey?.isEmpty == false ? apiKey : nil,
                andToken: token?.isEmpty == false ? token : nil,
                andOptions: options ?? [:],
                withImageResizing: shouldResizeImage
            ) { [weak self] data, error in
                guard let self = self else { return }
                self.handleAPIResponse(error: error, data: data, imagePath: imagePath)
            }
        }
    }

    /// This Functions takes input params from VisionSDK and calls OCR API and fetch response from the server to pass it to react native app, function is called from the codeScannerView delegate method
    /// - Parameters:
    ///   - image: Captured image from VisionSDK to pass into API
    ///   - withImageResizing: Resizing
  func getPredictionItemLabelCloud(
      withImage image: UIImage,
      imagePath:String?,
      token: String?,
      apiKey: String?,
      shouldResizeImage: Bool
    ) {
        let tokenValue = token?.isEmpty == false ? token : nil
        let apiKey = apiKey?.isEmpty == false ? apiKey : nil

        print("getPredictionItemLabelCloud", apiKey ?? "")
        print("getPredictionItemLabelCloud", tokenValue ?? "")

    VisionAPIManager.shared.callItemLabelsAPIWith(
        image ,
        andApiKey: apiKey,
        andToken: tokenValue,
        withImageResizing: shouldResizeImage
      ) {
            [weak self] data, error in

            // Early exit if self is deallocated.
            guard let self = self else { return }

            handleAPIResponse(error: error, data: data, imagePath: imagePath)
        }
    }

    /// This Functions takes input params from VisionSDK and calls OCR API and fetch response from the server to pass it to react native app, function is called from the codeScannerView delegate method
    /// - Parameters:
    ///   - image: Captured image from VisionSDK to pass into API

    func getPredictionDocumentClassificationCloud(
        withImage image: UIImage,
        imagePath:String?,
        token: String?,
        apiKey: String?,
        shouldResizeImage: Bool
      ) {
        let tokenValue = token?.isEmpty == false ? token : nil
        let apiKey = apiKey?.isEmpty == false ? apiKey : nil

        print("getPredictionDocumentClassificationCloud", apiKey ?? "")
        print("getPredictionDocumentClassificationCloud", tokenValue ?? "")

      VisionAPIManager.shared.callDocumentClassificationAPIWith(
          image ,
          andApiKey: apiKey,
          andToken: tokenValue,
          withImageResizing: shouldResizeImage
        ) {
            [weak self] data, error in

            // Early exit if self is deallocated.
            guard let self = self else { return }

            handleAPIResponse(error: error, data: data, imagePath:imagePath)
        }
    }

    /// This Functions takes input params from On-Device VisionSDK flow and calls OCR Matching API to translate response from Platforms format to Receive App format.
    /// - Parameters:
    ///   - uiImage: Captured image from VisionSDK to pass into API
    ///   - barcodes: Detected barcodes from VisionSDK to pass into API
    ///   - responseData: response data received from On-Device model and needs to be translated into another format.
    func callMatchingAPIWithImage(
        usingImage uiImage: UIImage,
        withbarCodes barcodes: [String],
        responseData: Data,
        imagePath:String?,
        token: String?,
        apiKey: String?,
        locationId: String?,
        options: [String: Any]?,
        metadata: [String: Any]?,
        recipient: [String: Any]?,
        sender: [String: Any]?,
        shouldResizeImage: Bool
      ) {
        var tokenValue: String? = nil
        if let token = self.token, !token.isEmpty {
            tokenValue = token
        }

      VisionAPIManager.shared.callMatchingAPIWith(
        uiImage,
        andBarcodes: barcodes,
        andApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil,
        andToken: token?.isEmpty == false ? token : nil,
        withResponseData: responseData,
        andLocationId: (locationId ?? "").isEmpty ? nil : locationId,
        andOptions: options ?? [:],
        andMetaData: metaData ?? [:],
        andRecipient: recipient ?? [:],
        andSender: sender ?? [:],
        withImageResizing: shouldResizeImage) { [weak self] data, error in

            guard let self = self else { return }

            handleAPIResponse(error: error, data: data, imagePath: imagePath)
        }
    }

    /// This method processes an optional `UIImage` of a label (which is converted to a `CIImage`) and passes it to the on-device OCR model for error reporting.
    /// - Parameters:
    ///   - uiImage: An optional `UIImage` of the label to be detected. This image is converted into a `CIImage` for processing by the OCR model. Default is `nil`.
    ///   - reportText: A string containing the report text (such as an error message or status update), which will be included in the error report. This is a required parameter.
    ///   - response: An optional data response (e.g., from a server or API) that may be included in the error report for additional context. Default is `nil`.
    ///   - modelType: A string representing the model type used for processing (e.g., "item_label", "shipping_label", "bill_of_lading"). This is a required parameter.
    ///   - modelSize: A string representing the model size used for processing (e.g., "large", "medium", "small"). This is a required parameter.
    /// - Note: The method uses the `OnDeviceOCRManager.shared.reportErrorWith` function to handle the OCR processing and error reporting.
    func reportError(
        uiImage: UIImage? = nil,
        reportText: String,
        response: Data? = nil,
        modelType: String,
        modelSize: String,
        errorFlags: [String: Bool]? = nil,
        token: String?,
        apiKey: String?
    ) {

      let tokenValue = token?.isEmpty == false ? token : nil
      let apiKey = apiKey?.isEmpty == false ? apiKey : nil

      var parentReportModel: VisionSDK.VSDKAnalyticsReportModel?


      guard let errorFlags = errorFlags else { return }
      let modelClass = getModelType(modelType)

      switch(modelClass){
      case .shippingLabel:
          let slModelToReport = VisionSDK.SLReportModel()
          slModelToReport.isCourierNameWrong = errorFlags["courierName"] ?? false
          slModelToReport.isSenderNameWrong = errorFlags["senderName"] ?? false
          slModelToReport.isSenderAddressWrong = errorFlags["senderAddres"] ?? false
          slModelToReport.isReceiverNameWrong = errorFlags["receiverName"] ?? false
          slModelToReport.isReceiverAddressWrong = errorFlags["receiverAddress"] ?? false
          slModelToReport.isDimensionsValueWrong = errorFlags["dimensions"] ?? false
          slModelToReport.isTrackingNoWrong = errorFlags["trackingNo"] ?? false
          slModelToReport.isWeightWrong = errorFlags["weight"] ?? false
          parentReportModel = slModelToReport

      case .billOfLading:
          let bolModelToReport = VisionSDK.BOLReportModel()
          bolModelToReport.isReferenceNoWrong = errorFlags["referenceNo"] ?? false
          bolModelToReport.isLoadNumberWrong = errorFlags["loadNumber"] ?? false
          bolModelToReport.isPurchaseOrderNumberWrong = errorFlags["purchaseOrderNumber"] ?? false
          bolModelToReport.isInvoiceNumberWrong = errorFlags["invoiceNumber"] ?? false
          bolModelToReport.isCustomerPurchaseOrderNumberWrong = errorFlags["customerPurchaseOrderNumber"] ?? false
          bolModelToReport.isOrderNumberWrong = errorFlags["orderNumber"] ?? false
          bolModelToReport.isBillOfLadingWrong = errorFlags["billOfLading"] ?? false
          bolModelToReport.isMasterBillOfLadingWrong = errorFlags["masterBillOfLading"] ?? false
          bolModelToReport.isLineBillOfLadingWrong = errorFlags["lineBillOfLading"] ?? false
          bolModelToReport.isHouseBillOfLadingWrong = errorFlags["houseBillOfLading"] ?? false
          bolModelToReport.isShippingIdWrong = errorFlags["shippingId"] ?? false
          bolModelToReport.isShippingDateWrong = errorFlags["shippingDate"] ?? false
          bolModelToReport.isDateWrong = errorFlags["date"] ?? false
          parentReportModel = bolModelToReport

      case .itemLabel:
          let ilModelToReport = VisionSDK.ILReportModel()
          ilModelToReport.isSupplierNameWrong = errorFlags["supplierName"] ?? false
          ilModelToReport.isItemNameWrong = errorFlags["itemName"] ?? false
          ilModelToReport.isItemSKUWrong = errorFlags["itemSKU"] ?? false
          ilModelToReport.isWeightWrong = errorFlags["weight"] ?? false
          ilModelToReport.isQuantityWrong = errorFlags["quantity"] ?? false
          ilModelToReport.isDimensionsValueWrong = errorFlags["dimensions"] ?? false
          ilModelToReport.isProductionDateWrong = errorFlags["productionDate"] ?? false
          ilModelToReport.isSupplierAddressWrong = errorFlags["supplierAddress"] ?? false
          parentReportModel = ilModelToReport


      case .documentClassification:
          let dcModelToReport = VisionSDK.DCReportModel()
          dcModelToReport.isDocumentClassWrong = errorFlags["documentClass"] ?? false
          parentReportModel = dcModelToReport

      @unknown default:
        print("⚠️ Unknown modelClass encountered: \(modelClass)")

      }


        // Call the `reportErrorWith` function of the OnDeviceOCRManager
        OnDeviceOCRManager.shared.reportErrorWith(
            apiKey,
            andToken: tokenValue,
            forModelClass: getModelType(modelType),
            withModelSize: getModelSize(modelSize) ?? VSDKModelExternalSize.large,
            image: uiImage?.ciImage ?? nil,
            reportText: reportText,
            response: response,
            reportModel: parentReportModel

        ) { responseCode in
            print("Full JSON Response reportError:", responseCode)
            // Update the UI after processing
            DispatchQueue.main.async {
                if responseCode == 1 {
                    print("Full JSON Response reportError 200:", responseCode)
                    // Handle successful response
                } else {
                    print("Full JSON Response reportError !200:", responseCode)
                    // Handle failure
                }
            }
        }
    }

    /// This Method handles server response in case of success or failure, in the case of API calls
    /// - Parameters:
    ///   - error: error received from API
    ///   - data: data received from API
    func handleAPIResponse(error: NSError?, data: Data? , imagePath: String?) {
        // Check if there's an error, and log it if present
        guard error == nil else {
            // print("API Error:", error?.localizedDescription ?? "Unknown error")
            DispatchQueue.main.async {
                self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "An unknown error occurred.")
            }
            return
        }

        // Check if data is nil and log it if so
        guard let data = data else {
            // print("API Error: Data of request was found nil.")
            DispatchQueue.main.async {
                self.callForOCRWithImageFailedWithMessage(message: "Data of request was found nil.")
            }
            return
        }

        // Parsing JSON response
        DispatchQueue.main.async {
            do {
                // Attempt to parse the data as JSON
                if let jsonResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                    print("Full JSON Response:", jsonResponse) // Print entire JSON response

                    // Extract response "data" and "status" fields
                    if let responseJson = jsonResponse["data"] {
                        print("Parsed Data Field:", responseJson) // Print extracted data field

                        // Check status code and handle success or error accordingly
                        if let statusCode = jsonResponse["status"] as? Int {
                            debugPrint("Status Code:", statusCode) // Print status code

                            if (200...205).contains(statusCode) {
                                self.callForOCRWithImageCompletedWithData(data: jsonResponse, imagePath: imagePath)
                            } else {
                                // Handle error message in case of non-successful status
                                let errorMessage = jsonResponse["message"] as? String ?? "An unexpected error occurred."
                                print("Error with status code \(statusCode):", errorMessage)
                                self.callForOCRWithImageFailedWithMessage(message: errorMessage)
                            }
                        } else {
                            print("Error: Missing or invalid status code in response.")
                            self.callForOCRWithImageFailedWithMessage(message: "Invalid status code in response.")
                        }
                    } else {
                        print("Error: Data field is missing in the JSON response.")
                        self.callForOCRWithImageFailedWithMessage(message: "Response data was found nil.")
                    }
                }
            } catch {
                // Log parsing error details and attempt to extract any available message
                print("Failed to parse JSON data:", error.localizedDescription)
                do {
                    if let jsonResponse = try JSONSerialization.jsonObject(with: data, options: .mutableContainers) as? [String: Any],
                       let message = jsonResponse["message"] as? String {
                        print("Message in error JSON:", message)
                        self.callForOCRWithImageFailedWithMessage(message: message)
                    } else {
                        print("Unable to extract message from error JSON.")
                        self.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
                    }
                } catch {
                    print("Final Parsing Error:", error.localizedDescription)
                    self.callForOCRWithImageFailedWithMessage(message: "Failed to parse response data.")
                }
            }
        }
    }

    /// This function returns the API response/data from here to React Native App as a prop in case of success.
    /// - Parameter data: Data to be sent to the React Native side
    func callForOCRWithImageCompletedWithData(data: [AnyHashable: Any], imagePath: String?) {
        guard let imagePath = imagePath, !imagePath.isEmpty else {
            print("❌ Provided image path is nil or empty.")
            onError?(["message": "Invalid image path provided."])
            return
        }

        let filePath = imagePath.hasPrefix("file://") ? imagePath : "file://\(imagePath)"

        if FileManager.default.fileExists(atPath: imagePath) {
            print("✅ Image exists at \(imagePath)")
            onOCRScan?(["data": data, "imagePath": filePath])
        } else {
            print("❌ Image does not exist at \(imagePath) after saving.")
            onError?(["message": "Image does not exist at the specified path."])
        }
    }

    /// This function returns the API response message from here to React Native App as a prop in case of failure.
    /// - Parameter message: Failure Message
    func callForOCRWithImageFailedWithMessage(message: String) {
        onError!(["message": message])
    }
}

//MARK: - Props from JS to Swift functions
//MARK: -
extension RNCodeScannerView {

    /// Sets the custom metaData from client/React Native side to control scanning inputs/outputs
    /// - Parameter metaData: metaData description
    @objc func setMetaData(_ metaData: NSDictionary) {
        self.metaData = metaData as? [String : Any]
    }

    /// Sets the pre-selected recipeint from client/React Native side to bulk scan and assign all the items to same recipient whoms contact_id has been passed
    /// - Parameter recipient: recipient description
    @objc func setRecipient(_ recipient: NSDictionary) {
        self.recipient = recipient as? [String : Any]
    }

    /// Sets the pre-selected sender from client/React Native side
    /// - Parameter sender: sender description
    @objc func setSender(_ sender: NSDictionary) {
        self.sender = sender as? [String : Any]
    }

    /// Sets the location Id for desired location to scan on specific location
    /// - Parameter locationId: Passed location Id from React Native App
    @objc func setLocationId(_ locationId: NSString) {
        self.locationId = locationId as String
    }

  
    /// Sets the Camera Scanning Mode for the desired needs, i.e. what kind of scanning user needs.
    /// - Parameter mode: possible values
    ///  | ocr | Scans and Extract Label Information
    ///  | barcode | Scans only Barcode or QR codes and returns results
    ///  | photo | Captures only Photo and doesn't scans anything
  @objc public func setMode(_ mode: NSString) {
      // 1️⃣ Early bail if we don’t have a scanner
      guard let codeScannerView = codeScannerView else { return }
      
      // 2️⃣ Normalize
      let lower = mode.lowercased
      
      // 3️⃣ Handle each mode
      switch lower {
        case "ocr":
            codeScannerView.setScanModeTo(.ocr)
            scanMode = .ocr
            
        case "barcode", "barcodesinglecapture":
            codeScannerView.setScanModeTo(.barCode)
            scanMode = .barCode
            
        case "photo":
            codeScannerView.setScanModeTo(.photo)
            scanMode = .photo
            
        case "barcodeorqrcode":
            codeScannerView.setScanModeTo(.autoBarCodeOrQRCode)
            scanMode = .autoBarCodeOrQRCode
            
        case "qrcode":
            codeScannerView.setScanModeTo(.qrCode)
            scanMode = .qrCode
            
        case "pricetag":
            // Spawn an async context for the network/auth call
            Task { [weak self] in
                guard let self = self else { return }
              let err = await VisionAPIManager.shared
                .checkScanningFeatureAuthenticationWithKey(VSDKConstants.apiKey, andToken: (self.token ?? "").isEmpty ? nil : self.token)
                
                // UI updates must happen on the main actor
                await MainActor.run {
                    if err == nil {
                        codeScannerView.setScanModeTo(.priceTag)
                        self.scanMode = .priceTag
                      let settings = VisionSDK.CodeScannerView.PriceTagDetectionSettings()
                      settings.shouldDisplayOnScreenIndicators = false
                      codeScannerView.setPriceTagDetectionSettingsTo(settings)
                      
                    } else {
                        print("❌ AUTH FAILED: \(err!.localizedDescription)")
                      self.onError?([
                                "message": err!.localizedDescription
                              ])
                    }
                }
            }
            
        default:
            codeScannerView.setScanModeTo(.barCode)
            scanMode = .barCode
      }
  }


    /// Sets the Camera Capture mode for the user desired setting.
    /// - Parameter captureMode: possible values
    /// | auto | Camera will scan and capture automatically
    /// | manual | User has to press the capture button and initiate process
    @objc func setCaptureMode(_ captureMode: NSString) {

        if captureMode == "auto" {
            self.captureMode = CaptureMode.auto
            codeScannerView?.setCaptureModeTo(.auto)
        }
        else {
            self.captureMode = CaptureMode.manual
            codeScannerView?.setCaptureModeTo(.manual)
        }
    }

    /// Handles the isMultipleScanEnabled of the Camera Device
    /// - Parameter isMultipleScanEnabled: isMultipleScanEnabled can be true or false
    @objc func setIsMultipleScanEnabled(_ isMultipleScanEnabled: Bool) {
        self.isMultipleScanEnabled = isMultipleScanEnabled ? .multiple : .single
        codeScannerView?.setCaptureTypeTo( isMultipleScanEnabled ? .multiple : .single )
    }

    /// API key for each Client, can be seperate for everyone.
    /// - Parameter apiKey: apiKey description
    @objc func setApiKey(_ apiKey: NSString) {
        VSDKConstants.apiKey = apiKey as String
    }

    /// Sets the VisionSDK Environment for the desired outputs and connects to the desired Database
    /// - Parameter environment: possible values | dev | qa | sandbox | prod | staging |
    @objc func setEnvironment(_ environment: NSString) {

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

    /// Sets the App Token for Internal Use to get authorized
    /// - Parameter token: token description
    @objc func setToken(_ token: NSString) {
        self.token = token as String
    }

    /// Sets the custom options from client/React Native side to control scanning inputs/outputs
    /// - Parameter options: options description
    @objc func setOptions(_ options: NSDictionary) {
        self.options = options as? [String : Any]
    }

    /// Sets the ocrMode, i.e. is user scanning in On Device OCR mode, Cloud or On Device with Api
    /// - Parameter ocrMode: possible values - > 'cloud' | 'on-device'
    @objc func setOcrMode(_ ocrMode: NSString) {
        self.ocrMode = ocrMode as String
    }


  @objc func setShouldResizeImage(_ shouldResizeImage: Bool){
    self.shouldResizeImage = shouldResizeImage as Bool
  }


    /// - Parameter ocrMode: possible values - > 'on-device-with-translation' | 'shipping_label' | 'bill_of_lading' | 'item_label' | 'document_classification'
    @objc func setOcrType(_ ocrType: NSString) {
        self.ocrType = ocrType as String
    }





    /// Gets the ModelType for On Device, i.e., returns the model class type based on the provided model size string.
    /// - Parameter modelType: parameter describes the Model Class type
    ///  Parameter : possible values
    ///  itemLabel | shippingLabel | billOfLading | document_classification
    ///   - If an invalid or unrecognized string is passed, the default model type returned is "shipping_label".
    /// - Returns: The corresponding `VSDKModelExternalClass` value for the provided model size string.
    func getModelType(_ modelType: String) -> VSDKModelExternalClass {
        switch modelType {
        case "item_label", "item-label":
             return VSDKModelExternalClass.itemLabel
         case "shipping_label", "shipping-label":
             return VSDKModelExternalClass.shippingLabel
         case "bill_of_lading", "bill-of-lading":
             return VSDKModelExternalClass.billOfLading
         case "document_classification", "document-classification":
             return VSDKModelExternalClass.documentClassification
        default:
            return VSDKModelExternalClass.shippingLabel
        }
    }

    /// Gets the ModelSize for On Device, i.e., returns the model size based on the provided model size string.
    /// - Parameter modelSize: parameter describes the Model size
    ///  Parameter : possible values
    ///  nano | micro | small | medium | large | xlarge
    ///   - If an invalid or unrecognized string is passed, the default model size returned is "large".
    /// - Returns: The corresponding `VSDKModelExternalSize` value for the provided model size string.
    func getModelSize(_ modelSize: String?) -> VSDKModelExternalSize? {
        guard let modelSize = modelSize else {
              return nil
          }
        switch modelSize {
        case "nano":
            return VSDKModelExternalSize.nano
        case "micro":
            return VSDKModelExternalSize.micro
        case "small":
            return VSDKModelExternalSize.small
        case "medium":
            return VSDKModelExternalSize.medium
        case "large":
            return VSDKModelExternalSize.large
        case "xlarge":
            return VSDKModelExternalSize.xlarge
        default:
            return VSDKModelExternalSize.micro
        }
    }

    /// Handles the Flash of the Camera Device
    /// - Parameter flash: flash can be true or false
    @objc func setFlash(_ flash: Bool) {
      if let videoDevice: AVCaptureDevice = try?codeScannerView?.videoDevice {
        DispatchQueue.main.async {
            if videoDevice.isTorchAvailable {
                try? videoDevice.lockForConfiguration()
                videoDevice.torchMode = flash ? .on : .off
                videoDevice.unlockForConfiguration()
            }
        }
      }
    }

    /// Handles the isEnableAutoOcrResponseWithImage of the Camera Device
    /// - Parameter isEnableAutoOcrResponseWithImage: isEnableAutoOcrResponseWithImage can be true or false
    @objc func setIsEnableAutoOcrResponseWithImage(_ isEnableAutoOcrResponseWithImage: Bool) {
        self.isEnableAutoOcrResponseWithImage = isEnableAutoOcrResponseWithImage
    }


    /// Handles the Zoom Level of the Camera Device
    /// - Parameter zoomLevel: zoomLevel can be any Integar Number
    @objc func setZoomLevel(_ zoomLevel: NSNumber) {
        var zoomFloatValue = zoomLevel.floatValue
        DispatchQueue.main.async {
          if let videoDevice: AVCaptureDevice = try? self.codeScannerView?.videoDevice {
            DispatchQueue.main.async {
                try? videoDevice.lockForConfiguration()

                if CGFloat(zoomFloatValue) < videoDevice.minAvailableVideoZoomFactor {
                    zoomFloatValue = Float(videoDevice.minAvailableVideoZoomFactor)
                }
                else if CGFloat(zoomFloatValue) > videoDevice.maxAvailableVideoZoomFactor {
                    zoomFloatValue = Float(videoDevice.maxAvailableVideoZoomFactor)
                }
                videoDevice.videoZoomFactor = CGFloat(zoomFloatValue)
                videoDevice.unlockForConfiguration()
            }
          }


        }
    }
}


// MARK: - Helper Methods for storing and Retrieving image from storage.
extension RNCodeScannerView {

    private func loadImage(from url: URL) -> UIImage? {
        // Check if the file exists at the given URL
        if FileManager.default.fileExists(atPath: url.path) {
            // Try to load the image data from the URL
            if let imageData = try? Data(contentsOf: url) {
                // Create and return a UIImage from the image data
                return UIImage(data: imageData)
            }
        }
        // Return nil if the image could not be loaded
        return nil
    }

    @objc public func saveImageToVisionSDK(image: UIImage, withName imageName: String) -> URL? {
        let fileManager = FileManager.default

        do {
            // Get the directory where images are stored
            let ocrImageDirectory = try ocrImageDirectoryURL()

            // Manage the number of images
            let imageFiles = try fileManager.contentsOfDirectory(at: ocrImageDirectory, includingPropertiesForKeys: nil, options: [])
            if imageFiles.count >= 10 {
                // Sort images by creation date
                let sortedImages = imageFiles.sorted { url1, url2 in
                    let attributes1 = try? fileManager.attributesOfItem(atPath: url1.path)
                    let attributes2 = try? fileManager.attributesOfItem(atPath: url2.path)
                    let date1 = attributes1?[.creationDate] as? Date ?? Date.distantPast
                    let date2 = attributes2?[.creationDate] as? Date ?? Date.distantPast
                    return date1 < date2
                }
                // Remove the oldest image
                if let oldestImage = sortedImages.first {
                    try fileManager.removeItem(at: oldestImage)
                }
            }

            // Save the new image
            if let data = image.pngData() {
                let imageURL = ocrImageDirectory.appendingPathComponent(imageName)
                try data.write(to: imageURL, options: .atomic)
                print("✅ Image saved successfully at [sivsdk]: \(imageURL.path)")
                return imageURL
            } else {
                print("❌ Image does not have PNG data.")
                return nil
            }
        } catch {
            print("❌ Error saving image: \(error.localizedDescription)")
            return nil
        }
    }

    @objc private func ocrImageDirectoryURL() throws -> URL {
        let fileManager = FileManager.default
        let documentsDirectory = try fileManager.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        let ocrImageDirectory = documentsDirectory.appendingPathComponent("VisionSDKOCRImages", conformingTo: .folder)

        // Create the directory if it doesn't exist
        if !fileManager.fileExists(atPath: ocrImageDirectory.path) {
            try fileManager.createDirectory(at: ocrImageDirectory, withIntermediateDirectories: true, attributes: nil)
        }

        return ocrImageDirectory
    }
}
//MARK: - Other Helper functions
//MARK: -
extension RNCodeScannerView {

    /// Send converted (UIImage to URL) image to client side, via event onImageCaptured
    /// - Parameter savedImageURL: url of the converted image
  func handleCapturedImage(
    withImage savedImageURL: URL?,
    barcodes: [VisionSDK.DetectedCode],
    nativeImage: UIImage,
    sharpnessScore: Float
  ) {
        if savedImageURL == nil {
            onImageCaptured!(["image": "Nil: URL not found"])
        }
        else {
          
          var codesArray: [[String: Any]] = []

          for code in barcodes{
            var codeInfo: [String: Any] = [:]
            codeInfo["scannedCode"] = code.stringValue
            codeInfo["symbology"] = code.symbology.stringValue()
            codeInfo["boundingBox"] = dict(from: code.boundingBox)
            
            if let gs1Info = code.extractedData {
              codeInfo["gs1ExtractedInfo"] = gs1Info
            }
            codesArray.append(codeInfo)
          }
          
          onImageCaptured!([
            "image": savedImageURL!.path,
            "barcodes": codesArray,
            "nativeImage":nativeImage,
            "sharpnessScore": sharpnessScore
          ])
        }
    }
}



// Convert UIImage to CIImage if it doesn't already have one
func convertToCIImage(from uiImage: UIImage) -> CIImage? {
    return uiImage.ciImage ?? CIImage(image: uiImage)
}
