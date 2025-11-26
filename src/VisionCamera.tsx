import React, {
  useImperativeHandle,
  useRef,
  forwardRef,
  useCallback,
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
} from './VisionCameraTypes';

export * from './VisionCameraTypes';

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

    // Expose handlers via ref to parent components
    useImperativeHandle(ref, () => ({
      // Captures an image using Fabric command
      capture: () => {
        if (VisionCameraViewRef.current) {
          Commands.capture(VisionCameraViewRef.current);
        }
      },

      // Stops the camera using Fabric command
      stop: () => {
        if (VisionCameraViewRef.current) {
          Commands.stop(VisionCameraViewRef.current);
        }
      },

      // Starts the camera using Fabric command
      start: () => {
        if (VisionCameraViewRef.current) {
          Commands.start(VisionCameraViewRef.current);
        }
      },

      // Toggles flash using Fabric command
      toggleFlash: (enabled: boolean) => {
        if (VisionCameraViewRef.current) {
          Commands.toggleFlash(VisionCameraViewRef.current, enabled);
        }
      },

      // Sets zoom level using Fabric command
      setZoom: (level: number) => {
        if (VisionCameraViewRef.current) {
          Commands.setZoom(VisionCameraViewRef.current, level);
        }
      },
    }), []);

    // Helper function to handle events
    const parseNativeEvent = useCallback(<T,>(event: any): T => {
      // Ensure event is an object before checking for 'nativeEvent'
      if (event && typeof event === 'object' && 'nativeEvent' in event) {
        return event.nativeEvent;
      }
      return event; // If no 'nativeEvent', return the event itself
    }, []);

    const onCaptureHandler = useCallback((event: any) => {
      const nativeEvent = parseNativeEvent<any>(event);
      // Parse barcodesJson back to barcodes array for backward compatibility
      if (nativeEvent.barcodesJson && typeof nativeEvent.barcodesJson === 'string') {
        try {
          nativeEvent.barcodes = JSON.parse(nativeEvent.barcodesJson);
          delete nativeEvent.barcodesJson;
        } catch (e) {
          console.error('Failed to parse barcodesJson:', e);
        }
      }
      onCapture(nativeEvent);
    }, [onCapture])

    const onErrorHandler = useCallback((event: any) =>
      onError(parseNativeEvent<VisionCameraErrorResult>(event)), [onError])

    const onRecognitionUpdateHandler = useCallback(
      (event: any) =>
        onRecognitionUpdate(parseNativeEvent<VisionCameraRecognitionUpdateEvent>(event)),
      [onRecognitionUpdate]
    )

    const onSharpnessScoreUpdateHandler = useCallback(
      (event: any) =>
        onSharpnessScoreUpdate(parseNativeEvent<VisionCameraSharpnessScoreEvent>(event)),
      [onSharpnessScoreUpdate]
    )

    const onBarcodeDetectedHandler = useCallback(
      (event: any) => {
        const nativeEvent = parseNativeEvent<any>(event);
        // Parse codesJson back to codes array for backward compatibility
        if (nativeEvent.codesJson && typeof nativeEvent.codesJson === 'string') {
          try {
            nativeEvent.codes = JSON.parse(nativeEvent.codesJson);
            delete nativeEvent.codesJson;
          } catch (e) {
            console.error('Failed to parse codesJson:', e);
          }
        }
        onBarcodeDetected(nativeEvent);
      },
      [onBarcodeDetected]
    )

    const onBoundingBoxesUpdateHandler = useCallback(
      (event: any) => {
        const nativeEvent = parseNativeEvent<any>(event);
        // Parse JSON fields back to arrays for backward compatibility
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
        onBoundingBoxesUpdate(nativeEvent);
      },
      [onBoundingBoxesUpdate]
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
