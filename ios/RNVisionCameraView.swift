import UIKit
import VisionSDK
import AVFoundation

@available(iOS 13.0, *)
class RNVisionCameraView: UIView {
  
  // MARK: - Events
  @objc var onCapture: RCTDirectEventBlock?
  @objc var onError: RCTDirectEventBlock?
  @objc var onRecognitionUpdate: RCTDirectEventBlock?
  @objc var onSharpnessScoreUpdate: RCTDirectEventBlock?
  @objc var onBarcodeDetected: RCTDirectEventBlock?
  @objc var onBoundingBoxesUpdate: RCTDirectEventBlock?
  
  // MARK: - Properties
  @objc var enableFlash: Bool = false {
    didSet {
      updateFlash()
    }
  }
  
  @objc var zoomLevel: NSNumber = 1.0 {
    didSet {
      updateZoom()
    }
  }
  
  @objc var scanMode: NSString? {
    didSet {
      updateScanMode()
    }
  }
  
  @objc var autoCapture: Bool = false {
    didSet {
      updateCaptureMode()
    }
  }
  
  @objc var scanArea: NSDictionary? {
    didSet {
      DispatchQueue.main.async {
        self.updateScanArea()
      }
    }
  }
  
  @objc var detectionConfig: NSDictionary? {
    didSet {
      updateDetectionConfig()
    }
  }
  
  @objc var frameSkip: NSNumber? {
    didSet {
      updateFrameSkip()
    }
  }

  @objc var cameraFacing: NSString? {
    didSet {
      updateCameraPosition()
    }
  }

  // MARK: - VisionSDK Components
  var cameraView: CodeScannerView?
  private var currentScanMode: CodeScannerMode = .photo
  private var currentCaptureMode: CaptureMode = .manual
  private var captureType: CaptureType = .multiple
  
  // MARK: - Initialization
  override init(frame: CGRect) {
    super.init(frame: frame)
    setupCamera()
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  private var hasStarted = false
  private var isRunning = false
  private var isDeallocating = false
  
  override func layoutSubviews() {
    super.layoutSubviews()
    cameraView?.frame = self.bounds

    // Auto-start camera after first layout
    if !hasStarted && !isDeallocating && bounds.size.width > 0 && bounds.size.height > 0 {
      hasStarted = true

      // IMPORTANT: Apply camera settings BEFORE starting the camera
      // This ensures initial camera position (front/back) is set correctly
      applyInitialCameraSettings()

      // Now start the camera with the correct settings
      guard let cameraView = cameraView, !isRunning else { return }

      // IMPORTANT: startRunning() is a blocking call that can take time
      // Move it to background queue to prevent main thread blocking
      DispatchQueue.global(qos: .userInitiated).async { [weak self] in
        guard let self = self, let cameraView = self.cameraView, !self.isDeallocating else { return }

        cameraView.startRunning()

        DispatchQueue.main.async {
          self.isRunning = true
        }
      }

      // Apply scan area settings after camera starts
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
        self?.updateScanArea()
      }
    }
  }
  
