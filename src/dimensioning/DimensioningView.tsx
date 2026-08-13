/**
 * DimensioningView
 *
 * NOTE: Dimensioning is iOS-only. On iOS 17+ LiDAR devices this renders the
 * native SDK's `DimensioningView`. On Android the native side renders a
 * "not supported on this platform" placeholder TextView; capture/error
 * callbacks never fire there. On non-LiDAR iOS devices the iOS native side
 * calls onError with LidarUnavailable.
 *
 * Gate rendering at the call site:
 *   const caps = await VisionDimensioning.deviceCapabilities();
 *   if (caps.lidar) { ... render <DimensioningView> ... }
 */

import React from 'react';
import { type ViewStyle } from 'react-native';
import type {
  DimensioningMeasurement,
  DimensioningError,
  DimensioningMode,
  DimensioningMeasurementUnit,
  DimensioningOverlayMode,
  DimensioningUpdate,
  DimensioningOverlayFrame,
  DimensioningTelemetryEvent,
} from './types';
import NativeDimensioningView, {
  Commands,
} from '../specs/DimensioningViewNativeComponent';

/** Imperative handle returned through `ref`. */
export interface DimensioningViewHandle {
  /**
   * Tears the AR view down and releases the rear camera. ARKit and
   * AVCaptureSession cannot share it, so call this before showing
   * <VisionCamera>.
   */
  stop: () => void;
  /** Re-creates the AR view after `stop()`. */
  start: () => void;
}

export interface DimensioningViewProps {
  style?: ViewStyle;

  /** Processing mode. Default: 'offline'. */
  mode?: DimensioningMode;

  /**
   * Measurement unit for captures. Honored as of VisionSDK 2.7.0 — each
   * measurement's `lengthUnit` / `widthUnit` / `heightUnit` reflects it.
   *
   * @default 'centimeters'
   */
  measurementUnit?: DimensioningMeasurementUnit;

  /** Maximum number of simultaneous tracked objects. Default: 5. */
  maximumTrackCount?: number;

  /**
   * How the SDK draws its overlay.
   *  - `'builtIn'` (default): the SDK draws boxes, planes and its HUD.
   *  - `'none'`: nothing is drawn.
   *  - `'callback'`: built-in graphics are suppressed and every overlay
   *    primitive is streamed to `onOverlayUpdate` so you can draw your own.
   *
   * @default 'builtIn'
   */
  overlayMode?: DimensioningOverlayMode;

  /**
   * Cloud segmentation endpoint, used when `mode="online"`. Leave unset to fall
   * back to the host app's `VSDKConstants` (the pre-2.7.0 behaviour).
   */
  cloudUrl?: string;
  /** API key for `mode="online"`. Falls back to `VSDKConstants.apiKey`. */
  cloudApiKey?: string;
  /** SDK id for `mode="online"`. Falls back to the VSDKConstants environment. */
  cloudSdkId?: string;

  /**
   * Gates `onTelemetry`. Off by default.
   *
   * @default false
   */
  enableTelemetry?: boolean;

  /** Called when a stable measurement is captured. iOS only. */
  onCapture?: (measurement: DimensioningMeasurement) => void;

  /** Called when an error occurs in the native view. iOS only. */
  onError?: (error: DimensioningError) => void;

  /**
   * Live guidance — fires continuously with the current tracking state and the
   * in-progress dimensions of every tracked box. Use it to drive a
   * "hold steady… ready" HUD. iOS only.
   */
  onMeasurementUpdate?: (update: DimensioningUpdate) => void;

  /**
   * Raw overlay geometry for the current frame, in view-space points.
   * **Only fires when `overlayMode="callback"`.** iOS only.
   */
  onOverlayUpdate?: (frame: DimensioningOverlayFrame) => void;

  /**
   * Per-capture diagnostics. Requires `enableTelemetry`. The
   * `'measurementAborted'` variant is the only signal for a capture that failed
   * mid-session — `onError` only reports pre-flight problems. iOS only.
   */
  onTelemetry?: (event: DimensioningTelemetryEvent) => void;
}

