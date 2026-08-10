import UIKit
import VisionSDK

// MARK: - FocusSettings Helper
@available(iOS 13.0, *)
extension VisionSDK.CodeScannerView.FocusSettings {
  /// Creates FocusSettings with default values for all required parameters
  static func makeDefault(
    focusImage: UIImage? = nil,
    focusImageRect: CGRect = .zero,
    shouldDisplayFocusImage: Bool = false,
    shouldScanInFocusImageRect: Bool = false,
    showCodeBoundariesInMultipleScan: Bool = false,
    validCodeBoundryBorderColor: UIColor = .green,
    validCodeBoundryBorderWidth: CGFloat = 2.0,
    validCodeBoundryFillColor: UIColor = UIColor.green.withAlphaComponent(0.3),
    inValidCodeBoundryBorderColor: UIColor = .red,
    inValidCodeBoundryBorderWidth: CGFloat = 2.0,
    inValidCodeBoundryFillColor: UIColor = UIColor.red.withAlphaComponent(0.3),
    showDocumentBoundries: Bool = false,
    documentBoundryBorderColor: UIColor = .blue,
    documentBoundryBorderWidth: CGFloat = 2.0,
    documentBoundryFillColor: UIColor = UIColor.blue.withAlphaComponent(0.3),
    focusImageTintColor: UIColor = .white,
    focusImageHighlightedColor: UIColor = .green
  ) -> VisionSDK.CodeScannerView.FocusSettings {
    return VisionSDK.CodeScannerView.FocusSettings(
      focusImage: focusImage,
      focusImageRect: focusImageRect,
      shouldDisplayFocusImage: shouldDisplayFocusImage,
      shouldScanInFocusImageRect: shouldScanInFocusImageRect,
      showCodeBoundariesInMultipleScan: showCodeBoundariesInMultipleScan,
      validCodeBoundryBorderColor: validCodeBoundryBorderColor,
      validCodeBoundryBorderWidth: validCodeBoundryBorderWidth,
      validCodeBoundryFillColor: validCodeBoundryFillColor,
      inValidCodeBoundryBorderColor: inValidCodeBoundryBorderColor,
      inValidCodeBoundryBorderWidth: inValidCodeBoundryBorderWidth,
      inValidCodeBoundryFillColor: inValidCodeBoundryFillColor,
      showDocumentBoundries: showDocumentBoundries,
      documentBoundryBorderColor: documentBoundryBorderColor,
      documentBoundryBorderWidth: documentBoundryBorderWidth,
      documentBoundryFillColor: documentBoundryFillColor,
      focusImageTintColor: focusImageTintColor,
      focusImageHighlightedColor: focusImageHighlightedColor
    )
  }
}

@available(iOS 13.0, *)
@objc(RNVisionCameraView)
class RNVisionCameraView: UIView {
  
  // MARK: - Events
  @objc var onCapture: RCTDirectEventBlock?
  @objc var onError: RCTDirectEventBlock?
  @objc var onRecognitionUpdate: RCTDirectEventBlock?
  @objc var onSharpnessScoreUpdate: RCTDirectEventBlock?
  @objc var onBarcodeDetected: RCTDirectEventBlock?
  @objc var onBoundingBoxesUpdate: RCTDirectEventBlock?
  // Camera Controls API (Phase 3) — throttled full-state event (§8).
  @objc var onCameraStateChanged: RCTDirectEventBlock?
  // Teardown-complete signal (consumer-requested) — fires once stopRunning(completion:)'s
  // closure runs, i.e. once AVCaptureSession.stopRunning() has genuinely returned. See
  // VisionCameraTypes.ts `VisionCameraStoppedEvent` for the cross-platform timing note.
  @objc var onCameraStopped: RCTDirectEventBlock?

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

  @objc var templateJson: NSString? {
    didSet {
      updateTemplate()
    }
  }

  @objc var showCodeBoundingBoxes: Bool = false {
    didSet {
      applyCodeBoundingBoxSettings()
    }
  }

  @objc var barcodeBoundingBoxBorderColor: NSString? {
    didSet {
      applyCodeBoundingBoxSettings()
    }
  }

  @objc var barcodeBoundingBoxBorderWidth: NSNumber = 3.0 {
    didSet {
      applyCodeBoundingBoxSettings()
    }
  }

  @objc var barcodeBoundingBoxFillColor: NSString? {
    didSet {
      applyCodeBoundingBoxSettings()
    }
  }

  // MARK: - Camera Controls API (Phase 3)
  // Canonical props; zoomLevel/enableFlash above stay as deprecated aliases feeding
  // the same native path. C2 fix: once a canonical prop is set here it wins over its
  // legacy alias for good (isTorchSet/isZoomRatioSet below) — see VisionCamera.tsx
  // (Task 18) for the JS-side precedence this mirrors.

  @objc var zoomRatio: NSNumber = 1.0 {
    didSet {
      isZoomRatioSet = true // C2: canonical prop now wins over zoomLevel, permanently
      // rampZoomRatio(_:durationMs:) below updates this same tracked value (so a later
      // facing switch reasserts the RAMP TARGET, mirroring Android's controlPropsFor)
      // without wanting the instant jump updateZoom() would otherwise apply here —
      // it drives the ramp itself via cameraView.rampZoomRatio(...) instead.
      if !isApplyingRampZoom {
        updateZoom() // zoomLevel and zoomRatio converge on the same setter
      }
    }
  }

  @objc var torch: Bool = false {
    didSet {
      isTorchSet = true // C2: canonical prop now wins over enableFlash, permanently
      updateFlash() // torch and enableFlash converge on the same setter
    }
  }

  // C2 fix: once a canonical prop (`torch`/`zoomRatio`) has been explicitly set by
  // JS, it wins over the legacy alias (`enableFlash`/`zoomLevel`) for the rest of
  // this view's lifetime — mirrors Android's ControlPropsState "new prop wins over
  // legacy" contract. Without this, `updateFlash`/`updateZoom` read the legacy prop
  // unconditionally, so e.g. setting only `torch=true` got silently overwritten by
  // `enableFlash`'s default (false) on the very next reassert.
  private var isTorchSet: Bool = false
  private var isZoomRatioSet: Bool = false
  // Set for the duration of rampZoomRatio(_:durationMs:)'s write to `zoomRatio` below,
  // so its didSet updates isZoomRatioSet/bookkeeping without also triggering updateZoom()'s
  // instant jump — the ramp itself is applied via cameraView.rampZoomRatio(...) instead.
  private var isApplyingRampZoom: Bool = false

  private var resolvedTorch: Bool {
    isTorchSet ? torch : enableFlash
  }

  private var resolvedZoomRatio: Float {
    isZoomRatioSet ? zoomRatio.floatValue : zoomLevel.floatValue
  }

  @objc var focusMode: NSString? {
    didSet {
      updateFocusMode()
    }
  }

