/**
 * Capture modes for the Vision SDK.
 * @type {'manual' | 'auto'}
 * @description Specifies the capture mode for the Vision SDK.
 * The 'manual' mode requires the user to trigger the capture action,
 * while 'auto' mode automatically triggers the capture when an object is detected.
 * @example 'manual'
 * @example 'auto'
 */
export type CaptureMode = 'manual' | 'auto';

/**
 * Modes for scanning or capturing in the Vision SDK.
 * @type {'barcode' | 'qrcode' | 'ocr' | 'photo' | 'barCodeOrQRCode'}
 * @description Defines the different scanning modes available in the Vision SDK.
 * - 'barcode': Scans for barcodes.
 * - 'qrcode': Scans for QR codes.
 * - 'ocr': Performs optical character recognition on an image.
 * - 'photo': Takes a standard photo without scanning for barcodes or QR codes.
 * - 'barCodeOrQRCode': Scans either barcode or QR code.
 * @example 'barcode'
 */
export type ScanMode =
  | 'barcode'
  | 'qrcode'
  | 'ocr'
  | 'photo'
  | 'barCodeOrQRCode'
  | 'priceTag'

/**
 * OCR modes supported by the Vision SDK.
 * @type {'cloud' | 'on-device' | 'on-device-with-translation' | 'bill-of-lading' | 'item_label' | 'document_classification'}
 * @description Specifies the modes for optical character recognition (OCR) in the Vision SDK.
 * - 'cloud': Performs OCR in the cloud.
 * - 'on-device': Performs OCR directly on the device.
 * - 'on-device-with-translation': Performs OCR on the device and translates the detected text.
 * - 'bill-of-lading': OCR mode specifically for reading bill of lading documents.
 * - 'item_label': OCR mode specifically for scanning item labels.
 * - 'document_classification': Classifies documents based on their content and performs OCR on the recognized documents.
 * @example 'cloud'
 */
export type OCRMode =
  | 'cloud'
  | 'on_device'

  | 'on-device'



export type OCRType =
  | 'shipping_label'
  | 'bill_of_lading'
  | 'item_label'
  | 'document_classification'
  | 'shipping-label'
  | 'bill-of-lading'
  | 'item-label'
  | 'document-classification'
  | 'on_device_with_translation'
  | 'on-device-with-translation'


export interface OCRConfig {
  type: OCRType;
  mode: OCRMode;
  size: ModuleSize;
}

/**
 * Environments for the Vision SDK operation.
 * @type {'dev' | 'qa' | 'staging' | 'prod' | 'sandbox'}
 * @description Defines the environment in which the Vision SDK is being used.
 * - 'dev': Development environment, typically used for testing new features.
 * - 'qa': Quality assurance environment, used for ensuring the application functions as expected.
 * - 'staging': Staging environment, a pre-production environment that mirrors the live environment.
 * - 'prod': Production environment, where the application is live and accessible by end users.
 * - 'sandbox': A safe, isolated environment for testing without affecting production data.
 * @example 'dev'
 */
export type Environment = 'dev' | 'qa' | 'staging' | 'prod' | 'sandbox';

/**
 * Module types for specific Vision SDK functionalities.
 * @type {'item_label' | 'shipping_label' | 'bill_of_lading' | 'document_classification'}
 * @description Specifies the module types used for specific Vision SDK functionalities.
 * Each module type corresponds to a different use case in the Vision SDK.
 * - 'item_label': Scans item labels for relevant information.
 * - 'shipping_label': Scans shipping labels for details such as address and tracking number.
 * - 'bill_of_lading': Handles scanning of bill of lading documents.
 * - 'document_classification': Classifies and processes different types of documents.
 * @example 'item_label'
 */
export type ModuleType =
  | 'item_label'
  | 'shipping_label'
  | 'bill_of_lading'
  | 'document_classification'
  //
  | 'shipping-label'
  | 'bill-of-lading'
  | 'item-label'
  | 'document-classification';

/**
 * Sizes for the modules used in the Vision SDK.
 * @type {'nano' | 'micro' | 'small' | 'medium' | 'large' | 'xlarge'}
 * @description Specifies the size categories for modules used in the Vision SDK. These sizes determine the amount of resources and processing power required.
 * - 'nano': The smallest size, suitable for very lightweight models.
 * - 'micro': Slightly larger than 'nano', still very light and fast.
 * - 'small': Suitable for small-scale models with minimal resource usage.
 * - 'medium': Balanced size for performance and functionality.
 * - 'large': Larger models that require more processing power.
 * - 'xlarge': The largest models, with extensive resources and heavy processing demands.
 * @example 'small'
 */
