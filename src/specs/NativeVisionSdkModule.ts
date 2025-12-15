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

  // DEPRECATED - Use unloadModel() or deleteModel() instead
  // /**
  //  * Unloads on-device models to free up memory
  //  * @param modelType - Model type to unload (empty string to unload all models)
  //  * @param shouldDeleteFromDisk - Whether to delete model files from disk
  //  * @returns Success message
  //  */
  // unLoadOnDeviceModels(
  //   modelType: string,
  //   shouldDeleteFromDisk: boolean
  // ): Promise<string>;

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

  // ============================================================================
  // MODEL MANAGEMENT METHODS
  // ============================================================================

  /**
   * Initialize the ModelManager singleton
   * @param configJson - Serialized ModelManagerConfig as JSON string
   */
  initializeModelManager(configJson: string): void;

  /**
   * Check if ModelManager is initialized
   * @returns true if initialized, false otherwise
   */
  isModelManagerInitialized(): boolean;

  /**
   * Download a model from server to disk
   * @param moduleJson - Serialized OCRModule as JSON string
   * @param apiKey - API key for authentication (can be null)
   * @param token - Authentication token (can be null)
   * @param platformType - Platform identifier (e.g., 'react_native')
   * @param requestId - Unique request ID for progress tracking
   */
  downloadModel(
    moduleJson: string,
    apiKey: string | null,
    token: string | null,
    platformType: string,
    requestId: string
  ): Promise<void>;

  /**
   * Cancel an active model download
   * @param moduleJson - Serialized OCRModule as JSON string
   * @returns true if download was cancelled, false if no active download
   */
  cancelDownload(moduleJson: string): Promise<boolean>;

  // NOT AVAILABLE IN iOS SDK - COMMENTED OUT FOR API CONSISTENCY
  // /**
  //  * Get the number of currently active downloads
  //  * @returns Number of active downloads
  //  */
  // getActiveDownloadCount(): number;

  /**
   * Load a model into ONNX Runtime memory
   * @param moduleJson - Serialized OCRModule as JSON string
   * @param apiKey - API key for authentication (can be null)
   * @param token - Authentication token (can be null)
   * @param platformType - Platform identifier (e.g., 'react_native')
   * @param executionProvider - ONNX execution provider (can be null, Android only)
   */
  loadOCRModel(
    moduleJson: string,
    apiKey: string | null,
    token: string | null,
    platformType: string,
    executionProvider: string | null
  ): Promise<void>;

  /**
   * Unload a model from memory (file remains on disk)
   * @param moduleJson - Serialized OCRModule as JSON string
   * @returns true if model was unloaded, false if not loaded
   */
  unloadModel(moduleJson: string): boolean;

  /**
   * Check if a model is currently loaded in memory
   * @param moduleJson - Serialized OCRModule as JSON string
   * @returns true if loaded, false otherwise
   */
  isModelLoaded(moduleJson: string): boolean;

  /**
   * Get the number of models currently loaded in memory
   * @returns Number of loaded models
   */
  getLoadedModelCount(): number;

  /**
   * Find all downloaded models by scanning the file system
   * @returns JSON string containing array of ModelInfo objects
   */
  findDownloadedModels(): Promise<string>;

  /**
   * Find a specific downloaded model
   * @param moduleJson - Serialized OCRModule as JSON string
   * @returns JSON string containing ModelInfo object or null
   */
  findDownloadedModel(moduleJson: string): Promise<string>;

  /**
   * Find all models currently loaded in memory
   * @returns JSON string containing array of ModelInfo objects
   */
  findLoadedModels(): Promise<string>;

  // NOT AVAILABLE IN iOS SDK - COMMENTED OUT FOR API CONSISTENCY
  // /**
  //  * Check if a newer model version is available on the server
  //  * @param moduleJson - Serialized OCRModule as JSON string
  //  * @param apiKey - API key for authentication (can be null)
  //  * @param token - Authentication token (can be null)
  //  * @param platformType - Platform identifier (e.g., 'react_native')
  //  * @returns JSON string containing ModelUpdateInfo object
  //  */
  // checkModelUpdates(
  //   moduleJson: string,
  //   apiKey: string | null,
  //   token: string | null,
  //   platformType: string
  // ): Promise<string>;

  /**
   * Delete a model from disk (unloads from memory first if loaded)
   * @param moduleJson - Serialized OCRModule as JSON string
   * @returns true if model was deleted, false if not downloaded
   */
  deleteModel(moduleJson: string): Promise<boolean>;

  /**
   * Perform on-device OCR prediction with explicit module selection
   * @param moduleJson - Serialized OCRModule as JSON string
   * @param imagePath - Local file path or remote URL to the image
   * @param barcodes - Array of barcode objects
   * @returns Prediction result as JSON string
   */
  predictWithModule(
    moduleJson: string,
    imagePath: string,
    barcodes: any[]
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
