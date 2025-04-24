import { ReactNode } from 'react';
import { StyleProp, ViewStyle } from 'react-native';

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
  | 'barCodeOrQRCode';

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
 * Props for the Vision SDK view component.
 */
export interface VisionSdkViewProps {
  /**
   * @type {ReactNode}
   * Optional children elements to render inside the SDK view.
   */
  children?: ReactNode;

  /**
   * @type {StyleProp<ViewStyle>}
   * Optional style to apply to the container.
   */
  style?: StyleProp<ViewStyle>;

  /**
   * @type {string}
   * Optional mode of operation (e.g., barcode scanning, OCR, etc.).
   */
  mode?: string;

  /**
   * @type {React.Ref<any>}
   * Optional reference to the component.
   */
  ref?: React.Ref<any>;

  /**
   * @type {string}
   * Optional API key for authentication with the Vision SDK.
   */
  apiKey?: string;

  /**
   * @type {string}
   * Optional token for SDK authentication.
   */
  token?: string;

  /**
   * @type {string}
   * Optional location identifier for specific operations.
   */
  locationId?: string;

  /**
   * @type {Record<string, any>}
   * Optional additional configuration options.
   */
  options?: Record<string, any>;

  /**
   * @type {string}
   * Optional environment for SDK operation (e.g., production or sandbox).
   */
  environment?: string;

  /**
   * @type {boolean}
   * Optional flag to enable or disable the flash.
   */
  flash?: boolean;

  /**
   * @type {boolean}
   * Optional flag to enable or disable multiple scans.
   */
  isMultipleScanEnabled?: boolean;

  /**
   * @type {number}
   * Optional zoom level for scanning or capturing.
   */
  zoomLevel?: number;

  /**
   * @type {string}
   * Optional capture mode (e.g., automatic or manual).
   */
  captureMode?: string;

  /**
   * @type {boolean}
   * Optional flag to show document boundaries on captured images.
   */
  showDocumentBoundaries?: boolean;

  /**
   * @type {string}
   * Optional OCR mode (e.g., cloud or on-device processing).
   */
  ocrMode?: string;

  /**
 * @type {string}
 * Optional OCR type (e.g., shipping_label or item_label).
 */
  ocrType?: string;


  /**
 * @type {boolean}
 * Optional flag to enable image resizing.
 */
  shouldResizeImage?: boolean;




  /**
   * @type {boolean}
   * Optional flag to enable auto OCR response with image.
   */
  isEnableAutoOcrResponseWithImage?: boolean;

  /**
   * @type {boolean}
   * Optional flag to show the scan frame.
   */
  showScanFrame?: boolean;

  /**
   * @type {boolean}
   * Optional flag to capture images with the scan frame.
   */
  captureWithScanFrame?: boolean;

  /**
   * @type {(event: ModelDownloadProgress | { nativeEvent: ModelDownloadProgress }) => void}
   * @param {ModelDownloadProgress | { nativeEvent: ModelDownloadProgress }} event Model download progress information.
   * Optional event handler for model download progress updates.
   */
  onModelDownloadProgress?: (
    event: ModelDownloadProgress | { nativeEvent: ModelDownloadProgress }
  ) => void;

  /**
   * @type {(event: BarcodeScanResult | { nativeEvent: BarcodeScanResult }) => void}
   * @param {BarcodeScanResult | { nativeEvent: BarcodeScanResult }} event Barcode scan result.
   * Optional event handler for barcode scan events.
   */
  onBarcodeScan?: (
    event: BarcodeScanResult | { nativeEvent: BarcodeScanResult }
  ) => void;

  /**
   * @type {(event: ImageCaptureEvent | { nativeEvent: ImageCaptureEvent }) => void}
   * @param {ImageCaptureEvent | { nativeEvent: ImageCaptureEvent }} event Captured image details.
   * Optional event handler for image capture events.
   */
  onImageCaptured?: (
    event: ImageCaptureEvent | { nativeEvent: ImageCaptureEvent }
  ) => void;

  /**
   * @type {(event: OCRScanResult | { nativeEvent: OCRScanResult }) => void}
   * @param {OCRScanResult | { nativeEvent: OCRScanResult }} event OCR scan result.
   * Optional event handler for OCR scan results.
   */
  onOCRScan?: (event: OCRScanResult | { nativeEvent: OCRScanResult }) => void;

