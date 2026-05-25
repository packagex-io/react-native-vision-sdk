/**
 * DimensioningView
 *
 * NOTE: Dimensioning is iOS-only. On iOS 17+ LiDAR devices this renders the
 * native VSDKDimensioningView. On Android the native side renders a
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
import type { DimensioningMeasurement, DimensioningError, DimensioningMode } from './types';
import NativeDimensioningView from '../specs/DimensioningViewNativeComponent';

export interface DimensioningViewProps {
  style?: ViewStyle;

  /** Processing mode. Default: 'offline'. */
  mode?: DimensioningMode;

  /**
   * Measurement unit requested.
   *
   * **Known limitation**: this prop is currently not honored on iOS. The native
   * SDK's convenience `configure(delegate:mode:maximumTrackCount:)` hard-codes
   * centimeters. Captures always come back in `cm` (visible on each
   * measurement's `lengthUnit` / `widthUnit` / `heightUnit` fields).
   * Will be wired through `VSDKDimensioningConfiguration` in a future release.
   *
   * @default 'centimeters'
   */
  measurementUnit?: string;

  /** Maximum number of simultaneous tracked objects. Default: 5. */
  maximumTrackCount?: number;

  /** Called when a stable measurement is captured. iOS only. */
  onCapture?: (measurement: DimensioningMeasurement) => void;

  /** Called when an error occurs in the native view. iOS only. */
  onError?: (error: DimensioningError) => void;
}

/**
 * <DimensioningView> — 3-D box measurement component.
 *
 * Renders the native Fabric component on both iOS and Android:
 *  - **iOS 17+ LiDAR**: live VSDKDimensioningView with capture/error events.
 *  - **iOS without LiDAR**: native view calls onError with LidarUnavailable (code 2).
 *  - **Android**: native side renders a placeholder TextView (no events).
 *
 * Gate entry on `VisionDimensioning.deviceCapabilities()` to avoid mounting
 * the view on unsupported devices.
 */
export function DimensioningView({
  style,
  mode = 'offline',
  measurementUnit = 'centimeters',
  maximumTrackCount = 5,
  onCapture,
  onError,
}: DimensioningViewProps) {
  const handleCapture = onCapture
    ? (event: { nativeEvent: { measurementJson: string } }) => {
        try {
          const measurement: DimensioningMeasurement = JSON.parse(
            event.nativeEvent.measurementJson
          );
          onCapture(measurement);
        } catch (err) {
          // Surface the parse failure to onError instead of silently swallowing it,
          // so consumers aren't left waiting for a capture that will never arrive.
          onError?.({
            code: 7,
            message: 'Failed to parse measurement payload from native',
            reason: err instanceof Error ? err.message : String(err),
          });
        }
      }
    : undefined;

  const handleError = onError
    ? (event: { nativeEvent: { code: number; message: string; reason?: string } }) => {
        onError(event.nativeEvent);
      }
    : undefined;

  return (
    <NativeDimensioningView
      style={style}
      mode={mode}
      measurementUnit={measurementUnit}
      maximumTrackCount={maximumTrackCount}
      onCapture={handleCapture}
      onError={handleError}
    />
  );
}
