/**
 * DimensioningView
 *
 * NOTE: Dimensioning is iOS-only. On iOS 17+ LiDAR devices this renders the
 * native VSDKDimensioningView. On all other platforms (Android, older iOS,
 * simulator) it renders an empty placeholder View.
 *
 * Gate rendering at the call site:
 *   const caps = await VisionDimensioning.deviceCapabilities();
 *   if (caps.lidar) { ... render <DimensioningView> ... }
 */

import React from 'react';
import { Platform, View, type ViewStyle } from 'react-native';
import type { DimensioningMeasurement, DimensioningError, DimensioningMode } from './types';
import NativeDimensioningView from '../specs/DimensioningViewNativeComponent';

export interface DimensioningViewProps {
  style?: ViewStyle;

  /** Processing mode. Default: 'offline'. */
  mode?: DimensioningMode;

  /** Measurement unit requested. Default: 'centimeters'. */
  measurementUnit?: string;

  /** Maximum number of simultaneous tracked objects. Default: 5. */
  maximumTrackCount?: number;

  /** Called when a stable measurement is captured. */
  onCapture?: (measurement: DimensioningMeasurement) => void;

  /** Called when an error occurs in the native view. */
  onError?: (error: DimensioningError) => void;
}

/**
 * <DimensioningView> — iOS-only 3-D box measurement component.
 *
 * Renders the native VSDKDimensioningView on iOS 17+ LiDAR devices.
 * Falls back to an empty View on Android and non-LiDAR / pre-iOS-17 devices
 * (the JS layer is expected to gate entry via deviceCapabilities()).
 */
export function DimensioningView({
  style,
  mode = 'offline',
  measurementUnit = 'centimeters',
  maximumTrackCount = 5,
  onCapture,
  onError,
}: DimensioningViewProps) {
  if (Platform.OS !== 'ios') {
    return <View style={style} />;
  }

  const handleCapture = onCapture
    ? (event: { nativeEvent: { measurementJson: string } }) => {
        try {
          const measurement: DimensioningMeasurement = JSON.parse(
            event.nativeEvent.measurementJson
          );
          onCapture(measurement);
        } catch {
          // malformed JSON from native — silently drop
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
