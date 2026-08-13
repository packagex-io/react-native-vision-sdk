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
//  - Event blocks are set by the Fabric ComponentView via objc_msgSend before
//    the view is laid out, mirroring the scanner pattern.
//  - Every event payload is serialised to a JSON string. Codegen cannot express
//    the nested arrays these carry (tracks, box vertices, plane boundaries), so
//    the JS side parses them in DimensioningView.tsx.

@objc(RNDimensioningView)
class RNDimensioningView: UIView {

  // MARK: - Event blocks (wired by DimensioningViewComponentView.mm)
  @objc var onCapture: RCTDirectEventBlock?
  @objc var onError: RCTDirectEventBlock?
  @objc var onMeasurementUpdate: RCTDirectEventBlock?
  @objc var onOverlayUpdate: RCTDirectEventBlock?
  @objc var onTelemetry: RCTDirectEventBlock?

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

  @objc var overlayMode: NSString = "builtIn" {
    didSet { reconfigure() }
  }

  @objc var cloudUrl: NSString = "" {
    didSet { reconfigure() }
  }

  @objc var cloudApiKey: NSString = "" {
    didSet { reconfigure() }
  }

  @objc var cloudSdkId: NSString = "" {
    didSet { reconfigure() }
  }

  @objc var enableTelemetry: Bool = false {
    didSet { reconfigure() }
  }

  // MARK: - Private state
  private var host: UIViewController?
  private var isSetupComplete = false
  /// Set by the `stop` command so lifecycle callbacks don't silently restart us.
  private var isStoppedByCommand = false
  /// DimensioningSession holds its telemetry sink weakly, so we must own it.
  private var telemetrySink: RNDimensioningTelemetrySink?

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

  // MARK: - Commands (dispatched from DimensioningViewComponentView.mm)

  /// Tears the AR view down, releasing the rear camera. ARKit and
  /// AVCaptureSession cannot share it, so JS calls this before showing the
  /// barcode scanner. The vendor's `DimensioningSession.shutdown()` is not
  /// reachable here — `DimensioningView` owns its session privately and exposes
  /// no handle — so dropping the hosted view is the available lever.
  @objc func stopDimensioning() {
    isStoppedByCommand = true
    teardown()
  }

  @objc func startDimensioning() {
    isStoppedByCommand = false
    guard isSetupComplete, host == nil else { return }
    setupDimensioningView()
  }

  // MARK: - Setup
  private func setupDimensioningView() {
    guard !isStoppedByCommand else { return }

    guard #available(iOS 17.0, *) else {
      sendError(code: 2, message: "Dimensioning requires iOS 17.0 or later.", reason: nil)
      return
    }

    let caps = Dimensioning.deviceCapabilities()
    guard caps.lidar else {
      sendError(code: 2, message: "Dimensioning requires a LiDAR-equipped device.", reason: nil)
      return
    }