  /**
   * @type {(event: string | { nativeEvent: string }) => void}
   * @param {string | { nativeEvent: string }} event - Event triggered when a template is created.
   * @description Optional event handler that triggers when a template is successfully created.
   * This callback receives the created template's ID or a native event.
   * @example
   * onCreateTemplate: (event) => console.log('Template Created:', event)
   */
  onCreateTemplate?: (event: string | { nativeEvent: string }) => void;

  /**
   * @type {(event: string[] | { nativeEvent: string }) => void}
   * @param {string[] | { nativeEvent: string }} event - Event triggered when templates are retrieved.
   * @description Optional event handler that triggers when templates are successfully retrieved.
   * This callback receives an array of template IDs or a native event.
   * @example
   * onGetTemplates: (event) => console.log('Templates Retrieved:', event)
   */
  onGetTemplates?: (event: string[] | { nativeEvent: string }) => void;

  /**
   * @type {(event: string | { nativeEvent: string }) => void}
   * @param {string | { nativeEvent: string }} event - Event triggered when a template is deleted by its ID.
   * @description Optional event handler that triggers when a template is successfully deleted using its ID.
   * This callback receives the ID of the deleted template or a native event.
   * @example
   * onDeleteTemplateById: (event) => console.log('Template Deleted:', event)
   */
  onDeleteTemplateById?: (event: string | { nativeEvent: string }) => void;

  /**
   * @type {(event: string | { nativeEvent: string }) => void}
   * @param {string | { nativeEvent: string }} event - Event triggered when multiple templates are deleted.
   * @description Optional event handler that triggers when multiple templates are successfully deleted.
   * This callback receives the IDs of the deleted templates or a native event.
   * @example
   * onDeleteTemplates: (event) => console.log('Templates Deleted:', event)
   */
  onDeleteTemplates?: (event: string | { nativeEvent: string }) => void;

  /**
   * @type {(event: DetectionResult | { nativeEvent: DetectionResult }) => void}
   * @param {DetectionResult | { nativeEvent: DetectionResult }} event Detection results (e.g., text, barcode, etc.).
   * Optional event handler for detection results.
   */
  onDetected?: (
    event: DetectionResult | { nativeEvent: DetectionResult }
  ) => void;

  /**
   * @type {(event: ErrorResult | { nativeEvent: ErrorResult }) => void}
   * @param {ErrorResult | { nativeEvent: ErrorResult }} event Error details.
   * Optional event handler for error events.
   */
  onError?: (event: ErrorResult | { nativeEvent: ErrorResult }) => void;
}

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
   * @type {string[]}
   * @description List of scanned barcodes detected in the image.
   * @example ['1234567890', '0987654321']
   */
  barcodes: string[];

  /**
   * @type {string | undefined}
   * @description Optional native image URI if available (such as from device camera).
   * @example 'file:///path/to/image.jpg'
   */
  nativeImage?: string;
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
}

export interface BarcodeResult {
  scannedCode: string; // The scanned barcode value
  gs1ExtractedInfo?: Record<string, string>; // Additional extracted information as a key-value map
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

/**
 * Exposes methods to control the Vision SDK from the parent component.
 */
export interface VisionSdkRefProps {
  /**
   * Stops the running process of the Vision SDK.
   * @description This method stops any ongoing process in the Vision SDK, such as scanning or image processing.
   * @example
   * visionSdkRef.current.stopRunningHandler();
   * @return {void}
   */
  stopRunningHandler: () => void;

  /**
   * Captures an image using the Vision SDK.
   * @description This method triggers the camera to capture an image for processing.
   * @example
   * visionSdkRef.current.cameraCaptureHandler();
   * @return {void}
   */
  cameraCaptureHandler: () => void;

  /**
   * Restarts the scanning process.
   * @description This method restarts the scanning process after it has been stopped or interrupted.
   * @example
   * visionSdkRef.current.restartScanningHandler();
   * @return {void}
   */
  restartScanningHandler: () => void;

  /**
   * Starts the running process of the Vision SDK.
   * @description This method starts the scanning or image processing task in the Vision SDK.
   * @example
   * visionSdkRef.current.startRunningHandler();
   * @return {void}
   */
  startRunningHandler: () => void;

