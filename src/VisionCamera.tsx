import React, {
  useImperativeHandle,
  useRef,
  forwardRef,
  useCallback,
  useEffect,
} from 'react';
import {
  StyleSheet,
} from 'react-native';
import { VisionCameraView } from './VisionCameraViewManager';
import { Commands } from './specs/VisionCameraViewNativeComponent';
import {
  VisionCameraProps,
  VisionCameraRefProps,
  VisionCameraErrorResult,
  VisionCameraRecognitionUpdateEvent,
  VisionCameraSharpnessScoreEvent,
  FocusSettings,
} from './VisionCameraTypes';

export * from './VisionCameraTypes';

// Extract nativeEvent from Fabric event wrapper
function parseNativeEvent<T>(event: any): T {
  if (event && typeof event === 'object' && 'nativeEvent' in event) {
    return event.nativeEvent;
  }
  return event;
}

// Camera component
const Camera = forwardRef<VisionCameraRefProps, VisionCameraProps>(
  (
    {
      children,
      enableFlash = false,
      zoomLevel = 1.0,
      scanMode = 'photo',
      autoCapture = false,
      scanArea,
      detectionConfig,
      frameSkip,
      cameraFacing = 'back',
      template = null,
      showNativeBoundingBoxes = false,
      onCapture = () => { },
      onError = () => { },
      onRecognitionUpdate = () => { },
      onSharpnessScoreUpdate = () => { },
      onBarcodeDetected = () => { },
      onBoundingBoxesUpdate = () => { },
    },
    ref
  ) => {
    // Ref for the Vision Camera View
    const VisionCameraViewRef = useRef(null);

    // Stable refs for callback props — handlers never invalidate regardless of consumer code
    const onCaptureRef = useRef(onCapture);
    const onErrorRef = useRef(onError);
    const onRecognitionUpdateRef = useRef(onRecognitionUpdate);
    const onSharpnessScoreUpdateRef = useRef(onSharpnessScoreUpdate);
    const onBarcodeDetectedRef = useRef(onBarcodeDetected);
    const onBoundingBoxesUpdateRef = useRef(onBoundingBoxesUpdate);

    useEffect(() => {
      onCaptureRef.current = onCapture;
      onErrorRef.current = onError;
      onRecognitionUpdateRef.current = onRecognitionUpdate;
      onSharpnessScoreUpdateRef.current = onSharpnessScoreUpdate;
      onBarcodeDetectedRef.current = onBarcodeDetected;
      onBoundingBoxesUpdateRef.current = onBoundingBoxesUpdate;
    });

    // Expose handlers via ref to parent components
    useImperativeHandle(ref, () => ({
      capture: () => {
        if (VisionCameraViewRef.current) {
          Commands.capture(VisionCameraViewRef.current);
        }
      },

      stop: () => {
        if (VisionCameraViewRef.current) {
          Commands.stop(VisionCameraViewRef.current);
        }
      },

      start: () => {
        if (VisionCameraViewRef.current) {
          Commands.start(VisionCameraViewRef.current);
        }
      },

      toggleFlash: (enabled: boolean) => {
        if (VisionCameraViewRef.current) {
          Commands.toggleFlash(VisionCameraViewRef.current, enabled);
        }
      },

      setZoom: (level: number) => {
        if (VisionCameraViewRef.current) {
          Commands.setZoom(VisionCameraViewRef.current, level);
        }
      },

      setFocusSettings: (settings: FocusSettings) => {
        if (VisionCameraViewRef.current) {
          Commands.setFocusSettings(VisionCameraViewRef.current, JSON.stringify(settings));
        }
      },
    }), []);

    // All handlers use refs — permanently stable, never cause native view prop updates
    const onCaptureHandler = useCallback((event: any) => {
      const nativeEvent = parseNativeEvent<any>(event);
      if (nativeEvent.barcodesJson && typeof nativeEvent.barcodesJson === 'string') {
        try {
          nativeEvent.barcodes = JSON.parse(nativeEvent.barcodesJson);
          delete nativeEvent.barcodesJson;
        } catch (e) {
          console.error('Failed to parse barcodesJson:', e);
        }
      }
      onCaptureRef.current(nativeEvent);
    }, [])

    const onErrorHandler = useCallback((event: any) =>
      onErrorRef.current(parseNativeEvent<VisionCameraErrorResult>(event)), [])

    const onRecognitionUpdateHandler = useCallback(
      (event: any) =>
        onRecognitionUpdateRef.current(parseNativeEvent<VisionCameraRecognitionUpdateEvent>(event)),
      []
    )

    const onSharpnessScoreUpdateHandler = useCallback(
      (event: any) =>
        onSharpnessScoreUpdateRef.current(parseNativeEvent<VisionCameraSharpnessScoreEvent>(event)),
      []
    )

    const onBarcodeDetectedHandler = useCallback(
      (event: any) => {
        const nativeEvent = parseNativeEvent<any>(event);
        if (nativeEvent.codesJson && typeof nativeEvent.codesJson === 'string') {
          try {
            nativeEvent.codes = JSON.parse(nativeEvent.codesJson);
            delete nativeEvent.codesJson;
          } catch (e) {
            console.error('Failed to parse codesJson:', e);
          }
        }
        onBarcodeDetectedRef.current(nativeEvent);
      },
      []
    )

    const onBoundingBoxesUpdateHandler = useCallback(
      (event: any) => {
        const nativeEvent = parseNativeEvent<any>(event);
        if (nativeEvent.barcodeBoundingBoxesJson && typeof nativeEvent.barcodeBoundingBoxesJson === 'string') {
          try {
            nativeEvent.barcodeBoundingBoxes = JSON.parse(nativeEvent.barcodeBoundingBoxesJson);
            delete nativeEvent.barcodeBoundingBoxesJson;
          } catch (e) {
            console.error('Failed to parse barcodeBoundingBoxesJson:', e);
          }
        }
        if (nativeEvent.qrCodeBoundingBoxesJson && typeof nativeEvent.qrCodeBoundingBoxesJson === 'string') {
          try {
            nativeEvent.qrCodeBoundingBoxes = JSON.parse(nativeEvent.qrCodeBoundingBoxesJson);
            delete nativeEvent.qrCodeBoundingBoxesJson;
          } catch (e) {
            console.error('Failed to parse qrCodeBoundingBoxesJson:', e);
          }
        }
        onBoundingBoxesUpdateRef.current(nativeEvent);
      },
      []
    )

    return (
      <>
        <VisionCameraView
          ref={VisionCameraViewRef}
          style={styles.flex}
          enableFlash={enableFlash}
          zoomLevel={zoomLevel}
          scanMode={scanMode}
          autoCapture={autoCapture}
          scanArea={scanArea}
          detectionConfig={detectionConfig}
          frameSkip={frameSkip}
          cameraFacing={cameraFacing}
          template={template}
          showNativeBoundingBoxes={showNativeBoundingBoxes}
          onCapture={onCaptureHandler}
          onError={onErrorHandler}
          onRecognitionUpdate={onRecognitionUpdateHandler}
          onSharpnessScoreUpdate={onSharpnessScoreUpdateHandler}
          onBarcodeDetected={onBarcodeDetectedHandler}
          onBoundingBoxesUpdate={onBoundingBoxesUpdateHandler}
        />
        {children}
      </>
    );
  }
);

export { Camera as VisionCamera };
export default Camera;

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
});