    present()
  }

  /// Builds the vendor configuration from the JS props.
  ///
  /// `mode: "online"` used to be resolved inside the SDK by the (now removed)
  /// VSDKDimensioningCredentialResolver, which read VSDKConstants. Explicit
  /// cloud props take precedence; when they are empty we fall back to that same
  /// VSDKConstants snapshot, so the pre-2.7.0 behaviour is unchanged and an
  /// unset API key still reports MissingCredentials (0).
  @available(iOS 17.0, *)
  private func makeConfiguration() -> DimensioningConfiguration? {
    let backend: DimensioningConfiguration.SegmentationBackend

    if (mode as String).lowercased() == "online" {
      let fallback = VSDKDimensioningCredentials.current

      let apiKey = (cloudApiKey as String).isEmpty ? fallback.apiKey : (cloudApiKey as String)
      guard !apiKey.isEmpty else {
        sendError(code: 0,
                  message: "Online dimensioning requires an API key — set the cloudApiKey prop or VSDKConstants.apiKey.",
                  reason: nil)
        return nil
      }

      let url: URL
      if (cloudUrl as String).isEmpty {
        url = fallback.cloudURL
      } else if let parsed = URL(string: cloudUrl as String), parsed.scheme != nil {
        url = parsed
      } else {
        sendError(code: 0,
                  message: "cloudUrl is not a valid URL: \(cloudUrl)",
                  reason: nil)
        return nil
      }

      let sdkID = (cloudSdkId as String).isEmpty ? fallback.sdkID : (cloudSdkId as String)
      backend = .cloud(url: url, apiKey: apiKey, sdkID: sdkID)
    } else {
      backend = .localOnly
    }

    return DimensioningConfiguration(
      segmentationBackend: backend,
      measurementUnit: Self.unit(from: measurementUnit as String),
      maximumTrackCount: maximumTrackCount,
      enableTelemetry: enableTelemetry,
      overlayMode: Self.overlay(from: overlayMode as String)
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
  private static func overlay(from name: String) -> DimensioningConfiguration.OverlayMode {
    switch name.lowercased() {
    case "none":     return .none
    case "callback": return .callback
    default:         return .builtIn
    }
  }

  @available(iOS 17.0, *)
  private func present() {
    guard let configuration = makeConfiguration() else { return }

    // Owned for the lifetime of the hosted view — the session's reference is weak.
    let sink = enableTelemetry
      ? RNDimensioningTelemetrySink { [weak self] json in self?.onTelemetry?(["telemetryJson": json]) }
      : nil
    telemetrySink = sink

    let dimensioningView = DimensioningView(
      configuration: configuration,
      telemetry: sink,
      onCapture: { [weak self] measurement in
        self?.emitCapture(measurement)
      },
      onMeasurementUpdate: { [weak self] update in
        self?.emitMeasurementUpdate(update)
      },
      onOverlayUpdate: { [weak self] frame in
        self?.emitOverlayUpdate(frame)
      }
    )

    let controller = UIHostingController(rootView: dimensioningView)
    controller.view.frame = bounds
    controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    controller.view.backgroundColor = .clear

    addSubview(controller.view)
    host = controller
  }

  private func teardown() {
    host?.view.removeFromSuperview()
    host = nil
    telemetrySink = nil
  }

  // MARK: - Reconfigure on prop change
  private func reconfigure() {
    guard isSetupComplete else { return }
    guard #available(iOS 17.0, *) else { return }

    teardown()
    setupDimensioningView()
  }

  // MARK: - Lifecycle
  override func didMoveToWindow() {
    super.didMoveToWindow()

    guard isSetupComplete, #available(iOS 17.0, *) else { return }

    if window != nil {
      if host == nil { setupDimensioningView() }
    } else {
      teardown()
    }
  }

  // MARK: - Serialisation helpers

  private static func point(_ p: CGPoint) -> [String: Any] {
    ["x": p.x, "y": p.y]
  }

  private static func rect(_ r: CGRect) -> [String: Any] {
    ["x": r.origin.x, "y": r.origin.y, "width": r.size.width, "height": r.size.height]
  }

  @available(iOS 17.0, *)
  private static func trackingState(_ state: DimensioningTrackingState) -> String {
    switch state {
    case .searching:   return "searching"
    case .groundFound: return "groundFound"
    case .boxDetected: return "boxDetected"
    case .stable:      return "stable"
    @unknown default:  return "unknown"
    }
  }

  @available(iOS 17.0, *)
  private static func measurementDict(_ m: DimensioningMeasurement) -> [String: Any] {
    [
      "id": m.id.uuidString,
      "trackId": m.trackId.uuidString,
      "timestamp": m.timestamp.timeIntervalSince1970,
      "length": m.length.value,
      "lengthUnit": m.length.unit.symbol,
      "width": m.width.value,
      "widthUnit": m.width.unit.symbol,
      "height": m.height.value,
      "heightUnit": m.height.unit.symbol,
      "distanceFromCamera": m.distanceFromCamera.value,
      "distanceFromCameraUnit": m.distanceFromCamera.unit.symbol,
      "confidence": m.confidence,
      "usedCloudSAM": m.usedCloudSAM,
      "volume": m.volume.value,
      "imagePixelSize": ["width": m.imagePixelSize.width, "height": m.imagePixelSize.height],
      "boxVertices2D": m.boxVertices2D.map(point),
      // Deliberately no image bytes: `imageData` is a full JPEG and shipping one
      // over the bridge on every capture would dwarf the rest of the payload.
      // boxVertices2D + imagePixelSize are enough to draw the box over a frame
      // the host captured itself.
    ]
  }

  private func emit(_ block: RCTDirectEventBlock?, key: String, payload: Any, what: String) {
    guard let block else { return }
    guard JSONSerialization.isValidJSONObject(payload),
          let data = try? JSONSerialization.data(withJSONObject: payload),
          let json = String(data: data, encoding: .utf8) else {
      // 7 = DimensioningErrorCode.InternalError (bridge/serialization failure;
      // not a real DimensioningError).
      sendError(code: 7, message: "Failed to serialise \(what)", reason: nil)
      return
    }
    block([key: json])
  }

  // MARK: - Event emission

  @available(iOS 17.0, *)
  private func emitCapture(_ measurement: DimensioningMeasurement) {
    emit(onCapture, key: "measurementJson",
         payload: Self.measurementDict(measurement), what: "measurement")
  }

  @available(iOS 17.0, *)
  private func emitMeasurementUpdate(_ update: DimensioningUpdate) {
    let payload: [String: Any] = [
      "trackingState": Self.trackingState(update.trackingState),
      "primaryTrackId": update.primaryTrackId?.uuidString as Any? ?? NSNull(),
      "tracks": update.tracks.map { track in
        [
          "id": track.id.uuidString,
          "measurement": track.measurement.map(Self.measurementDict) as Any? ?? NSNull(),
          "isStable": track.isStable,
          "normalizedScreenRect": Self.rect(track.normalizedScreenRect),
        ] as [String: Any]
      },
    ]
    emit(onMeasurementUpdate, key: "updateJson", payload: payload, what: "measurement update")
  }

  @available(iOS 17.0, *)
  private func emitOverlayUpdate(_ frame: DimensioningOverlayFrame) {
    let payload: [String: Any] = [
      "boxes": frame.boxes.map { box in
        [
          "trackId": box.trackId.uuidString,
          "isSelected": box.isSelected,
          "isStable": box.isStable,
          "boxVertices2D": box.boxVertices2D.map(Self.point),
          "contour2D": box.contour2D.map(Self.point),
          "boundingBox": Self.rect(box.boundingBox),
        ] as [String: Any]
      },
      "planes": frame.planes.map { plane in
        [
          "id": plane.id.uuidString,
          "isSelected": plane.isSelected,
          "center2D": plane.center2D.map(Self.point) as Any? ?? NSNull(),
          "boundary2D": plane.boundary2D.map(Self.point),
        ] as [String: Any]
      },
      "hud": [
        "trackingState": Self.trackingState(frame.hud.trackingState),
        "statusText": frame.hud.statusText,
        "guidanceText": frame.hud.guidanceText as Any? ?? NSNull(),
        "isCapturing": frame.hud.isCapturing,
        "groundPlanePrompt": frame.hud.groundPlanePrompt as Any? ?? NSNull(),
      ] as [String: Any],
    ]
    emit(onOverlayUpdate, key: "overlayJson", payload: payload, what: "overlay")
  }

  // MARK: - Helpers
  private func sendError(code: Int, message: String, reason: String?) {
    guard let onError = onError else { return }
    var payload: [String: Any] = ["code": code, "message": message]
    if let reason = reason { payload["reason"] = reason }
    onError(payload)
  }

  deinit {
    host?.view.removeFromSuperview()
  }
}

// MARK: - Telemetry sink
//
// DimensioningSession keeps `telemetry` as a weak reference, so RNDimensioningView
// owns this for the lifetime of the hosted view. Serialises here rather than in the
// view so the JSON shape lives next to the payload it mirrors.

@available(iOS 17.0, *)
final class RNDimensioningTelemetrySink: DimensioningTelemetrySink {

  private let deliver: (String) -> Void

  init(deliver: @escaping (String) -> Void) {
    self.deliver = deliver
  }

  nonisolated func receive(_ event: DimensioningEvent) {
    var payload: [String: Any]

    switch event {
    case .measurementCaptured(let p):
      payload = [
        "type": "measurementCaptured",
        "captureId": p.captureId.uuidString,
        "mode": p.mode,
        "lengthCm": p.lengthCm,
        "widthCm": p.widthCm,
        "heightCm": p.heightCm,
        "distanceM": p.distanceM,
        "confidence": p.confidence,
        "durationMs": p.durationMs,
        "samFrameCount": p.samFrameCount,
        "cloudRequested": p.cloudRequested,
        "cloudLanded": p.cloudLanded,
        "trackingState": p.trackingState,
      ]
      if let v = p.primaryResidualMm        { payload["primaryResidualMm"] = v }
      if let v = p.consensusLevel           { payload["consensusLevel"] = v }
      if let v = p.crossCheckAgreementCount { payload["crossCheckAgreementCount"] = v }
      if let v = p.topFaceInlierFraction    { payload["topFaceInlierFraction"] = v }

    case .measurementAborted(let p):
      payload = [
        "type": "measurementAborted",
        "captureId": p.captureId.uuidString,
        "reason": p.reason,
        "durationMs": p.durationMs,
        "samFrameCount": p.samFrameCount,
        "cloudRequested": p.cloudRequested,
      ]

    @unknown default:
      payload = ["type": "unknown"]
    }

    guard JSONSerialization.isValidJSONObject(payload),
          let data = try? JSONSerialization.data(withJSONObject: payload),
          let json = String(data: data, encoding: .utf8) else { return }

    let deliver = self.deliver
    DispatchQueue.main.async { deliver(json) }
  }
}
