import type { HostComponent, ViewProps } from 'react-native';
import type {
  DirectEventHandler,
  Int32,
  Float,
  Double,
  WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';

// Event type definitions
type CaptureEvent = Readonly<{
  image?: string;
  nativeImage?: string;
  sharpnessScore?: Float;
  // barcodes array passed as JSON string due to codegen limitations
  barcodesJson?: string;
}>;

type ErrorEvent = Readonly<{
  message?: string;
}>;

type RecognitionUpdateEvent = Readonly<{
  text?: boolean;
  barcode?: boolean;
  qrcode?: boolean;
  document?: boolean;
}>;

type SharpnessScoreUpdateEvent = Readonly<{
  sharpnessScore?: Float;
}>;

type BarcodeDetectedEvent = Readonly<{
  // codes array passed as JSON string due to codegen limitations
  codesJson?: string;
}>;

// Teardown-complete signal (consumer-requested, iOS-primary): fires once a stop()
// has genuinely finished tearing down the camera session — see VisionCameraTypes.ts
// `VisionCameraStoppedEvent` for the cross-platform semantics/timing note. No payload
// fields are needed; the event's occurrence IS the signal.
type CameraStoppedEvent = Readonly<{}>;

type BoundingBoxesUpdateEvent = Readonly<{
  // Arrays passed as JSON strings due to codegen limitations
  barcodeBoundingBoxesJson?: string;
  qrCodeBoundingBoxesJson?: string;
  documentBoundingBox?: Readonly<{
    x: Float;
    y: Float;
    width: Float;
    height: Float;
  }>;
}>;

// Camera Controls API (Phase 3) — throttled full-state event (§8). isPreviewActive is
// folded into this single event per the ratified "one throttled RN event" decision;
// there is no separate onCameraReady event.
type CameraStateChangedEvent = Readonly<{
  status?: string; // CameraStatus: 'idle' | 'starting' | 'running' | 'interrupted' | 'error'
  errorCode?: string; // CameraErrorCode
  errorMessage?: string;
  warningCode?: string; // CameraErrorCode
  warningMessage?: string;
  facing?: string; // LensFacing: 'back' | 'front'
  activeLensId?: string;
  zoomRatio?: Float;
  minZoomRatio?: Float;
  maxZoomRatio?: Float;
  torchEnabled?: boolean;
  focusMode?: string; // FocusMode: 'continuous' | 'single' | 'locked'
  isPreviewActive?: boolean;
}>;

// Component props interface
export interface NativeProps extends ViewProps {
  // Boolean properties
  enableFlash?: boolean;
  autoCapture?: boolean;
  showCodeBoundingBoxes?: boolean;

  // Native overlay style (only used when showCodeBoundingBoxes=true)
  barcodeBoundingBoxBorderColor?: string;
  barcodeBoundingBoxBorderWidth?: WithDefault<Double, 3.0>;
  barcodeBoundingBoxFillColor?: string;

  // Number properties
  zoomLevel?: WithDefault<Double, 1.0>;
  frameSkip?: WithDefault<Int32, 10>;

  // String properties
  scanMode?: WithDefault<string, 'photo'>; // 'photo' | 'barcode' | 'qrcode' | 'barcodeorqrcode' | 'ocr' | 'barcodesinglecapture'
  cameraFacing?: WithDefault<string, 'back'>; // 'back' | 'front'

  // Camera Controls API (Phase 3) — canonical props; zoomLevel/enableFlash above stay as
  // deprecated aliases feeding the same native path (see VisionCamera.tsx).
  pinnedLensId?: string; // undefined = Auto
  zoomRatio?: WithDefault<Double, 1.0>;
  torch?: WithDefault<boolean, false>;
  focusMode?: WithDefault<string, 'continuous'>; // FocusMode

  // Object properties - passed as JSON strings due to codegen limitations
  scanAreaJson?: string;
  detectionConfigJson?: string;
  templateJson?: string;

  // Event handlers
  onCapture?: DirectEventHandler<CaptureEvent>;
  onError?: DirectEventHandler<ErrorEvent>;
  onRecognitionUpdate?: DirectEventHandler<RecognitionUpdateEvent>;
  onSharpnessScoreUpdate?: DirectEventHandler<SharpnessScoreUpdateEvent>;
  onBarcodeDetected?: DirectEventHandler<BarcodeDetectedEvent>;
  onBoundingBoxesUpdate?: DirectEventHandler<BoundingBoxesUpdateEvent>;
  onCameraStateChanged?: DirectEventHandler<CameraStateChangedEvent>;
  onCameraStopped?: DirectEventHandler<CameraStoppedEvent>;
}

// Native commands interface
interface NativeCommands {
  capture: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  stop: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  start: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  rescan: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  toggleFlash: (viewRef: React.ElementRef<HostComponent<NativeProps>>, enabled: boolean) => void;
  setZoom: (viewRef: React.ElementRef<HostComponent<NativeProps>>, level: Float) => void;
  // Duration-based ramp — matches both natives' signature (iOS converts to Apple's
  // rate-based AVCaptureDevice.ramp internally; Android drives a ~60/sec ticker since
  // CameraX has no ramp primitive). See VisionCameraTypes.ts `rampZoomRatio` doc.
  rampZoomRatio: (viewRef: React.ElementRef<HostComponent<NativeProps>>, ratio: Float, durationMs: Int32) => void;
  setFocusSettings: (viewRef: React.ElementRef<HostComponent<NativeProps>>, settingsJson: string) => void;
  pauseDetection: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  resumeDetection: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  // NOTE: named setTorchEnabled (not setTorch) to avoid an Android codegen collision — the
  // `torch` prop already generates `setTorch(view, boolean)` on VisionCameraViewManagerInterface;
  // a same-named command with the same erased signature (view, boolean) fails javac
  // ("method already defined"). Verified via `javac` repro during Task 9 codegen regen.
  setTorchEnabled: (viewRef: React.ElementRef<HostComponent<NativeProps>>, enabled: boolean) => void;
  setFocusPoint: (viewRef: React.ElementRef<HostComponent<NativeProps>>, x: Float, y: Float) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['capture', 'stop', 'start', 'rescan', 'toggleFlash', 'setZoom', 'rampZoomRatio', 'setFocusSettings', 'pauseDetection', 'resumeDetection', 'setTorchEnabled', 'setFocusPoint'],
});

export default codegenNativeComponent<NativeProps>(
  'VisionCameraView'
) as HostComponent<NativeProps>;
