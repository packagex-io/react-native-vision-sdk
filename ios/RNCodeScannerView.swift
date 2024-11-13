
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
  @objc var onOCRScan: RCTDirectEventBlock?
  @objc var onDetected: RCTDirectEventBlock?
  @objc var onError: RCTDirectEventBlock?
  
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
  var captureMode: CaptureMode = .manual // Dynamic Prop | Optional:
  var captureType: CaptureType = .single // Static Prop | Optional:
  var sessionPreset: AVCaptureSession.Preset = .high
  
  //MARK: - On-Device OCR Specific Variables
  var ocrMode: String = "cloud" // Dynamic Prop | Optional:
  var onDeviceModelType: VSDKModelClass = VSDKModelClass.shippingLabel // Dynamic Prop | Optional:
  var onDeviceModelSize: VSDKModelSize = VSDKModelSize.large // Dynamic Prop | Optional:
  
  private var previousSize: CGSize = .zero
  
  override func layoutSubviews() {
    super.layoutSubviews()
    // View's size changed
    let newSize = bounds.size
    
    if previousSize != newSize {
      previousSize = newSize
      //            print("View size changed to \(newSize)")
      codeScannerView?.frame = self.bounds
    }
  }
  
  //MARK: - Initializer
  init() {
    
    super.init(frame: UIScreen.main.bounds)
    codeScannerView?.stopRunning()
    codeScannerView = CodeScannerView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height))
    self.addSubview(codeScannerView!)
    codeScannerView!.configure(delegate: self, sessionPreset: sessionPreset, captureMode: captureMode, captureType: captureType, scanMode: scanMode)
  }
  
  required init?(coder aDecoder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }
}

// MARK: - VisionSDK CodeScannerViewDelegate functions
//MARK: -
extension RNCodeScannerView: CodeScannerViewDelegate {
  func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didFailure error: NSError) {
    if onError != nil {
      onError!(["message": error.localizedDescription, "code": error.code])
    }
  }
  
  func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didSuccess code: [String]) {
    if onBarcodeScan != nil {
      onBarcodeScan!(["code": code])
    }
  }
  
  func codeScannerViewDidDetect(_ text: Bool, barCode: Bool, qrCode: Bool, document: Bool) {
    
    if onDetected != nil {
      onDetected!(["text": text, "barcode": barCode, "qrcode": qrCode, "document": document])
    }
  }
  
  func codeScannerView(_ scannerView: CodeScannerView, didCaptureOCRImage image: UIImage, withCroppedImge croppedImage: UIImage?, withbarCodes barcodes: [String]) {
    
    if let savedImageURL = saveImageToVisionSDK(image: image, withName: UUID().uuidString) {
      
        handleCapturedImage(withImage: savedImageURL, barcodes: barcodes, nativeImage: image)
                  switch ocrMode {
                  case "on-device":
                      self.getPrediction(withImage: image, andBarcodes: barcodes)
                  case "on-device-with-translation":
                      self.getPredictionWithCloudTransformations(withImage: image, andBarcodes: barcodes)
                  case "cloud":
                      self.getPredictionShippingLabelCloud(withImage: image, andBarcodes: barcodes)
                  case "bill-of-lading":
                      self.getPredictionBillOfLadingCloud(withImage: image, andBarcodes: barcodes, withImageResizing: true)
                  default:
                      print("default case")
                  }
    }
    else {
      onError!(["message": "Error converting image"])
    }
  }
}

// MARK: - VisionSDK On-Device call functions, onDeviceFlow
//MARK: -
extension RNCodeScannerView {
  
