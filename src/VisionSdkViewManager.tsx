import type { ReactNode } from 'react';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-vision-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

type VisionSdkProps = {
  children?: ReactNode;
  style?: ViewStyle;
  mode?: String;
  ref?: any;
  apiKey?: String;
  token?: String;
  locationId?: String;
  options?: any;
  environment?: String;
  captureMode?: String;
  delayTime: number;
  onBarcodeScanSuccess?: (e: any) => void;
  onOCRImageCaptured?: (e: any) => void;
  onOCRDataReceived?: (e: any) => void;
  onDetected?: (e: any) => void;
  onError?: (e: { nativeEvent: { message: any } }) => void;
};

const ComponentName = 'VisionSdkView';

export const VisionSdkView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<VisionSdkProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
