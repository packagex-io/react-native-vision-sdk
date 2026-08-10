import { ReactNode } from 'react';
import { StyleProp, ViewStyle } from 'react-native';
import type { CameraErrorCode, CameraStatus, FocusMode, LensFacing, TemplateData } from './types';

/**
 * Camera scan mode types
 */
export type VisionCameraScanMode =
  | 'photo'
  | 'barcode'
  | 'qrcode'
  | 'barcodeorqrcode'
  | 'ocr'
  | 'barcodesinglecapture';

/**
 * Camera facing direction types.
 * Alias of `LensFacing` (`./types`) — kept as a separate exported name for
 * backwards compatibility with existing consumers of this file.
 */
export type CameraFacing = LensFacing;

/**
 * Event triggered when an image is captured by the camera.
 */
export interface VisionCameraCaptureEvent {
  /**
   * @type {string}
   * @description Path to the captured image.
   */
  image: string;

  /**
   * @type {string | undefined}
   * @description Optional native image URI if available.
   */
  nativeImage?: string;

  /**
   * @type {number | undefined}
   * @description Optional sharpness score of the captured image.
   */
  sharpnessScore?: number;

  /**
   * @type {VisionCameraBarcodeResult[] | undefined}
   * @description Optional array of detected barcodes in the captured image.
   */
  barcodes?: VisionCameraBarcodeResult[];
}

/**
 * Represents an error event in the Vision Camera.
 */
export interface VisionCameraErrorResult {
  /**
   * @type {string}
   * @description A description of the error that occurred.
   */
  message: string;

  /**
   * @type {number | undefined}
   * @description Optional error code for more specific error identification.
   * iOS filters error codes 13, 14, 15, 16 for cleaner error handling.
   */
  code?: number;
}

/**
 * Event triggered continuously with recognition updates from the camera feed.
 */
export interface VisionCameraRecognitionUpdateEvent {
  /**
   * @type {boolean}
   * @description Whether text is detected in the viewfinder.
   */
  text: boolean;

  /**
   * @type {boolean}
   * @description Whether a barcode is detected in the viewfinder.
   */
  barcode: boolean;

  /**
   * @type {boolean}
   * @description Whether a QR code is detected in the viewfinder.
   */
  qrcode: boolean;

  /**
   * @type {boolean}
   * @description Whether a document is detected in the viewfinder.
   */
  document: boolean;
}

/**
 * Event triggered with image sharpness score from the camera feed.
 */
export interface VisionCameraSharpnessScoreEvent {
  /**
   * @type {number}
   * @description The sharpness score of the current camera feed image.
   */
  sharpnessScore: number;
}

/**
 * Represents a single detected barcode result.
 */
export interface VisionCameraBarcodeResult {
  /**
   * @type {string}
   * @description The scanned barcode value.
   */
  scannedCode: string;

  /**
   * @type {string}
   * @description The barcode symbology type (e.g., QR, EAN, UPC, Code128).
   */
  symbology: string;

  /**
   * @type {object}
   * @description Bounding box of the detected barcode in the camera view's
   * coordinate space. Units vary by event source:
   * - iOS (all events): UIView points (RN layout units).
   * - Android `onBoundingBoxesUpdate`: DP (RN layout units, matches iOS).
   * - Android `onBarcodeDetected` / `onCapture`: preview-view pixels.
   *
   * For overlays on the live preview, use `onBoundingBoxesUpdate` and treat
   * values as RN layout units on both platforms. For overlays on the captured
   * image, prefer `normalizedBoundingBox` and multiply by image width/height.
   */
  boundingBox: {
    x: number;
    y: number;
    width: number;
    height: number;
  };

  /**
   * @type {object}
   * @description Bounding box normalized to 0–1 in image coordinates with top-left
   * origin. Use this when overlaying on the captured image (the saved photo) —
   * it survives any aspect-ratio difference between the preview and the image.
   * Multiply by image width/height to get pixel coordinates.
   */
  normalizedBoundingBox?: {
    x: number;
    y: number;
    width: number;
    height: number;
  };

  /**
   * @type {Record<string, string> | undefined}
   * @description Additional GS1 extracted information as key-value pairs (if available).
   */
  gs1ExtractedInfo?: Record<string, string>;
}

/**
 * Event triggered when barcodes (including QR codes) are detected.
 */
export interface VisionCameraBarcodeDetectedEvent {
  /**
   * @type {VisionCameraBarcodeResult[]}
   * @description Array of detected barcode results.
   */
  codes: VisionCameraBarcodeResult[];
}

/**
 * Bounding box coordinates for VisionCamera
 */
export interface VisionCameraBoundingBox {
  /**
   * @type {number}
   * @description X coordinate of the bounding box (top-left corner).
   */
  x: number;

