import type { HostComponent, ViewProps } from 'react-native';
import type {
  DirectEventHandler,
  Int32,
  Float,
} from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';

// Event type definitions
type BarcodeScanEvent = Readonly<{
  // Full codes array passed as JSON string due to codegen array limitations
  codesJson?: string;
}>;

type ModelDownloadProgressEvent = Readonly<{
  progress: Float;
  downloadStatus?: boolean;
  isReady?: boolean;
}>;

type ImageCapturedEvent = Readonly<{
  uri?: string;
  width?: Int32;
  height?: Int32;
}>;

type SharpnessScoreEvent = Readonly<{
  sharpnessScore: Float;
}>;

type OCRScanEvent = Readonly<{
  // Data will be passed as a JSON string due to codegen limitations
  dataJson?: string;
}>;

type ErrorEvent = Readonly<{
  message?: string;
}>;

type BoundingBoxesDetectedEvent = Readonly<{
  // Arrays passed as JSON strings due to codegen limitations with ReadonlyArray
  barcodeBoundingBoxesJson?: string;
  qrCodeBoundingBoxesJson?: string;
  documentBoundingBox?: Readonly<{
    x: Float;
    y: Float;
    width: Float;
    height: Float;
  }>;
}>;

type PriceTagDetectedEvent = Readonly<{
  price?: string;
  sku?: string;
  boundingBox?: Readonly<{
    x: Float;
    y: Float;
    width: Float;
    height: Float;
  }>;
}>;

type DetectedEvent = Readonly<{
  type?: string;
  // Data will be passed as a JSON string due to codegen limitations
  dataJson?: string;
}>;

type TemplateEvent = Readonly<{
  success?: boolean;
  // Data will be passed as a JSON string due to codegen limitations
  dataJson?: string;
}>;

// Component props interface
export interface NativeProps extends ViewProps {
  // String properties
  mode?: string;
  captureMode?: string;
  apiKey?: string;
  token?: string;
  locationId?: string;
  environment?: string;
  ocrMode?: string;
  ocrType?: string;
  modelExecutionProviderAndroid?: string;

  // Boolean properties
  shouldResizeImage?: boolean;
  flash?: boolean;
  isEnableAutoOcrResponseWithImage?: boolean;
  isMultipleScanEnabled?: boolean;

  // Number properties
  zoomLevel?: Float;

  // Object property - passed as JSON string due to codegen limitations
  optionsJson?: string;

  // Event handlers
  onBarcodeScan?: DirectEventHandler<BarcodeScanEvent>;
  onModelDownloadProgress?: DirectEventHandler<ModelDownloadProgressEvent>;
  onImageCaptured?: DirectEventHandler<ImageCapturedEvent>;
  onSharpnessScore?: DirectEventHandler<SharpnessScoreEvent>;
  onOCRScan?: DirectEventHandler<OCRScanEvent>;
  onError?: DirectEventHandler<ErrorEvent>;
  onBoundingBoxesDetected?: DirectEventHandler<BoundingBoxesDetectedEvent>;
  onPriceTagDetected?: DirectEventHandler<PriceTagDetectedEvent>;
  onDetected?: DirectEventHandler<DetectedEvent>;
  onCreateTemplate?: DirectEventHandler<TemplateEvent>;
}

// Native commands interface
interface NativeCommands {
  // Basic camera commands (0-2, 7)
  captureImage: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  stopRunning: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  startRunning: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
  restartScanning: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;

  // Additional commands (3-6, 8-10)
  setMetaData: (viewRef: React.ElementRef<HostComponent<NativeProps>>, metaDataJson: string) => void;
  setRecipient: (viewRef: React.ElementRef<HostComponent<NativeProps>>, recipientJson: string) => void;
  setSender: (viewRef: React.ElementRef<HostComponent<NativeProps>>, senderJson: string) => void;
  configureOnDeviceModel: (
    viewRef: React.ElementRef<HostComponent<NativeProps>>,
    onDeviceConfigsJson: string,
    token: string,
    apiKey: string
  ) => void;
  setFocusSettings: (viewRef: React.ElementRef<HostComponent<NativeProps>>, focusSettingsJson: string) => void;
  setObjectDetectionSettings: (viewRef: React.ElementRef<HostComponent<NativeProps>>, objectDetectionSettingsJson: string) => void;
  setCameraSettings: (viewRef: React.ElementRef<HostComponent<NativeProps>>, cameraSettingsJson: string) => void;

  // Cloud prediction commands (11-17)
  getPrediction: (viewRef: React.ElementRef<HostComponent<NativeProps>>, image: string, barcodeJson: string) => void;
  getPredictionWithCloudTransformations: (
    viewRef: React.ElementRef<HostComponent<NativeProps>>,
    image: string,
    barcodeJson: string,
    token: string,
    apiKey: string,
    locationId: string,
    optionsJson: string,
    metadataJson: string,
    recipientJson: string,
    senderJson: string,
    shouldResizeImage: boolean
  ) => void;
  getPredictionShippingLabelCloud: (
    viewRef: React.ElementRef<HostComponent<NativeProps>>,
    image: string,
    barcodeJson: string,
    token: string,
    apiKey: string,
    locationId: string,
    optionsJson: string,
    metadataJson: string,
    recipientJson: string,
    senderJson: string,
    shouldResizeImage: boolean
  ) => void;
  getPredictionBillOfLadingCloud: (
    viewRef: React.ElementRef<HostComponent<NativeProps>>,
    image: string,
    barcodeJson: string,
    token: string,
    apiKey: string,
    locationId: string,
    optionsJson: string,
    shouldResizeImage: boolean
  ) => void;
  getPredictionItemLabelCloud: (
    viewRef: React.ElementRef<HostComponent<NativeProps>>,
    image: string,
    token: string,
    apiKey: string,
    shouldResizeImage: boolean
  ) => void;
  getPredictionDocumentClassificationCloud: (
    viewRef: React.ElementRef<HostComponent<NativeProps>>,
    image: string,
    token: string,
    apiKey: string,
    shouldResizeImage: boolean
  ) => void;
  reportError: (viewRef: React.ElementRef<HostComponent<NativeProps>>, dataJson: string, token: string, apiKey: string) => void;

  // Template command (18)
  createTemplate: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: [
    'captureImage',
    'stopRunning',
    'startRunning',
    'restartScanning',
    'setMetaData',
    'setRecipient',
    'setSender',
    'configureOnDeviceModel',
    'setFocusSettings',
    'setObjectDetectionSettings',
    'setCameraSettings',
    'getPrediction',
    'getPredictionWithCloudTransformations',
    'getPredictionShippingLabelCloud',
    'getPredictionBillOfLadingCloud',
    'getPredictionItemLabelCloud',
    'getPredictionDocumentClassificationCloud',
    'reportError',
    'createTemplate',
  ],
});

export default codegenNativeComponent<NativeProps>(
  'VisionSdkView'
) as HostComponent<NativeProps>;
