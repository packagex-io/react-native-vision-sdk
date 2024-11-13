import { ReactNode } from 'react';
import { StyleProp, ViewStyle } from 'react-native';

/**
 * Props for the Vision SDK view component.
 */
export interface VisionSdkViewProps {
  /** Optional children elements to render inside the SDK view */
  children?: ReactNode;

  /** Optional style to apply to the container */
  style?: StyleProp<ViewStyle>;

  /** Mode of operation (e.g., barcode scanning, OCR, etc.) */
  mode?: string;

  /** Reference to the component */
  ref?: React.Ref<any>;
  refProp?: React.Ref<any>;

  /** API key for authentication with the Vision SDK */
  apiKey?: string;

  /** Token for SDK authentication */
  token?: string;

  /** Location identifier for specific operations */
  locationId?: string;

  /** Optional additional configuration options */
  options?: Record<string, any>;

  /** The environment for SDK operation (e.g., production or sandbox) */
  environment?: string;

  /** Whether the flash should be enabled or not */
  flash?: boolean;

  /** Zoom level for scanning or capturing */
  zoomLevel?: number;

  /** Capture mode (e.g., automatic or manual) */
  captureMode?: string;

  /** Show document boundaries on captured images */
  showDocumentBoundaries?: boolean;

  /** OCR mode (e.g., cloud or on-device processing) */
  ocrMode: string;

  /** Whether the scan frame should be shown */
  showScanFrame?: boolean;

  /** Capture images with the scan frame */
  captureWithScanFrame?: boolean;

  /**
   * Event handler for model download progress updates.
   * @param event Model download progress information
   */
  onModelDownloadProgress: (
    event: ModelDownloadProgress | { nativeEvent: ModelDownloadProgress }
  ) => void;

  /**
   * Event handler for barcode scan events.
   * @param event Barcode scan result
   */
  onBarcodeScan: (
    event: BarcodeScanResult | { nativeEvent: BarcodeScanResult }
  ) => void;

  /**
   * Event handler for image capture events.
   * @param event Captured image details
   */
  onImageCaptured: (
    event: ImageCaptureEvent | { nativeEvent: ImageCaptureEvent }
  ) => void;

  /**
   * Event handler for OCR scan results.
   * @param event OCR scan result
   */
  onOCRScan: (event: OCRScanResult | { nativeEvent: OCRScanResult }) => void;

  /**
   * Event handler for detection results.
   * @param event Detection results (e.g., text, barcode, etc.)
   */
  onDetected: (
    event: DetectionResult | { nativeEvent: DetectionResult }
  ) => void;

  /**
   * Event handler for error events.
   * @param event Error details
   */
  onError: (event: ErrorResult | { nativeEvent: ErrorResult }) => void;
}

/**
 * Represents an event where an image is captured.
 */
export interface ImageCaptureEvent {
  /** Base64-encoded image data or URI */
  image: string;

  /** List of scanned barcodes */
  barcodes: string[];

  /** Optional native image URI (if available) */
  nativeImage?: string;
}

/**
 * Represents the result of an object detection event.
 */
export interface DetectionResult {
  /** Whether text was detected in the image */
  text: boolean;

  /** Whether a barcode was detected */
  barcode: boolean;

  /** Whether a QR code was detected */
  qrcode: boolean;

  /** Whether a document was detected */
  document: boolean;
}

/**
 * Represents the result of an OCR scan event.
 */
export interface OCRScanResult {
  /** The scanned text data (could be a string or more complex object) */
  data: any | string;
}

/**
 * Represents the result of a barcode scan event.
 */
export interface BarcodeScanResult {
  /** The scanned barcode value */
  code: string;
}

/**
 * Represents the download progress of a model.
 */
export interface ModelDownloadProgress {
  /** The progress percentage (0-100) */
  progress: number;

  /** Whether the model download was successful or not */
  downloadStatus: boolean;
}

/**
 * Represents an error event in the Vision SDK.
 */
export interface ErrorResult {
  /** Error message description */
  message: string;
}

/**
 * Exposes methods to control the Vision SDK from the parent component.
 */
export interface VisionSdkRefProps {
  /** Stops the running process of the Vision SDK */
  stopRunningHandler: () => void;

  /** Captures an image using the Vision SDK */
  cameraCaptureHandler: () => void;

  /** Restarts the scanning process */
  restartScanningHandler: () => void;

  /** Starts the running process of the Vision SDK */
  startRunningHandler: () => void;