  // MARK: - Camera Setup
  private func setupCamera() {
    // Use UIScreen bounds initially, will be adjusted in layoutSubviews
    cameraView = CodeScannerView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height))
    guard let cameraView = cameraView else {
      NSLog("[VisionCamera] Failed to create cameraView")
      return
    }

    self.addSubview(cameraView)

    // Configure with minimal settings
    cameraView.configure(
      delegate: self,
      sessionPreset: .high,
      captureMode: currentCaptureMode,
      captureType: captureType,
      scanMode: currentScanMode
    )

    // Disable default SDK bounding boxes
    updateScanArea()

    // Stop initially, will auto-start after layout
    cameraView.stopRunning()
  }

  /// Applies initial camera settings before the camera starts for the first time.
  /// This ensures properties like cameraFacing (front/back) are set correctly on initial load.
  private func applyInitialCameraSettings() {
    guard let cameraView = cameraView else { return }

    // Determine camera position from cameraFacing prop
    let position: VisionSDK.CameraPosition
    if let facingString = cameraFacing?.lowercased {
      position = facingString == "front" ? .front : .back
    } else {
      position = .back
    }

    // Create camera settings with position and frame skip
    let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
    cameraSettings.cameraPosition = position
    cameraSettings.nthFrameToProcess = frameSkip?.int64Value ?? 10

    // Apply settings BEFORE camera starts
    cameraView.setCameraSettingsTo(cameraSettings)
  }
  
  // MARK: - Camera Control
  @objc func start() {
    guard !isDeallocating else { return }
    guard let cameraView = cameraView else { return }
    guard !isRunning else { return }

    // IMPORTANT: startRunning() is a blocking call that can take time
    // Move it to background queue to prevent main thread blocking
    DispatchQueue.global(qos: .userInitiated).async { [weak self] in
      guard let self = self, !self.isDeallocating else { return }

      cameraView.startRunning()

      DispatchQueue.main.async {
        self.isRunning = true
      }
    }
  }

  @objc func stop() {
    guard let cameraView = cameraView else { return }
    guard isRunning else { return }

    // IMPORTANT: stopRunning() is a blocking call that can take time
    // Move it to background queue to prevent main thread blocking
    DispatchQueue.global(qos: .userInitiated).async { [weak self] in
      cameraView.stopRunning()

      DispatchQueue.main.async {
        self?.isRunning = false
      }
    }
  }

  @objc func capture() {
    cameraView?.capturePhoto()
  }
  
  @objc func toggleFlash(enabled: Bool) {
    self.enableFlash = enabled
    updateFlash()
  }
  
  @objc func setZoom(level: CGFloat) {
    self.zoomLevel = NSNumber(value: Float(level))
    updateZoom()
  }
  
  private func updateFlash() {
    guard let videoDevice = try? cameraView?.videoDevice else { return }
    
    DispatchQueue.main.async {
      if videoDevice.isTorchAvailable {
        try? videoDevice.lockForConfiguration()
        if self.enableFlash {
          try? videoDevice.setTorchModeOn(level: 1.0)
        } else {
          videoDevice.torchMode = .off
        }
        videoDevice.unlockForConfiguration()
      }
    }
  }
  
  private func updateZoom() {
    guard let videoDevice = try? cameraView?.videoDevice else { return }
    
    var zoomValue = zoomLevel.floatValue
    
    DispatchQueue.main.async {
      try? videoDevice.lockForConfiguration()
      
      if CGFloat(zoomValue) < videoDevice.minAvailableVideoZoomFactor {
        zoomValue = Float(videoDevice.minAvailableVideoZoomFactor)
      } else if CGFloat(zoomValue) > videoDevice.maxAvailableVideoZoomFactor {
        zoomValue = Float(videoDevice.maxAvailableVideoZoomFactor)
      }
      
      videoDevice.videoZoomFactor = CGFloat(zoomValue)
      videoDevice.unlockForConfiguration()
    }
  }
  
  private func updateCaptureMode() {
    guard let cameraView = cameraView else { return }
    
    let newCaptureMode: CaptureMode = autoCapture ? .auto : .manual
    currentCaptureMode = newCaptureMode
    cameraView.setCaptureModeTo(newCaptureMode)
  }
  
  private func updateScanArea() {
    guard let cameraView = cameraView else { return }
    
    // If no scan area provided, enable multiple scan and disable default SDK boxes
    guard let scanArea = scanArea else {
      captureType = .multiple
      cameraView.setCaptureTypeTo(captureType)
      
      let focusSettings = VisionSDK.CodeScannerView.FocusSettings()
      focusSettings.shouldDisplayFocusImage = false
      focusSettings.shouldScanInFocusImageRect = false
      focusSettings.showCodeBoundariesInMultipleScan = false
      focusSettings.showDocumentBoundries = false
      cameraView.setFocusSettingsTo(focusSettings)
      return
    }
    
    // When scan area is defined, use single capture mode
    captureType = .single
    cameraView.setCaptureTypeTo(captureType)
    
    let x = scanArea["x"] as? CGFloat ?? 0
    let y = scanArea["y"] as? CGFloat ?? 0
    let width = scanArea["width"] as? CGFloat ?? 0
    let height = scanArea["height"] as? CGFloat ?? 0
    
    let focusRect = CGRect(x: x, y: y, width: width, height: height)
    
    let focusSettings = VisionSDK.CodeScannerView.FocusSettings()
    focusSettings.shouldDisplayFocusImage = false
    focusSettings.shouldScanInFocusImageRect = true
    focusSettings.focusImageRect = focusRect
    focusSettings.showCodeBoundariesInMultipleScan = false
    focusSettings.showDocumentBoundries = false
    
    cameraView.setFocusSettingsTo(focusSettings)
    cameraView.rescan()
  }
  
  private func updateDetectionConfig() {
    guard let cameraView = cameraView, let config = detectionConfig else { return }
    
    let detectionSettings = VisionSDK.CodeScannerView.ObjectDetectionConfiguration()
    detectionSettings.isTextIndicationOn = config["text"] as? Bool ?? true
    detectionSettings.isBarCodeOrQRCodeIndicationOn = config["barcode"] as? Bool ?? true
    detectionSettings.isDocumentIndicationOn = config["document"] as? Bool ?? true
    detectionSettings.codeDetectionConfidence = config["barcodeConfidence"] as? Float ?? 0.5
    detectionSettings.documentDetectionConfidence = config["documentConfidence"] as? Float ?? 0.5
    detectionSettings.secondsToWaitBeforeDocumentCapture = config["documentCaptureDelay"] as? Double ?? 2.0
    cameraView.setObjectDetectionConfigurationTo(detectionSettings)
  }
  
  private func updateFrameSkip() {
    guard let cameraView = cameraView, let frameSkip = frameSkip else { return }
    
    let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
    cameraSettings.nthFrameToProcess = frameSkip.int64Value
    cameraView.setCameraSettingsTo(cameraSettings)
  }
  
  private func updateScanMode() {
    guard let cameraView = cameraView, let scanMode = scanMode else { return }

    let modeString = (scanMode as String).lowercased()

    switch modeString {
    case "ocr":
      cameraView.setScanModeTo(.ocr)
      currentScanMode = .ocr

    case "barcode", "barcodesinglecapture":
      cameraView.setScanModeTo(.barCode)
      currentScanMode = .barCode

    case "photo":
      cameraView.setScanModeTo(.photo)
      currentScanMode = .photo

    case "barcodeorqrcode":
      cameraView.setScanModeTo(.autoBarCodeOrQRCode)
      currentScanMode = .autoBarCodeOrQRCode

    case "qrcode":
      cameraView.setScanModeTo(.qrCode)
      currentScanMode = .qrCode

    default:
      cameraView.setScanModeTo(.barCode)
      currentScanMode = .barCode
    }
  }

  /// Updates camera position dynamically when cameraFacing prop changes.
  /// This method handles camera position changes after the camera has already started.
  /// Note: Switching camera position requires stopping and restarting the camera session.
  private func updateCameraPosition() {
    guard let cameraView = cameraView else { return }
    guard !isDeallocating else { return }

    // Check if camera has started - if not, settings will be applied by applyInitialCameraSettings()
    if !hasStarted { return }

    let position: VisionSDK.CameraPosition
    if let facingString = cameraFacing?.lowercased {
      position = facingString == "front" ? .front : .back
    } else {
      position = .back
    }

    // IMPORTANT: Camera position change requires stopping and restarting the camera
    // Use background queue to prevent main thread blocking
    if isRunning {
      DispatchQueue.global(qos: .userInitiated).async { [weak self] in
        guard let self = self, !self.isDeallocating else { return }

        cameraView.stopRunning()

        DispatchQueue.main.async {
          self.isRunning = false

          let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
          cameraSettings.cameraPosition = position
          cameraSettings.nthFrameToProcess = self.frameSkip?.int64Value ?? 10

          cameraView.setCameraSettingsTo(cameraSettings)

          // Restart camera with new position on background queue
          DispatchQueue.global(qos: .userInitiated).async {
            cameraView.startRunning()

            DispatchQueue.main.async {
              self.isRunning = true
            }
          }
        }
      }
    }
  }
  
  // MARK: - Helper Methods
  private func sendError(message: String) {
    guard let onError = onError else { return }
    onError(["message": message])
  }
  
  deinit {
    // Set flag FIRST to prevent any other operations from executing
    isDeallocating = true

    // Stop camera if running
    // Note: In deinit, we must stop synchronously to ensure cleanup completes
    // before the object is deallocated
    if isRunning {
      cameraView?.stopRunning()
      isRunning = false
    }

    // Clean up camera view reference
    if let cameraView = cameraView {
      cameraView.removeFromSuperview()
      self.cameraView = nil
    }
  }
}