  /**
   * @type {number}
   * @description Y coordinate of the bounding box (top-left corner).
   */
  y: number;

  /**
   * @type {number}
   * @description Width of the bounding box.
   */
  width: number;

  /**
   * @type {number}
   * @description Height of the bounding box.
   */
  height: number;
}

/**
 * Scan area/region configuration
 */
export interface ScanArea {
  /**
   * @type {number}
   * @description X coordinate of the scan area (top-left corner).
   */
  x: number;

  /**
   * @type {number}
   * @description Y coordinate of the scan area (top-left corner).
   */
  y: number;

  /**
   * @type {number}
   * @description Width of the scan area.
   */
  width: number;

  /**
   * @type {number}
   * @description Height of the scan area.
   */
  height: number;
}

/**
 * Object detection configuration
 */
export interface DetectionConfig {
  /**
   * @optional
   * @type {boolean}
   * @description Enable/disable text detection.
   * @default false
   */
  text?: boolean;

  /**
   * @optional
   * @type {boolean}
   * @description Enable/disable barcode/QR code detection.
   * @default false
   */
  barcode?: boolean;

  /**
   * @optional
   * @type {boolean}
   * @description Enable/disable document detection.
   * @default false
   */
  document?: boolean;

  /**
   * @optional
   * @type {boolean}
   * @description Enable/disable image sharpness scoring. iOS gates the
   * Laplacian sharpness computation in the native SDK behind this flag
   * (opt-in — avoids new Neural Engine/CPU work for existing consumers who
   * never asked for sharpness feedback). Android already computes sharpness
   * only when this flag is set.
   * @default false
   */
  sharpness?: boolean;

  /**
   * @optional
   * @type {number}
   * @description Minimum confidence threshold for barcode detection (0.0-1.0).
   * @default 0.5
   */
  barcodeConfidence?: number;

  /**
   * @optional
   * @type {number}
   * @description Minimum confidence threshold for document detection (0.0-1.0).
   * @default 0.5
   */
  documentConfidence?: number;

  /**
   * @optional
   * @type {number}
   * @description Delay in seconds before auto-capturing detected documents.
   * @default 2.0
   */
  documentCaptureDelay?: number;
}

/**
 * Detected code with bounding box information for VisionCamera
 */
export interface VisionCameraDetectedCodeBoundingBox {
  /**
   * @type {string}
   * @description The scanned barcode/QR code value.
   */
  scannedCode: string;

  /**
   * @type {string}
   * @description The barcode symbology type (e.g., QR, EAN, UPC, Code128).
   */
  symbology: string;

  /**
   * @type {Record<string, string> | undefined}
   * @description Additional GS1 extracted information as key-value pairs (if available).
   */
  gs1ExtractedInfo?: Record<string, string>;

  /**
   * @type {VisionCameraBoundingBox}
   * @description Bounding box of the detected code in the camera view's
   * coordinate space. iOS sends points; Android `onBoundingBoxesUpdate` sends
   * DP (so RN absolute-positioned overlays work on both platforms). Other
   * Android events send preview-view pixels — prefer `normalizedBoundingBox`
   * for cross-platform image overlays.
   */
  boundingBox: VisionCameraBoundingBox;

  /**
   * @type {VisionCameraBoundingBox}
   * @description Bounding box normalized to 0–1 in image coordinates with top-left
   * origin. Multiply by image width/height to overlay on the captured image.
   */
  normalizedBoundingBox?: VisionCameraBoundingBox;
}

/**
 * Event triggered continuously with bounding box updates for detected objects.
 */
export interface VisionCameraBoundingBoxesUpdateEvent {
  /**
   * @type {VisionCameraDetectedCodeBoundingBox[]}
   * @description Array of detected barcodes with full information including scanned code, symbology, and bounding box.
   */
  barcodeBoundingBoxes: VisionCameraDetectedCodeBoundingBox[];

  /**
   * @type {VisionCameraDetectedCodeBoundingBox[]}
   * @description Array of detected QR codes with full information including scanned code, symbology, and bounding box.
   */
  qrCodeBoundingBoxes: VisionCameraDetectedCodeBoundingBox[];

  /**
   * @type {VisionCameraBoundingBox}
   * @description Bounding box for detected document.
   */
  documentBoundingBox: VisionCameraBoundingBox;
}

/**
 * Event payload for the throttled (≤10Hz) camera-state stream (Camera Controls
 * API, Phase 3). Fires once immediately on listener attach with the current
 * state (replay), and status/error/warning transitions always bypass the
 * throttle.
 */
export interface VisionCameraStateEvent {
  /**
   * @type {CameraStatus}
   * @description Current camera session status.
   */
  status: CameraStatus;

  /**
   * @type {CameraErrorCode | undefined}
   * @description Fatal error code, only set when `status === 'error'`.
   */
  errorCode?: CameraErrorCode;

