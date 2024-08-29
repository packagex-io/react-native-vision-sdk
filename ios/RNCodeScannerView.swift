
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
    var delayTime: Double? // Dynamic Prop | Optional:
    var locationId: String? // Dynamic Prop | Optional:
    var options: [String: Any]? // Dynamic Prop | Optional:
    var metaData: [String: Any]? // Dynamic Prop | Optional:
    var height: Double? // Dynamic Prop | Optional:
    var recipient: [String: Any]? // Dynamic Prop | Optional:
    var sender: [String: Any]? // Dynamic Prop | Optional:
    var showScanFrame: Bool = true // Dynamic Prop | Optional:
    var captureWithScanFrame:Bool = true // Dynamic Prop | Optional:
    var codeScannerMode: CodeScannerMode = .barCode // Dynamic Prop | Optional:
    var captureMode: CaptureMode = .manual // Dynamic Prop | Optional:
    var currentMode: String = "barcode" // Dynamic Prop | Optional:
    
    var captureType: CaptureType = .single // Static Prop | Optional:
    var showDocumentBoundries: Bool = true // Static Prop | Optional:
    var documentBoundryBorderColor: UIColor = .orange // Static Prop | Optional:
    var documentBoundryFillColor: UIColor = .orange // Static Prop | Optional:
    var focusImageTintColor: UIColor = .white // Static Prop | Optional:
    var focusImageHighlightedColor: UIColor = .white // Static Prop | Optional:
    var isTextIndicationOn: Bool = true // Static Prop | Optional:
    var isBarCodeOrQRCodeIndicationOn: Bool = true // Static Prop | Optional:
    var isDocumentIndicationOn: Bool = true // Static Prop | Optional:
    var codeDetectionConfidence: Float = 0.5 // Static Prop | Optional:
    var documentDetectionConfidence: Float = 0.6 // Static Prop | Optional:
    var nthFrameToProcess: Int64 = 10 // Static Prop | Optional:
    var shouldAutoSaveCapturedImage: Bool = true // Static Prop | Optional:
    //MARK: - On-Device OCR Specific Variables
    var isOnDeviceOCR: Bool? // Dynamic Prop | Optional:
    var onDeviceModelType: VSDKModelClass = VSDKModelClass.shippingLabel // Dynamic Prop | Optional:
    var onDeviceModelSize: VSDKModelSize = VSDKModelSize.large // Dynamic Prop | Optional:
    
    
    override func layoutSubviews() {
        // If user initially set isOnDeviceOCR = true then configureOnDeviceModel method will be called from here
        configureOnDeviceModel()
    }
    
    //MARK: - Initializer
    init() {
        
        super.init(frame: UIScreen.main.bounds)
        codeScannerView?.stopRunning()
        codeScannerView = CodeScannerView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height))
                
        self.backgroundColor = UIColor.black
//        codeScannerView!.startRunning()
        self.addSubview(codeScannerView!)
        
        
//        let focusSettings = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame, shouldScanInFocusImageRect: self.captureWithScanFrame, showDocumentBoundries: showDocumentBoundries, documentBoundryBorderColor: documentBoundryBorderColor, documentBoundryFillColor: documentBoundryFillColor.withAlphaComponent(0.4), focusImageTintColor: focusImageTintColor, focusImageHighlightedColor: focusImageHighlightedColor)
        
//        let objectDetectionConfiguration = VisionSDK.CodeScannerView.ObjectDetectionConfiguration(isTextIndicationOn: isTextIndicationOn, isBarCodeOrQRCodeIndicationOn: isBarCodeOrQRCodeIndicationOn, isDocumentIndicationOn: isDocumentIndicationOn, codeDetectionConfidence: codeDetectionConfidence, documentDetectionConfidence: documentDetectionConfidence)
                