// MARK: - CodeScannerViewDelegate
@available(iOS 13.0, *)
extension RNVisionCameraView: CodeScannerViewDelegate {
  
  func codeScannerView(_ scannerView: CodeScannerView, didSuccess codes: [VisionSDK.DetectedCode]) {
    guard let onBarcodeDetected = onBarcodeDetected else { return }
    
    var codesArray: [[String: Any]] = []
    
    for code in codes {
      var codeInfo: [String: Any] = [:]
      codeInfo["scannedCode"] = code.stringValue
      codeInfo["symbology"] = code.symbology.stringValue()
      codeInfo["boundingBox"] = [
        "x": code.boundingBox.origin.x,
        "y": code.boundingBox.origin.y,
        "width": code.boundingBox.size.width,
        "height": code.boundingBox.size.height
      ]
      if let gs1Info = code.extractedData {
        codeInfo["gs1ExtractedInfo"] = gs1Info
      }
      codesArray.append(codeInfo)
    }
    
    onBarcodeDetected(["codes": codesArray])
    
    // Automatically restart scanning after barcode detection
    cameraView?.rescan()
  }
  
  func codeScannerView(_ scannerView: CodeScannerView, didFailure error: NSError) {
    guard let onError = onError else { return }
    onError(["message": error.localizedDescription, "code": error.code])
    // Restart scanning after error
    cameraView?.rescan()
  }
  