  /**
   * @type {string | undefined}
   * @description Human-readable message for `errorCode`.
   */
  errorMessage?: string;

  /**
   * @type {CameraErrorCode | undefined}
   * @description Non-fatal warning code (e.g. an unpinnable `pinnedLensId` falling back to Auto).
   */
  warningCode?: CameraErrorCode;

  /**
   * @type {string | undefined}
   * @description Human-readable message for `warningCode`.
   */
  warningMessage?: string;

  /**
   * @type {CameraFacing}
   * @description Which physical camera position is currently active.
   */
  facing: CameraFacing;

  /**
   * @type {string | undefined}
   * @description Id of the currently active lens (from `getCameraCapabilities()`), if known.
   */
  activeLensId?: string;

  /**
   * @type {number}
   * @description Current wide-normalized zoom ratio.
   */
  zoomRatio: number;

  /**
   * @type {number}
   * @description Minimum zoom ratio supported by the active lens/facing.
   */
  minZoomRatio: number;

  /**
   * @type {number}
   * @description Maximum zoom ratio supported by the active lens/facing.
   */
  maxZoomRatio: number;

  /**
   * @type {boolean}
   * @description Whether the torch/flash is currently on.
   */
  torchEnabled: boolean;

  /**
   * @type {FocusMode}
   * @description Currently active focus mode.
   */
  focusMode: FocusMode;

  /**
   * @type {boolean}
   * @description Whether the camera preview is actively rendering frames.
   * Ratified as part of this single throttled event — there is no separate
   * `onCameraReady` event.
   */
  isPreviewActive: boolean;
}

/**
 * Event payload for `onCameraStopped` — the "the old session is actually gone" signal
 * for a consumer-initiated `stop()` (e.g. before mounting a second camera screen).
 *
 * Contract (guarantees the native implementations must uphold, not just incidental
 * behavior a consumer happens to observe):
 *
 * - Exactly one `onCameraStopped` per consumer-initiated `stop()` call, whatever state
 *   the camera was already in — including calling `stop()` on an already-stopped
 *   camera. `stop()` never silently no-ops without emitting.
 * - Delivered even if the view unmounts while teardown is still in flight — unmounting
 *   must not suppress the event.
 * - Internal/automatic restarts the consumer never asked for (e.g. a facing-switch
 *   teardown-and-rebind) never emit this event, on either platform. Only a `stop()`
 *   the consumer actually called counts.
 * - If a `start()` call supersedes an in-flight `stop()` teardown (the consumer changed
 *   their mind mid-teardown), the event is still delivered rather than dropped — and the
 *   payload lets the consumer distinguish "teardown finished, camera is idle" from
 *   "teardown finished, but a `start()` already superseded it and the camera is running
 *   again."
 *   TODO(pr-199-reconcile): the exact shape of that disambiguation (a field added to
 *   this interface, or something else) is still being decided on the iOS side as part
 *   of reconciling the iOS/Android/RN branches for this change — treat `{}` below as a
 *   placeholder, not the final shape.
 *
 * Timing (a genuine, intentional platform difference — not faked parity): on iOS this is
 * driven by `CodeScannerView.stopRunning(completion:)`, which fires only once
 * `AVCaptureSession.stopRunning()` has actually returned — meaningfully LATER than
 * `onCameraStateChanged`'s `status: 'idle'`, which flips synchronously before the real
 * teardown completes. On Android, `CameraLifecycleCallback.onCameraStopped()` is
 * driven by the camera state listener's transition to `IDLE`, which reflects genuine
 * CameraX unbind completion — so it fires close to (not meaningfully after)
 * `onCameraStateChanged`'s own `status: 'idle'`. Both platforms guarantee the event
 * reflects real teardown of a consumer-initiated stop; the gap versus
 * `onCameraStateChanged` is the only currently-known timing difference, but see the
 * TODO above for a possible payload-shape difference still being finalized.
 */
export interface VisionCameraStoppedEvent {}

/**
 * Props for the Vision Camera view component.
 */
export interface VisionCameraViewProps {
  /**
   * @optional
   * @type {ReactNode}
   * @description Optional children elements to render inside the camera view.
   */
  children?: ReactNode;

  /**
   * @optional
   * @type {StyleProp<ViewStyle>}
   * @description Optional style to apply to the container.
   */
  style?: StyleProp<ViewStyle>;

  /**
   * @optional
   * @type {boolean}
   * @description Optional flag to enable or disable flash for capturing.
   * @deprecated Use `torch` instead. Feeds the same native path — if both are
   * set, `torch` wins and a one-time dev warning fires.
   */
  enableFlash?: boolean;