  @objc var pinnedLensId: NSString? {
    didSet {
      reassertControlProps()
    }
  }

  /// One-shot flag: set when `applyLensSelection()` falls back to `.automatic`
  /// because `pinnedLensId` named an unknown/unpinnable lens. Consumed (and cleared)
  /// by the next emitted `onCameraStateChanged` event as `warningCode: "lens-unavailable"`.
  private var pendingLensUnavailableWarning: Bool = false

  /// Last (facing, pinnedLensId) pair actually applied via `applyLensSelection()`.
  /// `reassertControlProps()` skips the lens re-apply when this hasn't changed —
  /// Group B persists the lens via `makeCameraConfiguration()` now, so re-resolving
  /// it on every RUNNING transition is a needless structural reconcile. `nil` means
  /// "never applied on the current cameraView" and always forces one application;
  /// reset in `setupCamera()` so a freshly created cameraView always gets a lens.
  private var lastAppliedLensKey: String?

  private func currentLensKey() -> String {
    let facing = (cameraFacing as String?)?.lowercased() == "front" ? "front" : "back"
    let lensId = (pinnedLensId as String?) ?? ""
    return "\(facing)|\(lensId)"
  }

  /// Builds a single coherent FocusSettings from the full current prop state:
  /// - scan-area focus rect (from `scanArea` / `captureType`)
  /// - bounding-box styling (from `showCodeBoundingBoxes` + `barcodeBoundingBox*`)
  ///
  /// Both `applyCodeBoundingBoxSettings()` and `updateScanArea()` route through
  /// here so the two prop groups can never clobber each other regardless of call order.
  private func buildFocusSettings() -> VisionSDK.CodeScannerView.FocusSettings {
    // --- bbox styling ---
    let borderColor = Self.parseARGBColor(barcodeBoundingBoxBorderColor as String?)
      ?? UIColor(red: 0.545, green: 0.361, blue: 0.965, alpha: 1.0)
    let fillColor = Self.parseARGBColor(barcodeBoundingBoxFillColor as String?)
      ?? UIColor(red: 0.545, green: 0.361, blue: 0.965, alpha: 0.20)
    let borderWidth = CGFloat(truncating: barcodeBoundingBoxBorderWidth)

    // --- scan-area focus rect ---
    let focusRect: CGRect
    let shouldScanInFocusRect: Bool
    if let scanArea = scanArea {
      let x = scanArea["x"] as? CGFloat ?? 0
      let y = scanArea["y"] as? CGFloat ?? 0
      let width = scanArea["width"] as? CGFloat ?? 0
      let height = scanArea["height"] as? CGFloat ?? 0
      focusRect = CGRect(x: x, y: y, width: width, height: height)
      shouldScanInFocusRect = true
    } else {
      focusRect = .zero
      shouldScanInFocusRect = false
    }

    return VisionSDK.CodeScannerView.FocusSettings.makeDefault(
      focusImageRect: focusRect,
      shouldDisplayFocusImage: false,
      shouldScanInFocusImageRect: shouldScanInFocusRect,
      showCodeBoundariesInMultipleScan: showCodeBoundingBoxes,
      validCodeBoundryBorderColor: borderColor,
      validCodeBoundryBorderWidth: borderWidth,
      validCodeBoundryFillColor: fillColor,
      inValidCodeBoundryBorderColor: borderColor,
      inValidCodeBoundryBorderWidth: borderWidth,
      inValidCodeBoundryFillColor: fillColor,
      showDocumentBoundries: false
    )
  }

  /// Applies showCodeBoundingBoxes + color props to the VisionSDK view via the
  /// FocusSettings API, which is the only bounding-box path exported to ObjC in
  /// the VisionSDK 2.3.x xcframework binary.  The direct `cameraView.showCode-
  /// BoundingBoxes` setter exists in the Swift source but is not @objc, so it is
  /// not reachable from the RN wrapper across the module boundary.
  private func applyCodeBoundingBoxSettings() {
    guard let cameraView = cameraView else { return }
    cameraView.setFocusSettingsTo(buildFocusSettings())
  }

  // MARK: - VisionSDK Components
  var cameraView: CodeScannerView?
  private var currentScanMode: CodeScannerMode = .photo
  private var currentCaptureMode: CaptureMode = .manual
  private var captureType: CaptureType = .multiple

  // MARK: - State Management
  private enum CameraState {
    case stopped
    case starting
    case running
    case stopping
  }

  // Three-state system for proper operation coalescing
  private var actualCameraState: CameraState = .stopped  // Real AVFoundation state
  private var targetState: CameraState = .stopped         // What user wants
  private var isTransitioning: Bool = false               // Is an operation in progress

  // Serial queue for camera operations to prevent race conditions
  private let cameraOperationQueue = DispatchQueue(label: "com.visionSDK.cameraOperations", qos: .userInitiated)

  private var isDeallocating = false
  private var isSetupComplete = false
  private var shouldAutoStart = true

  // MARK: - Camera Controls API (Phase 3) — state-event throttling
  private var lastCameraStateEmitTime: TimeInterval = 0
  private var lastCameraStateStatus: String?
  private let cameraStateThrottleSeconds: TimeInterval = 0.1 // 10 Hz, matches Android's CAMERA_STATE_THROTTLE_MS
  // Trailing-edge guarantee: the throttle above is a drop-throttle, so a burst of
  // state changes ending mid-window would otherwise leave JS on stale data. This
  // schedules a single deferred emit of the LATEST state at the window boundary;
  // a newer skipped emit cancels/replaces it, so exactly one trailing emit lands
  // with the freshest state.
  private var trailingCameraStateWorkItem: DispatchWorkItem?

  // MARK: - onCameraStopped contract (I1/I2/I3/I4 — see VisionCameraTypes.ts
  // `VisionCameraStoppedEvent` for the consumer-facing doc)
  // Bumped every time a REAL native transition begins (performStart() only —
  // performStop() can't re-enter while one is already in flight, thanks to
  // `isTransitioning`). performStop() captures the current value right before the real
  // AVCaptureSession teardown call; the completion closure compares against it to tell
  // whether a subsequent start() beat it back to RUNNING before the closure fired (I4) —
  // the completion still fires either way (never suppressed), just flagged stale.
  private var operationGeneration: Int = 0

  // Every consumer stop() call registers exactly one callback here, so it resolves
  // exactly once (I1) regardless of actualCameraState/isTransitioning at call time —
  // already-stopped, never-started, mid-teardown (a second stop()), and start-failed are
  // all covered. Flushed either by a genuine AVCaptureSession teardown's completion, or
  // immediately when there is nothing to tear down.
  private var pendingStopCallbacks: [(Bool) -> Void] = []