/**
 * <DimensioningView> — 3-D box measurement component.
 *
 * Renders the native Fabric component on both iOS and Android:
 *  - **iOS 17+ LiDAR**: live AR measurement with capture/error/update events.
 *  - **iOS without LiDAR**: native view calls onError with LidarUnavailable (code 2).
 *  - **Android**: native side renders a placeholder TextView (no events).
 *
 * Gate entry on `VisionDimensioning.deviceCapabilities()` to avoid mounting
 * the view on unsupported devices.
 */
export const DimensioningView = React.forwardRef<
  DimensioningViewHandle,
  DimensioningViewProps
>(function DimensioningView(
  {
    style,
    mode = 'offline',
    measurementUnit = 'centimeters',
    maximumTrackCount = 5,
    overlayMode = 'builtIn',
    cloudUrl,
    cloudApiKey,
    cloudSdkId,
    enableTelemetry = false,
    onCapture,
    onError,
    onMeasurementUpdate,
    onOverlayUpdate,
    onTelemetry,
  },
  ref
) {
  const nativeRef = React.useRef<React.ComponentRef<
    typeof NativeDimensioningView
  > | null>(null);

  React.useImperativeHandle(
    ref,
    () => ({
      stop: () => {
        if (nativeRef.current) Commands.stop(nativeRef.current);
      },
      start: () => {
        if (nativeRef.current) Commands.start(nativeRef.current);
      },
    }),
    []
  );

  // Every native event payload is a JSON string — codegen can't express the
  // nested arrays these carry. One parser, one error path: a malformed payload
  // surfaces as InternalError (7) rather than silently dropping the event.
  const parse = React.useCallback(
    <T,>(json: string, what: string, deliver: (value: T) => void) => {
      try {
        deliver(JSON.parse(json) as T);
      } catch (err) {
        onError?.({
          code: 7,
          message: `Failed to parse ${what} payload from native`,
          reason: err instanceof Error ? err.message : String(err),
        });
      }
    },
    [onError]
  );

  const handleCapture = onCapture
    ? (event: { nativeEvent: { measurementJson: string } }) =>
        parse<DimensioningMeasurement>(
          event.nativeEvent.measurementJson,
          'measurement',
          onCapture
        )
    : undefined;

  const handleMeasurementUpdate = onMeasurementUpdate
    ? (event: { nativeEvent: { updateJson: string } }) =>
        parse<DimensioningUpdate>(
          event.nativeEvent.updateJson,
          'measurement update',
          onMeasurementUpdate
        )
    : undefined;

  const handleOverlayUpdate = onOverlayUpdate
    ? (event: { nativeEvent: { overlayJson: string } }) =>
        parse<DimensioningOverlayFrame>(
          event.nativeEvent.overlayJson,
          'overlay',
          onOverlayUpdate
        )
    : undefined;

  const handleTelemetry = onTelemetry
    ? (event: { nativeEvent: { telemetryJson: string } }) =>
        parse<DimensioningTelemetryEvent>(
          event.nativeEvent.telemetryJson,
          'telemetry',
          onTelemetry
        )
    : undefined;

  const handleError = onError
    ? (event: {
        nativeEvent: { code: number; message: string; reason?: string };
      }) => onError(event.nativeEvent)
    : undefined;

  return (
    <NativeDimensioningView
      ref={nativeRef}
      style={style}
      mode={mode}
      measurementUnit={measurementUnit}
      maximumTrackCount={maximumTrackCount}
      overlayMode={overlayMode}
      cloudUrl={cloudUrl}
      cloudApiKey={cloudApiKey}
      cloudSdkId={cloudSdkId}
      enableTelemetry={enableTelemetry}
      onCapture={handleCapture}
      onError={handleError}
      onMeasurementUpdate={handleMeasurementUpdate}
      onOverlayUpdate={handleOverlayUpdate}
      onTelemetry={handleTelemetry}
    />
  );
});
