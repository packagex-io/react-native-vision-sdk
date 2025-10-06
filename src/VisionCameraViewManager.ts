import { requireNativeComponent, ViewStyle } from 'react-native';
import type { VisionCameraViewProps } from './VisionCameraTypes';

interface NativeProps extends VisionCameraViewProps {
  style?: ViewStyle;
}

export const VisionCameraView = requireNativeComponent<NativeProps>('VisionCameraView');