//        let cameraSettings = VisionSDK.CodeScannerView.CameraSettings(sessionPreset: sessionPreset, nthFrameToProcess: nthFrameToProcess, shouldAutoSaveCapturedImage: shouldAutoSaveCapturedImage)
        
        var sessionPreset: AVCaptureSession.Preset = .high
        if UIDevice.current.userInterfaceIdiom == .pad {
            sessionPreset = .hd1920x1080
        }
        
        
        let focusSettings = VisionSDK.CodeScannerView.FocusSettings()
        focusSettings.focusImage = nil
        focusSettings.focusImageRect = .zero
        focusSettings.shouldDisplayFocusImage = self.showScanFrame
        focusSettings.shouldScanInFocusImageRect = self.captureWithScanFrame
        focusSettings.showDocumentBoundries = self.showDocumentBoundries
        focusSettings.documentBoundryBorderColor = self.documentBoundryBorderColor
        focusSettings.documentBoundryFillColor = self.documentBoundryFillColor.withAlphaComponent(0.4)
        focusSettings.focusImageTintColor = self.focusImageTintColor
        focusSettings.focusImageHighlightedColor = self.focusImageHighlightedColor
        
        
        let objectDetectionConfiguration = VisionSDK.CodeScannerView.ObjectDetectionConfiguration()
        objectDetectionConfiguration.isTextIndicationOn = self.isTextIndicationOn
        objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = self.isBarCodeOrQRCodeIndicationOn
        objectDetectionConfiguration.isDocumentIndicationOn = self.isDocumentIndicationOn
        objectDetectionConfiguration.codeDetectionConfidence = self.codeDetectionConfidence
        objectDetectionConfiguration.documentDetectionConfidence = self.documentDetectionConfidence
        
        
        let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
        cameraSettings.sessionPreset = sessionPreset
        cameraSettings.nthFrameToProcess = self.nthFrameToProcess
        cameraSettings.shouldAutoSaveCapturedImage = self.shouldAutoSaveCapturedImage
        
        
        codeScannerView!.configure(delegate: self, focusSettings: focusSettings, objectDetectionConfiguration: objectDetectionConfiguration, cameraSettings: cameraSettings, captureMode: captureMode, captureType: captureType, scanMode: self.codeScannerMode)
        
        codeScannerView!.startRunning()
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

// MARK: - VisionSDK CodeScannerViewDelegate functions
//MARK: -
extension RNCodeScannerView: CodeScannerViewDelegate {
   
    func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didSuccess code: [String]) {
        if onBarcodeScan != nil {
            onBarcodeScan!(["code": code])
            if currentMode == "barcodeSingleCapture" {
                
            }
            else {
                DispatchQueue.main.asyncAfter(deadline: .now() + ((delayTime ?? 100)/1000)) {
                    scannerView.rescan()
                }
            }
        }
    }
    
    func codeScannerView(
        _ scannerView: VisionSDK.CodeScannerView, didFailure error: VisionSDK.CodeScannerError
    ) {
        if onError != nil {
            onError!(["data": error.rawValue])
        } else {
        }
    }
    
    func codeScannerViewDidDetect(_ text: Bool, barCode: Bool, qrCode: Bool, document: Bool) {
        
        if onDetected != nil {
            onDetected!(["text": text, "barcode": barCode, "qrcode": qrCode, "document": document])
        }
    }
    
    func codeScannerView(_ scannerView: CodeScannerView, didCaptureOCRImage image: UIImage, withCroppedImge croppedImage: UIImage?, withbarCodes barcodes: [String], savedImageURL: URL?) {
        
        if ((codeScannerView?.scanMode == .photo) ) {
//           self.callForImageCaptured(image:image)
            if savedImageURL == nil {
                onImageCaptured!(["image": "Nil: URL not found"])
            }
            else {
                onImageCaptured!(["image": "\(savedImageURL!)"])
            }
        }
        else {
            if let _ = isOnDeviceOCR, isOnDeviceOCR == true {
                self.callOCROnDeviceAPI(image.ciImage!, withbarCodes: barcodes)
            }
            else {
                self.callOCRAPIWithImage(image, andBarcodes: barcodes, savedImageURL: savedImageURL)
            }
        }
        
        DispatchQueue.main.async {
            scannerView.rescan()
        }
    }
}

