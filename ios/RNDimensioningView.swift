import UIKit
import SwiftUI
import VisionSDK
import VisionSDKDimensioning

// MARK: - RNDimensioningView
//
// Swift UIView subclass hosting MVDimensioning's SwiftUI `DimensioningView`
// from the VisionSDK/Dimensioning CocoaPods subspec.
//
// As of VisionSDK 2.7.0 the VSDK-prefixed wrapper (VSDKDimensioningView and its
// delegate) is gone; `import VisionSDKDimensioning` now re-exports the vendor's
// own API. There is no UIKit entry point any more, so the SwiftUI view is hosted
// in a UIHostingController and driven by closures instead of a delegate.
//
// Design notes:
//  - No compile-time guards (#if) anywhere in this file. All availability
//    checks happen at runtime via `Dimensioning.deviceCapabilities().lidar`
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
  private var host: UIViewController?
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

    host?.view.frame = bounds
  }

  // MARK: - Setup
  private func setupDimensioningView() {
    guard #available(iOS 17.0, *) else {
      sendError(code: 2, message: "Dimensioning requires iOS 17.0 or later.", reason: nil)
      return
    }

    let caps = Dimensioning.deviceCapabilities()
    guard caps.lidar else {
      sendError(code: 2, message: "Dimensioning requires a LiDAR-equipped device.", reason: nil)
      return
    }

    startDimensioning()
  }

  /// Builds the vendor configuration from the JS props.
  ///
  /// `mode: "online"` used to be resolved inside the SDK by the (now removed)
  /// VSDKDimensioningCredentialResolver, which read VSDKConstants. That mapping
  /// is reproduced here so the JS contract is unchanged: online reads the host
  /// app's VSDKConstants snapshot at start time, and fails with
  /// DimensioningErrorCode.MissingCredentials (0) when the API key is unset.
  @available(iOS 17.0, *)
  private func makeConfiguration() -> DimensioningConfiguration? {
    let backend: DimensioningConfiguration.SegmentationBackend

    if (mode as String).lowercased() == "online" {
      let creds = VSDKDimensioningCredentials.current
      guard !creds.apiKey.isEmpty else {
        sendError(code: 0,
                  message: "Online dimensioning requires VSDKConstants.apiKey to be set.",
                  reason: nil)
        return nil
      }
      backend = .cloud(url: creds.cloudURL, apiKey: creds.apiKey, sdkID: creds.sdkID)
    } else {
      backend = .localOnly
    }

    return DimensioningConfiguration(
      segmentationBackend: backend,
      measurementUnit: Self.unit(from: measurementUnit as String),
      maximumTrackCount: maximumTrackCount,
      // Vendor's built-in PostHog routing stays off; the RN layer surfaces
      // results through onCapture and nothing else.
      enableTelemetry: false
    )
  }

  private static func unit(from name: String) -> UnitLength {
    switch name.lowercased() {
    case "inches": return .inches
    case "meters": return .meters
    default:       return .centimeters
    }
  }

  @available(iOS 17.0, *)
  private func startDimensioning() {
    guard let configuration = makeConfiguration() else { return }

    let dimensioningView = DimensioningView(
      configuration: configuration,
      onCapture: { [weak self] measurement in
        self?.emitCapture(measurement)
      }
    )

    let controller = UIHostingController(rootView: dimensioningView)
    controller.view.frame = bounds
    controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    controller.view.backgroundColor = .clear

    addSubview(controller.view)
    host = controller
  }

  /// DimensioningView owns its session internally and exposes no handle, so the
  /// only way to release the camera is to tear the hosted view down. ARKit and
  /// AVCaptureSession cannot share the rear camera, so anything that starts a
  /// capture session afterwards must wait for this to happen.
  private func stopDimensioning() {
    host?.view.removeFromSuperview()
    host = nil
  }

  // MARK: - Reconfigure on prop change
  private func reconfigure() {
    guard isSetupComplete else { return }
    guard #available(iOS 17.0, *) else { return }

    stopDimensioning()
    setupDimensioningView()
  }

  // MARK: - Lifecycle
  override func didMoveToWindow() {
    super.didMoveToWindow()

    guard isSetupComplete, #available(iOS 17.0, *) else { return }

    if window != nil {
      if host == nil { startDimensioning() }
    } else {
      stopDimensioning()
    }
  }

  // MARK: - Helpers
  private func sendError(code: Int, message: String, reason: String?) {
    guard let onError = onError else { return }
    var payload: [String: Any] = ["code": code, "message": message]
    if let reason = reason { payload["reason"] = reason }
    onError(payload)
  }

  @available(iOS 17.0, *)
  private func emitCapture(_ measurement: DimensioningMeasurement) {
    guard let onCapture = onCapture else { return }

    // Serialise measurement to a JSON string so the Fabric event emitter
    // can pass it as a plain string field (avoids C++ nested struct codegen).
    // Field set is unchanged from 2.6.0 — DimensioningMeasurement now also
    // carries trackId / image / imageData / imagePixelSize / boxVertices2D,
    // which are deliberately not forwarded here (an image over the bridge on
    // every capture is not something the current JS contract asks for).
    let dict: [String: Any] = [
      "id": measurement.id.uuidString,
      "timestamp": measurement.timestamp.timeIntervalSince1970,
      "length": measurement.length.value,
      "lengthUnit": measurement.length.unit.symbol,
      "width": measurement.width.value,
      "widthUnit": measurement.width.unit.symbol,
      "height": measurement.height.value,
      "heightUnit": measurement.height.unit.symbol,
      "distanceFromCamera": measurement.distanceFromCamera.value,
      "distanceFromCameraUnit": measurement.distanceFromCamera.unit.symbol,
      "confidence": measurement.confidence,
      "usedCloudSAM": measurement.usedCloudSAM
    ]

    guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
          let jsonString = String(data: jsonData, encoding: .utf8) else {
      // 7 = DimensioningErrorCode.InternalError (bridge/serialization failure;
      // not a real DimensioningError).
      sendError(code: 7, message: "Failed to serialise measurement", reason: nil)
      return
    }

    onCapture(["measurementJson": jsonString])
  }

  deinit {
    host?.view.removeFromSuperview()
  }
}
