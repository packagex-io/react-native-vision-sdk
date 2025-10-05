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

    // MARK: - VisionSDK Components
    var cameraView: CodeScannerView?
    private var currentScanMode: CodeScannerMode = .photo
    private var currentCaptureMode: CaptureMode = .manual
    private var captureType: CaptureType = .single

    // MARK: - Initialization
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupCamera()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private var hasStarted = false

    override func layoutSubviews() {
        super.layoutSubviews()
        cameraView?.frame = self.bounds

        // Auto-start camera after first layout
        if !hasStarted && bounds.size.width > 0 && bounds.size.height > 0 {
            hasStarted = true
            cameraView?.startRunning()
        }
    }

    // MARK: - Camera Setup
    private func setupCamera() {
        // Use UIScreen bounds initially, will be adjusted in layoutSubviews
        cameraView = CodeScannerView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height))
        guard let cameraView = cameraView else { return }

        self.addSubview(cameraView)

        // Configure with minimal settings
        cameraView.configure(
            delegate: self,
            sessionPreset: .high,
            captureMode: currentCaptureMode,
            captureType: captureType,
            scanMode: currentScanMode
        )

        // Stop initially, will auto-start after layout
        cameraView.stopRunning()
    }

    // MARK: - Camera Control
    @objc func start() {
        cameraView?.startRunning()
    }

    @objc func stop() {
        cameraView?.stopRunning()
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

    // MARK: - Helper Methods
    private func sendError(message: String) {
        guard let onError = onError else { return }
        onError(["message": message])
    }

    deinit {
        stop()
    }
}

// MARK: - CodeScannerViewDelegate
@available(iOS 13.0, *)
extension RNVisionCameraView: CodeScannerViewDelegate {

    func codeScannerView(_ scannerView: CodeScannerView, didSuccess codes: [VisionSDK.DetectedBarcode]) {
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
        sendError(message: error.localizedDescription)
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

    func codeScannerView(_ imageSharpnessScore: Float) {
        guard let onSharpnessScoreUpdate = onSharpnessScoreUpdate else { return }

        onSharpnessScoreUpdate([
            "sharpnessScore": imageSharpnessScore
        ])
    }

    func codeScannerView(_ scannerView: CodeScannerView, didCaptureOCRImage image: UIImage, withCroppedImge croppedImage: UIImage?, withBarcodes barcodes: [DetectedBarcode], imageSharpnessScore: Float) {
        // Save image to temporary directory
        let tempDir = FileManager.default.temporaryDirectory
        let fileName = "camera_\(Date().timeIntervalSince1970).jpg"
        let fileURL = tempDir.appendingPathComponent(fileName)

        guard let imageData = image.jpegData(compressionQuality: 0.9) else {
            sendError(message: "Failed to convert image to JPEG")
            return
        }

        do {
            try imageData.write(to: fileURL)

            guard let onCapture = onCapture else { return }

            onCapture([
                "image": fileURL.path,
                "nativeImage": fileURL.absoluteString
            ])

            // Automatically restart scanning after capture
            cameraView?.rescan()
        } catch {
            sendError(message: "Failed to save image: \(error.localizedDescription)")
        }
    }
}
