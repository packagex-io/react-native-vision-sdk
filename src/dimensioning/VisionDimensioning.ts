/**
 * VisionDimensioning — static helpers for the dimensioning feature.
 *
 * **Dimensioning is iOS-only.** On Android the TurboModule is registered
 * as a stub that resolves with all-false capabilities (and prefetchModels
 * is a no-op), so these methods are safe to call cross-platform — they
 * just don't do anything useful on Android. Use the result of
 * `deviceCapabilities()` to decide whether to expose any dimensioning UI.
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