  /**
   * @optional
   * @type {number}
   * @description Optional zoom level for the camera.
   * @deprecated Use `zoomRatio` instead. Feeds the same native path — if both
   * are set, `zoomRatio` wins and a one-time dev warning fires.
   */
  zoomLevel?: number;

  /**
   * @optional
   * @type {string | undefined}
   * @description Pin a specific lens by id (from `VisionCore.getCameraCapabilities()`).
   * Undefined = Auto (OS picks the physical lens per zoom, default behavior).
   * An unknown or unpinnable id resolves to Auto with `warningCode: 'lens-unavailable'`
   * in the next `onCameraStateChanged` event — never a native throw.
   */
  pinnedLensId?: string;

  /**
   * @optional
   * @type {number | undefined}
   * @description Canonical zoom control. Wide-normalized absolute ratio: 1.0 = wide
   * lens at 1x, 0.5 = ultra-wide, 3.0 = telephoto territory — identical meaning on
   * both platforms. Supersedes the deprecated `zoomLevel` (same native path; if both
   * are set, `zoomRatio` wins and a one-time dev warning fires).
   * @default 1.0
   */
  zoomRatio?: number;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Canonical torch control. Supersedes the deprecated `enableFlash`
   * (same native path; if both are set, `torch` wins and a one-time dev warning fires).
   * @default false
   */
  torch?: boolean;

  /**
   * @optional
   * @type {FocusMode | undefined}
   * @description Camera focus mode: 'continuous' (AF-C), 'single' (AF-S), or 'locked'.
   * @default 'continuous'
   */
  focusMode?: FocusMode;

  /**
   * @optional
   * @param {VisionCameraStateEvent} event
   * @description Event handler for the throttled (≤10Hz) camera-state stream —
   * status, facing, active lens, zoom range, torch, focus mode, and any non-fatal
   * warning (e.g. an unpinnable `pinnedLensId` falling back to Auto). Fires once
   * immediately on attach with the current state (replay), and status/error/warning
   * transitions always bypass the throttle.
   */
  onCameraStateChanged?: (event: VisionCameraStateEvent) => void;

  /**
   * @optional
   * @param {VisionCameraStoppedEvent} event
   * @description Fires exactly once per consumer-initiated `stop()` call — including a
   * `stop()` on an already-stopped camera, and even if the view unmounts mid-teardown.
   * Never fires for internal/automatic restarts the consumer didn't request (e.g. a
   * facing-switch teardown-and-rebind). See `VisionCameraStoppedEvent`'s doc for the
   * full contract, the cross-platform timing note versus `onCameraStateChanged`'s
   * `status: 'idle'`, and a pending TODO on the start()-supersedes-stop() payload shape.
   */
  onCameraStopped?: (event: VisionCameraStoppedEvent) => void;

  /**
   * @optional
   * @type {VisionCameraScanMode}
   * @description Camera scan mode: 'photo', 'barcode', 'qrcode', 'barcodeorqrcode', 'ocr', 'barcodesinglecapture'.
   * @default 'photo'
   */
  scanMode?: VisionCameraScanMode;

  /**
   * @optional
   * @type {boolean}
   * @description Enable automatic capture when document is detected (mainly used with OCR mode).
   * @default false
   */
  autoCapture?: boolean;

  /**
   * @optional
   * @type {boolean}
   * @description When true, the native `BarcodeOverlayView` paints detected barcode bounding boxes
   *   directly on the camera surface (Choreographer-driven, spring-smoothed). Use this instead of
   *   drawing boxes from `onBoundingBoxesUpdate` in JS — eliminates the bridge/render path for
   *   overlay drawing. Callbacks still fire and can be used for data.
   * @default false
   */
  showCodeBoundingBoxes?: boolean;

  /**
   * Hex color for the native overlay border (default `#8B5CF6`). Only used when `showCodeBoundingBoxes=true`.
   *
   * Color format: `#RRGGBB` or, for alpha, **`#AARRGGBB` (alpha first)** — NOT the
   * CSS `#RRGGBBAA` convention. Both native parsers (iOS + Android) read the leading
   * byte as alpha, so e.g. 20%-opacity yellow is `#33FFD60A`, not `#FFD60A33`.
   */
  barcodeBoundingBoxBorderColor?: string;
  /** Native overlay border width in dp (default 3). Only used when `showCodeBoundingBoxes=true`. */
  barcodeBoundingBoxBorderWidth?: number;
  /** Fill color for the native overlay (default `#338B5CF6` = purple @ 20%). Use `#AARRGGBB` (alpha first) for alpha — see `barcodeBoundingBoxBorderColor`. Only used when `showCodeBoundingBoxes=true`. */
  barcodeBoundingBoxFillColor?: string;

  /**
   * @optional
   * @param {VisionCameraCaptureEvent} event
   * @type {(event: VisionCameraCaptureEvent) => void | undefined}
   * @description Event handler for image capture events.
   */
  onCapture?: (event: VisionCameraCaptureEvent) => void;