  /// Resolves every currently-queued `stop()` completion callback exactly once, each
  /// firing its own `onCameraStopped` (I1: never zero events). Called either
  /// immediately (nothing to tear down) or from the real `stopRunning(completion:)`
  /// closure once AVCaptureSession has genuinely finished (I2/I4).
  private func flushPendingStopCallbacks(wasSuperseded: Bool) {
    guard !pendingStopCallbacks.isEmpty else { return }
    let callbacks = pendingStopCallbacks
    pendingStopCallbacks.removeAll()
    for callback in callbacks {
      callback(wasSuperseded)
    }
  }

  // MARK: - Initialization
  override init(frame: CGRect) {
    super.init(frame: frame)
    // Camera setup is now deferred to layoutSubviews to avoid blocking init
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override func layoutSubviews() {
    super.layoutSubviews()

    // Ensure camera is setup and frame is adjusted
    if !isSetupComplete && bounds.size.width > 0 && bounds.size.height > 0 {
      // Defer heavy camera setup to avoid blocking initial layout
      setupCamera()
      isSetupComplete = true

      // Auto-start camera after setup
      if shouldAutoStart && !isDeallocating {
        // IMPORTANT: Apply camera settings BEFORE starting the camera
        applyInitialCameraSettings()

        // Apply settings that may have been set before camera was initialized
        updateScanMode()
        updateCaptureMode()
        updateDetectionConfig()
        updateFrameSkip()
        reassertControlProps() // zoomRatio/torch/focusMode/pinnedLensId
        applyCodeBoundingBoxSettings()

        // Start camera immediately - no need to delay
        self.start()

        // Apply scan area settings after camera starts
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
          self?.updateScanArea()
        }
      }
    }

    cameraView?.frame = self.bounds
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()

    // Only handle window changes after initial setup
    guard isSetupComplete, !isDeallocating else { return }

    if window != nil {
      // View added to window
      // Only recreate if camera was stopped (to clear frozen frames)
      // If camera is already running, no need to recreate
      if actualCameraState == .stopped {
        // Recreate camera view to clear any frozen frames from previous session
        recreateCameraView()
        self.start()
      }
    } else {
      // View removed from window - stop camera
      if actualCameraState == .running {
        stop()
      }
    }
  }

  private func recreateCameraView() {
    guard let oldCameraView = cameraView else { return }

    // Stop and remove old camera view
    oldCameraView.stopRunning()
    oldCameraView.removeFromSuperview()

    // Create fresh camera view
    setupCamera()

    // Re-apply props that may have been set before this recreation
    applyCodeBoundingBoxSettings()
    reassertControlProps() // zoomRatio/torch/focusMode/pinnedLensId

    // Ensure frame is correct
    cameraView?.frame = self.bounds
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

    // Fresh cameraView never had a lens applied — force one on the next
    // reassertControlProps(), regardless of whether (facing, pinnedLensId) happens
    // to match what was applied to the previous (now-discarded) cameraView.
    lastAppliedLensKey = nil

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

    // Replay-on-attach: emit the current camera state once immediately so
    // onCameraStateChanged listeners are never stale-undefined (spec §8).
    emitCameraState(cameraView.currentCameraState, bypassThrottle: true)

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
    guard !isDeallocating, cameraView != nil else {
      return
    }

    // Update target state (what user wants)
    targetState = .running

    // Trigger transition if needed
    executeTransitionIfNeeded()
  }

  @objc func stop() {
    guard !isDeallocating else {
      return
    }

    // Register this call's own notification up front — resolved exactly once below no
    // matter how the transition below plays out (I1: a consumer stop() must never go
    // unanswered — an already-stopped/never-started/mid-teardown camera all count).
    pendingStopCallbacks.append { [weak self] wasSuperseded in
      self?.onCameraStopped?(["wasSuperseded": wasSuperseded])
    }

    guard cameraView != nil else {
      // Nothing was ever set up to tear down — resolve immediately rather than hang.
      flushPendingStopCallbacks(wasSuperseded: false)
      return
    }

    // Update target state (what user wants)
    targetState = .stopped

    // Trigger transition if needed
    executeTransitionIfNeeded()

    // No real teardown is coming to flush the callback just queued — e.g. stop() called
    // while already .stopped (including right after a failed start(), or a second
    // stop() call with nothing left in flight). Resolve right away (I1).
    if !isTransitioning && actualCameraState == .stopped {
      flushPendingStopCallbacks(wasSuperseded: false)
    }
  }

  private func executeTransitionIfNeeded() {
    // If already transitioning, the completion handler will check again
    if isTransitioning {
      return
    }

    // If already at target state, nothing to do
    if actualCameraState == targetState {
      return
    }

    // Need to transition - determine direction
    if targetState == .running && actualCameraState == .stopped {
      performStart()
    } else if targetState == .stopped && actualCameraState == .running {
      performStop()
    }
  }

  private func performStart() {
    guard let cameraView = self.cameraView, !isDeallocating else { return }

    isTransitioning = true
    actualCameraState = .starting
    // I4: any real start invalidates in-flight stop completions captured before this
    // point — see performStop()'s `myGeneration` capture and its completion closure.
    operationGeneration += 1

    // Use serial queue to avoid blocking main thread and prevent overlapping operations
    cameraOperationQueue.async { [weak self] in
      guard let self = self, !self.isDeallocating else { return }
      guard let cameraView = self.cameraView else {
        DispatchQueue.main.async { [weak self] in
          self?.onTransitionComplete(success: false)
        }
        return
      }

      // Call startRunning on main thread (required by AVFoundation)
      DispatchQueue.main.sync {
        cameraView.startRunning()
      }

      DispatchQueue.main.async { [weak self] in
        guard let self = self, !self.isDeallocating else { return }
        self.onTransitionComplete(success: true)
      }
    }
  }

  private func performStop() {
    guard let cameraView = self.cameraView else {
      // Nothing was ever set up to tear down — don't leave queued stop() callbacks
      // hanging on a teardown that will never happen (I1).
      flushPendingStopCallbacks(wasSuperseded: false)
      return
    }

    isTransitioning = true
    actualCameraState = .stopping
    // Snapshot BEFORE kicking off the real teardown call below — compared inside its
    // completion closure to detect a start() that landed while we were mid-teardown (I4).
    let myGeneration = operationGeneration

    // Use serial queue to avoid blocking main thread and prevent overlapping operations
    cameraOperationQueue.async { [weak self] in
      guard let self = self else { return }
      guard let cameraView = self.cameraView else {
        DispatchQueue.main.async { [weak self] in
          self?.onTransitionComplete(success: false)
          self?.flushPendingStopCallbacks(wasSuperseded: false)
        }
        return
      }

      // Call stopRunning on main thread (required by AVFoundation). Uses the
      // completion-carrying overload (consumer-requested teardown-complete signal) so
      // `onCameraStopped` fires only once AVCaptureSession.stopRunning() has genuinely
      // returned — not merely once this call has been scheduled, which is all
      // `onTransitionComplete` below (and `onCameraStateChanged`'s `status: 'idle'`)
      // ever guaranteed. See VisionCameraTypes.ts `VisionCameraStoppedEvent`.
      DispatchQueue.main.sync {
        cameraView.stopRunning(completion: { [weak self] in
          DispatchQueue.main.async {
            guard let self = self else { return }
            // I4: if operationGeneration moved on, a start() began after this teardown
            // was kicked off — the completion still fires (never suppressed), just
            // flagged stale so the consumer doesn't mistake it for "torn down right now".
            let wasSuperseded = self.operationGeneration != myGeneration
            self.flushPendingStopCallbacks(wasSuperseded: wasSuperseded)
          }
        })
      }

      DispatchQueue.main.async { [weak self] in
        guard let self = self else { return }
        self.onTransitionComplete(success: true)
      }
    }
  }

