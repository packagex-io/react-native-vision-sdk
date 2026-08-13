/**
 * Types for the Dimensioning feature.
 * NOTE: Dimensioning is iOS-only. All types are usable cross-platform,
 * but the native view and module only function on iOS 17+ LiDAR devices.
 */

export type DimensioningMode = 'offline' | 'online';

export type DimensioningMeasurementUnit = 'centimeters' | 'inches' | 'meters';

/**
 * Error codes returned by the dimensioning native layer.
 * Values match VSDKDimensioningError ordinals in the iOS SDK.
 */
export enum DimensioningErrorCode {
  MissingCredentials = 0,
  NotConfigured = 1,
  LidarUnavailable = 2,
  ArSessionFailed = 3,
  NoGroundPlane = 4,
  CaptureTimedOut = 5,
  UserCancelled = 6,
  /** Bridge / serialization failure (not from the underlying VSDKDimensioningError). */
  InternalError = 7,
}

/** How the SDK draws its measurement overlay. */
export type DimensioningOverlayMode = 'builtIn' | 'none' | 'callback';

/** Coarse tracking progress, streamed through `onMeasurementUpdate`. */
export type DimensioningTrackingState =
  | 'searching'
  | 'groundFound'
  | 'boxDetected'
  | 'stable';

/** A 2-D point in the coordinate space documented by the field carrying it. */
export interface DimensioningPoint {
  x: number;
  y: number;
}

/** A single dimension measurement returned by onCapture. */
export interface DimensioningMeasurement {
  id: string;
  /** Stable id of the physical box — correlates with `DimensioningTrack.id`. */
  trackId: string;
  timestamp: number; // Unix seconds
  length: number;
  lengthUnit: string;
  width: number;
  widthUnit: string;
  height: number;
  heightUnit: string;
  distanceFromCamera: number;
  distanceFromCameraUnit: string;
  confidence: number; // 0.0 – 1.0
  usedCloudSAM: boolean;
  /** Volume in cubic metres. */
  volume: number;
  /** Pixel size of the captured frame; `{ width: 0, height: 0 }` when no frame was kept. */
  imagePixelSize: { width: number; height: number };
  /**
   * The 8 projected box corners, in the captured image's pixel space.
   * Indices 0–3 are the base face, 4–7 the top face, with corner *k* sitting
   * under corner *k+4* — enough to draw the measured box over the photo.
   * Empty when the SDK kept no frame.
   */
  boxVertices2D: DimensioningPoint[];
}

/** One tracked box, as reported by `onMeasurementUpdate`. */
export interface DimensioningTrack {
  id: string;
  /** Null until the pipeline has a reading for this box. */
  measurement: DimensioningMeasurement | null;
  isStable: boolean;
  /** Normalised (0–1) rect of the box on screen. */
  normalizedScreenRect: { x: number; y: number; width: number; height: number };
}

/** Live guidance payload — fires continuously while the camera is up. */
export interface DimensioningUpdate {
  trackingState: DimensioningTrackingState;
  tracks: DimensioningTrack[];
  /** Id of the box the SDK considers primary, if any. */
  primaryTrackId: string | null;
}

/** One box's overlay geometry, in view-space points. */
export interface DimensioningOverlay {
  trackId: string;
  isSelected: boolean;
  isStable: boolean;
  boxVertices2D: DimensioningPoint[];
  contour2D: DimensioningPoint[];
  boundingBox: { x: number; y: number; width: number; height: number };
}

/** A detected support plane, in view-space points. */
export interface DimensioningPlaneOverlay {
  id: string;
  isSelected: boolean;
  center2D: DimensioningPoint | null;
  boundary2D: DimensioningPoint[];
}

/** The SDK's own HUD state, so you can reproduce it when drawing your own. */
export interface DimensioningHUD {
  trackingState: DimensioningTrackingState;
  statusText: string;
  guidanceText: string | null;
  isCapturing: boolean;
  groundPlanePrompt: string | null;
}

/**
 * Every overlay primitive for one frame. Only delivered when
 * `overlayMode="callback"`, which also suppresses the SDK's built-in graphics.
 * All geometry is in view-space points, so it maps 1:1 onto the view.
 */
export interface DimensioningOverlayFrame {
  boxes: DimensioningOverlay[];
  planes: DimensioningPlaneOverlay[];
  hud: DimensioningHUD;
}

/** Emitted once per committed measurement when `enableTelemetry` is set. */
export interface DimensioningCapturedTelemetry {
  type: 'measurementCaptured';
  captureId: string;
  mode: string;
  lengthCm: number;
  widthCm: number;
  heightCm: number;
  distanceM: number;
  confidence: number;
  durationMs: number;
  samFrameCount: number;
  cloudRequested: boolean;
  cloudLanded: boolean;
  trackingState: string;
  primaryResidualMm?: number;
  consensusLevel?: string;
  crossCheckAgreementCount?: number;
  topFaceInlierFraction?: number;
}

/**
 * Emitted when a capture is abandoned. This is the **only** signal for a
 * capture that failed mid-session — the SDK's view has no error callback, so
 * `onError` only reports pre-flight problems.
 */
export interface DimensioningAbortedTelemetry {
  type: 'measurementAborted';
  captureId: string;
  /** e.g. 'timeout' | 'user_cancel' | 'no_dimensions' */
  reason: string;
  durationMs: number;
  samFrameCount: number;
  cloudRequested: boolean;
}

export type DimensioningTelemetryEvent =
  | DimensioningCapturedTelemetry
  | DimensioningAbortedTelemetry;

export interface DimensioningError {
  code: number;
  message: string;
  reason?: string;
}

/** Returned by VisionDimensioning.deviceCapabilities(). iOS-only. */
export interface DimensioningCapabilities {
  lidar: boolean;
  arWorldTracking: boolean;
  sceneReconstruction: boolean;
}
