import React, {
  useEffect,
  useImperativeHandle,
  useRef,
  forwardRef,
  useCallback,
} from 'react';
import {
  UIManager,
  findNodeHandle,
  StyleSheet,
  DeviceEventEmitter,
  Platform,
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';
import {
  VisionSdkProps,
  VisionSdkRefProps,
  ModelDownloadProgress,
  BarcodeScanResult,
  ImageCaptureEvent,
  OCRScanResult,
  DetectionResult,
  ErrorResult,
  ReportErrorType,
} from './types';

export * from './types';

// Camera component
const Camera = forwardRef<VisionSdkRefProps, VisionSdkProps>(
  (
    {
      children,
      apiKey = '',
      reRender,
      captureMode = 'manual',
      mode = 'barcode',
      token = '',
      locationId = '',
      options = {},
      environment = 'prod',
      isMultipleScanEnabled = false,
      flash = false,
      isEnableAutoOcrResponseWithImage = true,
      zoomLevel = 1.8,
      ocrMode = 'cloud',
      ocrType = 'shipping_label',
      onModelDownloadProgress = () => { },
      onBarcodeScan = () => { },
      onImageCaptured = () => { },
      onOCRScan = () => { },
      onDetected = () => { },
      onError = () => { },
      onCreateTemplate = () => { },
      onGetTemplates = () => { },
      onDeleteTemplateById = () => { },
      onDeleteTemplates = () => { },
      shouldResizeImage = true
    },
    ref
  ) => {
    // Ref for the Vision SDK View
    const VisionSDKViewRef = useRef(null);

    // Enum for Commands
    enum Commands {
      captureImage = 0,
      stopRunning,
      startRunning,
      setMetaData,
      setRecipient,
      setSender,
      configureOnDeviceModel,
      restartScanning,
      setFocusSettings,
      setObjectDetectionSettings,
      setCameraSettings,
      getPrediction,
      getPredictionWithCloudTransformations,
      getPredictionShippingLabelCloud,
      getPredictionBillOfLadingCloud,
      getPredictionItemLabelCloud,
      getPredictionDocumentClassificationCloud,
      reportError,
      createTemplate,
      getAllTemplates,
      deleteTemplateWithId,
      deleteAllTemplates,
    }

    /* Command functions using dispatchCommand helper with name and enum fallback */
    const dispatchCommand = useCallback((
      commandName: keyof typeof Commands,
      params: any[] = []
    ) => {
      try {
        // Log params for debugging
        console.log(`Dispatching command: ${commandName} with params:`, params);
        // Attempt to retrieve the command from the VisionSDKView's UIManager configuration. If not found, fall back to using the command from the Commands enum.
        const command =
          UIManager.getViewManagerConfig('VisionSDKView')?.Commands[
          commandName
          ] ?? Commands[commandName];
        // If command is not found in either UIManager or Commands, throw an error.
        if (command === undefined) {
          throw new Error(
            `Command "${commandName}" not found in VisionSDKView or Commands.`
          );
        }

        console.log(`📡 Dispatching command: ${commandName}`, params);

        // Dispatch the command with the provided parameters to the native module (VisionSDKView).
        const viewHandle = findNodeHandle(VisionSDKViewRef.current)
        if (!viewHandle) return
        UIManager.dispatchViewManagerCommand(
          viewHandle, // Find the native view reference
          command, // The command to dispatch
          params // Parameters to pass with the command
        );
      } catch (error: any) {
        console.error(`🚨 Error dispatching command: ${error.message}`);
        onError({ message: error.message });
      }
    }, [onError]);

    // Expose handlers via ref to parent components
    // This allows the parent component to call functions like cameraCaptureHandler, stopRunningHandler, etc.
    useImperativeHandle(ref, () => ({
      // 0: Captures an image using the 'captureImage' command
      cameraCaptureHandler: () => dispatchCommand('captureImage'),

      // 1: Stops the running process using the 'stopRunning' command
      stopRunningHandler: () => dispatchCommand('stopRunning'),

      // 2: Starts the running process using the 'startRunning' command
      startRunningHandler: () => dispatchCommand('startRunning'),

      // 3: Sets metadata using the 'setMetaData' command
      setMetadata: (param: any) => dispatchCommand('setMetaData', [param]), //relevant to only shipping label and matching api

      // 4: Sets the recipient information using the 'setRecipient' command
      setRecipient: (param: any) => dispatchCommand('setRecipient', [param]),//relevant to only shipping label and matching api

      // 5: Sets the sender information using the 'setSender' command
      setSender: (param: any) => dispatchCommand('setSender', [param]), //relevant to only shipping label and matching api

      // 6: Configures on-device model using the 'configureOnDeviceModel' command
      configureOnDeviceModel: (
        param: any,
        token: string | undefined | null,
        apiKey: string | undefined | null
      ) => dispatchCommand('configureOnDeviceModel', [param, token, apiKey]),

      // 7: Restarts the scanning process using the 'restartScanning' command
      restartScanningHandler: () => dispatchCommand('restartScanning'),

      // 8: Sets focus settings using the 'setFocusSettings' command
      setFocusSettings: (param: any) => dispatchCommand('setFocusSettings', [param]),

      // 9: Sets object detection settings using the 'setObjectDetectionSettings' command
      setObjectDetectionSettings: (param: any) => dispatchCommand('setObjectDetectionSettings', [param]),

      // 10: Sets camera settings using the 'setCameraSettings' command
      setCameraSettings: (param: any) => dispatchCommand('setCameraSettings', [param]),

      // 11: Retrieves prediction using the 'getPrediction' command with image and barcode parameters
      getPrediction: (image: string, barcode: string[]) => dispatchCommand('getPrediction', [image, barcode]),

      // bitmap = bitmap,
      // barcodeList = list,
      // locationId = locationId?.takeIf { it.isNotEmpty() },
      // options = options ?: emptyMap(),
      // metadata = metaData ?: emptyMap(),
      // recipient = recipient ?: emptyMap(),
      // sender = sender ?: emptyMap(),
      // onDeviceResponse = onDeviceResponse,
      // shouldResizeImage = shouldResizeImage

      // 12: Retrieves prediction with cloud transformations using the 'getPredictionWithCloudTransformations' command
      getPredictionWithCloudTransformations: (
        image: string,
        barcode: string[],
        token?: string,
        apiKey?: string,
        locationId?: string,
        options?: any,
        metadata?: any,
        recipient?: any,
        sender?: any,
        shouldResizeImage?: boolean

      ) =>
        dispatchCommand('getPredictionWithCloudTransformations', [
          image,
          barcode,
          token,
          apiKey,
          locationId,
          options,
          metadata,
          recipient,
          sender,
          shouldResizeImage
        ]),

      // 13: Retrieves prediction for shipping label cloud using the 'getPredictionShippingLabelCloud' command
      getPredictionShippingLabelCloud: (
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

      ) =>
        dispatchCommand('getPredictionShippingLabelCloud', [
          image,
          barcode,
          token,
          apiKey,
          locationId,
          options,
          metadata,
          recipient,
          sender,
          shouldResizeImage
        ]),

      // 14: Retrieves prediction for Bill of Lading cloud using the 'getPredictionBillOfLadingCloud' command
      getPredictionBillOfLadingCloud: (
        image: string,
        barcode: string[],
        token?: string,
        apiKey?: string,
        locationId?: string,
        options?: any,
        shouldResizeImage?: boolean
      ) =>
        dispatchCommand('getPredictionBillOfLadingCloud', [
          image,
          barcode,
          token,
          apiKey,
          locationId,
          options,
          shouldResizeImage,
        ]),

      // 15: Retrieves prediction for item label cloud using the 'getPredictionItemLabelCloud' command
      getPredictionItemLabelCloud: (
        image: string,
        token?: string,
        apiKey?: string,
        shouldResizeImage?: boolean
      ) =>
        dispatchCommand('getPredictionItemLabelCloud', [
          image,
          token,
          apiKey,
          shouldResizeImage
        ]),

      // 16: Retrieves prediction for document classification cloud using the 'getPredictionDocumentClassificationCloud' command
      getPredictionDocumentClassificationCloud: (
        image: string,
        token?: string,
        apiKey?: string,
        shouldResizeImage?: boolean
      ) =>
        dispatchCommand('getPredictionDocumentClassificationCloud', [
          image,
          token,
          apiKey,
          shouldResizeImage
        ]),

      // 17: Reports errors for on-device issues using the 'reportError' command
      reportError: (param: ReportErrorType, token?: string, apiKey?: string) =>
        dispatchCommand('reportError', [param, token, apiKey]), //check with the team, add token and apiKey

      // 18: Creates a new template using the 'createTemplate' command
      createTemplate: () => dispatchCommand('createTemplate'), //no implementation found in android wrapper for this

      // 19: Get all saved templates using the 'getAllTemplates' command
      getAllTemplates: () => dispatchCommand('getAllTemplates'), //no implementation found in android wrapper for this

      // 20: Deletes a specific template by its ID using the 'deleteTemplateWithId' command
      deleteTemplateWithId: (id: string) =>
        dispatchCommand('deleteTemplateWithId', [id]),  //no implementation found in android wrapper for this

      // 21: Deletes all templates from storage using the 'deleteAllTemplates' command
      deleteAllTemplates: () => dispatchCommand('deleteAllTemplates'),  //no implementation found in android wrapper for this
      // onCreateTemplate: () => dispatchCommand('deleteAllTemplates'),
    }), [dispatchCommand]);




    // Helper function to handle events
    const parseNativeEvent = useCallback(<T,>(event: any): T => {
      // Ensure event is an object before checking for 'nativeEvent'
      if (event && typeof event === 'object' && 'nativeEvent' in event) {
        return event.nativeEvent;
      }
      return event; // If no 'nativeEvent', return the event itself
    }, []);

    const onBarcodeScanHandler = useCallback((event: any) =>
      onBarcodeScan(parseNativeEvent<BarcodeScanResult>(event)),
      [onBarcodeScan])

    // const onImageCaptured = useCallback((event) =>  console.log('Image Captured:', event), []);
    const onModelDownloadProgressHandler = useCallback((event: any) =>
      onModelDownloadProgress(parseNativeEvent<ModelDownloadProgress>(event)), [onModelDownloadProgress]);

    const onImageCapturedHandler = useCallback((event: any) =>
      onImageCaptured(parseNativeEvent<ImageCaptureEvent>(event)), [onImageCaptured])

    const onDetectedHandler = useCallback(
      (event: any) => onDetected(parseNativeEvent<DetectionResult>(event)),
      [onDetected]
    );

    const onErrorHandler = useCallback((event: any) =>
      onError(parseNativeEvent<ErrorResult>(event)), [onError])


    const onCreateTemplateHandler = useCallback((event: any) =>
      onCreateTemplate(parseNativeEvent<any>(event)), [onCreateTemplate]);

    const onGetTemplateHandler = useCallback((event: any) =>
      onGetTemplates(parseNativeEvent<any>(event)), [onGetTemplates]);

    const onDeleteTemplateByIdHandler = useCallback((event: any) =>
      onDeleteTemplateById(parseNativeEvent<any>(event)), [onDeleteTemplateById])

    const onDeleteTemplatesaHandler = useCallback((event: any) =>
      onDeleteTemplates(parseNativeEvent<any>(event)), [onDeleteTemplates])

    const onOCRScanHandler = useCallback((event: any) => {
      let ocrEvent = parseNativeEvent<OCRScanResult>(event);
      // Parse data only if on Android and the data is a JSON string
      if (Platform.OS === 'android' && typeof ocrEvent.data === 'string') {
        try {
          // Attempt to parse the stringified JSON
          ocrEvent.data = JSON.parse(ocrEvent.data)?.data ?? ocrEvent.data;
        } catch (error) {
          // If JSON parsing fails, keep the original data or handle errors
          ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
        }
      } else {
        // For other platforms, ensure ocrEvent.data is in the correct format
        ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
      }
      // Ensure image_url and imagePath are populated correctly
      ocrEvent.data.image_url =
        ocrEvent?.data?.image_url ?? ocrEvent?.imagePath ?? '';
      ocrEvent.imagePath =
        ocrEvent?.data?.image_url ?? ocrEvent?.imagePath ?? '';
      // Pass the final ocrEvent to the callback function
      onOCRScan(ocrEvent);
    }, [onOCRScan]);


    // Subscribe event listeners on mount, and cleanup on unmount
    useEffect(() => {
      // Event listener setup
      const eventListeners = [
        ['onModelDownloadProgress', onModelDownloadProgressHandler],
        ['onBarcodeScan', onBarcodeScanHandler],
        ['onImageCaptured', onImageCapturedHandler],
        ['onOCRScan', onOCRScanHandler],
        ['onDetected', onDetectedHandler],
        ['onError', onErrorHandler],
        ['onCreateTemplate', onCreateTemplateHandler],
        ['onGetTemplates', onGetTemplateHandler],
        ['onDeleteTemplateById', onDeleteTemplateByIdHandler],
        ['onDeleteTemplates', onDeleteTemplatesaHandler],
      ];
      // Add listeners
      const subscriptions = eventListeners.map(([event, handler]) =>
        DeviceEventEmitter.addListener(
          event as string,
          handler as (event: any) => void
        )
      );

      // Cleanup listeners on unmount
      return () => {
        subscriptions.forEach((sub) => sub.remove());
      };
    }, []);


    return (
      <>
        <VisionSdkView
          ref={VisionSDKViewRef}
          key={reRender}
          style={styles.flex}
          isMultipleScanEnabled={isMultipleScanEnabled}
          apiKey={apiKey}
          mode={mode}
          isEnableAutoOcrResponseWithImage={isEnableAutoOcrResponseWithImage}
          captureMode={captureMode}
          ocrMode={ocrMode}
          ocrType={ocrType}
          shouldResizeImage={shouldResizeImage}
          token={token}
          locationId={locationId}
          options={options} // ideally this should be passed from variable, that is receiving data from ScannerContainer
          environment={environment}
          flash={flash}
          zoomLevel={zoomLevel}
          onBarcodeScan={onBarcodeScanHandler}
          onModelDownloadProgress={onModelDownloadProgressHandler}
          onImageCaptured={onImageCapturedHandler}
          onOCRScan={onOCRScanHandler}
          onDetected={onDetectedHandler}
          onError={onErrorHandler}
          onCreateTemplate={onCreateTemplateHandler}
          onGetTemplates={onGetTemplateHandler}
          onDeleteTemplateById={onDeleteTemplateByIdHandler}
          onDeleteTemplates={onDeleteTemplatesaHandler}
        >
          {children}
        </VisionSdkView>
      </>
    );
  }
);

export default Camera;

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  buttonText: {
    fontSize: 15,
    color: 'yellow',
  },
  unbuttonText: {
    fontSize: 15,
    color: 'white',
  },
  zoomBlock: {
    paddingHorizontal: 5,
    borderRadius: 30,
    height: 40,
    flexDirection: 'row',
    backgroundColor: '#00000050',
    justifyContent: 'space-between',
  },
  iconStyle: {
    width: 55,
    height: 55,
    bottom: 8,
  },
  autoManualBlock: {
    borderRadius: 4,
    paddingHorizontal: 2,
    height: 40,
    alignItems: 'center',
    flexDirection: 'row',
    backgroundColor: 'grey',
    justifyContent: 'space-between',
  },
  autoManualButton: {
    backgroundColor: '#fff',
    width: 80,
    borderRadius: 4,
    height: 30,
    marginHorizontal: 4,
    paddingHorizontal: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  unautoManualButton: {
    backgroundColor: 'grey',
    width: 80,
    borderRadius: 4,
    height: 30,
    marginHorizontal: 4,
    paddingHorizontal: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  circle: {
    width: 30,
    height: 30,
    borderRadius: 40,
    marginVertical: 5,
    marginHorizontal: 5,
    backgroundColor: '#00000050',
  },
  childrenContainer: {
    paddingTop: 20,
    justifyContent: 'space-between',
    position: 'absolute',
    top: 0,
    left: 0,
    height: '100%',
    width: '100%',
  },
});
