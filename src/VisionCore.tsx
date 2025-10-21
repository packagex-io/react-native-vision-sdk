import { NativeModules, NativeEventEmitter } from 'react-native'

const { VisionSdkModule } = NativeModules

console.log("🔍 VisionSdkModule:", VisionSdkModule); // Debugging

if(!VisionSdkModule){
  console.log("VisionCore native module (VisionSdkModule) not defined")
}

const eventEmitter = new NativeEventEmitter(VisionSdkModule)

export const VisionCore = {

  /**
 * Sets environment and initializes sdk.
 * @param {string} environment - environment.
 */
  setEnvironment: (environment: 'staging' | 'dev' | 'sandbox' | 'qa' | 'production') => {
    VisionSdkModule.setEnvironment(environment)
  },


  /**
 * Loads on-device models without requiring the camera view.
 * @param {string} token - Authentication token.
 * @param {string} apiKey - API Key.
 * @param {string} modelType - Model type ("shipping_label", "bill_of_lading", etc.).
 * @param {string} modelSize - Model size ("nano", "micro", "small", "medium", "large", "xlarge").
 */

  loadModel: async (token: string | null, apiKey: string | null, modelType: string, modelSize: string) => {
    try {
      await VisionSdkModule.loadOnDeviceModels(token, apiKey, modelType, modelSize);
    } catch (error) {
      throw error
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
      const result = await VisionSdkModule.unLoadOnDeviceModels(modelType, shouldDeleteFromDisk);
      return result;
    } catch (error) {
      throw error
    }
  },
  /**
  * Subscribes to model download progress updates.
  */
  onModelDownloadProgress: (callback: (progress: number, downloadStatus: boolean, isReady: boolean) => void) => {
    return eventEmitter.addListener("onModelDownloadProgress", (event) => {
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
    metadata: {[key: string]: any} | null = {},
  ) => {
    try {
      const r = await VisionSdkModule.logItemLabelDataToPx(
        imageUri,
        barcodes,
        responseData,
        token,
        apiKey,
        shouldResizeImage,
        metadata
      );
      return r
    } catch (error) {
      throw error
    }
  },

  logShippingLabelDataToPx: async (
    imageUri: string,
    barcodes: string[],
    responseData: any,
    token: string | null,
    apiKey: string | null,
    locationId: string | null,
    options: {[key: string]: any} | null,
    metadata: {[key: string]: any} | null,
    recipient: {[key: string]: any} | null,
    sender: {[key: string]: any} | null,
    shouldResizeImage: boolean = true
  ) => {

    try {
      const r = await VisionSdkModule.logShippingLabelDataToPx(
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
      )

      return r
    } catch(err){
      throw err
    }

  },

  /**
   * Performs on-device OCR prediction on an image.
   * @param {string} imagePath - Path to the image file.
   * @param {string[]} barcodes - Array of barcode strings detected in the image.
   * @returns {Promise<string>} - OCR prediction result.
   */
  predict: async (imagePath: string, barcodes: string[] = []) => {
    try {
      const result = await VisionSdkModule.predict(imagePath, barcodes);
      return result;
    } catch (error) {
      console.error("Failed to get on-device prediction:", error);
      throw error;
    }
  },

  /**
   * Performs cloud-based shipping label prediction.
   * @param {string} imagePath - Path to the image file.
   * @param {string[]} barcodes - Array of barcode strings.
   * @param {object} options - Optional parameters for cloud prediction.
   */
  predictShippingLabelCloud: async (
    imagePath: string,
    barcodes: string[] = [],
    options: {
      token?: string | null,
      apiKey?: string | null,
      locationId?: string | null,
      options?: {[key: string]: any} | null,
      metadata?: {[key: string]: any} | null,
      recipient?: {[key: string]: any} | null,
      sender?: {[key: string]: any} | null,
      shouldResizeImage?: boolean
    } = {}
  ) => {
    try {
      const result = await VisionSdkModule.predictShippingLabelCloud(
        imagePath,
        barcodes,
        options.token,
        options.apiKey,
        options.locationId,
        options.options,
        options.metadata,
        options.recipient,
        options.sender,
        options.shouldResizeImage
      );
      return result;
    } catch (error) {
      console.error("Failed to get shipping label cloud prediction:", error);
      throw error;
    }
  },

  /**
   * Performs cloud-based item label prediction.
   * @param {string} imagePath - Path to the image file.
   * @param {object} options - Optional parameters for cloud prediction.
   */
  predictItemLabelCloud: async (
    imagePath: string,
    options: {
      token?: string | null,
      apiKey?: string | null,
      shouldResizeImage?: boolean
    } = {}
  ) => {
    try {
      const result = await VisionSdkModule.predictItemLabelCloud(
        imagePath,
        options.token,
        options.apiKey,
        options.shouldResizeImage
      );
      return result;
    } catch (error) {
      console.error("Failed to get item label cloud prediction:", error);
      throw error;
    }
  },

  /**
   * Performs cloud-based bill of lading prediction.
   * @param {string} imagePath - Path to the image file.
   * @param {string[]} barcodes - Array of barcode strings.
   * @param {object} options - Optional parameters for cloud prediction.
   */
  predictBillOfLadingCloud: async (
    imagePath: string,
    barcodes: string[] = [],
    options: {
      token?: string | null,
      apiKey?: string | null,
      locationId?: string | null,
      options?: {[key: string]: any} | null,
      shouldResizeImage?: boolean
    } = {}
  ) => {
    try {
      const result = await VisionSdkModule.predictBillOfLadingCloud(
        imagePath,
        barcodes,
        options.token,
        options.apiKey,
        options.locationId,
        options.options,
        options.shouldResizeImage
      );
      return result;
    } catch (error) {
      console.error("Failed to get bill of lading cloud prediction:", error);
      throw error;
    }
  },

  /**
   * Performs cloud-based document classification prediction.
   * @param {string} imagePath - Path to the image file.
   * @param {object} options - Optional parameters for cloud prediction.
   */
  predictDocumentClassificationCloud: async (
    imagePath: string,
    options: {
      token?: string | null,
      apiKey?: string | null,
      shouldResizeImage?: boolean
    } = {}
  ) => {
    try {
      const result = await VisionSdkModule.predictDocumentClassificationCloud(
        imagePath,
        options.token,
        options.apiKey,
        options.shouldResizeImage
      );
      return result;
    } catch (error) {
      console.error("Failed to get document classification cloud prediction:", error);
      throw error;
    }
  },

  /**
   * Performs hybrid prediction using on-device model with cloud transformations.
   * @param {string} imagePath - Path to the image file.
   * @param {string[]} barcodes - Array of barcode strings.
   * @param {object} options - Optional parameters for hybrid prediction.
   */
  predictWithCloudTransformations: async (
    imagePath: string,
    barcodes: string[] = [],
    options: {
      token?: string | null,
      apiKey?: string | null,
      locationId?: string | null,
      options?: {[key: string]: any} | null,
      metadata?: {[key: string]: any} | null,
      recipient?: {[key: string]: any} | null,
      sender?: {[key: string]: any} | null,
      shouldResizeImage?: boolean
    } = {}
  ) => {
    try {
      const result = await VisionSdkModule.predictWithCloudTransformations(
        imagePath,
        barcodes,
        options.token,
        options.apiKey,
        options.locationId,
        options.options,
        options.metadata,
        options.recipient,
        options.sender,
        options.shouldResizeImage
      );
      return result;
    } catch (error) {
      console.error("Failed to get hybrid prediction:", error);
      throw error;
    }
  }
}
