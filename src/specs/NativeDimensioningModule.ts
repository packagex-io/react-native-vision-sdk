import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

/**
 * TurboModule interface for static dimensioning helpers.
 * NOTE: These methods are iOS-only. Calling from Android returns
 * a rejected promise / default values.
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
