import { requireNativeComponent, UIManager, Platform } from 'react-native';
import { VisionSdkViewProps } from './types';

const LINKING_ERROR =
  `The package 'react-native-vision-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ComponentName = 'VisionSdkView';

// Try to import the Fabric (codegen) component first
// Falls back to legacy component if not available
let VisionSdkViewNative;

try {
  // Try to import the codegen spec (New Architecture)
  VisionSdkViewNative = require('./specs/VisionSdkViewNativeComponent').default;
} catch (e) {
  // Fall back to legacy requireNativeComponent (Old Architecture)
  VisionSdkViewNative = UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<VisionSdkViewProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
}

export const VisionSdkView = VisionSdkViewNative;
