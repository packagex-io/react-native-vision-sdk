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
      console.log("Model loaded");
    } catch (error) {
      console.error("Failed to load models:", error);
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
    shouldResizeImage: boolean = true
  ) => {
    try {
      const r = await VisionSdkModule.logItemLabelDataToPx(
        imageUri,
        barcodes,
        responseData,
        token,
        apiKey,
        shouldResizeImage
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

  }
}