  /**
   * @optional
   * @param {VisionCameraErrorResult} event
   * @type {(event: VisionCameraErrorResult) => void | undefined}
   * @description Event handler for error events.
   */
  onError?: (event: VisionCameraErrorResult) => void;

  /**
   * @optional
   * @param {VisionCameraRecognitionUpdateEvent} event
   * @type {(event: VisionCameraRecognitionUpdateEvent) => void | undefined}
   * @description Event handler for continuous recognition updates from the camera feed.
   * Reports what objects (text, barcode, qrcode, document) are detected in the viewfinder.
   */
  onRecognitionUpdate?: (event: VisionCameraRecognitionUpdateEvent) => void;

  /**
   * @optional
   * @param {VisionCameraSharpnessScoreEvent} event
   * @type {(event: VisionCameraSharpnessScoreEvent) => void | undefined}
   * @description Event handler for continuous sharpness score updates from the camera feed.
   */
  onSharpnessScoreUpdate?: (event: VisionCameraSharpnessScoreEvent) => void;

  /**
   * @optional
   * @param {VisionCameraBarcodeDetectedEvent} event
   * @type {(event: VisionCameraBarcodeDetectedEvent) => void | undefined}
   * @description Event handler for barcode/QR code detection events.
   * Triggered when barcodes or QR codes are detected in scan modes: barcode, qrcode, barcodeorqrcode, barcodesinglecapture.
   */
  onBarcodeDetected?: (event: VisionCameraBarcodeDetectedEvent) => void;

  /**
   * @optional
   * @param {VisionCameraBoundingBoxesUpdateEvent} event
   * @type {(event: VisionCameraBoundingBoxesUpdateEvent) => void | undefined}
   * @description Event handler for continuous bounding box updates from the camera feed.
   * Reports bounding boxes for detected objects (barcodes, QR codes, documents) in the viewfinder.
   */
  onBoundingBoxesUpdate?: (event: VisionCameraBoundingBoxesUpdateEvent) => void;

  /**
   * @optional
   * @type {ScanArea}
   * @description Optional scan area to restrict scanning to a specific region of the camera feed.
   * When provided, only objects within this area will be detected.
   */
  scanArea?: ScanArea;

  /**
   * @optional
   * @type {DetectionConfig}
   * @description Optional object detection configuration to control which objects to detect and confidence thresholds.
   */
  detectionConfig?: DetectionConfig;

  /**
   * @optional
   * @type {number}
   * @description Optional frame skip interval for performance optimization.
   * Process every Nth frame (e.g., 10 = process 1 out of every 10 frames).
   * Higher values = better performance, lower detection frequency.
   * @default 10
   */
  frameSkip?: number;

  /**
   * @optional
   * @type {CameraFacing}
   * @description Camera facing direction - 'back' for rear camera or 'front' for front-facing camera.
   * @default 'back'
   */
  cameraFacing?: CameraFacing;

  /**
   * @optional
   * @type {TemplateData | null}
   * @description Optional template to apply for template matching via the native SDK.
   * Pass a TemplateData object to apply a template, or null to remove it.
   */
  template?: TemplateData | null;

  /**
   * @optional
   * @type {React.Ref<any>}
   * @description Optional reference to the component.
   */
  ref?: React.Ref<any>;
}

/**
 * Focus settings for configuring focus image, code boundaries, and document boundaries.
 */
export interface FocusSettings {
  /**
   * @optional
   * @type {boolean}
   * @description Whether to display the focus image overlay.
   * @default false
   */
  shouldDisplayFocusImage?: boolean;

  /**
   * @optional
   * @type {boolean}
   * @description Whether to restrict scanning to the focus image rect area.
   * @default false
   */
  shouldScanInFocusImageRect?: boolean;

  /**
   * @optional
   * @type {boolean}
   * @description Whether to show code boundaries when scanning multiple codes.
   * @default false
   */
  showCodeBoundariesInMultipleScan?: boolean;

  /**
   * @optional
   * @type {string}
   * @description Border color for valid code boundaries (hex color string, e.g., '#2abd51').
   * @default '#00ff00'
   */
  validCodeBoundaryBorderColor?: string;

  /**
   * @optional
   * @type {number}
   * @description Border width for valid code boundaries.
   * @default 2
   */
  validCodeBoundaryBorderWidth?: number;

  /**
   * @optional
   * @type {string}
   * @description Fill color for valid code boundaries. 8-digit hex is `#AARRGGBB` (alpha first),
   *   NOT CSS `#RRGGBBAA` — e.g. green @ 30% is '#4D00ff00'. `#RRGGBBAA` is not supported.
   * @default '#4D00ff00'
   */
  validCodeBoundaryFillColor?: string;

