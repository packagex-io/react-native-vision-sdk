import { requireNativeComponent, UIManager, Platform } from 'react-native';
import { VisionSdkViewProps } from './types';

const LINKING_ERROR =
  `The package 'react-native-vision-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ComponentName = 'VisionSdkView';

export const VisionSdkView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<VisionSdkViewProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