// MARK: - VisionSDK On-Device call function, callOCROnDeviceAPI
//MARK: -
extension RNCodeScannerView {
    
    /// This method initialises and setup On-Device OCR model to detect labels, can be called from client side, will download and prepare model only if codeScannerMode == ocr
    func configureOnDeviceModel() {
        if (isOnDeviceOCR ?? false) && self.codeScannerMode == .ocr {
//            if self.codeScannerMode == .ocr { // as we will download on-device model only when mode == ocr
                setupDownloadOnDeviceOCR { }
//            }
        }
        else {
            debugPrint("Something went wrong! -------- >")
        }
    }
    
    /// This Method downloads and prepare offline / On Device OCR for use in the App.
    /// - Parameter completion: completionHandler
    func setupDownloadOnDeviceOCR(completion: @escaping () -> Void) {
        
        
        
        var tokenValue: String? = nil
        if let token = token, !token.isEmpty {
            tokenValue = token
        }
        
//        VisionAPIManager.shared.callScanAPIWith(image, andBarcodes: barcodes, andApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil, andToken: tokenValue, andLocationId: locationId ?? "", andOptions: options ?? [:], andMetaData: metaData ?? [:], andRecipient: recipient ?? [:], andSender: sender ?? [:]
//        ) {
            
            
        
//        if !VSDKConstants.apiKey.isEmpty {
            OnDeviceOCRManager.shared.prepareOfflineOCR(withApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil, andToken: tokenValue, forModelClass: onDeviceModelType, withModelSize: onDeviceModelSize) { currentProgress, totalSize in
                debugPrint(((currentProgress / totalSize) * 100))
                self.onModelDownloadProgress!(["progress": ((currentProgress / totalSize)), "downloadStatus": false])
            } withCompletion: { error in
                
                if error == nil {
                    self.onModelDownloadProgress!(["progress": (1), "downloadStatus": true])
                    completion()
                }
                else {
                    self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "We got On-Device OCR error!")
                }
            }
//        }
//        
//        else if let tkn = self.token, !tkn.isEmpty {
//            OnDeviceOCRManager.shared.prepareOfflineOCR(andToken: tkn, forModelClass: onDeviceModelType, withModelSize: onDeviceModelSize) { currentProgress, totalSize in
//                debugPrint(((currentProgress / totalSize) * 100))
//                self.onModelDownloadProgress!(["progress": ((currentProgress / totalSize)), "downloadStatus": false])
//            } withCompletion: { error in
//                
//                if error == nil {
//                    self.onModelDownloadProgress!(["progress": (1), "downloadStatus": true])
//                    completion()
//                }
//                else {
//                    self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "We got On-Device OCR error!")
//                }
//            }
//        }
//        else {
//            debugPrint("Empty Token")
//        }
    }
    
    /// This method pass ciImage of label to downloaded on-device OCR model that extracts informations from label and returns the response
    /// - Parameters:
    ///   - ciImage: ciImage of label that is going to be detected
    ///   - barcodes: barcodes that are detected by SDK within the label
    func callOCROnDeviceAPI( _ ciImage: CIImage, withbarCodes barcodes: [String]) {
        
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
                
                DispatchQueue.main.async {
                    do {
                        if let jsonResponse = try JSONSerialization.jsonObject(with: responseData) as? [String: Any] {
                            self?.callForOCRWithImageCompletedWithData(data: jsonResponse)
                        }
                        else {
                            self?.callForOCRWithImageFailedWithMessage(message: "Something went wrong!")
                        }
                    }
                    catch let error {
                        self?.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
                    }
                }
            }
        }
    }
}
// MARK: - VisionSDK API call function, callOCRAPIWithImage
//MARK: -
extension RNCodeScannerView {
    