  /// This method initialises and setup On-Device OCR model to detect labels, can be called from client side, will download and prepare model only if scanMode == ocr
 func configureOnDeviceModel() {
    // Calling the setupDownloadOnDeviceOCR method
    setupDownloadOnDeviceOCR { 
        // Completion block after OCR setup
        // Add any actions you want to take after the OCR is downloaded and prepared
        debugPrint("On-Device OCR setup completed")
    }
}

/// This method downloads and prepares offline / On Device OCR for use in the app.
/// - Parameter completion: completionHandler
func setupDownloadOnDeviceOCR(completion: @escaping () -> Void) {
    var tokenValue: String? = nil
    if let token = token, !token.isEmpty {
        tokenValue = token
    }
    
    
    // Calling the prepareOfflineOCR method with progress tracking
    OnDeviceOCRManager.shared.prepareOfflineOCR(withApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil,
                                                andToken: tokenValue,
                                                forModelClass: onDeviceModelType,
                                                withModelSize: onDeviceModelSize) { currentProgress, totalSize, isModelAlreadyDownloaded in
        // If the model is already downloaded, set progress to 100% and download status to true
        if isModelAlreadyDownloaded {
            self.onModelDownloadProgress!(["progress": (1),
                                           "downloadStatus": true]) // Indicate that the model is already downloaded
        } else {
            // Progress tracking and debugging output
            debugPrint(String(format: "Download progress: %.2f%%", (currentProgress / totalSize) * 100))
            
            // Calling the download progress handler
            self.onModelDownloadProgress!(["progress": ((currentProgress / totalSize)),
                                           "downloadStatus": false]) // Update download status to false during download
        }
    } withCompletion: { error in
        // Handling download completion
        if error == nil {
            // If no error, set progress to 100% and download status to true
            self.onModelDownloadProgress!(["progress": (1),
                                           "downloadStatus": true]) // Indicating successful download completion
            completion() // Call completion to indicate success
        } else {
          self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "We got On-Device OCR error!")
        }
    }
}

    /// This method pass ciImage of label to downloaded on-device OCR model that extracts informations from label and returns the response
    /// - Parameters:
    ///   - uiImage: uiImage of label that is going to be detected
    ///   - barcodes: barcodes that are detected by SDK within the label
    func getPrediction(withImage uiImage: UIImage, andBarcodes barcodes: [String]) {
        guard let ciImage = convertToCIImage(from: uiImage) else {
            callForOCRWithImageFailedWithMessage(message: "Failed to convert UIImage to CIImage.")
            return
        }

        OnDeviceOCRManager.shared.extractDataFromImage(ciImage, withBarcodes: barcodes) { [weak self] data, error in
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
                self?.onDeviceAPIResponse(responseData: responseData)
            }
        }
    }

    /// This method pass ciImage of label to downloaded on-device OCR model that extracts informations from label and returns the response
    /// - Parameters:
    ///   - uiImage: uiImage of label that is going to be detected
    ///   - barcodes: barcodes that are detected by SDK within the label
    func getPredictionWithCloudTransformations(withImage uiImage: UIImage, andBarcodes barcodes: [String]) {
        guard let ciImage = convertToCIImage(from: uiImage) else {
            callForOCRWithImageFailedWithMessage(message: "Failed to convert UIImage to CIImage.")
            return
        }
        OnDeviceOCRManager.shared.extractDataFromImage(ciImage, withBarcodes: barcodes) { [weak self] data, error in
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
          
          if self?.ocrMode == "on-device-with-translation" { // on-device + matching API case
            self?.callMatchingAPIWithImage(usingImage: uiImage, withbarCodes: barcodes, responseData: responseData)
          }
          
          else {
            self?.onDeviceAPIResponse(responseData: responseData)
          }
        }
      }
    }
  /// This Method is responsible for sending the on-device extracted data object to client side.
  /// - Parameter responseData: responseData that needs to be sent back to client
  func onDeviceAPIResponse(responseData: Data) {
    DispatchQueue.main.async {
      do {
        if let jsonResponse = try JSONSerialization.jsonObject(with: responseData) as? [String: Any] {
          self.callForOCRWithImageCompletedWithData(data: jsonResponse)
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
    func getPredictionShippingLabelCloud(withImage image: UIImage, andBarcodes barcodes: [String]) {
    
    var tokenValue: String? = nil
    if let token = token, !token.isEmpty {
      tokenValue = token
    }

    VisionAPIManager.shared.callScanAPIWith(image, andBarcodes: barcodes, andApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil, andToken: tokenValue, andLocationId: locationId ?? "", andOptions: options ?? [:], andMetaData: metaData ?? [:], andRecipient: recipient ?? [:], andSender: sender ?? [:]
    ) {
      [weak self] data, error in
      guard let self = self else { return }
      handleAPIResponse(error: error, data: data)
    }
  }
    /// This Functions takes input params from VisionSDK and calls OCR API and fetch response from the server to pass it to react native app, function is called from the codeScannerView delegate method
    /// - Parameters:
    ///   - image: Captured image from VisionSDK to pass into API
    ///   - barcodes: Detected barcodes from VisionSDK to pass into API
    ///   - withImageResizing: Resizing
    func getPredictionBillOfLadingCloud(withImage image: UIImage, andBarcodes barcodes: [String], withImageResizing: Bool) {
        let tokenValue = token?.isEmpty == false ? token : nil
        let apiKey = VSDKConstants.apiKey.isEmpty ? nil : VSDKConstants.apiKey
        VisionAPIManager.shared.getPredictionBillOfLadingCloud(image, andBarcodes: barcodes,andApiKey: apiKey,andToken: tokenValue, withImageResizing: withImageResizing) { 
          [weak self] data, error in
            
            // Early exit if self is deallocated.
            guard let self = self else { return }
            
            handleAPIResponse(error: error, data: data)
        }
    }
  
  /// This Functions takes input params from On-Device VisionSDK flow and calls OCR Matching API to translate response from Platforms format to Receive App format.
  /// - Parameters:
  ///   - uiImage: Captured image from VisionSDK to pass into API
  ///   - barcodes: Detected barcodes from VisionSDK to pass into API
  ///   - responseData: response data received from On-Device model and needs to be translated into another format.
  func callMatchingAPIWithImage(usingImage uiImage: UIImage, withbarCodes barcodes: [String], responseData: Data) {
    
    var tokenValue: String? = nil
    if let token = self.token, !token.isEmpty {
      tokenValue = token
    }
    
    VisionAPIManager.shared.callMatchingAPIWith(uiImage, andBarcodes: barcodes, andApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil, andToken: tokenValue, withResponseData: responseData, andLocationId: (locationId ?? "").isEmpty ? nil : locationId, andOptions: options ?? [:], andMetaData: metaData ?? [:], andRecipient: recipient ?? [:], andSender: sender ?? [:]) { [weak self] data, error in
      
      guard let self = self else { return }
      
      handleAPIResponse(error: error, data: data)
    }
  }
  
  /// This Method handles server response in case of success or failure, in the case of API calls
  /// - Parameters:
  ///   - error: error received from API
  ///   - data: data received from API
    func handleAPIResponse(error: NSError?, data: Data?) {
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
                                self.callForOCRWithImageCompletedWithData(data: jsonResponse)
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
  func callForOCRWithImageCompletedWithData(data: [AnyHashable: Any]) {
    if onOCRScan != nil {
      onOCRScan!(["data": data])
    } else {
      onError!(["message": "not found"])
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
  @objc func setMode(_ mode: NSString) {
    
    if codeScannerView == nil {
      return
    }
    
    if mode.lowercased == "ocr" {
      codeScannerView!.setScanModeTo(.ocr)
      scanMode = .ocr
    }
    else if mode.lowercased == "barcode" || mode == "barcodeSingleCapture" {
      codeScannerView!.setScanModeTo(.barCode)
      scanMode = .barCode
    }
    else if mode.lowercased == "photo" {
      codeScannerView!.setScanModeTo(.photo)
      scanMode = .photo
    }
    else if mode.lowercased == "barcodeorqrcode" {
      codeScannerView!.setScanModeTo(.autoBarCodeOrQRCode)
      scanMode = .autoBarCodeOrQRCode
    }
    else if mode.lowercased == "qrcode" {
      codeScannerView!.setScanModeTo(.qrCode)
      scanMode = .qrCode
    }
    else {
      codeScannerView!.setScanModeTo(.barCode)
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
  /// - Parameter ocrMode: possible values - > 'cloud' | 'on-device' | 'on-device-with-translation'
  @objc func setOcrMode(_ ocrMode: NSString) {
    self.ocrMode = ocrMode as String
  }
  
  /// Sets the ModelType for On Device, i.e. Which model class should be used for scanning
  /// - Parameter modelType: parameter describes the Model Class type
  ///  Parameter : possible values
  ///  itemLabel | shippingLabel | billOfLading
  @objc func setModelType(_ modelType: NSString) {
    
    switch modelType {
    case "item_label":
      onDeviceModelType = VSDKModelClass.itemLabel
      break
    case "shipping_label":
      onDeviceModelType = VSDKModelClass.shippingLabel
      break
    case "bill_of_lading":
      onDeviceModelType = VSDKModelClass.billOfLading
      break
    default:
      onDeviceModelType = VSDKModelClass.shippingLabel
      break
    }
  }
  
  /// Sets the ModelSize for On Device Camera, i.e. Which model size should be used for scanning
  /// - Parameter modelSize: parameter describes the Model size
  ///  Parameter : possible values
  ///  nano | micro | small | medium | large | xlarge
  @objc func setModelSize(_ modelSize: NSString) {
    
    switch modelSize {
    case "nano":
      onDeviceModelSize = VSDKModelSize.nano
      break
    case "micro":
      onDeviceModelSize = VSDKModelSize.micro
      break
    case "small":
      onDeviceModelSize = VSDKModelSize.small
      break
    case "medium":
      onDeviceModelSize = VSDKModelSize.medium
      break
    case "large":
      onDeviceModelSize = VSDKModelSize.large
      break
    case "xlarge":
      onDeviceModelSize = VSDKModelSize.xlarge
      break
    default:
      onDeviceModelSize = VSDKModelSize.micro
      break
    }
  }
  
  /// Handles the Flash of the Camera Device
  /// - Parameter flash: flash can be true or false
  @objc func setFlash(_ flash: Bool) {
    let videoDevice: AVCaptureDevice = CodeScannerView.videoDevice
    DispatchQueue.main.async {
      if videoDevice.isTorchAvailable {
        try? videoDevice.lockForConfiguration()
        videoDevice.torchMode = flash ? .on : .off
        videoDevice.unlockForConfiguration()
      }
    }
  }
  
  /// Handles the Zoom Level of the Camera Device
  /// - Parameter zoomLevel: zoomLevel can be any Integar Number
  @objc func setZoomLevel(_ zoomLevel: NSNumber) {
    var zoomFloatValue = zoomLevel.floatValue
    DispatchQueue.main.async {
      let videoDevice: AVCaptureDevice = VisionSDK.CodeScannerView.videoDevice
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
      
      // Check the number of images in the directory
      let imageFiles = try fileManager.contentsOfDirectory(at: ocrImageDirectory, includingPropertiesForKeys: nil, options: [])
      
      // If there are more than 10 images, delete the oldest one
      if imageFiles.count >= 10 {
        // Sort images by creation date
        let sortedImages = imageFiles.sorted { (url1, url2) -> Bool in
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
        try data.write(to: imageURL)
        return imageURL
      } else {
        print("Image does not have PNG data, thus not writable")
        return nil
      }
      
    } catch let error {
      print("Error saving image: \(error.localizedDescription)")
      return nil
    }
  }
  
  @objc private func removeAllSavedImages() {
    
    let fileManager = FileManager.default
    
    do {
      let ocrImageDirectory = try ocrImageDirectoryURL()
      try fileManager.removeItem(at: ocrImageDirectory)
    }
    catch let error {
      print("Error removing stored Images: \(error.localizedDescription)")
    }
  }
  
  private func ocrImageDirectoryURL() throws -> URL {
    
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
    func handleCapturedImage(withImage savedImageURL: URL?, barcodes: [String], nativeImage: UIImage) {
    if savedImageURL == nil {
      onImageCaptured!(["image": "Nil: URL not found"])
    }
    else {
        onImageCaptured!(["image": savedImageURL!.path, "barcodes": barcodes, "nativeImage":nativeImage])
    }
  }
}



// Convert UIImage to CIImage if it doesn't already have one
func convertToCIImage(from uiImage: UIImage) -> CIImage? {
    return uiImage.ciImage ?? CIImage(image: uiImage)
}
