import { requireNativeComponent, UIManager, Platform } from 'react-native';
import { VisionSdkViewProps } from './types';

const LINKING_ERROR =
  `The package 'react-native-vision-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ComponentName = 'VisionSdkView';

// Check if running in bridgeless mode (new architecture)
// @ts-ignore
const isBridgeless = global.RN$Bridgeless === true;

let VisionSdkViewNative;

if (isBridgeless) {
  // Use Fabric component (New Architecture)
  try {
    VisionSdkViewNative = require('./specs/VisionSdkViewNativeComponent').default;
    console.log('✅ VisionSdkView: Using Fabric component (New Architecture)');
  } catch (e) {
    throw new Error(LINKING_ERROR);
  }
} else {
  // Use legacy requireNativeComponent (Old Architecture)
  VisionSdkViewNative = requireNativeComponent<VisionSdkViewProps>(ComponentName);
  console.log('✅ VisionSdkView: Using Legacy component (Old Architecture)');
}

export const VisionSdkView = VisionSdkViewNative;