  /**
   * Sets metadata for processing in the Vision SDK.
   * @param {any} value - The metadata to be set (can be an object).
   * @description This method allows you to set metadata that will be processed by the Vision SDK.
   * @example
   * visionSdkRef.current.setMetadata({ orderId: 12345, customerName: 'John Doe' });
   * @return {void}
   */
  setMetadata: (value: any) => void;

  /**
   * Sets recipient details in the Vision SDK.
   * @param {any} value - The recipient data (could be an object with recipient details).
   * @description This method is used to set recipient details such as name, address, etc.
   * @example
   * visionSdkRef.current.setRecipient({ name: 'Jane Smith', address: '123 Main St' });
   * @return {void}
   */
  setRecipient: (value: any) => void;

  /**
   * Sets sender details in the Vision SDK.
   * @param {any} value - The sender data (could be an object with sender details).
   * @description This method is used to set sender details such as name, address, etc.
   * @example
   * visionSdkRef.current.setSender({ name: 'John Doe', address: '456 Elm St' });
   * @return {void}
   */
  setSender: (value: any) => void;

  /**
   * Configures the on-device model for processing in the Vision SDK.
   * @param {object} payload - Configuration data for the on-device model.
   * @param {ModuleType} payload.type - The type of the module to configure (e.g., 'item_label', 'shipping_label').
   * @param {ModuleSize} [payload.size] - (Optional) The size of the device module (e.g., 'nano', 'small').
   * @description This method configures the module that will be used for processing images.
   * @example
   * visionSdkRef.current.configureOnDeviceModel({ type: 'item_label', size: 'small' });
   * @return {void}
   */
  configureOnDeviceModel: (payload: {
    type: ModuleType;
    size?: ModuleSize;
  }, token?: string | undefined | null, apiKey?: string | undefined | null) => void;

  /**
   * Gets a prediction based on the provided image and barcode.
   * @param {any} image - The image to be analyzed.
   * @param {string[]} barcode - An array of barcode values to analyze alongside the image.
   * @description This method processes an image and its associated barcodes to get a prediction.
   * @example
   * visionSdkRef.current.getPrediction(image, ['1234567890']);
   * @return {void}
   */
  getPrediction: (image: any, barcode: string[]) => void;

  /**
   * Gets a prediction with cloud transformations.
   * @param {any} image - The image to be analyzed.
   * @param {string[]} barcode - An array of barcode values to analyze alongside the image.
   * @param {boolean} [withImageResizing=true] - (Optional) Whether to resize the image (default: true).
   * @description This method processes an image with additional cloud transformations.
   * @example
   * visionSdkRef.current.getPredictionWithCloudTransformations(image, ['1234567890']);
   * @return {void}
   */
  getPredictionWithCloudTransformations: (
    image: any,
    barcode: string[],
    token?: string,
    apiKey?: string,
    locationId?: string,
    options?: any,
    metadata?: any,
    recipient?: any,
    sender?: any,
    shouldResizeImage?: boolean
  ) => void;

  /**
   * Gets a prediction for a shipping label using cloud processing.
   * @param {any} image - The image of the shipping label.
   * @param {string[]} barcode - Array of barcode strings associated with the shipping label.
   * @param {boolean} [withImageResizing=true] - (Optional) Whether to resize the image (default: true).
   * @description This method processes a shipping label to retrieve predictions using cloud processing.
   * @example
   * visionSdkRef.current.getPredictionShippingLabelCloud(image, ['9876543210']);
   * @return {void}
   */
  getPredictionShippingLabelCloud: (
    image: any,
    barcode: string[],
    token?: string,
    apiKey?: string,
    locationId?: string,
    options?: Record<string, any>,
    metadata?: any,
    recipient?: any,
    sender?: any,
    shouldResizeImage?: boolean
  ) => void;

  /**
   * Gets a prediction for Bill of Lading using cloud processing.
   * @param {any} image - The image of the Bill of Lading.
   * @param {string[]} barcode - Array of barcode strings.
   * @param {boolean} [withImageResizing=true] - (Optional) Whether to resize the image (default: true).
   * @description This method retrieves predictions for a Bill of Lading using cloud processing.
   * @example
   * visionSdkRef.current.getPredictionBillOfLadingCloud(image, ['1234567890']);
   * @return {void}
   */
  getPredictionBillOfLadingCloud: (
    image: any,
    barcode: string[],
    token?: string,
    apiKey?: string,
    locationId?: string,
    options?: Record<string, any>,
    shouldResizeImage?: boolean
  ) => void;

