import UIKit
import VisionSDK


class RNCodeScannerView : UIView, CodeScannerViewDelegate {
    
  // events from swift to Js
  @objc var onBarcodeScanSuccess : RCTDirectEventBlock?
  
  @objc var onOCRDataReceived : RCTDirectEventBlock?

  @objc var onDetected : RCTDirectEventBlock?
  
  var codeScannerView : CodeScannerView?
  @objc var onError : RCTDirectEventBlock?



  
  //prop from Js to Swift
  @objc func setMode(_ mode: NSString) {
    
    if codeScannerView == nil {
      return
    }
    
    if (mode == "ocr") {
      codeScannerView!.setScanModeTo(.ocr)
      codeScannerView!.isBarCodeOrQRCodeIndicationOn = true
      
    } else if (mode == "barcode") {
      codeScannerView!.setScanModeTo(.barCode)
      codeScannerView!.isBarCodeOrQRCodeIndicationOn = true

    } else {
      codeScannerView!.setScanModeTo(.qrCode)
      codeScannerView!.isBarCodeOrQRCodeIndicationOn = true
    }
  }
//    prop from Js to Swift
//   @objc func setautoMode(_ autoMode: NSString) {
////     if codeScannerView == nil {
////       return
////     }
////       codeScannerView?.setCaptureModeTo(.auto)
////    if (autoMode == "auto") {
////       codeScannerView!.setCaptureModeTo(.auto)
////    }else{
////        codeScannerView!.setCaptureModeTo(.manual)
////    }
//   }

    @objc func setCaptureMode(_ captureMode: NSString){
        if(captureMode == "auto"){
            codeScannerView?.setCaptureModeTo(.auto)
        }else{
            codeScannerView?.setCaptureModeTo(.manual)
        }
    }
//  prop from Js to Swift
//   @objc func setAPIKey(_ apiKey: NSString) {
//     Constants.apiKey = apiKey as String
//   }
    @objc func setApiKey(_ apiKey: NSString){
        Constants.apiKey = apiKey as String
    }
  
    func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didSuccess code: [String]) {
    if onBarcodeScanSuccess != nil {
      onBarcodeScanSuccess!(["code": code])
      scannerView.rescan()
    }
  }
    
  func codeScannerView(_ scannerView: VisionSDK.CodeScannerView, didFailure error: VisionSDK.CodeScannerError) {
//          onError!(["message":error]);
    }
  
  func codeScannerViewDidDetect(_ text: Bool, barCode: Bool, qrCode: Bool) {
    if onDetected != nil {
      onDetected!(["text" : text, "barCode" : barCode, "qrCode" : qrCode])
    }
  }
  
  func codeScannerView(_ scannerView: CodeScannerView, didCaptureOCRImage image: UIImage, withbarCodes barcodes: [String]) {
    self.callOCRAPIWithImage(image, andBarcodes: barcodes)

  }
  
  func callForOCRWithImageInProgress() {
    // do stuff while vision api call is in progress
  }
  
  func callForOCRWithImageCompletedWithData(data: Dictionary<AnyHashable, Any>){
    if onOCRDataReceived != nil {
      onOCRDataReceived!(["data" : data])
    }
    else {
            onError!(["message":"not found"]);
    }
  }
  
  func callForOCRWithImageFailedWithMessage(message: String){
          onError!(["message":message]);
     
  }
  
  
  init() {
    super.init(frame: UIScreen.main.bounds)
    
   codeScannerView = CodeScannerView(frame: UIScreen.main.bounds)
    

    codeScannerView!.configure(delegate: self, input: .init(focusImage: nil, shouldDisplayFocusImage: true, shouldScanInFocusImageRect: true, isTextIndicationOn: true, isBarCodeOrQRCodeIndicationOn: true, sessionPreset: .high, nthFrameToProcess: 10, captureMode: .manual, captureType: .single), scanMode: .barCode)



    codeScannerView!.startRunning()

    self.addSubview(codeScannerView!)
  }
  

  required init?(coder aDecoder: NSCoder) {
      fatalError("init(coder:) has not been implemented")
    }


}




extension RNCodeScannerView {
    
    private func callOCRAPIWithImage(_ image: UIImage, andBarcodes barcodes: [String]) {
        
        self.callForOCRWithImageInProgress()
        
//        VisionAPIManager.shared.callScanAPIWith(image, andBarcodes: barcodes, withMailroomId: 777644, andApiKey: "XRI8HaRVOM5P9OS2Mhakq3voRSOs66hK6j7CSWUG") {
//
//            [weak self] data, response, error in
      
        VisionAPIManager.shared.callScanAPIWith(image, andBarcodes: barcodes, andApiKey: !Constants.apiKey.isEmpty ? Constants.apiKey : "key_stag_7da7b5e917tq2eCckhc5QnTr1SfpvFGjwbTfpu1SQYy242xPjBz2mk3hbtzxVw1zj5K5XBF") {

              [weak self] data, response, error in
            
            guard let self = self else { return }
            
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
                    
                    if let jsonResponse = try JSONSerialization.jsonObject(with: data) as? [String: Any], let responseJson = jsonResponse["data"] {
                                                
                      if (try? JSONSerialization.data(withJSONObject: responseJson)) != nil {
                        self.callForOCRWithImageCompletedWithData(data: jsonResponse)
                     }
                    }
                }
                catch let error {
                    
                   
                    do {
                        // create json object from data or use JSONDecoder to convert to Model stuct
                        if let jsonResponse = try JSONSerialization.jsonObject(with: data, options: .mutableContainers) as? [String: Any] {
                            if let message = jsonResponse["message"] as? String {
                              self.callForOCRWithImageFailedWithMessage(message: message)
                            }
                            else {
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

