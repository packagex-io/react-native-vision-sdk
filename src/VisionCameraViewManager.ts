import React from 'react';
import { requireNativeComponent, UIManager, Platform } from 'react-native';
import type { VisionCameraViewProps } from './VisionCameraTypes';

const LINKING_ERROR =
  `The package 'react-native-vision-sdk' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const ComponentName = 'VisionCameraView';

// Try to import the Fabric (codegen) component first
// Falls back to legacy component if not available
let VisionCameraViewNative;
let isFabric = false;

try {
  // Try to import the codegen spec (New Architecture)
  VisionCameraViewNative = require('./specs/VisionCameraViewNativeComponent').default;
  isFabric = true;
  console.log('✅ VisionCameraView: Using Fabric component');
} catch (e) {
  // Fall back to legacy requireNativeComponent (Old Architecture)
  VisionCameraViewNative = UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<VisionCameraViewProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
  console.log('✅ VisionCameraView: Using Legacy component');
}

// Wrapper component to handle prop conversion for Fabric
const VisionCameraViewWrapper = React.forwardRef<any, VisionCameraViewProps>((props, ref) => {
  // If using Fabric, convert object props to JSON strings
  const fabricProps = React.useMemo(() => {
    if (!isFabric) return props;

    const convertedProps: any = { ...props };

    // Convert scanArea object to JSON string, or clear it if undefined
    if (props.scanArea && typeof props.scanArea === 'object') {
      convertedProps.scanAreaJson = JSON.stringify(props.scanArea);
    } else {
      convertedProps.scanAreaJson = '';
    }
    delete convertedProps.scanArea;

    // Convert detectionConfig object to JSON string, or clear it if undefined
    if (props.detectionConfig && typeof props.detectionConfig === 'object') {
      convertedProps.detectionConfigJson = JSON.stringify(props.detectionConfig);
    } else {
      convertedProps.detectionConfigJson = '';
    }
    delete convertedProps.detectionConfig;

    return convertedProps;
  }, [
    props.scanArea,
    props.detectionConfig,
    props.enableFlash,
    props.zoomLevel,
    props.scanMode,
    props.autoCapture,
    props.frameSkip,
    props.cameraFacing,
    props.onCapture,
    props.onError,
    props.onRecognitionUpdate,
    props.onSharpnessScoreUpdate,
    props.onBarcodeDetected,
    props.onBoundingBoxesUpdate,
  ]);

  // For legacy architecture, pass props as-is
  return React.createElement(VisionCameraViewNative, { ...(isFabric ? fabricProps : props), ref });
});

VisionCameraViewWrapper.displayName = 'VisionCameraView';

export const VisionCameraView = VisionCameraViewWrapper;
