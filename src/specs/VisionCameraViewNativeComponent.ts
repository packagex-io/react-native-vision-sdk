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

// Component props interface
export interface NativeProps extends ViewProps {
  // Boolean properties
  enableFlash?: boolean;
  autoCapture?: boolean;

  // Number properties
  zoomLevel?: WithDefault<Double, 1.0>;
  frameSkip?: WithDefault<Int32, 10>;

  // String properties
  scanMode?: WithDefault<string, 'photo'>; // 'photo' | 'barcode' | 'qrcode' | 'barcodeorqrcode' | 'ocr' | 'barcodesinglecapture'
  cameraFacing?: WithDefault<string, 'back'>; // 'back' | 'front'

  // Object properties - passed as JSON strings due to codegen limitations
  scanAreaJson?: string;
  detectionConfigJson?: string;

  // Event handlers
  onCapture?: DirectEventHandler<CaptureEvent>;
  onError?: DirectEventHandler<ErrorEvent>;
  onRecognitionUpdate?: DirectEventHandler<RecognitionUpdateEvent>;
  onSharpnessScoreUpdate?: DirectEventHandler<SharpnessScoreUpdateEvent>;
  onBarcodeDetected?: DirectEventHandler<BarcodeDetectedEvent>;
  onBoundingBoxesUpdate?: DirectEventHandler<BoundingBoxesUpdateEvent>;
}

// Native commands interface
interface NativeCommands {
  capture: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  stop: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  start: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  toggleFlash: (viewRef: React.ElementRef<HostComponent<NativeProps>>, enabled: boolean) => void;
  setZoom: (viewRef: React.ElementRef<HostComponent<NativeProps>>, level: Float) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['capture', 'stop', 'start', 'toggleFlash', 'setZoom'],
});

export default codegenNativeComponent<NativeProps>(
  'VisionCameraView'
) as HostComponent<NativeProps>;
