/**
 * Dimensioning barrel.
 *
 * NOTE: Dimensioning is iOS-only. The <DimensioningView> component and
 * VisionDimensioning helpers only function on iOS 17+ LiDAR devices.
 */

export { DimensioningView } from './DimensioningView';
export type {
  DimensioningViewProps,
  DimensioningViewHandle,
} from './DimensioningView';
export { VisionDimensioning, deviceCapabilities, prefetchModels } from './VisionDimensioning';
export { DimensioningErrorCode } from './types';
export type {
  DimensioningMeasurement,
  DimensioningError,
  DimensioningMode,
  DimensioningMeasurementUnit,
  DimensioningCapabilities,
  // Live guidance (onMeasurementUpdate)
  DimensioningUpdate,
  DimensioningTrack,
  DimensioningTrackingState,
  // Custom overlays (overlayMode="callback" + onOverlayUpdate)
  DimensioningOverlayMode,
  DimensioningOverlayFrame,
  DimensioningOverlay,
  DimensioningPlaneOverlay,
  DimensioningHUD,
  DimensioningPoint,
  // Telemetry (enableTelemetry + onTelemetry)
  DimensioningTelemetryEvent,
  DimensioningCapturedTelemetry,
  DimensioningAbortedTelemetry,
} from './types';
