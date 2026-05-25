import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

/**
 * TurboModule interface for static dimensioning helpers.
 * NOTE: Dimensioning is iOS-only. The module is registered on both
 * platforms; on Android the implementation is a stub that resolves
 * `deviceCapabilities()` with all-false flags and `prefetchModels()`
 * with `undefined`.
 */
export interface Spec extends TurboModule {
  /**
   * Returns a JSON-encoded VSDKDimensioningCapabilities object.
   * Fields: { lidar: boolean, arWorldTracking: boolean, sceneReconstruction: boolean }
   * iOS-only.
   */
  deviceCapabilities(): Promise<string>;

  /**
   * Pre-warm the bundled CoreML dimensioning models so the first capture
   * session doesn't pay JIT-compile cost. Safe to call multiple times.
   * iOS-only.
   */
  prefetchModels(): Promise<void>;
}

export default TurboModuleRegistry.get<Spec>('DimensioningModule');