export type ModuleSize =
  | 'nano'
  | 'micro'
  | 'small'
  | 'medium'
  | 'large'
  | 'xlarge';

/**
 * Represents an event where an image is captured.
 * @interface
 */
export interface ImageCaptureEvent {
  /**
   * @type {string}
   * @description Base64-encoded image data or URI.
   * @example 'data:image/png;base64,iVBORw0KGgoAAAANSUhEU...'
   */
  image: string;

  /**
   * @type {BarcodeResult[]}
   * @description List of scanned barcodes detected in the image with detailed information.
   * @example [{ scannedCode: '1234567890', symbology: 'QR', gs1ExtractedInfo: {...}, boundingBox: {...} }]
   */
  barcodes: BarcodeResult[];

  /**
   * @type {string | undefined}
   * @description Optional native image URI if available (such as from device camera).
   * @example 'file:///path/to/image.jpg'
   */
  nativeImage?: string;

  /**
   * @type {number}
   * @description sharpness value for the captured image, could be used for blur detection.
   * @example 0.85
   */
  sharpnessScore?: number;
}

export interface SharpnessScoreEvent {
  sharpnessScore: number;
}

/**
 * Represents the result of an object detection event.
 * @interface
 */
export interface DetectionResult {
  /**
   * @type {boolean}
   * @description Indicates whether text was detected in the image.
   * @example true
   */
  text: boolean;

  /**
   * @type {boolean}
   * @description Indicates whether a barcode was detected.
   * @example true
   */
  barcode: boolean;

  /**
   * @type {boolean}
   * @description Indicates whether a QR code was detected.
   * @example false
   */
  qrcode: boolean;

  /**
   * @type {boolean}
   * @description Indicates whether a document was detected.
   * @example false
   */
  document: boolean;
}

/**
 * Represents the result of an OCR scan event.
 * @interface
 */
export interface OCRScanResult {
  /**
   * @type {any | string}
   * @description The scanned text data. Can be a string or a more complex object containing structured data.
   * @example 'Invoice #12345'
   */
  data: any | string;

  /**
   * @type {string | undefined}
   * @description Optional path of the image associated with the OCR result.
   * @example '/path/to/image.jpg'
   */
  imagePath?: string;
}

/**
 * Represents the result of a barcode scan event.
 * @interface
 */

export interface BarcodeScanResult {
  /**
   * @type {BarcodeResult[]}
   * @description An array of scanned barcode results, where each result contains detailed information.
   */
  codes: BarcodeResult[];
  /**
   * @type {string}
   * @description JSON string containing the codes array (used internally for Fabric architecture)
   * @internal
   */
  codesJson?: string;
}

export interface BarcodeResult {
  scannedCode: string; // The scanned barcode value
  symbology?: string; // Barcode symbology type (e.g., QR, EAN, CODE128)
  gs1ExtractedInfo?: Record<string, string>; // Additional extracted information as a key-value map
  boundingBox?: BoundingBox; // Bounding box coordinates of the detected barcode
}

/**
 * Represents the download progress of a model.
 * @interface
 */
export interface ModelDownloadProgress {
  /**
   * @type {number}
   * @description The progress percentage (0-100).
   * @example 75
   */
  progress: number;

  /**
   * @type {boolean}
   * @description Whether the model download was successful or not.
   * @example true
   */
  downloadStatus: boolean;
}

/**
 * Represents an error event in the Vision SDK.
 * @interface
 */
export interface ErrorResult {
  /**
   * @type {string}
   * @description A description of the error that occurred.
   * @example 'Failed to initialize the Vision SDK.'
   */
  message: string;
}

export interface BoundingBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface DetectedCodeBoundingBox {
  scannedCode: string;
  symbology: string;
  gs1ExtractedInfo?: Record<string, string>;
  boundingBox: BoundingBox;
}

