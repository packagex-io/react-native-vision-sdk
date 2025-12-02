import React from 'react';
import VisionCameraViewNative from './specs/VisionCameraViewNativeComponent';
import type { VisionCameraViewProps } from './VisionCameraTypes';

// Wrapper component to handle prop conversion for Fabric
const VisionCameraViewWrapper = React.forwardRef<any, VisionCameraViewProps>((props, ref) => {
  // Convert object props to JSON strings for Fabric
  const fabricProps = React.useMemo(() => {
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
    props.cameraFacing,
    props.frameSkip,
    props.autoCapture,
  ]);

  return React.createElement(VisionCameraViewNative, { ...fabricProps, ref });
});

VisionCameraViewWrapper.displayName = 'VisionCameraView';

export const VisionCameraView = VisionCameraViewWrapper;