  /**
   * Gets a prediction for item label using cloud processing.
   * @param {any} image - The image of the item label.
   * @param {string[]} barcode - (Optional) Array of barcode strings associated with the shipping label.
   * @param {boolean} [withImageResizing=true] - (Optional) Whether to resize the image (default: true).
   * @description This method retrieves predictions for an item label using cloud processing.
   * @example
   * visionSdkRef.current.getPredictionItemLabelCloud(image);
   * @return {void}
   */
  getPredictionItemLabelCloud: (
    image: string,
    token?: string,
    apiKey?: string,
    shouldResizeImage?: boolean
  ) => void;

  /**
   * Gets a prediction for document classification using cloud processing.
   * @param {any} image - The image of the document to classify.
   * @param {string[]} barcode - (Optional) Array of barcode strings associated with the shipping label.
   * @param {boolean} [withImageResizing=true] - (Optional) Whether to resize the image (default: true).
   * @description This method retrieves predictions for document classification using cloud processing.
   * @example
   * visionSdkRef.current.getPredictionDocumentClassificationCloud(image);
   * @return {void}
   */
  getPredictionDocumentClassificationCloud: (
    image: string,
    token?: string,
    apiKey?: string,
    shouldResizeImage?: boolean
  ) => void;

  /**
   * Reports an error for handling errors on the device.
   * @param {ReportErrorType} payload - An object containing error details.
   * @param {string} payload.reportText - A custom error message.
   * @param {ModuleSize} payload.size - The size of the device module where the error occurred.
   * @param {ModuleType} payload.type - The type of the device module where the error occurred.
   * @param {string} [payload.image] - (Optional) Captured UI image related to the error.
   * @param {string} [payload.response] - (Optional) Device module response message.
   * @description This method is used to report errors for debugging.
   * @example
   * visionSdkRef.current.reportError({
   *   reportText: 'Error processing image.',
   *   size: 'small',
   *   type: 'shipping_label',
   *   image: capturedImage,
   * });
   * @return {void}
   */
  reportError: (payload: ReportErrorType, token?: string, apiKey?: string) => void;

  /**
   * Creates a new template.
   * @description This method allows you to create a new template in the Vision SDK.
   * @example
   * visionSdkRef.current.createTemplate();
   * @return {void}
   */
  createTemplate: (callback?: (res: any, err: any) => void) => void;

  /**
   * Gets all saved templates.
   * @description This method retrieves all templates saved in the Vision SDK.
   * @example
   * visionSdkRef.current.getAllTemplates();
   * @return {void}
   */
  getAllTemplates: () => void;

  /**
   * Deletes a specific template by its ID.
   * @param {string} id - The unique identifier of the template to be deleted.
   * @description This method deletes a template with a given ID.
   * @example
   * visionSdkRef.current.deleteTemplateWithId('template123');
   * @return {void}
   */
  deleteTemplateWithId: (id: string) => void;

  /**
   * Deletes all templates from storage.
   * @description This method deletes all templates stored in the Vision SDK.
   * @example
   * visionSdkRef.current.deleteAllTemplates();
   * @return {void}
   */
  deleteAllTemplates: () => void;

  /**
   * Sets the focus settings for the Vision SDK.
   * @param {any} settings - Focus settings configuration.
   * @description This method configures the focus settings for the SDK to improve image capture quality.
   * @example
   * visionSdkRef.current.setFocusSettings({ autoFocus: true });
   * @return {void}
   */
  setFocusSettings: (settings: any) => void;

  /**
   * Sets object detection settings for the Vision SDK.
   * @param {any} settings - Object detection settings, such as confidence threshold.
   * @description This method configures the object detection settings.
   * @example
   * visionSdkRef.current.setObjectDetectionSettings({ confidenceThreshold: 0.85 });
   * @return {void}
   */
  setObjectDetectionSettings: (settings: any) => void;