export interface BoundingBoxesDetectedResult {
  barcodeBoundingBoxes: Array<DetectedCodeBoundingBox>;
  qrCodeBoundingBoxes: Array<DetectedCodeBoundingBox>;
  documentBoundingBox: BoundingBox;
  // Internal fields used by Fabric architecture (parsed and removed by wrapper)
  barcodeBoundingBoxesJson?: string;
  qrCodeBoundingBoxesJson?: string;
}

export interface PriceTagDetectionResult {
  price: String;
  sku: String;
  boundingBox: BoundingBox
}

/**
 * Represents a report error type data for Vision SDK.
 * @interface
 */
export interface ReportErrorType {
  /**
   * @type {string}
   * @description A custom error message for the report.
   * @example 'The module encountered an unexpected failure.'
   */
  reportText: string;

  /**
   * @type {ModuleType}
   * @description The type of error being reported, specifying the kind of label associated.
   * @example 'item_label'
   */
  type: ModuleType;

  /**
   * @type {ModuleSize}
   * @description The size category of the report, allowing flexibility in defining error types.
   * @example 'medium'
   */
  size: ModuleSize;

  /**
   * @type {string | undefined}
   * @description An optional image associated with the error report.
   * @example { uri: 'file:///path/to/error-image.jpg' }
   */
  image?: string;

  /**
   * @type {any | undefined}
   * @description Optional response data or message associated with the error report.
   * @example { responseCode: 500, message: 'Internal Server Error' }
   */
  response?: any;
  errorFlags?: ShippingLabelErrorFlags | ItemLabelErrorFlags | BillOfLadingErrorFlags | DocumentClassificationErrorFlags;
}


export interface ShippingLabelErrorFlags {
  trackingNo?: boolean;
  courierName?: boolean;
  weight?: boolean;
  dimensions?: boolean;
  receiverName?: boolean;
  receiverAddress?: boolean;
  senderName?: boolean;
  senderAddres?: boolean;
}

export interface ItemLabelErrorFlags {
  supplierName?: boolean;
  itemName?: boolean;
  itemSKU?: boolean;
  weight?: boolean;
  quantity?: boolean;
  dimensions?: boolean;
  productionDate?: boolean;
  supplierAddress?: boolean;
}

export interface BillOfLadingErrorFlags {
  referenceNo?: boolean;
  loadNumber?: boolean;
  purchaseOrderNumber?: boolean;
  invoiceNumber?: boolean;
  customerPurchaseOrderNumber?: boolean;
  orderNumber?: boolean;
  billOfLading?: boolean;
  masterBillOfLading?: boolean;
  lineBillOfLading?: boolean;
  houseBillOfLading?: boolean;
  shippingId?: boolean;
  shippingDate?: boolean;
  date?: boolean;
}

export interface DocumentClassificationErrorFlags {
  documentClass?: boolean;
}

// ============================================================================
// MODEL MANAGEMENT TYPES
// ============================================================================

/**
 * OCR Module specification for model management
 * @description Specifies which ML model to use for OCR operations
 */
export type OCRModule = {
  /**
   * Type of OCR module
   */
  type: 'shipping_label' | 'bill_of_lading' | 'item_label' | 'document_classification';

  /**
   * Model size - larger models are more accurate but slower
   */
  size: 'nano' | 'micro' | 'small' | 'medium' | 'large' | 'xlarge';

  /**
   * Optional configuration for specific modules
   */
  options?: {
    /**
     * Enable additional key-value extraction (bill_of_lading and item_label only)
     */
    enableAdditionalAttributes?: boolean;
  };
};

/**
 * Execution provider for ONNX Runtime (Android only)
 * @description Specifies which hardware acceleration to use
 * @type {'CPU' | 'NNAPI' | 'XNNPACK'}
 */
export type ExecutionProvider = 'CPU' | 'NNAPI' | 'XNNPACK';

/**
 * Configuration for ModelManager initialization
 * @interface
 */
export interface ModelManagerConfig {
  /**
   * Maximum number of simultaneous model downloads
   * @default 2
   */
  maxConcurrentDownloads?: number;

  /**
   * Enable debug logging for model operations
   * @default true
   */
  enableLogging?: boolean;
}

/**
 * Download progress information
 * @interface
 */
export interface DownloadProgress {
  /**
   * The module being downloaded
   */
  module: OCRModule;

  /**
   * Download progress from 0.0 to 1.0
   */
  progress: number;
}

/**
 * Information about a downloaded or loaded model
 * @interface
 */