  /**
   * Sets metadata for processing in the Vision SDK.
   * @param value The metadata to be set (can be an object)
   */
  setMetadata: (value: any) => void;

  /**
   * Sets recipient details in the Vision SDK.
   * @param value The recipient data (could be an object with recipient details)
   */
  setRecipient: (value: any) => void;

  /**
   * Sets sender details in the Vision SDK.
   * @param value The sender data (could be an object with sender details)
   */
  setSender: (value: any) => void;

  /**
   * Configures the on-device model for processing in the Vision SDK.
   * @param val Configuration for the on-device model
   */
  configureOnDeviceModel: (val: any) => void;

  /**
   * Gets a prediction based on the provided image and barcode.
   * @param image The image to be analyzed
   * @param barcode An array of barcode values to analyze alongside the image
   */
  getPrediction: (image: any, barcode: string[]) => void;

  /**
   * Gets a prediction with cloud transformations.
   * @param image The image to be analyzed
   * @param barcode An array of barcode values to analyze alongside the image
   */
  getPredictionWithCloudTransformations: (
    image: any,
    barcode: string[]
  ) => void;

  /**
   * Gets a prediction for a shipping label using cloud processing.
   * @param image The image of the shipping label
   * @param barcode Array of barcode strings associated with the shipping label
   */
  getPredictionShippingLabelCloud: (image: any, barcode: string[]) => void;

  /**
   * Gets a prediction for Bill of Lading using cloud processing.
   * @param image The image of the Bill of Lading
   * @param barcode Array of barcode strings
   * @param withImageResizing Whether to resize the image (default: true)
   */
  getPredictionBillOfLadingCloud: (
    image: any,
    barcode: string[],
    withImageResizing: boolean
  ) => void;

  /**
   * Sets the focus settings for the Vision SDK.
   * @param val Focus parameters to be set (e.g., focus distance, autofocus)
   */
  setFocusSettings: (val: any) => void;

  /**
   * Sets the object detection settings for the Vision SDK.
   * @param val Object detection settings (e.g., confidence threshold, detection area)
   */
  setObjectDetectionSettings: (val: any) => void;

  /**
   * Sets the camera settings for the Vision SDK.
   * @param val Camera settings (e.g., resolution, FPS)
   */
  setCameraSettings: (val: any) => void;
}

/**
 * Props for the Vision SDK component.
 */
export interface VisionSdkProps {
  /** Optional children elements to render inside the SDK component */
  children?: ReactNode;

  /** Optional reference to the component */
  refProp?: React.Ref<any>;

  /** API key for authentication with the SDK */
  apiKey?: string;

  /** Trigger to re-render the component */
  reRender?: string;

  /** Capture mode (e.g., 'manual' or 'auto') */
  captureMode?: 'manual' | 'auto';

  /** Mode of scanning (e.g., 'barcode', 'qrcode', 'ocr', etc.) */
  mode?: 'barcode' | 'qrcode' | 'ocr' | 'photo' | 'barCodeOrQRCode';

  /** Token for SDK authentication */
  token?: string;

  /** Location ID for specific operations */
  locationId?: string;

  /** Optional additional configuration options */
  options?: Record<string, any>;

  /** Environment for SDK operation (e.g., 'dev' or 'qa' or 'staging' or 'prod' or 'sandbox') */
  environment?: 'dev' | 'qa' | 'staging' | 'prod' | 'sandbox';

  /** Whether flash should be enabled */
  flash?: boolean;

  /** Zoom level for scanning or capturing */
  zoomLevel?: number;

  /** OCR mode (e.g., cloud, on-device) */
  ocrMode?:
    | 'cloud'
    | 'on-device'
    | 'on-device-with-translation'
    | 'bill-of-lading';

  /**
   * Event handler for model download progress.
   * @param event Model download progress details
   */
  onModelDownloadProgress?: (event: ModelDownloadProgress) => void;

  /**
   * Event handler for barcode scan.
   * @param event Barcode scan result
   */
  onBarcodeScan?: (event: BarcodeScanResult) => void;

  /**
   * Event handler for image capture.
   * @param event Captured image details
   */
  onImageCaptured?: (event: ImageCaptureEvent) => void;

  /**
   * Event handler for OCR scan result.
   * @param event OCR scan result
   */
  onOCRScan?: (event: OCRScanResult) => void;

  /**
   * Event handler for detected objects.
   * @param event Detection results
   */
  onDetected?: (event: DetectionResult) => void;

  /**
   * Event handler for error events.
   * @param event Error details
   */
  onError?: (event: ErrorResult) => void;
}
