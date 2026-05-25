import UIKit
import VisionSDK
import VisionSDKDimensioning

// MARK: - RNDimensioningView
//
// Swift UIView subclass that wraps VSDKDimensioningView from the
// VisionSDK/Dimensioning CocoaPods subspec.
//
// Design notes:
//  - No compile-time guards (#if) anywhere in this file. All availability
//    checks happen at runtime via `VSDKDimensioning.deviceCapabilities().lidar`
//    and the @available(iOS 17, *) attribute on the inner start/stop helpers.
//  - Event blocks (onCapture, onError) are set by the Fabric ComponentView
//    via objc_msgSend before the view is laid out, mirroring the scanner pattern.

@objc(RNDimensioningView)
class RNDimensioningView: UIView {

  // MARK: - Event blocks (wired by DimensioningViewComponentView.mm)
  @objc var onCapture: RCTDirectEventBlock?
  @objc var onError: RCTDirectEventBlock?

  // MARK: - Props
  @objc var mode: NSString = "offline" {
    didSet { reconfigure() }
  }

  @objc var measurementUnit: NSString = "centimeters" {
    didSet { reconfigure() }
  }

  @objc var maximumTrackCount: NSInteger = 5 {
    didSet { reconfigure() }
  }

  // MARK: - Private state
  private var dimView: VSDKDimensioningView?
  private var isSetupComplete = false

  // MARK: - Init
  override init(frame: CGRect) {
    super.init(frame: frame)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  // MARK: - Layout
  override func layoutSubviews() {
    super.layoutSubviews()

    if !isSetupComplete && bounds.size.width > 0 && bounds.size.height > 0 {
      isSetupComplete = true
      setupDimensioningView()
    }

    dimView?.frame = bounds
  }

  // MARK: - Setup
  private func setupDimensioningView() {
    guard #available(iOS 17.0, *) else {
      sendError(code: 2, message: "Dimensioning requires iOS 17.0 or later.", reason: nil)
      return
    }

    let caps = VSDKDimensioning.deviceCapabilities()
    guard caps.lidar else {
      sendError(code: 2, message: "Dimensioning requires a LiDAR-equipped device.", reason: nil)
      return
    }

    startDimensioning()
  }

  @available(iOS 17.0, *)
  private func startDimensioning() {
    let dv = VSDKDimensioningView(frame: bounds)
    dv.autoresizingMask = [.flexibleWidth, .flexibleHeight]

    let selectedMode: VSDKDimensioningMode = (mode as String).lowercased() == "online" ? .online : .offline
    dv.configure(
      delegate: self,
      mode: selectedMode,
      maximumTrackCount: maximumTrackCount
    )

    addSubview(dv)
    dimView = dv
    dv.startRunning()
  }

  // MARK: - Reconfigure on prop change
  private func reconfigure() {
    guard isSetupComplete else { return }
    guard #available(iOS 17.0, *) else { return }

    dimView?.stopRunning()
    dimView?.removeFromSuperview()
    dimView = nil

    setupDimensioningView()
  }

  // MARK: - Lifecycle
  override func didMoveToWindow() {
    super.didMoveToWindow()

    guard isSetupComplete, #available(iOS 17.0, *) else { return }

    if window != nil {
      dimView?.startRunning()
    } else {
      dimView?.stopRunning()
    }
  }

  // MARK: - Helpers
  private func sendError(code: Int, message: String, reason: String?) {
    guard let onError = onError else { return }
    var payload: [String: Any] = ["code": code, "message": message]
    if let reason = reason { payload["reason"] = reason }
    onError(payload)
  }

  deinit {
    if #available(iOS 17.0, *) {
      dimView?.stopRunning()
    }
  }
}

// MARK: - VSDKDimensioningViewDelegate
extension RNDimensioningView: VSDKDimensioningViewDelegate {

  func dimensioningView(_ view: VSDKDimensioningView, didCapture measurement: VSDKDimensioningMeasurement) {
    guard let onCapture = onCapture else { return }

    // Serialise measurement to a JSON string so the Fabric event emitter
    // can pass it as a plain string field (avoids C++ nested struct codegen).
    let dict: [String: Any] = [
      "id": measurement.id.uuidString,
      "timestamp": measurement.timestamp.timeIntervalSince1970,
      "length": measurement.length.doubleValue,
      "lengthUnit": measurement.length.unit.symbol,
      "width": measurement.width.doubleValue,
      "widthUnit": measurement.width.unit.symbol,
      "height": measurement.height.doubleValue,
      "heightUnit": measurement.height.unit.symbol,
      "distanceFromCamera": measurement.distanceFromCamera.doubleValue,
      "distanceFromCameraUnit": measurement.distanceFromCamera.unit.symbol,
      "confidence": measurement.confidence,
      "usedCloudSAM": measurement.usedCloudSAM
    ]

    guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
          let jsonString = String(data: jsonData, encoding: .utf8) else {
      sendError(code: -1, message: "Failed to serialise measurement", reason: nil)
      return
    }

    onCapture(["measurementJson": jsonString])
  }

  func dimensioningView(_ view: VSDKDimensioningView, didFailWithError error: NSError) {
    sendError(code: error.code, message: error.localizedDescription, reason: error.userInfo["reason"] as? String)
  }
}