    /// This Functions takes input params from VisionSDK and calls OCR API and fetch response from the server to pass it to react native app, function is called from the codeScannerView delegate method
    /// - Parameters:
    ///   - image: Captured image from VisionSDK to pass into API
    ///   - barcodes: Detected barcodes from VisionSDK to pass into API
    ///   - savedImageURL: Captured Image URL that Vision SDK returns to instantly view as thumbnail
    private func callOCRAPIWithImage(_ image: UIImage, andBarcodes barcodes: [String], savedImageURL: URL?) {
        
        if savedImageURL == nil {
            onImageCaptured!(["image": "Nil: URL not found"])
        }
        else {
            onImageCaptured!(["image": "\(savedImageURL!)"])
        }
        
     
        var tokenValue: String? = nil
        if let token = token, !token.isEmpty {
            tokenValue = token
        }
        
        VisionAPIManager.shared.callScanAPIWith(image, andBarcodes: barcodes, andApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil, andToken: tokenValue, andLocationId: locationId ?? "", andOptions: options ?? [:], andMetaData: metaData ?? [:], andRecipient: recipient ?? [:], andSender: sender ?? [:]
        ) {
            
            [weak self] data, error in
            
            if let data = data {
                let json = String(data: data, encoding: String.Encoding.utf8)
            }
            
            guard let self = self else { return }
            
            // Check if there's an error or response data is nil
            guard error == nil else {
                DispatchQueue.main.async {
                    self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "")
                }
                return
            }
            
//            guard let response = response else {
//                DispatchQueue.main.async {
//                    self.callForOCRWithImageFailedWithMessage(message: "Response of request was found nil")
//                }
//                return
//            }
            
            guard let data = data else {
                DispatchQueue.main.async {
                    self.callForOCRWithImageFailedWithMessage(message: "Data of request was found nil")
                }
                return
            }
            
//            _ = (response as! HTTPURLResponse).statusCode
            
            DispatchQueue.main.async {
                
                do {
                    
                    if let jsonResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let responseJson = jsonResponse["data"] {
                        
                        if (try? JSONSerialization.data(withJSONObject: responseJson)) != nil {
                            print(jsonResponse)
                            if jsonResponse["status"] as? Int == 401 {
                                self.callForOCRWithImageFailedWithMessage(message: jsonResponse["message"] as? String ?? "Something went wrong!")
                            }
                            else {
                                self.callForOCRWithImageCompletedWithData(data: jsonResponse)
                            }
                        }
                    }
                }
                catch let error {
                    do {
                        // create json object from data or use JSONDecoder to convert to Model stuct
                        if let jsonResponse = try JSONSerialization.jsonObject(
                            with: data, options: .mutableContainers) as? [String: Any]
                        {
                            if let message = jsonResponse["message"] as? String {
                                self.callForOCRWithImageFailedWithMessage(message: message)
                            }
                            else {
                                self.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
                            }
                            
                        }
                        else {
                            throw URLError(.badServerResponse)
                        }
                    }
                    catch let error {
                        self.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
                    }
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
    /// Handles the Device Torch to set it to On/Off
    /// - Parameter setOn: setOn true means device torch is on and vice versa.
    func setTorchActive(isOn: Bool) {
        let videoDevice: AVCaptureDevice = CodeScannerView.videoDevice
        
        DispatchQueue.main.async {
            if videoDevice.isTorchAvailable {
                try? videoDevice.lockForConfiguration()
                videoDevice.torchMode = isOn ? .on : .off
                videoDevice.unlockForConfiguration()
            }
        }
    }
    
    /// Handles the Zoom Level of the Camera Device
    /// - Parameter zoomLevel: zoomLevel can be any Integar Number
    func setZoomTo(_ zoomLevel: NSNumber) {
//        assert(Thread.isMainThread)
        let videoDevice: AVCaptureDevice = VisionSDK.CodeScannerView.videoDevice
        DispatchQueue.main.async {
            try? videoDevice.lockForConfiguration()
            videoDevice.videoZoomFactor = CGFloat(zoomLevel)
            videoDevice.unlockForConfiguration()
        }
    }

    /// Sets the custom camera screen height from client/React Native side to control camera screen height
    /// - Parameter height: height description - > The possible value of height can be between 0.0 to 1.0, i.e. 0.5 means 50% of height of screen.
    @objc func setHeight(_ height: NSNumber) {
//        codeScannerView!.deConfigure()
        codeScannerView!.frame = CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height * CGFloat((height)))
//        ConfigureCodeScannerView()
//        codeScannerView!.startRunning()
//        self.addSubview(codeScannerView!)
        codeScannerView!.layoutIfNeeded()
    }
    
    /// Sets the custom metaData from client/React Native side to control scanning inputs/outputs
    /// - Parameter metaData: metaData description
    @objc func setMetaData(_ metaData: NSString) {
        self.metaData = convertToDictionary(text: metaData as? String ?? "")
    }
    
    /// Sets the pre-selected recipeint from client/React Native side to bulk scan and assign all the items to same recipient whoms contact_id has been passed
    /// - Parameter recipient: recipient description
    @objc func setRecipient(_ recipient: NSString) {
        self.recipient = convertToDictionary(text: recipient as? String ?? "")
    }
    
    /// Sets the pre-selected sender from client/React Native side
    /// - Parameter sender: sender description
    @objc func setSender(_ sender: NSString) {
        self.sender = convertToDictionary(text: sender as? String ?? "")
    }
    
    @objc func setShowScanFrame(_ showScanFrame: Bool) {
        self.showScanFrame = showScanFrame
        codeScannerView!.focusSettings.shouldDisplayFocusImage = showScanFrame
//        let focusSetting = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame, shouldScanInFocusImageRect: self.captureWithScanFrame, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
//        codeScannerView!.focusSettings = focusSetting
    }
    
    @objc func setFlash(_ flash: Bool) {
        setTorchActive(isOn: flash)
    }
    
    @objc func setCaptureWithScanFrame(_ captureWithScanFrame: Bool) {
        self.captureWithScanFrame = captureWithScanFrame
        codeScannerView!.focusSettings.shouldScanInFocusImageRect = captureWithScanFrame
//        let focusSetting = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame, shouldScanInFocusImageRect: self.captureWithScanFrame, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
//        codeScannerView!.focusSettings = focusSetting
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
        
        if mode == "ocr" {
            codeScannerView!.setScanModeTo(.ocr)
            codeScannerMode = .ocr
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = true
        }
        else if mode == "barcode" || mode == "barcodeSingleCapture" {
            currentMode = String(mode)
            codeScannerView!.setScanModeTo(.barCode)
            codeScannerMode = .barCode
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = true
        }
        else if mode == "photo" {
            codeScannerView!.setScanModeTo(.photo)
            codeScannerMode = .photo
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = false
        }
        else {
            codeScannerView!.setScanModeTo(.qrCode)
            codeScannerMode = .qrCode
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = true
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
    
    /// Sets the delay time, i.e. how much delay should be there after one scan is scanned, or camera button is tapped.
    /// - Parameter delayTime: delayTime as seconds, 1000 = 1 second
    @objc func setDelayTime(_ delayTime: NSNumber) {
        self.delayTime = delayTime as? Double
    }
    
    /// Sets the custom options from client/React Native side to control scanning inputs/outputs
    /// - Parameter options: options description
    @objc func setOptions(_ options: NSString) {
        self.options = convertToDictionary(text: options as? String ?? "")
    }
    
    /// Sets the ondeviceOCR, i.e. is user scanning in On Device OCR mode or not.
    /// - Parameter onDeviceOCR: returns true or false
    @objc func setIsOnDeviceOCR(_ isOnDeviceOCR: Bool) {
        self.isOnDeviceOCR = isOnDeviceOCR
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
    
    @objc func setShowDocumentBoundries(_ showDocumentBoundries: Bool) {
//        self.showDocumentBoundries = showDocumentBoundries
    }
    
    @objc func setDocumentBoundryBorderColor(_ documentBoundryBorderColor: NSString) {
//        if let color = UIColor(hex: documentBoundryBorderColor as String) {
//            self.documentBoundryBorderColor = color
//        }
    }
    
    @objc func setDocumentBoundryFillColor(_ documentBoundryFillColor: NSString) {
//        if let color = UIColor(hex: documentBoundryFillColor as String) {
//            self.documentBoundryFillColor = color
//        }
    }
    
    @objc func setFocusImageTintColor(_ focusImageTintColor: NSString) {
//        if let color = UIColor(hex: focusImageTintColor as String) {
//            self.focusImageTintColor = color
//        }
    }
    
    @objc func setFocusImageHighlightedColor(_ focusImageHighlightedColor: NSString) {
//        if let color = UIColor(hex: focusImageHighlightedColor as String) {
//            self.focusImageHighlightedColor = color
//        }
    }
    
//    @objc func setCodeDetectionConfidence(_ codeDetectionConfidence: Float) {
//        self.codeDetectionConfidence = codeDetectionConfidence
//    }
    
    @objc func setNthFrameToProcess(_ nthFrameToProcess: NSNumber) {
//        self.nthFrameToProcess = nthFrameToProcess.int64Value
    }
    
}

//MARK: - Other Helper functions
//MARK: -
extension RNCodeScannerView {
    
    /// This function reconfigure the changes requested by ReInitializeSDK method, or if its called from Init() then first time properties are set.
//    func ConfigureCodeScannerView() {
//        let focusSettings = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame, shouldScanInFocusImageRect: self.captureWithScanFrame, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
//        
//        let objectDetectionConfiguration = VisionSDK.CodeScannerView.ObjectDetectionConfiguration(isTextIndicationOn: true, isBarCodeOrQRCodeIndicationOn: true, isDocumentIndicationOn: false, codeDetectionConfidence: 0.5, documentDetectionConfidence: 0.6)
//        
//        var sessionPreset: AVCaptureSession.Preset = .high
//        
//        if UIDevice.current.userInterfaceIdiom == .pad {
//            sessionPreset = .hd1920x1080
//        }
//        
//        let cameraSettings = VisionSDK.CodeScannerView.CameraSettings(sessionPreset: sessionPreset, nthFrameToProcess: 10, shouldAutoSaveCapturedImage: true)
//        
//        codeScannerView!.configure(delegate: self, focusSettings: focusSettings, objectDetectionConfiguration: objectDetectionConfiguration, cameraSettings: cameraSettings, captureMode: captureMode, captureType: .single, scanMode: self.codeScannerMode ?? .barCode)
//        
//    }
    
    /// Converts the string input to swift supported dictionary
    /// - Parameter text: Inputs JSON formatted string
    /// - Returns: Returns Swift supported dictionary
    func convertToDictionary(text: String) -> [String: Any]? {
        if let data = text.data(using: .utf8) {
            do {
                return try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
            } catch {
                print(error.localizedDescription)
            }
        }
        return nil
    }
}


extension UIColor {
    
    convenience init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if hexSanitized.hasPrefix("#") {
            hexSanitized.remove(at: hexSanitized.startIndex)
        }

        guard hexSanitized.count == 6 else { return nil }

        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)

        let red = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
        let green = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
        let blue = CGFloat(rgb & 0x0000FF) / 255.0

        self.init(red: red, green: green, blue: blue, alpha: 1.0)
    }
}
