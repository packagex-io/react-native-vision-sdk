/**
 * VisionDimensioning — static helpers for the dimensioning feature.
 *
 * NOTE: Both methods are iOS-only. On Android the TurboModule is not
 * registered and the calls will throw "Native module DimensioningModule
 * not found". Guard calls with `Platform.OS === 'ios'` at the call site,
 * or catch the error.
 */

import NativeDimensioningModule from '../specs/NativeDimensioningModule';
import type { DimensioningCapabilities } from './types';

/**
 * Returns the device's dimensioning capability flags.
 * iOS-only.
 *
 * @returns Parsed DimensioningCapabilities object.
 */
export async function deviceCapabilities(): Promise<DimensioningCapabilities> {
  if (!NativeDimensioningModule) {
    throw new Error('DimensioningModule is not available on this platform.');
  }
  const json = await NativeDimensioningModule.deviceCapabilities();
  return JSON.parse(json) as DimensioningCapabilities;
}

/**
 * Pre-warm the bundled CoreML models for the dimensioning pipeline.
 * Call once on app launch (after setting VSDKConstants.apiKey if using
 * online mode). Cheap and idempotent on subsequent calls.
 * iOS-only.
 */
export async function prefetchModels(): Promise<void> {
  if (!NativeDimensioningModule) {
    throw new Error('DimensioningModule is not available on this platform.');
  }
  await NativeDimensioningModule.prefetchModels();
}

export const VisionDimensioning = {
  deviceCapabilities,
  prefetchModels,
};