  /**
   * @optional
   * @type {string}
   * @description Border color for invalid code boundaries (hex color string, e.g., '#cc0829').
   * @default '#ff0000'
   */
  inValidCodeBoundaryBorderColor?: string;

  /**
   * @optional
   * @type {number}
   * @description Border width for invalid code boundaries.
   * @default 2
   */
  inValidCodeBoundaryBorderWidth?: number;

  /**
   * @optional
   * @type {string}
   * @description Fill color for invalid code boundaries. 8-digit hex is `#AARRGGBB` (alpha first),
   *   NOT CSS `#RRGGBBAA` — e.g. red @ 30% is '#4Dff0000'. `#RRGGBBAA` is not supported.
   * @default '#4Dff0000'
   */
  inValidCodeBoundaryFillColor?: string;

  /**
   * @optional
   * @type {boolean}
   * @description Whether to show document boundaries.
   * @default false
   */
  showDocumentBoundaries?: boolean;

  /**
   * @optional
   * @type {string}
   * @description Border color for document boundaries (hex color string, e.g., '#241616').
   * @default '#0000ff'
   */
  documentBoundaryBorderColor?: string;

  /**
   * @optional
   * @type {string}
   * @description Fill color for document boundaries. 8-digit hex is `#AARRGGBB` (alpha first),
   *   NOT CSS `#RRGGBBAA` — e.g. blue @ 30% is '#4D0000ff'. `#RRGGBBAA` is not supported.
   * @default '#4D0000ff'
   */
  documentBoundaryFillColor?: string;

  /**
   * @optional
   * @type {string}
   * @description Tint color for the focus image (hex color string, e.g., '#ffffff').
   * @default '#ffffff'
   */
  focusImageTintColor?: string;

  /**
   * @optional
   * @type {string}
   * @description Highlighted color for the focus image when object is detected (hex color string, e.g., '#e30000').
   * @default '#00ff00'
   */
  focusImageHighlightedColor?: string;
}

/**
 * Exposes methods to control the Vision Camera from the parent component.
 */
export interface VisionCameraRefProps {
  /**
   * Captures an image using the camera.
   * @description This method triggers the camera to capture an image.
   */
  capture: () => void;

  /**
   * Stops the camera.
   * @description This method stops the camera preview.
   */
  stop: () => void;

  /**
   * Starts the camera.
   * @description This method starts the camera preview.
   */
  start: () => void;

  /**
   * Tears down the camera session and rebuilds it from scratch.
   * @description Required on Android for repeated captures: the Android SDK calls
   * stopScanning() inside its onCaptureSuccess handler, which leaves isScanning=false.
   * Subsequent capture() calls bail out with CallStartCameraOrRescanBeforeCapture
   * (auto-rescan-after-capture was disabled in v3.0.x to fix overlay flicker). On
   * iOS the SDK auto-rescans internally after capture/detection, so calling
   * rescan() there is a no-op-equivalent — safe but redundant. Calling on both
   * platforms is safe and recommended for cross-platform consumers.
   */
  rescan: () => void;

  /**
   * Toggles the flash mode.
   * @param {boolean} enabled - Whether flash should be enabled.
   */
  toggleFlash: (enabled: boolean) => void;

  /**
   * Sets the zoom level.
   * @param {number} level - The zoom level to set.
   */
  setZoom: (level: number) => void;

  /**
   * Ramps the zoom smoothly from whatever's currently applied to `ratio`, over
   * `durationMs`, instead of jumping there in one tick like `setZoom`. Duration-based
   * on both platforms: iOS converts `durationMs` to Apple's rate-based
   * `AVCaptureDevice.ramp(toVideoZoomFactor:withRate:)` internally
   * (`rate = log2(target/current) / durationSeconds`); Android has no ramp primitive in
   * CameraX, so it drives a ~60/sec ticker toward the target instead. A new call cancels
   * any ramp already in flight, and an instant `setZoom`/`zoomRatio` prop change mid-ramp
   * cancels it too.
   * @param {number} ratio - Target wide-normalized zoom ratio (same meaning as `zoomRatio`).
   * @param {number} durationMs - Duration of the ramp in milliseconds.
   */
  rampZoomRatio: (ratio: number, durationMs: number) => void;

  /**
   * Configures focus settings including focus image, code boundaries, and document boundaries.
   * @param {FocusSettings} settings - The focus settings to apply.
   */
  setFocusSettings: (settings: FocusSettings) => void;

  /**
   * Sets the torch (flash) on/off. Fire-and-forget — the result lands in the
   * next `onCameraStateChanged` event's `torchEnabled` field.
   * @param {boolean} enabled - Whether the torch should be on.
   */
  setTorch: (enabled: boolean) => void;