  private func onTransitionComplete(success: Bool) {
    let wasStarting = actualCameraState == .starting

    // Update actual state based on what operation completed
    if wasStarting {
      actualCameraState = success ? .running : .stopped
      // Re-assert declarative control props on every RUNNING transition (spec §8).
      if success { reassertControlProps() }
    } else if actualCameraState == .stopping {
      actualCameraState = .stopped
    }

    isTransitioning = false

    // Check if we need another transition
    if targetState != actualCameraState {
      executeTransitionIfNeeded()
    } else if wasStarting && !success && targetState == .stopped {
      // start() failed (e.g. permission denied) while stop() was the target — there is
      // no real teardown in flight for any queued stop() callbacks to ride along with,
      // so resolve them here instead of leaving them hanging (I1).
      flushPendingStopCallbacks(wasSuperseded: false)
    }
  }

  @objc func capture() {
    cameraView?.capturePhoto()
  }

  /// Tear down the camera + analyzer + overlay and rebuild from scratch.
  /// Exposed for parity with Android, where it's required for repeated captures
  /// (the Android SDK's stopScanning() runs inside onCaptureSuccess and leaves
  /// isScanning=false until rescan resets it). On iOS this just delegates to
  /// the existing internal cameraView.rescan() — safe to call after each capture.
  @objc func rescan() {
    // Android parity: rescan() on a stopped/unbound camera must rebuild it, not
    // silently no-op. CodeScannerView.rescan() only issues a fresh bind when the
    // underlying VSDKCameraSession is already .idle/.error (see its own doc comment) —
    // while this view's own actualCameraState tracker is anything but running/starting,
    // route through start() instead, so targetState/actualCameraState/
    // reassertControlProps stay in sync with the camera actually coming back up.
    guard actualCameraState == .running || actualCameraState == .starting else {
      NSLog("[RNVisionCameraView] rescan: camera not running (state=%@) — routing to start()", String(describing: actualCameraState))
      start()
      return
    }
    // Running: CodeScannerView.rescan() re-arms detection (clears detected codes/overlays,
    // shouldReturnDetectedCodes=true) — no visible preview change by design.
    cameraView?.rescan()
  }

  /// Mode-agnostic universal pause: stops per-frame detection analysis while
  /// keeping the camera session/preview alive. See CodeScannerView.pauseDetection()
  /// (vision-sdk-ios). Does not affect capture()/manual capturePhoto() calls.
  @objc func pauseDetection() {
    cameraView?.pauseDetection()
  }

  /// Resumes detection after a pauseDetection() call. See
  /// CodeScannerView.resumeDetection() (vision-sdk-ios).
  @objc func resumeDetection() {
    cameraView?.resumeDetection()
  }

  @objc func toggleFlash(enabled: Bool) {
    // Docs review C1 — identical bug class to setZoom below: this wrote only the legacy
    // `enableFlash` alias, but `resolvedTorch` permanently prefers the canonical `torch`
    // prop once `isTorchSet` flips true — which happens on EVERY mount (the codegen spec
    // declares `torch` with WithDefault<boolean, false>, so Fabric dispatches the default
    // at view creation). updateFlash() then re-applied torch=false, actively forcing the
    // torch OFF — toggleFlash was not just dead, it was inverted. Route through the
    // canonical prop's didSet, mirroring setTorchEnabled below.
    torch = enabled
  }
  
  @objc func setZoom(level: CGFloat) {
    // Route through the canonical `zoomRatio` prop's didSet, mirroring setTorchEnabled
    // below — otherwise this command is a permanent no-op once `isZoomRatioSet` has
    // flipped true, which happens on EVERY view mount: the codegen spec declares
    // `zoomRatio` with `WithDefault<Double, 1.0>`, so Fabric dispatches its default
    // value at view creation regardless of whether JS ever explicitly binds the prop.
    // Previously this wrote only the legacy `zoomLevel` alias, which `resolvedZoomRatio`
    // never reads once isZoomRatioSet is true — the imperative setZoom() command (the
    // zoom slider's drag handler) was silently dropped from the very first frame.
    zoomRatio = NSNumber(value: Float(level))
  }

  /// Duration-based ramped zoom (Android/iOS parity, spec §8 follow-up) — routes to
  /// `CodeScannerView.rampZoomRatio(_:durationMs:)`, which eases `videoZoomFactor` via
  /// `AVCaptureDevice.ramp(toVideoZoomFactor:withRate:)` instead of jumping like `setZoom`
  /// above. Also updates the tracked `zoomRatio` bookkeeping (via `isApplyingRampZoom`,
  /// see its declaration) so a facing switch mid-ramp reasserts the ramp's TARGET, not a
  /// stale pre-ramp value.
  @objc(rampZoomRatioWithRatio:durationMs:)
  func rampZoomRatio(_ ratio: CGFloat, durationMs: NSNumber) {
    guard !isDeallocating else { return }
    // Record the target unconditionally — mirrors setZoom's existing pattern above.
    // A call landing before `cameraView` exists (e.g. a mount-effect race) must not
    // lose the target; reassertControlProps() re-applies it via updateZoom() once the
    // camera comes up (RUNNING transition), same as any other declarative control prop.
    // That re-apply is an instant jump rather than a ramp (there's no live session to
    // animate yet anyway) — matches Android's "stores it and reaches the target" parity.
    isApplyingRampZoom = true
    zoomRatio = NSNumber(value: Float(ratio))
    isApplyingRampZoom = false

    guard let cameraView = cameraView else { return }
    cameraView.rampZoomRatio(Float(ratio), durationMs: durationMs.intValue)
  }

  // Camera Controls API (Phase 3) — commands. setFocusSettings below stays SEPARATE/
  // unaliased: it configures overlay styling (focus-image display, bbox colors), a
  // different concern from these one-shot autofocus/torch runtime controls.
  @objc func setTorchEnabled(_ enabled: Bool) {
    // Route through the `torch` prop's didSet so this command updates the same
    // resolved-value store as C2 (isTorchSet + updateFlash) — otherwise the value
    // is lost on the next reassertControlProps() (facing change / RUNNING transition).
    torch = enabled
  }

