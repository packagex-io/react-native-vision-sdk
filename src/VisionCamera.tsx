import React, {
  useImperativeHandle,
  useRef,
  forwardRef,
  useCallback,
} from 'react';
import { StyleSheet } from 'react-native';
import { VisionCameraView } from './VisionCameraViewManager';
import { Commands } from './specs/VisionCameraViewNativeComponent';
import {
  VisionCameraProps,
  VisionCameraRefProps,
  VisionCameraErrorResult,
  VisionCameraRecognitionUpdateEvent,
  VisionCameraSharpnessScoreEvent,
  VisionCameraStateEvent,
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

// iOS's Fabric typed emitter delivers absent optional strings as '""'; Android
// sends `undefined`. Normalize once here so JS consumers see identical shapes
// cross-platform.
const CAMERA_STATE_OPTIONAL_STRING_KEYS = [
  'errorCode',
  'errorMessage',
  'warningCode',
  'warningMessage',
  'activeLensId',
] as const;

function normalizeCameraStateEvent(event: VisionCameraStateEvent): VisionCameraStateEvent {
  const normalized: any = { ...event };
  for (const key of CAMERA_STATE_OPTIONAL_STRING_KEYS) {
    if (normalized[key] === '') {
      normalized[key] = undefined;
    }
  }
  return normalized;
}

// Module-level "warn once" set for dev-mode prop-collision warnings — this
// repo has no existing precedent for a one-shot dev warning helper, so this
// introduces the minimal one.
const warnedOnceKeys = new Set<string>();
function warnOnce(key: string, message: string) {
  if (__DEV__ && !warnedOnceKeys.has(key)) {
    warnedOnceKeys.add(key);
    console.warn(message);
  }
}

// Camera component
const Camera = forwardRef<VisionCameraRefProps, VisionCameraProps>(
  (
    {
      children,
      style,
      enableFlash = false,
      zoomLevel = 1.0,
      pinnedLensId,
      zoomRatio,
      torch,
      focusMode = 'continuous',
      scanMode = 'photo',
      autoCapture = false,
      showCodeBoundingBoxes = false,
      barcodeBoundingBoxBorderColor,
      barcodeBoundingBoxBorderWidth,
      barcodeBoundingBoxFillColor,
      scanArea,
      detectionConfig,
      frameSkip,
      cameraFacing = 'back',
      template = null,
      onCapture = () => { },
      onError = () => { },
      onRecognitionUpdate = () => { },
      onSharpnessScoreUpdate = () => { },
      onBarcodeDetected = () => { },
      onBoundingBoxesUpdate = () => { },
      onCameraStateChanged = () => { },
    },
    ref
  ) => {
    // Ref for the Vision Camera View
    const VisionCameraViewRef = useRef(null);

    // Stable refs for callback props — updated synchronously during render
    // so handlers always point at the latest props with no stale window
    const onCaptureRef = useRef(onCapture);
    const onErrorRef = useRef(onError);
    const onRecognitionUpdateRef = useRef(onRecognitionUpdate);
    const onSharpnessScoreUpdateRef = useRef(onSharpnessScoreUpdate);
    const onBarcodeDetectedRef = useRef(onBarcodeDetected);
    const onBoundingBoxesUpdateRef = useRef(onBoundingBoxesUpdate);
    const onCameraStateChangedRef = useRef(onCameraStateChanged);
    onCaptureRef.current = onCapture;
    onErrorRef.current = onError;
    onRecognitionUpdateRef.current = onRecognitionUpdate;
    onSharpnessScoreUpdateRef.current = onSharpnessScoreUpdate;
    onBarcodeDetectedRef.current = onBarcodeDetected;
    onBoundingBoxesUpdateRef.current = onBoundingBoxesUpdate;
    onCameraStateChangedRef.current = onCameraStateChanged;

    // Prop collision resolution (Camera Controls API, spec §8): the canonical
    // prop wins when both the deprecated and new prop are set. Distinguishing
    // "explicitly passed" from "equals its default" isn't reliably knowable
    // once destructured with defaults, so this is a best-effort heuristic
    // (warns only when the deprecated prop's value diverges from its own
    // default while the new prop is also present) — it will under-warn but
    // will never over-warn on a consumer who only ever uses the new props.
    const resolvedZoom = zoomRatio !== undefined ? zoomRatio : zoomLevel;
    if (zoomRatio !== undefined && zoomLevel !== 1.0) {
      warnOnce(
        'zoom-collision',
        '[VisionCamera] Both `zoomLevel` (deprecated) and `zoomRatio` are set — `zoomRatio` wins. Remove `zoomLevel`.'
      );
    }
    const resolvedTorch = torch !== undefined ? torch : enableFlash;
    if (torch !== undefined && enableFlash !== false) {
      warnOnce(
        'torch-collision',
        '[VisionCamera] Both `enableFlash` (deprecated) and `torch` are set — `torch` wins. Remove `enableFlash`.'
      );
    }

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

      // Tear down the camera + analyzer + overlay and rebuild from scratch.
      // Required after a successful capture (or on Android-specific lifecycle
      // recovery) because the native SDK calls stopScanning() inside its
      // onCaptureSuccess handler, which leaves isScanning=false. Subsequent
      // capture() calls bail out with CallStartCameraOrRescanBeforeCapture.
      // Auto-rescan was disabled in v3.0.x to fix overlay flicker, so consumers
      // must invoke this imperatively after each capture for repeated captures
      // to work.
      rescan: () => {
        if (VisionCameraViewRef.current) {
          Commands.rescan(VisionCameraViewRef.current);
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

      // Named setTorchEnabled (not setTorch) on the native command layer to
      // avoid an Android codegen collision with the `torch` prop's generated
      // setTorch(view, boolean) setter — see src/specs/VisionCameraViewNativeComponent.ts.
      setTorch: (enabled: boolean) => {
        if (VisionCameraViewRef.current) {
          Commands.setTorchEnabled(VisionCameraViewRef.current, enabled);
        }
      },

      setFocusPoint: (x: number, y: number) => {
        if (VisionCameraViewRef.current) {
          Commands.setFocusPoint(VisionCameraViewRef.current, x, y);
        }
      },

      pauseDetection: () => {
        if (VisionCameraViewRef.current) {
          Commands.pauseDetection(VisionCameraViewRef.current);
        }
      },

      resumeDetection: () => {
        if (VisionCameraViewRef.current) {
          Commands.resumeDetection(VisionCameraViewRef.current);
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

    const onCameraStateChangedHandler = useCallback(
      (event: any) =>
        onCameraStateChangedRef.current(
          normalizeCameraStateEvent(parseNativeEvent<VisionCameraStateEvent>(event))
        ),
      []
    )

    return (
      <>
        <VisionCameraView
          ref={VisionCameraViewRef}
          style={[styles.flex, style]}
          enableFlash={resolvedTorch}
          zoomLevel={resolvedZoom}
          zoomRatio={resolvedZoom}
          torch={resolvedTorch}
          pinnedLensId={pinnedLensId}
          focusMode={focusMode}
          scanMode={scanMode}
          autoCapture={autoCapture}
          showCodeBoundingBoxes={showCodeBoundingBoxes}
          barcodeBoundingBoxBorderColor={barcodeBoundingBoxBorderColor}
          barcodeBoundingBoxBorderWidth={barcodeBoundingBoxBorderWidth}
          barcodeBoundingBoxFillColor={barcodeBoundingBoxFillColor}
          scanArea={scanArea}
          detectionConfig={detectionConfig}
          frameSkip={frameSkip}
          cameraFacing={cameraFacing}
          template={template}
          onCapture={onCaptureHandler}
          onError={onErrorHandler}
          onRecognitionUpdate={onRecognitionUpdateHandler}
          onSharpnessScoreUpdate={onSharpnessScoreUpdateHandler}
          onBarcodeDetected={onBarcodeDetectedHandler}
          onBoundingBoxesUpdate={onBoundingBoxesUpdateHandler}
          onCameraStateChanged={onCameraStateChangedHandler}
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