export interface ModelInfo {
  /**
   * The OCR module
   */
  module: OCRModule;

  /**
   * Model version string (date-based, e.g., "2025-05-05")
   */
  version: string;

  /**
   * Unique model version identifier from backend
   */
  versionId: string | null;

  /**
   * Model release date
   */
  dateString: string;

  /**
   * Whether currently loaded in memory
   */
  isLoaded: boolean;
}

/**
 * Result of checking for model updates
 * @interface
 */
export interface ModelUpdateInfo {
  /**
   * The OCR module checked
   */
  module: OCRModule;

  /**
   * Currently downloaded version (null if not downloaded)
   */
  currentVersion: string | null;

  /**
   * Latest version available on server
   */
  latestVersion: string;

  /**
   * Whether a newer version is available
   */
  updateAvailable: boolean;

  /**
   * Human-readable status message
   */
  message: string;
}

/**
 * Bounding box coordinates
 * @interface
 */
export interface BoundingBox {
  /**
   * X coordinate (left edge)
   */
  x: number;

  /**
   * Y coordinate (top edge)
   */
  y: number;

  /**
   * Width of bounding box
   */
  width: number;

  /**
   * Height of bounding box
   */
  height: number;
}

/**
 * Barcode information for OCR predictions
 * @interface
 */
export interface DetectedBarcode {
  /**
   * Barcode value/string
   */
  scannedCode: string;

  /**
   * Barcode symbology/format (e.g., 'CODE_128', 'QR_CODE', 'EAN_13')
   */
  symbology: string;

  /**
   * GS1 extracted information (optional)
   * Key-value pairs of GS1 application identifiers and their values
   * @example { "01": "12345678901234", "17": "250101", "10": "LOT123" }
   */
  gs1ExtractedInfo?: Record<string, string>;

  /**
   * Bounding box coordinates (optional)
   */
  boundingBox?: BoundingBox;
}

/**
 * Template code information
 * Represents a single barcode in a template
 * @interface
 */
export interface TemplateCode {
  /**
   * Barcode value/string
   */
  codeString: string;

  /**
   * Barcode symbology (e.g., 'code128', 'qrcode', 'ean13')
   */
  codeSymbology: string;

  /**
   * Bounding box coordinates (optional)
   * Uses iOS CGRect format for cross-platform compatibility
   */
  boundingBox?: BoundingBox;
}

/**
 * Template data structure
 * Templates define barcode matching patterns for scanning
 * @interface
 */
export interface TemplateData {
  /**
   * Unique template identifier
   */
  id: string;

  /**
   * Array of template codes to match
   */
  templateCodes: TemplateCode[];
}

/**
 * Model management exception types
 * @interface
 */
export interface ModelException {
  /**
   * Type of exception
   */
  type: 'SdkNotInitialized' | 'RootedDevice' | 'NoNetwork' | 'Network' | 'Storage' | 'ModelNotFound' | 'Load';

  /**
   * Error message
   */
  message: string;

  /**
   * The module that caused the error (if applicable)
   */
  module?: OCRModule;

  /**
   * Underlying cause of the error
   */
  cause?: any;

  /**
   * Required storage space in bytes (StorageException only)
   */
  requiredBytes?: number;

  /**
   * Available storage space in bytes (StorageException only)
   */
  availableBytes?: number;

  /**
   * Reason for the error (ModelNotFoundException only)
   */
  reason?: string;
}

/**
 * Lifecycle event callbacks for model operations
 * @interface
 */
export interface ModelLifecycleListener {
  /**
   * Called when a model download starts
   */
  onDownloadStarted?: (module: OCRModule) => void;

  /**
   * Called when a model download completes successfully
   */
  onDownloadCompleted?: (module: OCRModule) => void;

  /**
   * Called when a model download fails
   */
  onDownloadFailed?: (module: OCRModule, error: ModelException) => void;

  /**
   * Called when a model download is cancelled
   */
  onDownloadCancelled?: (module: OCRModule) => void;

  /**
   * Called when a model is loaded into memory
   */
  onModelLoaded?: (module: OCRModule) => void;

  /**
   * Called when a model is unloaded from memory
   */
  onModelUnloaded?: (module: OCRModule) => void;

  /**
   * Called when a model is deleted from disk
   */
  onModelDeleted?: (module: OCRModule) => void;
}