  @objc func setFocusPoint(_ x: CGFloat, _ y: CGFloat) {
    cameraView?.setFocusPoint(CGPoint(x: x, y: y))
  }

  @objc func setFocusSettings(jsonString: NSString) {
    guard let cameraView = cameraView else { return }
    guard let data = (jsonString as String).data(using: .utf8),
          let settings = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
      NSLog("[RNVisionCameraView] Failed to parse focus settings JSON")
      return
    }

    let shouldDisplayFocusImage = settings["shouldDisplayFocusImage"] as? Bool ?? false
    // showCodeBoundingBoxes prop takes precedence; setFocusSettings cannot turn boxes off
    // when the dedicated prop has enabled them.
    let showCodeBoundariesInMultipleScan =
      showCodeBoundingBoxes || (settings["showCodeBoundariesInMultipleScan"] as? Bool ?? false)
    // If scanArea is active, always honour the focus rect from the prop — the JSON
    // caller cannot override it (nor would they know the correct rect).
    let shouldScanInFocusImageRect: Bool
    if scanArea != nil {
      shouldScanInFocusImageRect = true
    } else {
      shouldScanInFocusImageRect = settings["shouldScanInFocusImageRect"] as? Bool ?? false
    }
    let showDocumentBoundaries = settings["showDocumentBoundaries"] as? Bool ?? false

    // When showCodeBoundingBoxes is active the dedicated barcode* props supply the
    // colors; fall back to the JSON-supplied or default values otherwise.
    let defaultBorderColor: UIColor = showCodeBoundingBoxes
      ? (Self.parseARGBColor(barcodeBoundingBoxBorderColor as String?) ?? UIColor(red: 0.545, green: 0.361, blue: 0.965, alpha: 1.0))
      : .green
    let defaultFillColor: UIColor = showCodeBoundingBoxes
      ? (Self.parseARGBColor(barcodeBoundingBoxFillColor as String?) ?? UIColor(red: 0.545, green: 0.361, blue: 0.965, alpha: 0.20))
      : UIColor.green.withAlphaComponent(0.3)
    let defaultBorderWidth: CGFloat = showCodeBoundingBoxes
      ? CGFloat(truncating: barcodeBoundingBoxBorderWidth)
      : 2.0
    let validCodeBoundaryBorderColor = Self.parseColor(settings["validCodeBoundaryBorderColor"] as? String) ?? defaultBorderColor
    let validCodeBoundaryBorderWidth = settings["validCodeBoundaryBorderWidth"] as? CGFloat ?? defaultBorderWidth
    let validCodeBoundaryFillColor = Self.parseColor(settings["validCodeBoundaryFillColor"] as? String) ?? defaultFillColor

    let inValidCodeBoundaryBorderColor = Self.parseColor(settings["inValidCodeBoundaryBorderColor"] as? String) ?? .red
    let inValidCodeBoundaryBorderWidth = settings["inValidCodeBoundaryBorderWidth"] as? CGFloat ?? 2.0
    let inValidCodeBoundaryFillColor = Self.parseColor(settings["inValidCodeBoundaryFillColor"] as? String) ?? UIColor.red.withAlphaComponent(0.3)

    let documentBoundaryBorderColor = Self.parseColor(settings["documentBoundaryBorderColor"] as? String) ?? .blue
    let documentBoundaryFillColor = Self.parseColor(settings["documentBoundaryFillColor"] as? String) ?? UIColor.blue.withAlphaComponent(0.3)

    let focusImageTintColor = Self.parseColor(settings["focusImageTintColor"] as? String) ?? .white
    let focusImageHighlightedColor = Self.parseColor(settings["focusImageHighlightedColor"] as? String) ?? .green

    // Carry the scan-area focus rect when active so this JSON path cannot lose it.
    let jsonFocusRect: CGRect
    if let scanArea = scanArea {
      let x = scanArea["x"] as? CGFloat ?? 0
      let y = scanArea["y"] as? CGFloat ?? 0
      let width = scanArea["width"] as? CGFloat ?? 0
      let height = scanArea["height"] as? CGFloat ?? 0
      jsonFocusRect = CGRect(x: x, y: y, width: width, height: height)
    } else {
      jsonFocusRect = .zero
    }

    let focusSettings = VisionSDK.CodeScannerView.FocusSettings.makeDefault(
      focusImageRect: jsonFocusRect,
      shouldDisplayFocusImage: shouldDisplayFocusImage,
      shouldScanInFocusImageRect: shouldScanInFocusImageRect,
      showCodeBoundariesInMultipleScan: showCodeBoundariesInMultipleScan,
      validCodeBoundryBorderColor: validCodeBoundaryBorderColor,
      validCodeBoundryBorderWidth: validCodeBoundaryBorderWidth,
      validCodeBoundryFillColor: validCodeBoundaryFillColor,
      inValidCodeBoundryBorderColor: inValidCodeBoundaryBorderColor,
      inValidCodeBoundryBorderWidth: inValidCodeBoundaryBorderWidth,
      inValidCodeBoundryFillColor: inValidCodeBoundaryFillColor,
      showDocumentBoundries: showDocumentBoundaries,
      documentBoundryBorderColor: documentBoundaryBorderColor,
      documentBoundryFillColor: documentBoundaryFillColor,
      focusImageTintColor: focusImageTintColor,
      focusImageHighlightedColor: focusImageHighlightedColor
    )