  /**
   * Sets the camera settings for the Vision SDK.
   * @param {any} settings - Camera settings (e.g., resolution, exposure).
   * @description This method configures camera settings for optimal image capture.
   * @example
   * visionSdkRef.current.setCameraSettings({ resolution: 'high', exposure: 'auto' });
   * @return {void}
   */
  setCameraSettings: (settings: any) => void;
}

/**
 * Props for the Vision SDK component.
 */
export interface VisionSdkProps {
  /**
   * @optional
   * @type {ReactNode | undefined}
   * @description Optional children elements to render inside the SDK component.
   * This allows you to nest other components or elements inside the SDK component.
   * @example <VisionSdk>{<SomeChildComponent />}</VisionSdk>
   */
  children?: ReactNode;

  /**
   * @optional
   * @type {React.Ref<any> | undefined}
   * @description Optional reference to the component.
   * Use this prop to get a reference to the SDK component for programmatic control.
   * @example const ref = useRef(null);
   */
  refProp?: React.Ref<any>;

  /**
   * @optional
   * @type {string | undefined}
   * @description Optional API key for authentication with the SDK.
   * The API key is required for connecting with the SDK service.
   * @example "API_KEY_1234"
   */
  apiKey?: string;

  /**
   * @optional
   * @type {string | undefined}
   * @description Optional trigger to re-render the component.
   * This prop can be used to trigger a re-render when this value changes.
   * @example "trigger"
   */
  reRender?: string;

  /**
   * @optional
   * @type {CaptureMode | undefined}
   * @description Optional capture mode (e.g., 'manual' or 'auto').
   * Define whether the capture should be 'manual' or 'auto'.
   * @example 'manual'
   */
  captureMode?: CaptureMode;

  /**
   * @optional
   * @type {ScanMode | undefined}
   * @description Optional mode of scanning (e.g., 'barcode', 'qrcode', 'ocr', etc.).
   * Choose the type of scan that the SDK should perform.
   * @example 'barcode'
   */
  mode?: ScanMode;

  /**
   * @optional
   * @type {string | undefined}
   * @description Optional token for SDK authentication.
   * Provide a token for SDK authentication if required.
   * @example "SDK_TOKEN_5678"
   */
  token?: string;

  /**
   * @optional
   * @type {string | undefined}
   * @description Optional location ID for specific operations.
   * Use this if a location-specific operation is needed in the SDK.
   * @example 'location123'
   */
  locationId?: string;

  /**
   * @optional
   * @type {Record<string, any> | undefined}
   * @description Optional additional configuration options.
   * Provide any additional settings for SDK customization.
   * @example { customOption: true }
   */
  options?: Record<string, any>;

  /**
   * @optional
   * @type {Environment | undefined}
   * @description Optional environment for SDK operation (e.g., 'dev', 'qa', 'staging', 'prod', or 'sandbox').
   * Specify the environment in which the SDK should operate.
   * @example 'prod'
   */
  environment?: Environment;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Optional flag to enable or disable flash for scanning or capturing.
   * Use this to enable or disable the flash functionality during scanning.
   * @example true
   */
  flash?: boolean;

  /**
   * @optional
   * @type {boolean | undefined}
   * @description Optional flag to enable or disable multiple scans.
   * Use this to allow multiple scans (e.g., scanning several barcodes in succession).
   * @example true
   */
  isMultipleScanEnabled?: boolean;

  /**
   * @optional
   * @type {number | undefined}
   * @description Optional zoom level for scanning or capturing.
   * Adjust the zoom level for the scanning or capturing operation.
   * @example 2
   */
  zoomLevel?: number;

  /**
   * @optional
   * @type {OCRMode | undefined}
   * @description Optional OCR mode (e.g., cloud, on-device).
   * Define whether the OCR operation should happen in the cloud or on-device.
   * @example 'cloud'
   */
  ocrMode?: OCRMode;

  /**
 * @optional
 * @type {OCRType| undefined}
 * @description Optional OCR type (e.g., shipping_label, item_label, bill_of_lading, document_classification).
 * Define whether the OCR operation should happen in the cloud or on-device.
 * @example 'shipping_label'
 */
  ocrType?: OCRType;


