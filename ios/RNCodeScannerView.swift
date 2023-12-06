import UIKit
import VisionSDK
import AVFoundation

class RNCodeScannerView: UIView, CodeScannerViewDelegate {
    
    // events from swift to Js
    @objc var onBarcodeScanSuccess: RCTDirectEventBlock?
    @objc var onOCRImageCaptured: RCTDirectEventBlock?
    @objc var onOCRDataReceived: RCTDirectEventBlock?
    
    @objc var onDetected: RCTDirectEventBlock?
    
    var codeScannerView: CodeScannerView?
    @objc var onError: RCTDirectEventBlock?
    
    var token: String?
    var delayTime: Double?
    var locationId: String?
    var options: [String: Any]?
    var showScanFrame: Bool?
    var captureWithScanFrame:Bool?
    
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
    
    //prop from Js to Swift
    @objc func setMode(_ mode: NSString) {
        
        if codeScannerView == nil {
            return
        }
        
        if mode == "ocr" {
            codeScannerView!.setScanModeTo(.ocr)
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = true
            
        } else if mode == "barcode" {
            codeScannerView!.setScanModeTo(.barCode)
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = true
            
        } else {
            codeScannerView!.setScanModeTo(.qrCode)
            codeScannerView!.objectDetectionConfiguration.isBarCodeOrQRCodeIndicationOn = true
        }
    }
    
    @objc func setCaptureMode(_ captureMode: NSString) {
    
        if captureMode == "auto" {
            codeScannerView?.setCaptureModeTo(.auto)
        } else {
            codeScannerView?.setCaptureModeTo(.manual)
        }
    }
    
    @objc func setApiKey(_ apiKey: NSString) {
        Constants.apiKey = apiKey as String
    }
    
    @objc func setEnvironment(_ environment: String) {
        switch environment {
            
        case "dev":
            Constants.apiEnvironment = .dev
            break
        case "qa":
            Constants.apiEnvironment = .qa
            break
        case "sandbox":
            Constants.apiEnvironment = .sandbox
            break
        case "prod":
            Constants.apiEnvironment = .production
            break
        case "staging":
            Constants.apiEnvironment = .staging
            break
        default:
            Constants.apiEnvironment = .dev
        }
    }
    
    @objc func setToken(_ token: NSString) {
        self.token = token as String
    }
    
    
    @objc func setDelayTime(_ delayTime: NSNumber) {
        self.delayTime = delayTime as? Double
    }
    
    @objc func setOptions(_ options: NSString) {
        self.options = convertToDictionary(text: options as? String ?? "")
    }
    
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
    