    cameraView.setFocusSettingsTo(focusSettings)
  }

  /// Parses a hex color string into a UIColor. 8-digit strings are `#AARRGGBB`
  /// (alpha first), matching Android's Color.parseColor and `parseARGBColor` below.
  ///
  /// Previously this used RRGGBBAA (alpha last), which disagreed with both Android
  /// and the barcode-bbox color path (`parseARGBColor`) — the same `#AARRGGBB`
  /// string rendered differently per platform/field. Unified on `#AARRGGBB` so one
  /// format is correct everywhere; this delegates to the single implementation.
  private static func parseColor(_ hex: String?) -> UIColor? {
    return parseARGBColor(hex)
  }

  /// Parses a hex color string in Android AARRGGBB format (e.g., "#8B5CF6", "#338B5CF6") into a UIColor.
  /// For 6-digit strings (#RRGGBB), alpha defaults to 1.0 (fully opaque).
  /// For 8-digit strings (#AARRGGBB), the first byte is alpha — matching Android's Color.parseColor behaviour.
  private static func parseARGBColor(_ hex: String?) -> UIColor? {
    guard let hex = hex else { return nil }
    let cleanHex = hex.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "#", with: "")

    var hexValue: UInt64 = 0
    let scanner = Scanner(string: cleanHex)
    guard scanner.scanHexInt64(&hexValue) else { return nil }

    if cleanHex.count == 6 {
      let r = CGFloat((hexValue & 0xFF0000) >> 16) / 255.0
      let g = CGFloat((hexValue & 0x00FF00) >> 8) / 255.0
      let b = CGFloat(hexValue & 0x0000FF) / 255.0
      return UIColor(red: r, green: g, blue: b, alpha: 1.0)
    } else if cleanHex.count == 8 {
      // AARRGGBB — alpha occupies the most-significant byte
      let a = CGFloat((hexValue & 0xFF000000) >> 24) / 255.0
      let r = CGFloat((hexValue & 0x00FF0000) >> 16) / 255.0
      let g = CGFloat((hexValue & 0x0000FF00) >> 8) / 255.0
      let b = CGFloat(hexValue & 0x000000FF) / 255.0
      return UIColor(red: r, green: g, blue: b, alpha: a)
    }
    return nil
  }
  
  // Camera Controls API (Phase 3): both delegate to CodeScannerView, which already
  // centralizes the switch-over-factor zoom conversion and torch control internally
  // (core-routed through cameraSession.controller) — no raw AVCaptureDevice access
  // needed here anymore. Deleted: the videoDevice lockForConfiguration()/torchMode/
  // virtualDeviceSwitchOverVideoZoomFactors hack this used to hand-roll.
  private func updateFlash() {
    guard let cameraView = cameraView else { return }
    cameraView.setFlashTurnedOn(resolvedTorch)
  }

  private func updateZoom() {
    guard let cameraView = cameraView else { return }
    cameraView.setZoomRatio(resolvedZoomRatio)
  }

  private func updateFocusMode() {
    guard let cameraView = cameraView, let mode = focusMode as String? else { return }
    let vsdkMode: VSDKFocusMode
    switch mode.lowercased() {
    case "single": vsdkMode = .single
    case "locked": vsdkMode = .locked
    default: vsdkMode = .continuous
    }
    cameraView.setFocusMode(vsdkMode)
  }

  /// Resolves `pinnedLensId` (a JS-supplied lens id string) against the lenses
  /// available for EITHER facing and pins it. Unknown/unpinnable ids NEVER throw —
  /// they fall back to `.automatic` and set a one-shot warning flag that the next
  /// `onCameraStateChanged` event surfaces (spec §8).
  ///
  /// Bug fix (parity with Android QA Group A #2) — a pinnable lens id only ever
  /// appears under its OWN facing's list (a front lens is never returned by
  /// `lenses(for: .back)` and vice versa), so gating this lookup to whatever facing
  /// the `cameraFacing` prop currently holds meant pinning a lens from the OTHER
  /// facing silently fell back to Auto with a warning — reproduced on-device:
  /// pinning id=5 (front) while `cameraFacing` is still "back" logged "pinnedLensId
  /// '5' unknown or unpinnable for facing=back". Search both facings so the pin
  /// resolves regardless of prop-set order, then bring the camera's facing in line
  /// with whichever facing the resolved lens actually belongs to.
  private func applyLensSelection() {
    guard let cameraView = cameraView else { return }
    // `CodeScannerView.setLensSelection` builds its `VSDKCameraConfiguration` from
    // `cameraSettings.cameraPosition` (the facing last applied via
    // `setCameraSettingsTo`), NOT from this view's `cameraFacing` prop — so facing and
    // the resolved lens must agree before we hand it off, in EVERY branch below,
    // otherwise the SDK is asked to select a lens under a facing it doesn't belong to.
    let currentFacing: VSDKLensFacing = (cameraFacing as String?)?.lowercased() == "front" ? .front : .back

    guard let lensId = pinnedLensId as String?, !lensId.isEmpty else {
      // Bug fix (parity with Android QA Group A #3) — Auto/unpin must bring facing back
      // in line with the `cameraFacing` prop. A prior cross-facing pin (below) only ever
      // touches CodeScannerView's internal cameraSettings.cameraPosition directly, out of
      // band from this prop; nothing else ever reset it back. Without this, unpinning
      // after a FRONT lens pin called setLensSelection(.automatic) while the SDK's
      // internal facing was still front — Auto stayed stuck on the front camera instead
      // of returning to back. syncFacing(to:) is a no-op at the SDK layer when facing
      // already matches, so this is safe to call unconditionally.
      syncFacing(to: currentFacing, on: cameraView)
      cameraView.setLensSelection(.automatic)
      return
    }
    let capabilities = VSDKCameraCapabilities.snapshot()
    let lenses = capabilities.lenses(for: .back) + capabilities.lenses(for: .front)
    guard let lens = lenses.first(where: { $0.id == lensId && $0.isPinnable }),
          let selection = try? VSDKLensSelection.pin(lens) else {
      print("[RNVisionCameraView] pinnedLensId '\(lensId)' unknown or unpinnable — falling back to Auto")
      syncFacing(to: currentFacing, on: cameraView)
      cameraView.setLensSelection(.automatic)
      pendingLensUnavailableWarning = true
      return
    }
    // Bug fix (on-device: "pin front, then pin back — never returns to back"): the old
    // `if lens.facing != currentFacing` gate compared against the cameraFacing PROP, but a
    // prior cross-facing pin changed CodeScannerView's INTERNAL cameraSettings.cameraPosition
    // out of band from that prop. Pinning a back lens while prop=back but internal=front
    // skipped the sync, so setLensSelection built its configuration under FRONT facing and
    // the back pin never took. syncFacing is a no-op at the SDK layer when facing already
    // matches (see the Auto branch above), so call it unconditionally with the RESOLVED
    // lens's facing — the only value that's correct in every ordering.
    syncFacing(to: lens.facing, on: cameraView)
    cameraView.setLensSelection(selection)
  }

  /// Pushes `facing` into CodeScannerView's own `cameraSettings.cameraPosition` — the
  /// only state `setLensSelection`/`applyLensSelection` actually key off of. Shared by
  /// every `applyLensSelection()` branch (pinned cross-facing switch, Auto/unpin
  /// reconciliation, and unknown-lens fallback) so all three agree on one code path.
  private func syncFacing(to facing: VSDKLensFacing, on cameraView: CodeScannerView) {
    let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
    cameraSettings.cameraPosition = facing == .front ? .front : .back
    cameraSettings.nthFrameToProcess = frameSkip?.int64Value ?? 10
    cameraView.setCameraSettingsTo(cameraSettings)
  }

  /// Re-asserts the current declarative camera-control prop values (pinnedLensId/
  /// zoomRatio/torch/focusMode) against the native view. A facing or lens change
  /// resets some of these to SDK defaults (spec §5.4); calling this after such a
  /// change, and after every transition into `.running`, makes sure the declared
  /// prop values always converge (spec §8).
  private func reassertControlProps() {
    guard cameraView != nil else { return }
    let lensKey = currentLensKey()
    if lensKey != lastAppliedLensKey {
      applyLensSelection()
      lastAppliedLensKey = lensKey
    }
    updateZoom()
    updateFlash()
    updateFocusMode()
  }


  private func updateCaptureMode() {
    guard let cameraView = cameraView else { return }

    let newCaptureMode: CaptureMode = autoCapture ? .auto : .manual
    currentCaptureMode = newCaptureMode
    cameraView.setCaptureModeTo(newCaptureMode)
  }
  
  private func updateScanArea() {
    guard let cameraView = cameraView else { return }

    // Update captureType to match the presence/absence of scanArea, then
    // apply a single coherent FocusSettings that carries both the focus rect
    // and the current bbox styling — neither clobbers the other.
    if scanArea != nil {
      captureType = .single
    } else {
      captureType = .multiple
    }
    cameraView.setCaptureTypeTo(captureType)

    cameraView.setFocusSettingsTo(buildFocusSettings())

    if scanArea != nil {
      cameraView.rescan()
    }
  }
  
  private func updateDetectionConfig() {
    guard let cameraView = cameraView, let config = detectionConfig else { return }

    let detectionSettings = VisionSDK.CodeScannerView.ObjectDetectionConfiguration()
    detectionSettings.isTextIndicationOn = config["text"] as? Bool ?? false
    detectionSettings.isBarCodeOrQRCodeIndicationOn = config["barcode"] as? Bool ?? false
    detectionSettings.isDocumentIndicationOn = config["document"] as? Bool ?? false
    detectionSettings.isImageSharpnessIndicationOn = config["sharpness"] as? Bool ?? false
    detectionSettings.codeDetectionConfidence = config["barcodeConfidence"] as? Float ?? 0.5
    detectionSettings.documentDetectionConfidence = config["documentConfidence"] as? Float ?? 0.5
    detectionSettings.secondsToWaitBeforeDocumentCapture = config["documentCaptureDelay"] as? Double ?? 2.0
    cameraView.setObjectDetectionConfigurationTo(detectionSettings)
  }

  private func updateTemplate() {
    guard let cameraView = cameraView else { return }

    let detectionSettings = VisionSDK.CodeScannerView.ObjectDetectionConfiguration()

    // Apply existing detection config settings first
    if let config = detectionConfig {
      detectionSettings.isTextIndicationOn = config["text"] as? Bool ?? false
      detectionSettings.isBarCodeOrQRCodeIndicationOn = config["barcode"] as? Bool ?? false
      detectionSettings.isDocumentIndicationOn = config["document"] as? Bool ?? false
      detectionSettings.isImageSharpnessIndicationOn = config["sharpness"] as? Bool ?? false
      detectionSettings.codeDetectionConfidence = config["barcodeConfidence"] as? Float ?? 0.5
      detectionSettings.documentDetectionConfidence = config["documentConfidence"] as? Float ?? 0.5
      detectionSettings.secondsToWaitBeforeDocumentCapture = config["documentCaptureDelay"] as? Double ?? 2.0
    }

    // Apply or remove template
    if let jsonString = templateJson as String?, !jsonString.isEmpty {
      if let templateData = jsonString.data(using: .utf8) {
        detectionSettings.selectedTemplate = templateData
      } else {
        NSLog("[RNVisionCameraView] Failed to convert template JSON to data")
      }
    } else {
      detectionSettings.selectedTemplate = nil
    }

    cameraView.setObjectDetectionConfigurationTo(detectionSettings)
  }
  
  private func updateFrameSkip() {
    guard let cameraView = cameraView, let frameSkip = frameSkip else { return }

    let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
    cameraSettings.nthFrameToProcess = frameSkip.int64Value
    if let facingString = cameraFacing?.lowercased {
      cameraSettings.cameraPosition = facingString == "front" ? .front : .back
    }
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

    // Reapply scan area after changing scan mode
    // Scan mode changes can reset focus settings
    updateScanArea()
  }

  /// Updates camera position dynamically when cameraFacing prop changes.
  ///
  /// Bug fix (parity with Android QA Group A #2): this used to manually stopRunning()
  /// then setCameraSettingsTo() then startRunning() after a fixed 0.15s delay — a
  /// leftover from before CodeScannerView grew its own live in-place facing switch
  /// (cameraSession.update(with:), spec §11/Task 11's freeze-bridge-masked swapInput).
  /// That manual stop landed BEFORE setCameraSettingsTo, which flips the underlying
  /// VSDKCameraSession to .idle; setCameraSettingsTo's own cameraSession.update(with:)
  /// call is then a same-tick no-op merge-only against an .idle session (see
  /// SessionReconciler.update(with:)'s `.idle` case) — beginCameraSwitchBridge() still
  /// fired regardless, freezing a preview that had already gone dark. The subsequent
  /// facing switch back only ever "worked" by accident, via the delayed startRunning()
  /// picking up the coalesced pendingConfiguration — a race, not a guarantee, which is
  /// exactly why Front→Back reproduced as stuck while Back→Front didn't.
  ///
  /// Just delegating straight to setCameraSettingsTo (mirroring applyLensSelection's
  /// facing-switch branch, which never did any manual stop/restart) lets
  /// CodeScannerView's own live switch run as designed: a same-session in-place input
  /// swap while RUNNING, or a plain pendingConfiguration merge while idle/starting —
  /// either way, no lost facing changes.
  private func updateCameraPosition() {
    guard let cameraView = cameraView else { return }
    guard !isDeallocating else { return }
    guard isSetupComplete else { return }

    let position: VisionSDK.CameraPosition
    if let facingString = cameraFacing?.lowercased {
      position = facingString == "front" ? .front : .back
    } else {
      position = .back
    }

    let cameraSettings = VisionSDK.CodeScannerView.CameraSettings()
    cameraSettings.cameraPosition = position
    cameraSettings.nthFrameToProcess = frameSkip?.int64Value ?? 10
    cameraView.setCameraSettingsTo(cameraSettings)

    // Facing changed — the active lens (and its zoom/torch/focus capabilities) reset
    // for the new facing (spec §5.4/§8); re-assert declarative control props once the
    // in-place switch has had time to settle. CodeScannerView exposes no completion
    // callback for this (didChangeCameraState fires on every intermediate transition
    // too, and reasserting from inside it would re-trigger itself via applyZoom/
    // TorchIfRunning's own state emits) — a short settle delay mirrors the pattern
    // already used elsewhere in this file (e.g. updateScanArea's post-start delay).
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
      self?.reassertControlProps()
    }
    // Docs review H1 — the swap is async on the session queue with no completion callback;
    // if it lands AFTER the 0.3s reassert, its runtimeSettings.resetToDefaults() erases the
    // torch/zoom the reassert just wrote and nothing re-fires (status stays .running through
    // an in-place swap). A second, idempotent reassert past any realistic swap duration
    // closes that window. ponytail: two timers, state-driven reassert if this ever flakes.
    DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
      self?.reassertControlProps()
    }
  }
  
  // MARK: - Helper Methods
  private func sendError(message: String) {
    guard let onError = onError else { return }
    onError(["message": message])
  }
  
  deinit {
    print("[RNVisionCameraView] deinit called")
    // Set flag FIRST to prevent any other operations from executing
    isDeallocating = true

    // Stop camera if running
    // Note: In deinit, we must stop synchronously to ensure cleanup completes
    // before the object is deallocated
    if actualCameraState == .running || actualCameraState == .starting {
      cameraView?.stopRunning()
      actualCameraState = .stopped
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
      // 0–1 normalized rect in image coordinates, top-left origin.
      // Use this when overlaying on the captured image — it survives
      // aspect-ratio differences between preview and saved photo.
      codeInfo["normalizedBoundingBox"] = [
        "x": code.normalizedBoundingBox.origin.x,
        "y": code.normalizedBoundingBox.origin.y,
        "width": code.normalizedBoundingBox.size.width,
        "height": code.normalizedBoundingBox.size.height
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
        "boundingBox": dict(from: code.boundingBox),
        "normalizedBoundingBox": dict(from: code.normalizedBoundingBox)
      ]
    }

    let qrCodeBoundingBoxes = qrCode.map { code in
      return [
        "scannedCode": code.stringValue,
        "symbology": code.symbology.stringValue(),
        "gs1ExtractedInfo": code.extractedData ?? [:],
        "boundingBox": dict(from: code.boundingBox),
        "normalizedBoundingBox": dict(from: code.normalizedBoundingBox)
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
            "boundingBox": dict(from: barcode.boundingBox),
            "normalizedBoundingBox": dict(from: barcode.normalizedBoundingBox)
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

  // MARK: - Camera Controls API (Phase 3) — state delegate → throttled event

  func codeScannerView(_ scannerView: CodeScannerView, didChangeCameraState state: VSDKCameraState) {
    // Guards against a state callback landing mid-teardown (deinit sets this first).
    guard !isDeallocating else { return }
    emitCameraState(state, bypassThrottle: false)
  }

  private func cameraStatusString(_ status: VSDKCameraStatus) -> String {
    switch status {
    case .idle: return "idle"
    case .starting: return "starting"
    case .running: return "running"
    case .interrupted: return "interrupted"
    case .error: return "error"
    @unknown default: return "idle"
    }
  }

  private func cameraErrorCodeString(_ error: NSError) -> String {
    switch VSDKCameraErrorCode(rawValue: error.code) {
    case .permissionDenied: return "permission-denied"
    case .lensUnavailable: return "lens-unavailable"
    case .configurationFailed: return "configuration-failed"
    // VisionSDK 2.6.0 (consumer-requested surfaced camera errors) — append-only cases
    // distinguishing an AVCaptureSession interruption/runtime error from a benign
    // .interrupted status transition. iOS-only: Android's CameraError sealed class has
    // no equivalent yet (see CameraErrorCode's doc in src/types.ts). Previously these
    // fell into `default` and were misreported as "configuration-failed".
    case .sessionInterrupted: return "session-interrupted"
    case .sessionRuntimeError: return "session-runtime-error"
    default: return "configuration-failed"
    }
  }

  /// Builds and emits the `onCameraStateChanged` payload. Throttled to ≤10Hz,
  /// trailing-edge-guaranteed; status/errorCode/warningCode transitions bypass
  /// the throttle entirely (spec §8). Payload keys must match Android's emitter
  /// and the locked `CameraStateChangedEvent` codegen shape exactly.
  private func emitCameraState(_ state: VSDKCameraState, bypassThrottle: Bool) {
    let statusString = cameraStatusString(state.status)
    let statusChanged = lastCameraStateStatus != statusString
    let now = Date().timeIntervalSince1970
    let hasError = state.error != nil
    let hasWarning = state.warning != nil || pendingLensUnavailableWarning
    let shouldEmit = bypassThrottle || statusChanged || hasError || hasWarning ||
        (now - lastCameraStateEmitTime) >= cameraStateThrottleSeconds

    guard shouldEmit else {
      scheduleTrailingCameraStateEmit(state)
      return
    }

    // This emit already carries the latest state — any pending trailing emit is stale.
    trailingCameraStateWorkItem?.cancel()
    trailingCameraStateWorkItem = nil
    performCameraStateEmit(state, statusString: statusString)
  }

  /// Schedules a single deferred emit of `state` at the throttle window boundary
  /// (trailing edge). Cancels/replaces any previously scheduled trailing emit, so
  /// only the freshest state from a burst ever lands once the window elapses.
  private func scheduleTrailingCameraStateEmit(_ state: VSDKCameraState) {
    trailingCameraStateWorkItem?.cancel()
    let elapsed = Date().timeIntervalSince1970 - lastCameraStateEmitTime
    let remaining = max(0, cameraStateThrottleSeconds - elapsed)
    let workItem = DispatchWorkItem { [weak self] in
      guard let self = self, !self.isDeallocating else { return }
      self.trailingCameraStateWorkItem = nil
      self.performCameraStateEmit(state, statusString: self.cameraStatusString(state.status))
    }
    trailingCameraStateWorkItem = workItem
    DispatchQueue.main.asyncAfter(deadline: .now() + remaining, execute: workItem)
  }

  private func performCameraStateEmit(_ state: VSDKCameraState, statusString: String) {
    lastCameraStateEmitTime = Date().timeIntervalSince1970
    lastCameraStateStatus = statusString

    var payload: [String: Any] = [
      "status": statusString,
      "facing": state.facing == .front ? "front" : "back",
      "zoomRatio": state.zoomRatio,
      "minZoomRatio": state.minZoomRatio,
      "maxZoomRatio": state.maxZoomRatio,
      "torchEnabled": state.isTorchEnabled,
      "isPreviewActive": state.isPreviewActive,
    ]
    switch state.focusMode {
    case .single: payload["focusMode"] = "single"
    case .locked: payload["focusMode"] = "locked"
    default: payload["focusMode"] = "continuous"
    }
    if let lens = state.activeLens { payload["activeLensId"] = lens.id }
    if let error = state.error {
      payload["errorCode"] = cameraErrorCodeString(error)
      payload["errorMessage"] = error.localizedDescription
    }
    if let warning = state.warning {
      payload["warningCode"] = cameraErrorCodeString(warning)
      payload["warningMessage"] = warning.localizedDescription
    } else if pendingLensUnavailableWarning {
      payload["warningCode"] = "lens-unavailable"
      // Matches Android's cameraStateToMap() warningMessage string exactly.
      payload["warningMessage"] = "pinnedLensId unavailable — falling back to Auto"
      pendingLensUnavailableWarning = false // one-shot; consumed
    }
    onCameraStateChanged?(payload)
  }
}