  @objc func codeScannerViewDidDetect(_ text: Bool, barCode: Bool, qrCode: Bool, document: Bool) {
    guard let onRecognitionUpdate = onRecognitionUpdate else { return }
    
    onRecognitionUpdate([
      "text": text,
      "barcode": barCode,
      "qrcode": qrCode,
      "document": document
    ])
  }
  
  // Helper to convert CGRect to dictionary
  fileprivate func dict(from rect: CGRect) -> [String: CGFloat] {
    return [
      "x": rect.origin.x,
      "y": rect.origin.y,
      "width": rect.size.width,
      "height": rect.size.height
    ]
  }
  
  func codeScannerViewDidDetectBoxes(_ text: Bool, barCode: [DetectedCode], qrCode: [DetectedCode], document: CGRect) {
    
    guard let onBoundingBoxesUpdate = onBoundingBoxesUpdate else { return }
    
    
    
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
  
  func codeScannerViewdidUpdateSceneWithSharpness(_ imageSharpnessScore: Float, onCameraLiveGuidance: CameraLiveGuidance) {
    guard let onSharpnessScoreUpdate = onSharpnessScoreUpdate else { return }
    
    onSharpnessScoreUpdate([
      "sharpnessScore": imageSharpnessScore
    ])
  }
  
  
  func codeScannerView(_ scannerView: CodeScannerView, didCaptureOCRImage image: UIImage, withCroppedImge croppedImage: UIImage?, withBarcodes barcodes: [DetectedCode], imageSharpnessScore: Float) {
    // Save image to temporary directory
    let tempDir = FileManager.default.temporaryDirectory
    let fileName = "camera_\(Date().timeIntervalSince1970).jpg"
    let fileURL = tempDir.appendingPathComponent(fileName)
    
    guard let imageData = image.jpegData(compressionQuality: 0.9) else {
      sendError(message: "Failed to convert image to JPEG")
      cameraView?.rescan()
      return
    }
    
    do {
      try imageData.write(to: fileURL)
      
      guard let onCapture = onCapture else { return }
      
      onCapture([
        "image": fileURL.path,
        "nativeImage": fileURL.absoluteString,
        "sharpnessScore": imageSharpnessScore,
        "barcodes": barcodes.map { barcode in
          return [
            "scannedCode": barcode.stringValue,
            "symbology": barcode.symbology.stringValue(),
            "gs1ExtractedInfo": barcode.extractedData ?? [:],
            "boundingBox": dict(from: barcode.boundingBox)
          ]
        }
      ])
      
      // Automatically restart scanning after capture
      cameraView?.rescan()
    } catch {
      sendError(message: "Failed to save image: \(error.localizedDescription)")
      cameraView?.rescan()
    }
  }
}