  /**
   * Triggers a one-shot focus+metering pass at the given point (normalized 0-1,
   * top-left origin), under whatever `focusMode` is currently set. Does not
   * change `focusMode` itself.
   * @param {number} x - Normalized x coordinate (0-1).
   * @param {number} y - Normalized y coordinate (0-1).
   */
  setFocusPoint: (x: number, y: number) => void;

  /**
   * Pauses detection while keeping the camera session/preview alive.
   * @description Mode-agnostic universal pause: stops the underlying
   * per-frame Vision/CoreML (iOS) or MLKit/ONNX (Android) analysis work
   * without stopping the camera session/preview, and clears any in-flight
   * detection overlays. Use this instead of stop()/start() for a "captured,
   * showing loading spinner" moment where the live preview should stay
   * visible. Does not affect capture()/captureImage() or the Dimensioning
   * module (out of scope by design).
   */
  pauseDetection: () => void;

  /**
   * Resumes detection after a pauseDetection() call.
   */
  resumeDetection: () => void;
}

/**
 * Props for the Vision Camera component.
 */
export interface VisionCameraProps {
  /**
   * @optional
   * @type {ReactNode | undefined}
   * @description Optional children elements to render inside the camera component.
   */
  children?: ReactNode;

  /**
   * @optional
   * @type {StyleProp<ViewStyle>}
   * @description Style for the camera view container.
   */
  style?: StyleProp<ViewStyle>;

  /**
   * @optional
   * @type {React.Ref<any> | undefined}
   * @description Optional reference to the component.
   */
  refProp?: React.Ref<any>;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Optional flag to enable or disable flash for capturing.
   * @deprecated Use `torch` instead. Feeds the same native path — if both are
   * set, `torch` wins and a one-time dev warning fires.
   */
  enableFlash?: boolean;

  /**
   * @optional
   * @type {number | undefined}
   * @description Optional zoom level for the camera.
   * @deprecated Use `zoomRatio` instead. Feeds the same native path — if both
   * are set, `zoomRatio` wins and a one-time dev warning fires.
   */
  zoomLevel?: number;

  /**
   * @optional
   * @type {string | undefined}
   * @description Pin a specific lens by id (from `VisionCore.getCameraCapabilities()`).
   * Undefined = Auto (OS picks the physical lens per zoom, default behavior).
   * An unknown or unpinnable id resolves to Auto with `warningCode: 'lens-unavailable'`
   * in the next `onCameraStateChanged` event — never a native throw.
   */
  pinnedLensId?: string;

  /**
   * @optional
   * @type {number | undefined}
   * @description Canonical zoom control. Wide-normalized absolute ratio: 1.0 = wide
   * lens at 1x, 0.5 = ultra-wide, 3.0 = telephoto territory — identical meaning on
   * both platforms. Supersedes the deprecated `zoomLevel` (same native path; if both
   * are set, `zoomRatio` wins and a one-time dev warning fires).
   * @default 1.0
   */
  zoomRatio?: number;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Canonical torch control. Supersedes the deprecated `enableFlash`
   * (same native path; if both are set, `torch` wins and a one-time dev warning fires).
   * @default false
   */
  torch?: boolean;

  /**
   * @optional
   * @type {FocusMode | undefined}
   * @description Camera focus mode: 'continuous' (AF-C), 'single' (AF-S), or 'locked'.
   * @default 'continuous'
   */
  focusMode?: FocusMode;

  /**
   * @optional
   * @param {VisionCameraStateEvent} event
   * @description Event handler for the throttled (≤10Hz) camera-state stream —
   * status, facing, active lens, zoom range, torch, focus mode, and any non-fatal
   * warning (e.g. an unpinnable `pinnedLensId` falling back to Auto). Fires once
   * immediately on attach with the current state (replay), and status/error/warning
   * transitions always bypass the throttle.
   */
  onCameraStateChanged?: (event: VisionCameraStateEvent) => void;

  /**
   * @optional
   * @param {VisionCameraStoppedEvent} event
   * @description Fires exactly once per consumer-initiated `stop()` call — including a
   * `stop()` on an already-stopped camera, and even if the view unmounts mid-teardown.
   * Never fires for internal/automatic restarts the consumer didn't request (e.g. a
   * facing-switch teardown-and-rebind). See `VisionCameraStoppedEvent`'s doc for the
   * full contract, the cross-platform timing note versus `onCameraStateChanged`'s
   * `status: 'idle'`, and a pending TODO on the start()-supersedes-stop() payload shape.
   */
  onCameraStopped?: (event: VisionCameraStoppedEvent) => void;

