
import UIKit
import VisionSDK
import AVFoundation

//MARK: - React Native CodeScannerView Wrapper
//MARK: -
class RNCodeScannerView: UIView {
    
    //MARK: - Events from swift to Js, on any update to sent back
    @objc var onBarcodeScanSuccess: RCTDirectEventBlock?
    @objc var onImageCaptured: RCTDirectEventBlock?
    @objc var onOCRDataReceived: RCTDirectEventBlock?
    @objc var onDetected: RCTDirectEventBlock?
    @objc var onError: RCTDirectEventBlock?
    
    //MARK: - CodeScannerView Instance
    var codeScannerView: CodeScannerView?
    
    //MARK: - Local Variables
    var token: String?
    var delayTime: Double?
    var locationId: String?
    var options: [String: Any]?
    var metaData: [String: Any]?
    var height: Double?
    var recipient: [String: Any]?
    var sender: [String: Any]?
    var showScanFrame: Bool?
    var captureWithScanFrame:Bool?
    var codeScannerMode: CodeScannerMode?
    var currentMode: String?
    
    //MARK: - Initializer
    init() {
        
        super.init(frame: UIScreen.main.bounds)
        codeScannerView?.stopRunning()
        codeScannerView = CodeScannerView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height))
        
//        let focusSettings = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame ??  false, shouldScanInFocusImageRect: self.captureWithScanFrame ?? true, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
//        
//        let objectDetectionConfiguration = VisionSDK.CodeScannerView.ObjectDetectionConfiguration(isTextIndicationOn: true, isBarCodeOrQRCodeIndicationOn: true, isDocumentIndicationOn: false, codeDetectionConfidence: 0.5, documentDetectionConfidence: 0.9)
//        
//        var sessionPreset: AVCaptureSession.Preset = .high
//        
//        if UIDevice.current.userInterfaceIdiom == .pad {
//            sessionPreset = .hd1920x1080
//        }
//        
//        let cameraSettings = VisionSDK.CodeScannerView.CameraSettings(sessionPreset: sessionPreset, nthFrameToProcess: 10, shouldAutoSaveCapturedImage: true)
//        
//        codeScannerView!.configure(delegate: self, focusSettings: focusSettings, objectDetectionConfiguration: objectDetectionConfiguration, cameraSettings: cameraSettings, captureMode: (self.codeScannerMode == .ocr) ? .manual : .auto, captureType: .single, scanMode: .barCode)
//        
        self.backgroundColor = UIColor.black
        codeScannerView!.startRunning()
        self.addSubview(codeScannerView!)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

// MARK: - VisionSDK CodeScannerViewDelegate functions
//MARK: -
extension RNCodeScannerView: CodeScannerViewDelegate {
   
    func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didSuccess code: [String]) {
        if onBarcodeScanSuccess != nil {
            onBarcodeScanSuccess!(["code": code])
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
        //          onError!(["message":error]);
    }
    
    func codeScannerViewDidDetect(_ text: Bool, barCode: Bool, qrCode: Bool, document: Bool) {
        
        if onDetected != nil {
            onDetected!(["text": text, "barCode": barCode, "qrCode": qrCode])
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
            self.callOCRAPIWithImage(image, andBarcodes: barcodes, savedImageURL: savedImageURL)
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
        
//        debugPrint(options)
//        self.callForImageCaptured(image:image)
        if savedImageURL == nil {
            onImageCaptured!(["image": "Nil: URL not found"])
        }
        else {
            onImageCaptured!(["image": "\(savedImageURL!)"])
        }
        
        VisionAPIManager.shared.callScanAPIWith(image, andBarcodes: barcodes, andApiKey: !VSDKConstants.apiKey.isEmpty ? VSDKConstants.apiKey : nil, andToken: token ?? "", andLocationId: locationId ?? "", andOptions: options ?? [:], andMetaData: metaData ?? [:], andRecipient: recipient ?? [:], andSender: sender ?? [:]
        ) {
            
            [weak self] data, error in
            
//            debugPrint(response)
//            debugPrint(error)
            
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
        if onOCRDataReceived != nil {
            onOCRDataReceived!(["data": data])
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
      
        codeScannerView!.deConfigure()
//        codeScannerView!.stopRunning()
        codeScannerView!.frame = CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height * CGFloat((height)))
        ConfigureCodeScannerView(isReinitializationRequest: true)
//        self.backgroundColor = UIColor.black
        codeScannerView!.startRunning()
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
        let focusSetting = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame ??  true, shouldScanInFocusImageRect: self.captureWithScanFrame ?? true, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
        codeScannerView!.focusSettings = focusSetting
    }
    
    @objc func setCaptureWithScanFrame(_ captureWithScanFrame: Bool) {
        self.captureWithScanFrame = captureWithScanFrame
        let focusSetting = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame ??  true, shouldScanInFocusImageRect: self.captureWithScanFrame ?? true, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
        codeScannerView!.focusSettings = focusSetting
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
            codeScannerView?.setCaptureModeTo(.auto)
        }
        else {
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
            VSDKConstants.apiEnvironment = .dev
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
    
    
    
}

//MARK: - Other Helper functions
//MARK: -
extension RNCodeScannerView {
    
    /// This function reconfigure the changes requested by ReInitializeSDK method, or if its called from Init() then first time properties are set.
    /// - Parameter isReinitializationRequest: isReinitializationRequest defines whether user called this method while re-initialising or it is called from Init(),
    /// isReinitializationRequest: true mean user called this function
    /// isReinitializationRequest: false mean, method is called from Init()
    func ConfigureCodeScannerView(isReinitializationRequest: Bool = false) {
        let focusSettings = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame ??  false, shouldScanInFocusImageRect: self.captureWithScanFrame ?? true, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
        
        let objectDetectionConfiguration = VisionSDK.CodeScannerView.ObjectDetectionConfiguration(isTextIndicationOn: true, isBarCodeOrQRCodeIndicationOn: true, isDocumentIndicationOn: false, codeDetectionConfidence: 0.5, documentDetectionConfidence: 0.9)
        
        var sessionPreset: AVCaptureSession.Preset = .high
        
        if UIDevice.current.userInterfaceIdiom == .pad {
            sessionPreset = .hd1920x1080
        }
        
        let cameraSettings = VisionSDK.CodeScannerView.CameraSettings(sessionPreset: sessionPreset, nthFrameToProcess: 10, shouldAutoSaveCapturedImage: true)
        
        codeScannerView!.configure(delegate: self, focusSettings: focusSettings, objectDetectionConfiguration: objectDetectionConfiguration, cameraSettings: cameraSettings, captureMode: (self.codeScannerMode == .ocr) ? .manual : .auto, captureType: .single, scanMode: self.codeScannerMode ?? .barCode)
        
    }
    
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

