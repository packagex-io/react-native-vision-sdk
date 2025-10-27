import React, {
  useImperativeHandle,
  useRef,
  forwardRef,
  useCallback,
} from 'react';
import {
  UIManager,
  findNodeHandle,
  StyleSheet,
} from 'react-native';
import { VisionCameraView } from './VisionCameraViewManager';
import {
  VisionCameraProps,
  VisionCameraRefProps,
  VisionCameraCaptureEvent,
  VisionCameraErrorResult,
  VisionCameraRecognitionUpdateEvent,
  VisionCameraSharpnessScoreEvent,
  VisionCameraBarcodeDetectedEvent,
  VisionCameraBoundingBoxesUpdateEvent,
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

    // Enum for Commands
    enum Commands {
      capture = 0,
      stop,
      start,
      toggleFlash,
      setZoom,
    }

    /* Command functions using dispatchCommand helper with name and enum fallback */
    const dispatchCommand = useCallback((
      commandName: keyof typeof Commands,
      params: any[] = []
    ) => {
      try {
        // Attempt to retrieve the command from the VisionCameraView's UIManager configuration. If not found, fall back to using the command from the Commands enum.
        const command =
          UIManager.getViewManagerConfig('VisionCameraView')?.Commands[
          commandName
          ] ?? Commands[commandName];
        // If command is not found in either UIManager or Commands, throw an error.
        if (command === undefined) {
          throw new Error(
            `Command "${commandName}" not found in VisionCameraView or Commands.`
          );
        }

        // Dispatch the command with the provided parameters to the native module (VisionCameraView).
        const viewHandle = findNodeHandle(VisionCameraViewRef.current)
        if (!viewHandle) return
        UIManager.dispatchViewManagerCommand(
          viewHandle, // Find the native view reference
          command, // The command to dispatch
          params // Parameters to pass with the command
        );
      } catch (error: any) {
        console.error(`🚨 Error dispatching command: ${error.message}`);
        onError({ message: error.message });
      }
    }, [onError]);

    // Expose handlers via ref to parent components
    useImperativeHandle(ref, () => ({
      // 0: Captures an image using the 'capture' command
      capture: () => dispatchCommand('capture'),

      // 1: Stops the camera using the 'stop' command
      stop: () => dispatchCommand('stop'),

      // 2: Starts the camera using the 'start' command
      start: () => dispatchCommand('start'),

      // 3: Toggles flash using the 'toggleFlash' command
      toggleFlash: (enabled: boolean) => dispatchCommand('toggleFlash', [enabled]),

      // 4: Sets zoom level using the 'setZoom' command
      setZoom: (level: number) => dispatchCommand('setZoom', [level]),
    }), [dispatchCommand]);

    // Helper function to handle events
    const parseNativeEvent = useCallback(<T,>(event: any): T => {
      // Ensure event is an object before checking for 'nativeEvent'
      if (event && typeof event === 'object' && 'nativeEvent' in event) {
        return event.nativeEvent;
      }
      return event; // If no 'nativeEvent', return the event itself
    }, []);

    const onCaptureHandler = useCallback((event: any) =>
      onCapture(parseNativeEvent<VisionCameraCaptureEvent>(event)),
      [onCapture])

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
      (event: any) =>
        onBarcodeDetected(parseNativeEvent<VisionCameraBarcodeDetectedEvent>(event)),
      [onBarcodeDetected]
    )

    const onBoundingBoxesUpdateHandler = useCallback(
      (event: any) =>
        onBoundingBoxesUpdate(parseNativeEvent<VisionCameraBoundingBoxesUpdateEvent>(event)),
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