  /**
   * @optional
   * @type {VisionCameraScanMode | undefined}
   * @description Camera scan mode: 'photo', 'barcode', 'qrcode', 'barcodeorqrcode', 'ocr', 'barcodesinglecapture'.
   * @default 'photo'
   */
  scanMode?: VisionCameraScanMode;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Enable automatic capture when document is detected (mainly used with OCR mode).
   * @default false
   */
  autoCapture?: boolean;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description When true, native `BarcodeOverlayView` paints detected boxes on the camera surface (Choreographer-driven). Skip drawing in JS when this is on.
   * @default false
   */
  showCodeBoundingBoxes?: boolean;

  /**
   * Hex color for the native overlay border (default `#8B5CF6`). Only used when `showCodeBoundingBoxes=true`.
   *
   * Color format: `#RRGGBB` or, for alpha, **`#AARRGGBB` (alpha first)** — NOT the
   * CSS `#RRGGBBAA` convention. Both native parsers (iOS + Android) read the leading
   * byte as alpha, so e.g. 20%-opacity yellow is `#33FFD60A`, not `#FFD60A33`.
   */
  barcodeBoundingBoxBorderColor?: string;
  /** Native overlay border width in dp (default 3). Only used when `showCodeBoundingBoxes=true`. */
  barcodeBoundingBoxBorderWidth?: number;
  /** Fill color for the native overlay (default `#338B5CF6` = purple @ 20%). Use `#AARRGGBB` (alpha first) for alpha — see `barcodeBoundingBoxBorderColor`. Only used when `showCodeBoundingBoxes=true`. */
  barcodeBoundingBoxFillColor?: string;

  /**
   * @optional
   * @param {VisionCameraCaptureEvent} event
   * @type {(event: VisionCameraCaptureEvent) => void | undefined}
   * @description Event handler for image capture events.
   */
  onCapture?: (event: VisionCameraCaptureEvent) => void;

  /**
   * @optional
   * @param {VisionCameraErrorResult} event
   * @type {(event: VisionCameraErrorResult) => void | undefined}
   * @description Event handler for error events.
   */
  onError?: (event: VisionCameraErrorResult) => void;

  /**
   * @optional
   * @param {VisionCameraRecognitionUpdateEvent} event
   * @type {(event: VisionCameraRecognitionUpdateEvent) => void | undefined}
   * @description Event handler for continuous recognition updates from the camera feed.
   * Reports what objects (text, barcode, qrcode, document) are detected in the viewfinder.
   */
  onRecognitionUpdate?: (event: VisionCameraRecognitionUpdateEvent) => void;

  /**
   * @optional
   * @param {VisionCameraSharpnessScoreEvent} event
   * @type {(event: VisionCameraSharpnessScoreEvent) => void | undefined}
   * @description Event handler for continuous sharpness score updates from the camera feed.
   */
  onSharpnessScoreUpdate?: (event: VisionCameraSharpnessScoreEvent) => void;

  /**
   * @optional
   * @param {VisionCameraBarcodeDetectedEvent} event
   * @type {(event: VisionCameraBarcodeDetectedEvent) => void | undefined}
   * @description Event handler for barcode/QR code detection events.
   * Triggered when barcodes or QR codes are detected in scan modes: barcode, qrcode, barcodeorqrcode, barcodesinglecapture.
   */
  onBarcodeDetected?: (event: VisionCameraBarcodeDetectedEvent) => void;

  /**
   * @optional
   * @param {VisionCameraBoundingBoxesUpdateEvent} event
   * @type {(event: VisionCameraBoundingBoxesUpdateEvent) => void | undefined}
   * @description Event handler for continuous bounding box updates from the camera feed.
   * Reports bounding boxes for detected objects (barcodes, QR codes, documents) in the viewfinder.
   */
  onBoundingBoxesUpdate?: (event: VisionCameraBoundingBoxesUpdateEvent) => void;

  /**
   * @optional
   * @type {ScanArea}
   * @description Optional scan area to restrict scanning to a specific region of the camera feed.
   * When provided, only objects within this area will be detected.
   */
  scanArea?: ScanArea;

  /**
   * @optional
   * @type {DetectionConfig}
   * @description Optional object detection configuration to control which objects to detect and confidence thresholds.
   */
  detectionConfig?: DetectionConfig;

  /**
   * @optional
   * @type {number}
   * @description Optional frame skip interval for performance optimization.
   * Process every Nth frame (e.g., 10 = process 1 out of every 10 frames).
   * Higher values = better performance, lower detection frequency.
   * @default 10
   */
  frameSkip?: number;

  /**
   * @optional
   * @type {CameraFacing}
   * @description Camera facing direction - 'back' for rear camera or 'front' for front-facing camera.
   * @default 'back'
   */
  cameraFacing?: CameraFacing;

  /**
   * @optional
   * @type {TemplateData | null}
   * @description Optional template to apply for template matching via the native SDK.
   * Pass a TemplateData object to apply a template, or null to remove it.
   */
  template?: TemplateData | null;
}
