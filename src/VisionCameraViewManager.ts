import React from 'react';
import VisionCameraViewNative from './specs/VisionCameraViewNativeComponent';
import type { VisionCameraViewProps } from './VisionCameraTypes';

// Wrapper component to handle prop conversion for Fabric
const VisionCameraViewWrapper = React.forwardRef<any, VisionCameraViewProps>((props, ref) => {
  const {
    scanArea,
    detectionConfig,
    template,
    // Event handlers — pass through directly, not through useMemo
    onCapture,
    onError,
    onRecognitionUpdate,
    onSharpnessScoreUpdate,
    onBarcodeDetected,
    onBoundingBoxesUpdate,
    // Everything else is a simple value prop
    ...valueProps
  } = props;

  // Only recompute JSON strings when the serialized values actually change
  const scanAreaJson = React.useMemo(
    () => (scanArea && typeof scanArea === 'object') ? JSON.stringify(scanArea) : '',
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [scanArea?.x, scanArea?.y, scanArea?.width, scanArea?.height]
  );

  const detectionConfigJson = React.useMemo(
    () => (detectionConfig && typeof detectionConfig === 'object') ? JSON.stringify(detectionConfig) : '',
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [detectionConfig?.text, detectionConfig?.barcode, detectionConfig?.document, detectionConfig?.documentConfidence, detectionConfig?.documentCaptureDelay, detectionConfig?.barcodeConfidence]
  );

  const templateJson = React.useMemo(
    () => (template && typeof template === 'object') ? JSON.stringify(template) : '',
    [template]
  );

  const nativeProps: any = {
    ...valueProps,
    scanAreaJson,
    detectionConfigJson,
    templateJson,
    onCapture,
    onError,
    onRecognitionUpdate,
    onSharpnessScoreUpdate,
    onBarcodeDetected,
    onBoundingBoxesUpdate,
    ref,
  };

  return React.createElement(VisionCameraViewNative, nativeProps);
});

VisionCameraViewWrapper.displayName = 'VisionCameraView';

export const VisionCameraView = VisionCameraViewWrapper;
