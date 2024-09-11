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
  apiKey?: string;
  token?: string;
  locationId?: string;
  options?: any;
  environment?: string;
  flash?: boolean;
  zoomLevel?: number;
  captureMode?: string;
  showDocumentBoundaries?: boolean;
  isOnDeviceOCR: boolean;
  showScanFrame?: boolean;
  captureWithScanFrame?: boolean;
  onBarcodeScan?: (e: any) => void;
  onModelDownloadProgress?: (e: any) => void;
  onImageCaptured?: (e: any) => void;
  onOCRScan?: (e: any) => void;
  onDetected?: (e: any) => void;
  onError?: (e: any) => void;
};

const ComponentName = 'VisionSdkView';

export const VisionSdkView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<VisionSdkProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