  /**
* @optional
* @type {boolean}
* @description Optional shouldResizeImage (true, false).
* Defines whether to resize image before sending it to the api.
* @example 'shipping_label'
*/
  shouldResizeImage?: boolean;


  /**
   * @optional
   * @type {boolean | undefined}
   * @description Optional flag to enable or disable auto OCR response with image.
   * This enables auto OCR responses that include the image with the OCR result.
   * @example true
   */
  isEnableAutoOcrResponseWithImage?: boolean;

  /**
   * @optional
   * @param {ModelDownloadProgress} event
   * @type {(event: ModelDownloadProgress) => void | undefined}
   * @description Event handler for model download progress.
   * This callback is triggered to report the progress of model downloads.
   * @example (event) => console.log(event)
   * @return {void}
   */
  onModelDownloadProgress?: (event: ModelDownloadProgress) => void;

  /**
   * @optional
   * @param {BarcodeScanResult} event
   * @type {(event: BarcodeScanResult) => void | undefined}
   * @description Event handler for barcode scan results.
   * This callback is triggered when a barcode is successfully scanned.
   * @example (event) => console.log(event)
   * @return {void}
   */
  onBarcodeScan?: (event: BarcodeScanResult) => void;

  /**
   * @optional
   * @param {ImageCaptureEvent} event
   * @type {(event: ImageCaptureEvent) => void | undefined}
   * @description Event handler for image capture events.
   * This callback is triggered when an image is captured by the SDK.
   * @example (event) => console.log(event)
   * @return {void}
   */
  onImageCaptured?: (event: ImageCaptureEvent) => void;

  /**
   * @optional
   * @param {OCRScanResult} event
   * @type {(event: OCRScanResult) => void | undefined}
   * @description Event handler for OCR scan results.
   * This callback is triggered when OCR results are obtained.
   * @example (event) => console.log(event)
   * @return {void}
   */
  onOCRScan?: (event: OCRScanResult) => void;

  /**
   * @optional
   * @param {string } event - The event triggered when a template is created.
   * @type {(event: string ) => void | undefined}
   * @description Event handler that triggers when a template is successfully created.
   * This callback receives the created template's ID or a native event.
   * @example
   * onCreateTemplate: (event) => console.log('Template Created:', event)
   * @return {void}
   */

  onCreateTemplate?: (event: string) => void;

  /**
   * @optional
   * @param {string[]} event - The event triggered when templates are retrieved.
   * @type {(event: string[]) => void | undefined}
   * @description Event handler that triggers when templates are successfully retrieved.
   * This callback receives an array of template IDs or a native event.
   * @example
   * onGetTemplates: (event) => console.log('Templates Retrieved:', event)
   * @return {void}
   */
  onGetTemplates?: (event: string[]) => void;

  /**
   * @optional
   * @param {string} event - The event triggered when a template is deleted by ID.
   * @type {(event: string) => void | undefined}
   * @description Event handler that triggers when a template is successfully deleted using its ID.
   * This callback receives the ID of the deleted template or a native event.
   * @example
   * onDeleteTemplateById: (event) => console.log('Template Deleted:', event)
   * @return {void}
   */
  onDeleteTemplateById?: (event: string | { nativeEvent: string }) => void;

  /**
   * @optional
   * @param {string } event - The event triggered when multiple templates are deleted.
   * @type {(event: string) => void | undefined}
   * @description Event handler that triggers when multiple templates are successfully deleted.
   * This callback receives the ID(s) of the deleted templates or a native event.
   * @example
   * onDeleteTemplates: (event) => console.log('Templates Deleted:', event)
   * @return {void}
   */
  onDeleteTemplates?: (event: string) => void;

  /**
   * @optional
   * @param {DetectionResult} event
   * @type {(event: DetectionResult) => void | undefined}
   * @description Event handler for detected objects.
   * This callback is triggered when objects are detected by the SDK.
   * @example (event) => console.log(event)
   * @return {void}
   */
  onDetected?: (event: DetectionResult) => void;

  /**
   * @optional
   * @param {ErrorResult} event
   * @type {(event: ErrorResult) => void | undefined}
   * @description Event handler for error events.
   * This callback is triggered when an error occurs within the SDK.
   * @example (event) => console.error(event)
   * @return {void}
   */
  onError?: (event: ErrorResult) => void;
}
