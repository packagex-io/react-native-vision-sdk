import { NativeEventEmitter, Platform } from 'react-native';
import NativeVisionSdkModule from './specs/NativeVisionSdkModule';
import type {
  OCRModule,
  ExecutionProvider,
  ModelManagerConfig,
  DownloadProgress,
  ModelInfo,
  DetectedBarcode,
} from './types';

// New Architecture only - use TurboModule
const VisionSdkModuleNative = NativeVisionSdkModule;

if (!VisionSdkModuleNative) {
  throw new Error('❌ VisionCore TurboModule (VisionSdkModule) not found. Make sure the native module is properly linked.');
}

const eventEmitter = new NativeEventEmitter(VisionSdkModuleNative);

// Helper to convert objects to JSON strings for TurboModule
const toJsonString = (obj: any): string => {
  if (obj === null || obj === undefined) return '';
  if (typeof obj === 'string') return obj;
  try {
    return JSON.stringify(obj);
  } catch {
    return '';
  }
};

// Helper to parse JSON strings back to objects
const fromJsonString = (str: string): any => {
  if (!str) return null;
  try {
    return JSON.parse(str);
  } catch {
    return str;
  }
};

// Helper to generate UUID for unique request tracking
const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

export const VisionCore = {
  /**
   * Sets environment and initializes sdk.
   * @param {string} environment - environment.
   */
  setEnvironment: (environment: 'staging' | 'dev' | 'sandbox' | 'qa' | 'production') => {
    VisionSdkModuleNative.setEnvironment(environment);
  },

  /**
   * Loads on-device models without requiring the camera view.
   * @deprecated Use loadOCRModel() instead. This method will be removed in v3.0.0
   * @param {string} token - Authentication token.
   * @param {string} apiKey - API Key.
   * @param {string} modelType - Model type ("shipping_label", "bill_of_lading", etc.).
   * @param {string} modelSize - Model size ("nano", "micro", "small", "medium", "large", "xlarge").
   */
  loadModel: async (
    token: string | null,
    apiKey: string | null,
    modelType: string,
    modelSize: string
  ) => {
    try {
      await VisionSdkModuleNative.loadOnDeviceModels(
        token ?? '',
        apiKey ?? '',
        modelType,
        modelSize
      );
    } catch (error) {
      throw error;
    }
  },

  // DEPRECATED - Use unloadModel() or deleteModel() instead
  // /**
  //  * Unloads on-device models to free up memory.
  //  * @deprecated Use unloadModel() to unload from memory or deleteModel() to delete from disk. This method will be removed in v3.0.0
  //  * @param {string | null} modelType - Model type to unload (null to unload all models).
  //  * @param {boolean} shouldDeleteFromDisk - Whether to delete model files from disk.
  //  * @returns {Promise<string>} - Success message.
  //  */
  // unLoadModel: async (modelType: string | null = null, shouldDeleteFromDisk: boolean = false) => {
  //   try {
  //     const result = await VisionSdkModuleNative.unLoadOnDeviceModels(
  //       modelType ?? '',
  //       shouldDeleteFromDisk
  //     );
  //     return result;
  //   } catch (error) {
  //     throw error;
  //   }
  // },

  /**
   * Subscribes to model download progress updates.
   * @deprecated Use downloadModel() with progressListener parameter instead. This method will be removed in v3.0.0
   */
  onModelDownloadProgress: (
    callback: (progress: number, downloadStatus: boolean, isReady: boolean) => void
  ) => {
    return eventEmitter.addListener('onModelDownloadProgress', (event) => {
      callback(event.progress, event.downloadStatus, event.isReady);
    });
  },

  logItemLabelDataToPx: async (
    imageUri: string,
    barcodes: string[],
    responseData: any,
    token: string | null,
    apiKey: string | null,
    shouldResizeImage: boolean = true,
    metadata: { [key: string]: any } | null = {}
  ) => {
    try {
      // TurboModule expects JSON strings for complex objects
      const result = await VisionSdkModuleNative.logItemLabelDataToPx(
        imageUri,
        barcodes,
        toJsonString(responseData),
        token,
        apiKey,
        shouldResizeImage,
        toJsonString(metadata)
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  logShippingLabelDataToPx: async (
    imageUri: string,
    barcodes: string[],
    responseData: any,
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: { [key: string]: any } | null,
    metadata: { [key: string]: any } | null,
    recipient: { [key: string]: any } | null,
    sender: { [key: string]: any } | null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.logShippingLabelDataToPx(
        imageUri,
        barcodes,
        toJsonString(responseData),
        token,
        apiKey,
        locationId,
        toJsonString(options),
        toJsonString(metadata),
        toJsonString(recipient),
        toJsonString(sender),
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  logBillOfLadingDataToPx: async (
    imageUri: string,
    barcodes: string[],
    responseData: any,
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: { [key: string]: any } | null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.logBillOfLadingDataToPx(
        imageUri,
        barcodes,
        toJsonString(responseData),
        token,
        apiKey,
        locationId,
        toJsonString(options),
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  logDocumentClassificationDataToPx: async (
    imageUri: string,
    responseData: any,
    token: string | null,
    apiKey: string | null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.logDocumentClassificationDataToPx(
        imageUri,
        toJsonString(responseData),
        token,
        apiKey,
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  /**
   * Performs on-device OCR prediction.
   * @param {string} imagePath - Local file path or remote URL to the image.
   * @param {any[]} barcodes - Array of barcode objects (optional).
   * @returns {Promise<string>} - JSON string with prediction results.
   */
  predict: async (
    imagePath: string,
    barcodes: any[] = []
  ) => {
    try {
      const result = await VisionSdkModuleNative.predict(imagePath, barcodes);
      const parsedResult = fromJsonString(result);

      // Transform extended_response for Android item label only
      if (
        Platform.OS === 'android' &&
        parsedResult?.data?.object === 'item_label_inference' &&
        parsedResult?.extended_response &&
        Array.isArray(parsedResult.extended_response)
      ) {
        parsedResult.extended_response = {
          raw_response: parsedResult.extended_response
        };
      }

      return parsedResult;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Performs cloud-based shipping label prediction.
   */
  predictShippingLabelCloud: async (
    imagePath: string,
    barcodes: string[] = [],
    token: string | null = null,
    apiKey: string | null = null,
    locationId: string | null = null,
    options: { [key: string]: any } | null = null,
    metadata: { [key: string]: any } | null = null,
    recipient: { [key: string]: any } | null = null,
    sender: { [key: string]: any } | null = null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.predictShippingLabelCloud(
        imagePath,
        barcodes,
        token,
        apiKey,
        locationId,
        JSON.stringify(options || {}),
        JSON.stringify(metadata || {}),
        JSON.stringify(recipient || {}),
        JSON.stringify(sender || {}),
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  /**
   * Performs cloud-based item label prediction.
   */
  predictItemLabelCloud: async (
    imagePath: string,
    token: string | null = null,
    apiKey: string | null = null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.predictItemLabelCloud(
        imagePath,
        token,
        apiKey,
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  /**
   * Performs cloud-based bill of lading prediction.
   */
  predictBillOfLadingCloud: async (
    imagePath: string,
    barcodes: string[] = [],
    token: string | null = null,
    apiKey: string | null = null,
    locationId: string | null = null,
    options: { [key: string]: any } | null = null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.predictBillOfLadingCloud(
        imagePath,
        barcodes,
        token,
        apiKey,
        locationId,
        JSON.stringify(options || {}),
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  /**
   * Performs cloud-based document classification prediction.
   */
  predictDocumentClassificationCloud: async (
    imagePath: string,
    token: string | null = null,
    apiKey: string | null = null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.predictDocumentClassificationCloud(
        imagePath,
        token,
        apiKey,
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  /**
   * Performs on-device prediction followed by cloud transformations.
   */
  predictWithCloudTransformations: async (
    imagePath: string,
    barcodes: string[] = [],
    token: string | null = null,
    apiKey: string | null = null,
    locationId: string | null = null,
    options: { [key: string]: any } | null = null,
    metadata: { [key: string]: any } | null = null,
    recipient: { [key: string]: any } | null = null,
    sender: { [key: string]: any } | null = null,
    shouldResizeImage: boolean = true
  ) => {
    try {
      const result = await VisionSdkModuleNative.predictWithCloudTransformations(
        imagePath,
        barcodes,
        token,
        apiKey,
        locationId,
        JSON.stringify(options || {}),
        JSON.stringify(metadata || {}),
        JSON.stringify(recipient || {}),
        JSON.stringify(sender || {}),
        shouldResizeImage
      );
      return fromJsonString(result);
    } catch (error) {
      throw error;
    }
  },

  // ============================================================================
  // MODEL MANAGEMENT METHODS
  // ============================================================================

  /**
   * Initialize the ModelManager singleton.
   * Must be called once during app startup before using model management features.
   *
   * @param config - Configuration options for ModelManager
   * @throws Error if already initialized
   *
   * @example
   * VisionCore.initializeModelManager({
   *   maxConcurrentDownloads: 2,
   *   enableLogging: __DEV__
   * });
   */
  initializeModelManager: (config: ModelManagerConfig = {}) => {
    const configWithDefaults = {
      maxConcurrentDownloads: config.maxConcurrentDownloads ?? 2,
      enableLogging: config.enableLogging ?? true,
    };
    VisionSdkModuleNative.initializeModelManager(toJsonString(configWithDefaults));
  },

  /**
   * Check if ModelManager has been initialized
   *
   * @returns true if initialized, false otherwise
   *
   * @example
   * if (!VisionCore.isModelManagerInitialized()) {
   *   VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });
   * }
   */
  isModelManagerInitialized: (): boolean => {
    return VisionSdkModuleNative.isModelManagerInitialized();
  },

  /**
   * Download a model from server to disk with progress tracking
   *
   * @param module - The OCRModule to download
   * @param apiKey - API key for authentication (optional)
   * @param token - Authentication token (optional)
   * @param progressListener - Callback for download progress updates (optional)
   *
   * @example
   * await VisionCore.downloadModel(
   *   { type: 'shipping_label', size: 'micro' },
   *   apiKey,
   *   null,
   *   (progress) => console.log('Progress:', progress.progress * 100 + '%')
   * );
   */
  downloadModel: async (
    module: OCRModule,
    apiKey?: string | null,
    token?: string | null,
    progressListener?: (progress: DownloadProgress) => void
  ) => {
    const requestId = generateUUID();
    let subscription: any = null;

    if (progressListener) {
      let lastProgressTime = 0;
      const THROTTLE_MS = 250; // ~4 updates per second

      subscription = eventEmitter.addListener(
        'onModelDownloadProgress',
        (event) => {
          // Only process events for this specific download request
          if (event.requestId !== requestId) {
            return;
          }

          const progress: DownloadProgress = {
            module: fromJsonString(event.moduleJson || event.module),
            progress: event.progress
          };

          const now = Date.now();
          const timeSinceLastUpdate = now - lastProgressTime;

          // Always send first update (0%) and last update (1.0)
          // Throttle intermediate updates
          if (
            progress.progress === 0 ||
            progress.progress === 1.0 ||
            timeSinceLastUpdate >= THROTTLE_MS
          ) {
            lastProgressTime = now;
            progressListener(progress);
          }
        }
      );
    }

    try {
      await VisionSdkModuleNative.downloadModel(
        toJsonString(module),
        apiKey ?? null,
        token ?? null,
        'react_native', // Hardcoded since this is a React Native library
        requestId
      );
    } finally {
      subscription?.remove();
    }
  },

  /**
   * Cancel an active model download
   *
   * @param module - The OCRModule to cancel
   * @returns true if download was cancelled, false if no active download
   *
   * @example
   * const cancelled = await VisionCore.cancelDownload({ type: 'shipping_label', size: 'micro' });
   */
  cancelDownload: async (module: OCRModule): Promise<boolean> => {
    return await VisionSdkModuleNative.cancelDownload(toJsonString(module));
  },

  // NOT AVAILABLE IN iOS SDK - COMMENTED OUT FOR API CONSISTENCY
  // /**
  //  * Get the number of currently active downloads
  //  *
  //  * @returns Number of active downloads
  //  *
  //  * @example
  //  * const activeCount = VisionCore.getActiveDownloadCount();
  //  */
  // getActiveDownloadCount: (): number => {
  //   return VisionSdkModuleNative.getActiveDownloadCount();
  // },

  /**
   * Load a model into ONNX Runtime memory for inference
   *
   * @param module - The OCRModule to load
   * @param apiKey - API key for authentication (optional)
   * @param token - Authentication token (optional)
   * @param executionProvider - ONNX execution provider (defaults to 'CPU', Android only)
   *   - 'CPU': Works on all devices (default for maximum compatibility)
   *   - 'NNAPI': Hardware acceleration via Android Neural Networks API
   *   - 'XNNPACK': Optimized CPU kernels
   *
   * @example
   * // Default - CPU for maximum compatibility
   * await VisionCore.loadOCRModel(
   *   { type: 'shipping_label', size: 'micro' },
   *   apiKey
   * );
   *
   * // With NNAPI for better performance (Android)
   * await VisionCore.loadOCRModel(
   *   { type: 'shipping_label', size: 'micro' },
   *   apiKey,
   *   null,
   *   'NNAPI'
   * );
   */
  loadOCRModel: async (
    module: OCRModule,
    apiKey?: string | null,
    token?: string | null,
    executionProvider?: ExecutionProvider
  ) => {
    const provider = executionProvider ?? 'CPU';
    await VisionSdkModuleNative.loadOCRModel(
      toJsonString(module),
      apiKey ?? null,
      token ?? null,
      'react_native', // Hardcoded since this is a React Native library
      provider
    );
  },

  /**
   * Unload a model from memory (file remains on disk)
   *
   * @param module - The OCRModule to unload
   * @returns true if model was unloaded, false if not loaded
   *
   * @example
   * const unloaded = VisionCore.unloadModel({ type: 'shipping_label', size: 'micro' });
   */
  unloadModel: (module: OCRModule): boolean => {
    return VisionSdkModuleNative.unloadModel(toJsonString(module));
  },

  /**
   * Check if a model is currently loaded in memory
   *
   * @param module - The OCRModule to check
   * @returns true if loaded, false otherwise
   *
   * @example
   * const isLoaded = VisionCore.isModelLoaded({ type: 'shipping_label', size: 'micro' });
   */
  isModelLoaded: (module: OCRModule): boolean => {
    return VisionSdkModuleNative.isModelLoaded(toJsonString(module));
  },

  /**
   * Get the number of models currently loaded in memory
   *
   * @returns Number of loaded models
   *
   * @example
   * const loadedCount = VisionCore.getLoadedModelCount();
   */
  getLoadedModelCount: (): number => {
    return VisionSdkModuleNative.getLoadedModelCount();
  },

  /**
   * Find all downloaded models by scanning the file system
   *
   * @returns Array of ModelInfo objects
   *
   * @example
   * const models = await VisionCore.findDownloadedModels();
   * models.forEach(model => {
   *   console.log(`${model.module.type} ${model.module.size}: v${model.version}, loaded: ${model.isLoaded}`);
   * });
   */
  findDownloadedModels: async (): Promise<ModelInfo[]> => {
    const result = await VisionSdkModuleNative.findDownloadedModels();
    return fromJsonString(result) || [];
  },

  /**
   * Find a specific downloaded model
   *
   * @param module - The OCRModule to find
   * @returns ModelInfo object or null if not downloaded
   *
   * @example
   * const modelInfo = await VisionCore.findDownloadedModel({ type: 'shipping_label', size: 'micro' });
   * if (modelInfo) {
   *   console.log('Version:', modelInfo.version);
   * }
   */
  findDownloadedModel: async (module: OCRModule): Promise<ModelInfo | null> => {
    const result = await VisionSdkModuleNative.findDownloadedModel(toJsonString(module));
    return fromJsonString(result);
  },

  /**
   * Find all models currently loaded in memory
   *
   * @returns Array of ModelInfo objects for loaded models
   *
   * @example
   * const loadedModels = await VisionCore.findLoadedModels();
   * console.log(`${loadedModels.length} models in memory`);
   */
  findLoadedModels: async (): Promise<ModelInfo[]> => {
    const result = await VisionSdkModuleNative.findLoadedModels();
    return fromJsonString(result) || [];
  },

  // NOT AVAILABLE IN iOS SDK - COMMENTED OUT FOR API CONSISTENCY
  // /**
  //  * Check if a newer model version is available on the server
  //  *
  //  * @param module - The OCRModule to check
  //  * @param apiKey - API key for authentication (optional)
  //  * @param token - Authentication token (optional)
  //  * @param platformType - Platform identifier (defaults to 'react_native')
  //  * @returns ModelUpdateInfo with update availability
  //  *
  //  * @example
  //  * const updateInfo = await VisionCore.checkModelUpdates(
  //  *   { type: 'shipping_label', size: 'micro' },
  //  *   apiKey
  //  * );
  //  * if (updateInfo.updateAvailable) {
  //  *   console.log(updateInfo.message);
  //  * }
  //  */
  // checkModelUpdates: async (
  //   module: OCRModule,
  //   apiKey?: string | null,
  //   token?: string | null,
  //   platformType: string = 'react_native'
  // ): Promise<ModelUpdateInfo> => {
  //   const result = await VisionSdkModuleNative.checkModelUpdates(
  //     toJsonString(module),
  //     apiKey ?? null,
  //     token ?? null,
  //     platformType
  //   );
  //   return fromJsonString(result);
  // },

  /**
   * Delete a model from disk (unloads from memory first if loaded)
   *
   * @param module - The OCRModule to delete
   * @returns true if model was deleted, false if not downloaded
   *
   * @example
   * const deleted = await VisionCore.deleteModel({ type: 'shipping_label', size: 'micro' });
   */
  deleteModel: async (module: OCRModule): Promise<boolean> => {
    return await VisionSdkModuleNative.deleteModel(toJsonString(module));
  },

  /**
   * Perform on-device OCR prediction with explicit module selection
   * Allows dynamic switching between different models without reconfiguration
   *
   * @param module - The OCRModule to use for prediction
   * @param imagePath - Local file path or remote URL to the image
   * @param barcodes - Array of detected barcode objects (optional)
   * @returns Prediction result object
   *
   * @example
   * const barcodes: DetectedBarcode[] = [
   *   {
   *     scannedCode: "1234567890128",
   *     symbology: "CODE_128",
   *     gs1ExtractedInfo: { "01": "12345678901234" },
   *     boundingBox: { x: 100, y: 200, width: 150, height: 50 }
   *   }
   * ];
   * const result = await VisionCore.predictWithModule(
   *   { type: 'shipping_label', size: 'micro' },
   *   imagePath,
   *   barcodes
   * );
   */
  predictWithModule: async (
    module: OCRModule,
    imagePath: string,
    barcodes: DetectedBarcode[] = []
  ) => {
    try {
      const result = await VisionSdkModuleNative.predictWithModule(
        toJsonString(module),
        imagePath,
        barcodes
      );
      const parsedResult = fromJsonString(result);

      // Transform extended_response for Android item label
      if (
        Platform.OS === 'android' &&
        parsedResult?.data?.object === 'item_label_inference' &&
        parsedResult?.extended_response &&
        Array.isArray(parsedResult.extended_response)
      ) {
        parsedResult.extended_response = {
          raw_response: parsedResult.extended_response
        };
      }

      return parsedResult;
    } catch (error) {
      throw error;
    }
  },

};
