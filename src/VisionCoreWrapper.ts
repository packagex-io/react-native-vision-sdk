import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

// Try to import TurboModule (New Architecture)
let VisionSdkModuleNative;
let isTurboModuleEnabled = false;

// Check if running in bridgeless mode (new architecture)
// @ts-ignore
const isBridgeless = global.RN$Bridgeless === true;

if (isBridgeless) {
  // New architecture - use TurboModule
  try {
    const TurboModuleSpec = require('./specs/NativeVisionSdkModule').default;
    if (TurboModuleSpec) {
      VisionSdkModuleNative = TurboModuleSpec;
      isTurboModuleEnabled = true;
      console.log('✅ VisionCore: Using TurboModule (New Architecture)');
    }
  } catch (e) {
    // Fall back to NativeModules
    VisionSdkModuleNative = NativeModules.VisionSdkModule;
    isTurboModuleEnabled = false;
    console.log('✅ VisionCore: Using Legacy NativeModule fallback');
  }
} else {
  // Old architecture - use legacy NativeModule
  VisionSdkModuleNative = NativeModules.VisionSdkModule;
  isTurboModuleEnabled = false;
  console.log('✅ VisionCore: Using Legacy NativeModule (Old Architecture)');
}

if (!VisionSdkModuleNative) {
  console.warn('❌ VisionCore native module (VisionSdkModule) not defined');
}

const eventEmitter = new NativeEventEmitter(VisionSdkModuleNative);

// Helper to convert objects to JSON strings for TurboModule compatibility
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
      await VisionSdkModuleNative.loadOnDeviceModels(token, apiKey, modelType, modelSize);
    } catch (error) {
      throw error;
    }
  },

  /**
   * Unloads on-device models to free up memory.
   * @param {string | null} modelType - Model type to unload (null to unload all models).
   * @param {boolean} shouldDeleteFromDisk - Whether to delete model files from disk.
   * @returns {Promise<string>} - Success message.
   */
  unLoadModel: async (modelType: string | null = null, shouldDeleteFromDisk: boolean = false) => {
    try {
      const result = await VisionSdkModuleNative.unLoadOnDeviceModels(
        modelType,
        shouldDeleteFromDisk
      );
      return result;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Subscribes to model download progress updates.
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
      if (isTurboModuleEnabled) {
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
      } else {
        // Legacy module handles objects directly
        const result = await VisionSdkModuleNative.logItemLabelDataToPx(
          imageUri,
          barcodes,
          responseData,
          token,
          apiKey,
          shouldResizeImage,
          metadata
        );
        return result;
      }
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
      if (isTurboModuleEnabled) {
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
      } else {
        const result = await VisionSdkModuleNative.logShippingLabelDataToPx(
          imageUri,
          barcodes,
          responseData,
          token,
          apiKey,
          locationId,
          options,
          metadata,
          recipient,
          sender,
          shouldResizeImage
        );
        return result;
      }
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
      if (isTurboModuleEnabled) {
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
      } else {
        const result = await VisionSdkModuleNative.logBillOfLadingDataToPx(
          imageUri,
          barcodes,
          responseData,
          token,
          apiKey,
          locationId,
          options,
          shouldResizeImage
        );
        return result;
      }
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
      if (isTurboModuleEnabled) {
        const result = await VisionSdkModuleNative.logDocumentClassificationDataToPx(
          imageUri,
          toJsonString(responseData),
          token,
          apiKey,
          shouldResizeImage
        );
        return fromJsonString(result);
      } else {
        const result = await VisionSdkModuleNative.logDocumentClassificationDataToPx(
          imageUri,
          responseData,
          token,
          apiKey,
          shouldResizeImage
        );
        return result;
      }
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
      return fromJsonString(result);
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
};
