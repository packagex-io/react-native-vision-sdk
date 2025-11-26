import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  /**
   * Sets the environment for the Vision SDK
   * @param environment - The environment to set ('staging' | 'dev' | 'sandbox' | 'qa' | 'production')
   */
  setEnvironment(environment: string): void;

  /**
   * Loads on-device models without requiring the camera view
   * @param token - Authentication token (empty string if not provided)
   * @param apiKey - API Key (empty string if not provided)
   * @param modelType - Model type ("shipping_label", "bill_of_lading", etc.)
   * @param modelSize - Model size ("nano", "micro", "small", "medium", "large", "xlarge")
   */
  loadOnDeviceModels(
    token: string,
    apiKey: string,
    modelType: string,
    modelSize: string
  ): Promise<void>;

  /**
   * Unloads on-device models to free up memory
   * @param modelType - Model type to unload (empty string to unload all models)
   * @param shouldDeleteFromDisk - Whether to delete model files from disk
   * @returns Success message
   */
  unLoadOnDeviceModels(
    modelType: string,
    shouldDeleteFromDisk: boolean
  ): Promise<string>;

  /**
   * Logs item label data to PackageX
   * @param imageUri - URI of the image
   * @param barcodes - Array of barcodes
   * @param responseData - Response data object as JSON string (due to codegen limitations with complex objects)
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param shouldResizeImage - Whether to resize the image
   * @param metadata - Metadata object as JSON string (due to codegen limitations)
   * @returns Result as JSON string
   */
  logItemLabelDataToPx(
    imageUri: string,
    barcodes: string[],
    responseData: string,
    token: string | null,
    apiKey: string | null,
    shouldResizeImage: boolean,
    metadata: string
  ): Promise<string>;

  /**
   * Logs shipping label data to PackageX
   * @param imageUri - URI of the image
   * @param barcodes - Array of barcodes
   * @param responseData - Response data object as JSON string
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param locationId - Location ID (can be null)
   * @param options - Options object as JSON string
   * @param metadata - Metadata object as JSON string
   * @param recipient - Recipient object as JSON string
   * @param sender - Sender object as JSON string
   * @param shouldResizeImage - Whether to resize the image
   * @returns Result as JSON string
   */
  logShippingLabelDataToPx(
    imageUri: string,
    barcodes: string[],
    responseData: string,
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: string,
    metadata: string,
    recipient: string,
    sender: string,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Logs Bill of Lading data to PackageX
   * @param imageUri - URI of the image
   * @param barcodes - Array of barcodes
   * @param responseData - Response data object as JSON string
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param locationId - Location ID (can be null)
   * @param options - Options object as JSON string
   * @param shouldResizeImage - Whether to resize the image
   * @returns Result as JSON string
   */
  logBillOfLadingDataToPx(
    imageUri: string,
    barcodes: string[],
    responseData: string,
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: string,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Logs document classification data to PackageX
   * @param imageUri - URI of the image
   * @param responseData - Response data object as JSON string
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param shouldResizeImage - Whether to resize the image
   * @returns Result as JSON string
   */
  logDocumentClassificationDataToPx(
    imageUri: string,
    responseData: string,
    token: string | null,
    apiKey: string | null,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Performs on-device OCR prediction
   * @param imagePath - Local file path or remote URL to the image
   * @param barcodes - Array of barcodes
   * @returns Prediction result as JSON string
   */
  predict(
    imagePath: string,
    barcodes: string[]
  ): Promise<string>;

  /**
   * Performs cloud-based shipping label prediction
   * @param imagePath - Local file path or remote URL to the image
   * @param barcodes - Array of barcodes
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param locationId - Location ID (can be null)
   * @param options - Options object as JSON string
   * @param metadata - Metadata object as JSON string
   * @param recipient - Recipient object as JSON string
   * @param sender - Sender object as JSON string
   * @param shouldResizeImage - Whether to resize the image
   * @returns Prediction result as JSON string
   */
  predictShippingLabelCloud(
    imagePath: string,
    barcodes: string[],
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: string,
    metadata: string,
    recipient: string,
    sender: string,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Performs cloud-based item label prediction
   * @param imagePath - Local file path or remote URL to the image
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param shouldResizeImage - Whether to resize the image
   * @returns Prediction result as JSON string
   */
  predictItemLabelCloud(
    imagePath: string,
    token: string | null,
    apiKey: string | null,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Performs cloud-based bill of lading prediction
   * @param imagePath - Local file path or remote URL to the image
   * @param barcodes - Array of barcodes
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param locationId - Location ID (can be null)
   * @param options - Options object as JSON string
   * @param shouldResizeImage - Whether to resize the image
   * @returns Prediction result as JSON string
   */
  predictBillOfLadingCloud(
    imagePath: string,
    barcodes: string[],
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: string,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Performs cloud-based document classification prediction
   * @param imagePath - Local file path or remote URL to the image
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param shouldResizeImage - Whether to resize the image
   * @returns Prediction result as JSON string
   */
  predictDocumentClassificationCloud(
    imagePath: string,
    token: string | null,
    apiKey: string | null,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Performs on-device prediction followed by cloud transformations
   * @param imagePath - Local file path or remote URL to the image
   * @param barcodes - Array of barcodes
   * @param token - Authentication token (can be null)
   * @param apiKey - API Key (can be null)
   * @param locationId - Location ID (can be null)
   * @param options - Options object as JSON string
   * @param metadata - Metadata object as JSON string
   * @param recipient - Recipient object as JSON string
   * @param sender - Sender object as JSON string
   * @param shouldResizeImage - Whether to resize the image
   * @returns Prediction result as JSON string
   */
  predictWithCloudTransformations(
    imagePath: string,
    barcodes: string[],
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: string,
    metadata: string,
    recipient: string,
    sender: string,
    shouldResizeImage: boolean
  ): Promise<string>;

  /**
   * Adds a listener for the specified event
   * This is required for TurboModule event emitters
   */
  addListener(eventName: string): void;

  /**
   * Removes a specified number of listeners for the specified event
   * This is required for TurboModule event emitters
   */
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('VisionSdkModule');