    @objc func setLocationId(_ locationId: NSString) {
        self.locationId = locationId as String
    }
    
    
    func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didSuccess code: [String]) {
        if onBarcodeScanSuccess != nil {
            onBarcodeScanSuccess!(["code": code])
            DispatchQueue.main.asyncAfter(deadline: .now() + ((delayTime ?? 100)/1000)) {
                scannerView.rescan()
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
        
        self.callOCRAPIWithImage(image, andBarcodes: barcodes)
    }
    
    
    func callForOCRWithImageInProgress(image:UIImage) {
        
        // do stuff here while vision api call is in progress
        
        // Get the document directory URL
        let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        
        // Append a unique filename to the document directory URL
        let uniqueFilename = UUID().uuidString // You can use a unique identifier or any desired name
        let fileURL = documentsDirectory.appendingPathComponent(uniqueFilename)
        
        // Convert the UIImage to Data and save it to the file URL
        if let imageData = image.pngData() {
            do {
                try imageData.write(to: fileURL)
                print("Image saved at \(fileURL)")
                onOCRImageCaptured!(["image": "\(fileURL)"])
            } catch {
                print("Error saving image: \(error)")
            }
        }
    }
    
    func callForOCRWithImageCompletedWithData(data: [AnyHashable: Any]) {
        if onOCRDataReceived != nil {
            onOCRDataReceived!(["data": data])
        } else {
            onError!(["message": "not found"])
        }
    }
    
    func callForOCRWithImageFailedWithMessage(message: String) {
        onError!(["message": message])
        
    }
    
    // toggles torch
    func setTorchActive(_ setOn: Bool) {
        assert(Thread.isMainThread)
        let videoDevice: AVCaptureDevice = VisionSDK.CodeScannerView.videoDevice
        try? videoDevice.lockForConfiguration()
        videoDevice.torchMode = setOn ? .on : .off
        videoDevice.unlockForConfiguration()
    }
    func setZoomTo(_ setOn: NSNumber) {
        assert(Thread.isMainThread)
        let videoDevice: AVCaptureDevice = VisionSDK.CodeScannerView.videoDevice
        try? videoDevice.lockForConfiguration()
        videoDevice.videoZoomFactor = CGFloat(setOn)
        videoDevice.unlockForConfiguration()
    }
    
    init() {
        super.init(frame: UIScreen.main.bounds)
        
        codeScannerView = CodeScannerView(frame: UIScreen.main.bounds)
        
        let focusSettings = VisionSDK.CodeScannerView.FocusSettings(focusImage: nil, focusImageRect: .zero, shouldDisplayFocusImage: self.showScanFrame ??  false, shouldScanInFocusImageRect: self.captureWithScanFrame ?? true, showDocumentBoundries: true, documentBoundryBorderColor: .orange, documentBoundryFillColor: UIColor.orange.withAlphaComponent(0.3), focusImageTintColor: .white, focusImageHighlightedColor: .white)
        
        let objectDetectionConfiguration = VisionSDK.CodeScannerView.ObjectDetectionConfiguration(isTextIndicationOn: true, isBarCodeOrQRCodeIndicationOn: true, isDocumentIndicationOn: false, codeDetectionConfidence: 0.5, documentDetectionConfidence: 0.9)
        
        var sessionPreset: AVCaptureSession.Preset = .high
        
        if UIDevice.current.userInterfaceIdiom == .pad {
            sessionPreset = .hd1920x1080
        }
        
        let cameraSettings = VisionSDK.CodeScannerView.CameraSettings(sessionPreset: sessionPreset, nthFrameToProcess: 10, shouldAutoSaveCapturedImage: false)
        
        codeScannerView!.configure(delegate: self, focusSettings: focusSettings, objectDetectionConfiguration: objectDetectionConfiguration, cameraSettings: cameraSettings, captureMode: .manual, captureType: .single, scanMode: .barCode)
        
        self.backgroundColor = UIColor.black
        
        codeScannerView!.startRunning()
        
        self.addSubview(codeScannerView!)
    }
    
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension RNCodeScannerView {
    
    private func callOCRAPIWithImage(
        _ image: UIImage, andBarcodes barcodes: [String]
    ) {
        
        
        debugPrint(options)
        
        self.callForOCRWithImageInProgress(image:image)
        
        VisionAPIManager.shared.callScanAPIWith(
            image, andBarcodes: barcodes, andApiKey: !Constants.apiKey.isEmpty ? Constants.apiKey : nil,
            andToken: token ?? "", andLocationId: locationId ?? "", andOptions: options ?? [:]
        ) {
            
            [weak self] data, response, error in
            
            
            print(response)
            debugPrint(error)
            
            
            
            if let data = data {
                let json = String(data: data, encoding: String.Encoding.utf8)
                // print("Failure Response: \(json)")
            }
            
            guard let self = self else {
                return
            }
            
            // Check if there's an error or response data is nil
            guard error == nil else {
                DispatchQueue.main.async {
                    self.callForOCRWithImageFailedWithMessage(message: error?.localizedDescription ?? "")
                }
                
                return
            }
            
            guard let response = response else {
                DispatchQueue.main.async {
                    self.callForOCRWithImageFailedWithMessage(message: "Response of request was found nil")
                }
                return
            }
            
            guard let data = data else {
                DispatchQueue.main.async {
                    self.callForOCRWithImageFailedWithMessage(message: "Data of request was found nil")
                }
                return
            }
            
            _ = (response as! HTTPURLResponse).statusCode
            
            DispatchQueue.main.async {
                
                do {
                    
                    if let jsonResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let responseJson = jsonResponse["data"]
                    {
                        
                        if (try? JSONSerialization.data(withJSONObject: responseJson)) != nil {
                            self.callForOCRWithImageCompletedWithData(data: jsonResponse)
                        }
                    }
                } catch let error {
                    
                    do {
                        // create json object from data or use JSONDecoder to convert to Model stuct
                        if let jsonResponse = try JSONSerialization.jsonObject(
                            with: data, options: .mutableContainers) as? [String: Any]
                        {
                            if let message = jsonResponse["message"] as? String {
                                self.callForOCRWithImageFailedWithMessage(message: message)
                            } else {
                                self.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
                            }
                            
                        } else {
                            throw URLError(.badServerResponse)
                        }
                    } catch let error {
                        self.callForOCRWithImageFailedWithMessage(message: error.localizedDescription)
                    }
                }
                
            }
        }
    }
}
