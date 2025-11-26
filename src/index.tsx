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
  Platform
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
  BoundingBoxesDetectedResult,
  PriceTagDetectionResult,
  SharpnessScoreEvent,
} from './types';
import { correctOcrEvent } from './utils';

// Import Fabric commands for new architecture
let FabricCommands: any = null;
let isFabricEnabled = false;

// Check if running in bridgeless mode (new architecture)
// @ts-ignore
const isBridgeless = global.RN$Bridgeless === true;

if (isBridgeless) {
  try {
    const spec = require('./specs/VisionSdkViewNativeComponent');
    if (spec && spec.Commands) {
      FabricCommands = spec.Commands;
      isFabricEnabled = true;
    }
  } catch (e) {
    // Not using Fabric, will fall back to UIManager
    isFabricEnabled = false;
  }
}

export * from './types';
export * from './VisionCore';
export { VisionCamera } from './VisionCamera';
export * from './VisionCameraTypes';


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
      onSharpnessScore = () => { },
      onPriceTagDetected = () => { },
      onOCRScan = () => { },
      onDetected = () => { },
      onBoundingBoxesDetected = () => { },
      onError = () => { },
      onCreateTemplate = () => { },
      onGetTemplates = () => { },
      onDeleteTemplateById = () => { },
      onDeleteTemplates = () => { },
      shouldResizeImage = true,
      modelExecutionProviderAndroid = 'NNAPI'
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
        if (!VisionSDKViewRef.current) {
          console.warn(`🚨 No ref for command: ${commandName}`);
          return;
        }

        // Convert object parameters to JSON strings
        const commandsNeedingJsonConversion = [
          'setMetaData',
          'setRecipient',
          'setSender',
          'configureOnDeviceModel',
          'setFocusSettings',
          'setObjectDetectionSettings',
          'setCameraSettings'
        ];

        let processedParams = params;
        if (commandsNeedingJsonConversion.includes(commandName)) {
          processedParams = params.map((param, index) => {
            if (index === 0 && typeof param === 'object' && param !== null) {
              return JSON.stringify(param);
            }
            return param;
          });
        }

        // Convert null/undefined to empty strings for Fabric
        processedParams = processedParams.map(param => {
          if (param === null || param === undefined) {
            return '';
          }
          return param;
        });

        // Try to use Fabric commands first (new architecture) - Android only for now
        // iOS still uses the RCT_EXTERN_METHOD pattern which works via UIManager
        if (Platform.OS === 'android' && isFabricEnabled && FabricCommands && FabricCommands[commandName]) {
          console.log(`📤 Dispatching Fabric command: ${commandName}`);
          FabricCommands[commandName](VisionSDKViewRef.current, ...processedParams);
          return;
        }

        // Fallback to UIManager for old architecture
        const viewHandle = findNodeHandle(VisionSDKViewRef.current);
        if (!viewHandle) {
          console.warn(`🚨 No viewHandle for command: ${commandName}`);
          return;
        }

        const viewManagerConfig = UIManager.getViewManagerConfig('VisionSdkView');
        const commandId = viewManagerConfig?.Commands?.[commandName] ?? Commands[commandName];

        if (commandId === undefined) {
          throw new Error(
            `Command "${commandName}" not found in VisionSdkView or Commands.`
          );
        }

        console.log(`📤 Dispatching UIManager command: ${commandName}, commandId: ${commandId}`);
        UIManager.dispatchViewManagerCommand(
          viewHandle,
          commandId,
          processedParams
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
          shouldResizeImage ?? true
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
          shouldResizeImage ?? true
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
          shouldResizeImage ?? true,
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
          shouldResizeImage ?? true
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
          shouldResizeImage ?? true
        ]),

      // 17: Reports errors for on-device issues using the 'reportError' command
      reportError: (param: ReportErrorType, token?: string, apiKey?: string) =>
        dispatchCommand('reportError', [JSON.stringify(param), token || '', apiKey || '']),

      // 18: Creates a new template using the 'createTemplate' command
      createTemplate: () => dispatchCommand('createTemplate'), //no implementation found in android wrapper for this

      // 19: Get all saved templates using the 'getAllTemplates' command
      getAllTemplates: () => dispatchCommand('getAllTemplates'), //no implementation found in android wrapper for this

      // 20: Deletes a specific template by its ID using the 'deleteTemplateWithId' command
      deleteTemplateWithId: (id: string) =>
        dispatchCommand('deleteTemplateWithId', [id]),  //no implementation found in android wrapper for this

      // 21: Deletes all templates from storage using the 'deleteAllTemplates' command
      deleteAllTemplates: () => dispatchCommand('deleteAllTemplates'),  //no implementation found in android wrapper for this
    }), [dispatchCommand]);




    // Helper function to handle events
    const parseNativeEvent = useCallback(<T,>(event: any): T => {
      // Ensure event is an object before checking for 'nativeEvent'
      if (event && typeof event === 'object' && 'nativeEvent' in event) {
        return event.nativeEvent;
      }
      return event; // If no 'nativeEvent', return the event itself
    }, []);

    const onBarcodeScanHandler = useCallback((event: any) => {
      let barcodeEvent = parseNativeEvent<BarcodeScanResult>(event);

      // Handle Fabric architecture - parse codesJson if present
      if (barcodeEvent.codesJson !== undefined) {
        if (typeof barcodeEvent.codesJson === 'string' && barcodeEvent.codesJson.length > 0) {
          try {
            barcodeEvent.codes = JSON.parse(barcodeEvent.codesJson);
          } catch (error) {
            console.warn("Failed to parse barcode codesJson:", error);
            barcodeEvent.codes = [];
          }
        }
        // Remove the internal codesJson field
        delete barcodeEvent.codesJson;
      }

      // Ensure codes array exists (for backward compatibility)
      if (!barcodeEvent.codes || !Array.isArray(barcodeEvent.codes)) {
        barcodeEvent.codes = [];
      }

      onBarcodeScan(barcodeEvent);
    }, [onBarcodeScan])

    const onModelDownloadProgressHandler = useCallback((event: any) =>
      onModelDownloadProgress(parseNativeEvent<ModelDownloadProgress>(event)), [onModelDownloadProgress]);

    const onImageCapturedHandler = useCallback((event: any) =>
      onImageCaptured(parseNativeEvent<ImageCaptureEvent>(event)), [onImageCaptured])


    const onSharpnessScoreHandler = useCallback((event: any) =>
      onSharpnessScore(parseNativeEvent<SharpnessScoreEvent>(event)), [onSharpnessScore])


    const onPriceTagDetectedHandler = useCallback((event: any) => onPriceTagDetected(parseNativeEvent(event)), [onPriceTagDetected])

    const onDetectedHandler = useCallback(
      (event: any) => onDetected(parseNativeEvent<DetectionResult>(event)),
      [onDetected]
    );

    const onBoundingBoxesDetectedHandler = useCallback((event: any) => {
      let boundingBoxEvent = parseNativeEvent<BoundingBoxesDetectedResult>(event);

      // Handle Fabric architecture - parse JSON strings if present
      if (boundingBoxEvent.barcodeBoundingBoxesJson !== undefined) {
        if (typeof boundingBoxEvent.barcodeBoundingBoxesJson === 'string' && boundingBoxEvent.barcodeBoundingBoxesJson.length > 0) {
          try {
            boundingBoxEvent.barcodeBoundingBoxes = JSON.parse(boundingBoxEvent.barcodeBoundingBoxesJson);
          } catch (error) {
            console.warn("Failed to parse barcodeBoundingBoxesJson:", error);
            boundingBoxEvent.barcodeBoundingBoxes = [];
          }
        }
        // Remove the internal JSON field
        delete (boundingBoxEvent as any).barcodeBoundingBoxesJson;
      }

      if (boundingBoxEvent.qrCodeBoundingBoxesJson !== undefined) {
        if (typeof boundingBoxEvent.qrCodeBoundingBoxesJson === 'string' && boundingBoxEvent.qrCodeBoundingBoxesJson.length > 0) {
          try {
            boundingBoxEvent.qrCodeBoundingBoxes = JSON.parse(boundingBoxEvent.qrCodeBoundingBoxesJson);
          } catch (error) {
            console.warn("Failed to parse qrCodeBoundingBoxesJson:", error);
            boundingBoxEvent.qrCodeBoundingBoxes = [];
          }
        }
        // Remove the internal JSON field
        delete (boundingBoxEvent as any).qrCodeBoundingBoxesJson;
      }

      // Ensure arrays exist (for backward compatibility)
      if (!boundingBoxEvent.barcodeBoundingBoxes || !Array.isArray(boundingBoxEvent.barcodeBoundingBoxes)) {
        boundingBoxEvent.barcodeBoundingBoxes = [];
      }
      if (!boundingBoxEvent.qrCodeBoundingBoxes || !Array.isArray(boundingBoxEvent.qrCodeBoundingBoxes)) {
        boundingBoxEvent.qrCodeBoundingBoxes = [];
      }

      onBoundingBoxesDetected(boundingBoxEvent);
    }, [onBoundingBoxesDetected])

    const onErrorHandler = useCallback((event: any) =>
      onError(parseNativeEvent<ErrorResult>(event)), [onError])


    const onCreateTemplateHandler = useCallback((event: any) => {
      console.log("ON CREATE TEMPLATE HANDLER: ", event)
      let templateEvent = parseNativeEvent<any>(event);

      // Handle Fabric architecture (dataJson) vs Legacy architecture (data)
      if (templateEvent.dataJson && typeof templateEvent.dataJson === 'string') {
        try {
          const parsed = JSON.parse(templateEvent.dataJson);
          // If it's an array with a single element (wrapped primitive), unwrap it
          templateEvent.data = Array.isArray(parsed) && parsed.length === 1 ? parsed[0] : parsed;
        } catch (error) {
          console.warn("Failed to parse template dataJson:", error);
          templateEvent.data = templateEvent.dataJson;
        }
      }

      onCreateTemplate(templateEvent)
    }, [onCreateTemplate]);

    const onGetTemplateHandler = useCallback((event: any) => {
      console.log("ON GET TEMPLATES HANDLER: ", event)
      let templateEvent = parseNativeEvent<any>(event);

      // Handle Fabric architecture (dataJson) vs Legacy architecture (data)
      if (templateEvent.dataJson && typeof templateEvent.dataJson === 'string') {
        try {
          const parsed = JSON.parse(templateEvent.dataJson);
          // Data should already be an array from Swift
          templateEvent.data = parsed;
        } catch (error) {
          console.warn("Failed to parse template dataJson:", error);
          templateEvent.data = [];
        }
      }

      // Ensure data is always an array for getAllTemplates
      if (!Array.isArray(templateEvent.data)) {
        templateEvent.data = templateEvent.data ? [templateEvent.data] : [];
      }

      onGetTemplates(templateEvent)
    }, [onGetTemplates]);

    const onDeleteTemplateByIdHandler = useCallback((event: any) => {
      console.log("ON DELETE TEMPLATE BY ID HANDLER: ", event)
      let templateEvent = parseNativeEvent<any>(event);

      // Handle Fabric architecture (dataJson) vs Legacy architecture (data)
      if (templateEvent.dataJson && typeof templateEvent.dataJson === 'string') {
        try {
          const parsed = JSON.parse(templateEvent.dataJson);
          // If it's an array with a single element (wrapped primitive), unwrap it
          templateEvent.data = Array.isArray(parsed) && parsed.length === 1 ? parsed[0] : parsed;
        } catch (error) {
          console.warn("Failed to parse template dataJson:", error);
          templateEvent.data = templateEvent.dataJson;
        }
      }

      onDeleteTemplateById(templateEvent)
    }, [onDeleteTemplateById])

    const onDeleteTemplatesHandler = useCallback((event: any) => {
      console.log("ON DELETE TEMPLATES HANDLER: ", event)
      let templateEvent = parseNativeEvent<any>(event);

      // Handle Fabric architecture (dataJson) vs Legacy architecture (data)
      if (templateEvent.dataJson && typeof templateEvent.dataJson === 'string') {
        try {
          const parsed = JSON.parse(templateEvent.dataJson);
          // If it's an array with a single element (wrapped primitive), unwrap it
          templateEvent.data = Array.isArray(parsed) && parsed.length === 1 ? parsed[0] : parsed;
        } catch (error) {
          console.warn("Failed to parse template dataJson:", error);
          templateEvent.data = templateEvent.dataJson;
        }
      }

      onDeleteTemplates(templateEvent)
    }, [onDeleteTemplates])

    const onOCRScanHandler = useCallback((event: any) => {
      let ocrEvent = parseNativeEvent<OCRScanResult>(event);

      // Handle Fabric architecture (dataJson) vs Legacy architecture (data)
      if ((ocrEvent as any).dataJson && typeof (ocrEvent as any).dataJson === 'string') {
        try {
          // Fabric architecture: Parse dataJson string
          const parsedData = JSON.parse((ocrEvent as any).dataJson);
          ocrEvent.data = parsedData?.data ?? parsedData ?? null;
        } catch (error) {
          console.error("Failed to parse dataJson:", error);
          ocrEvent.data = null;
        }
      } else if (Platform.OS === 'android' && typeof ocrEvent.data === 'string') {
        // Legacy Android: Parse data if it's a JSON string
        try {
          ocrEvent.data = JSON.parse(ocrEvent.data)?.data ?? ocrEvent.data;
        } catch (error) {
          ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
        }
      } else {
        // Legacy iOS and other platforms: Ensure data is in correct format
        ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
      }

      // Ensure ocrEvent.data exists before setting properties on it
      if (!ocrEvent.data) {
        ocrEvent.data = {} as any;
      }

      // Ensure image_url and imagePath are populated correctly
      ocrEvent.data.image_url =
        ocrEvent?.data?.image_url ?? ocrEvent?.imagePath ?? '';
      ocrEvent.imagePath =
        ocrEvent?.data?.image_url ?? ocrEvent?.imagePath ?? '';


      const correctedOcrEvent = correctOcrEvent(ocrEvent)
      onOCRScan(correctedOcrEvent);
    }, [onOCRScan]);

    const eventHandlersRef = useRef({
      onBarcodeScan: onBarcodeScanHandler,
      onModelDownloadProgress: onModelDownloadProgressHandler,
      onImageCaptured: onImageCapturedHandler,
      onSharpnessScore: onSharpnessScoreHandler,
      onPriceTagDetected: onPriceTagDetectedHandler,
      onOCRScan: onOCRScanHandler,
      onDetected: onDetectedHandler,
      onBoundingBoxesDetected: onBoundingBoxesDetectedHandler,
      onError: onErrorHandler,
      onCreateTemplate: onCreateTemplateHandler,
      onGetTemplates: onGetTemplateHandler,
      onDeleteTemplateById: onDeleteTemplateByIdHandler,
      onDeleteTemplates: onDeleteTemplatesHandler
    })

    useEffect(() => {
      eventHandlersRef.current.onBarcodeScan = onBarcodeScanHandler
      eventHandlersRef.current.onModelDownloadProgress = onModelDownloadProgressHandler
      eventHandlersRef.current.onImageCaptured = onImageCapturedHandler
      eventHandlersRef.current.onSharpnessScore = onSharpnessScoreHandler
      eventHandlersRef.current.onPriceTagDetected = onPriceTagDetectedHandler
      eventHandlersRef.current.onOCRScan = onOCRScanHandler
      eventHandlersRef.current.onDetected = onDetectedHandler
      eventHandlersRef.current.onError = onErrorHandler
      eventHandlersRef.current.onCreateTemplate = onCreateTemplateHandler
      eventHandlersRef.current.onGetTemplates = onGetTemplateHandler
      eventHandlersRef.current.onDeleteTemplateById = onDeleteTemplateByIdHandler
      eventHandlersRef.current.onDeleteTemplates = onDeleteTemplatesHandler
      eventHandlersRef.current.onBoundingBoxesDetected = onBoundingBoxesDetectedHandler
    }, [
      onBarcodeScan,
      onModelDownloadProgress,
      onImageCaptured,
      onSharpnessScore,
      onPriceTagDetected,
      onOCRScan,
      onDetected,
      onBoundingBoxesDetected,
      onError,
      onCreateTemplate,
      onGetTemplates,
      onDeleteTemplateById,
      onDeleteTemplates
    ])

    // Subscribe event listeners on mount, and cleanup on unmount
    useEffect(() => {
      // Event listener setup

      const eventListeners = [
        ['onModelDownloadProgress', (event: any) => eventHandlersRef.current.onModelDownloadProgress(event)],
        ['onBarcodeScan', (event: any) => eventHandlersRef.current.onBarcodeScan(event)],
        ['onImageCaptured', (event: any) => eventHandlersRef.current.onImageCaptured(event)],
        ['onSharpnessScore', (event: any) => eventHandlersRef.current.onSharpnessScore(event)],

        ['onPriceTagDetected', (event: PriceTagDetectionResult) => eventHandlersRef.current.onPriceTagDetected(event)],
        ['onOCRScan', (event: any) => eventHandlersRef.current.onOCRScan(event)],
        ['onDetected', (event: any) => eventHandlersRef.current.onDetected(event)],
        ['onBoundingBoxesDetected', (event: BoundingBoxesDetectedResult) => eventHandlersRef.current.onBoundingBoxesDetected(event)],
        ['onError', (event: any) => eventHandlersRef.current.onError(event)],
        ['onCreateTemplate', (event: any) => eventHandlersRef.current.onCreateTemplate(event)],
        ['onGetTemplates', (event: any) => eventHandlersRef.current.onGetTemplates(event)],
        ['onDeleteTemplateById', (event: any) => eventHandlersRef.current.onDeleteTemplateById(event)],
        ['onDeleteTemplates', (event: any) => eventHandlersRef.current.onDeleteTemplates(event)],
      ];

      // Ensure no duplicate listeners exist
      eventListeners.forEach(([event]) => DeviceEventEmitter.removeAllListeners(event as string));


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
          modelExecutionProviderAndroid={modelExecutionProviderAndroid}
          onBarcodeScan={onBarcodeScanHandler}
          onModelDownloadProgress={onModelDownloadProgressHandler}
          onImageCaptured={onImageCapturedHandler}
          onSharpnessScore={onSharpnessScoreHandler}
          onPriceTagDetected={onPriceTagDetectedHandler}
          onOCRScan={onOCRScanHandler}
          onDetected={onDetectedHandler}
          onBoundingBoxesDetected={onBoundingBoxesDetectedHandler}
          onError={onErrorHandler}
          onCreateTemplate={onCreateTemplateHandler}
          onGetTemplates={onGetTemplateHandler}
          onDeleteTemplateById={onDeleteTemplateByIdHandler}
          onDeleteTemplates={onDeleteTemplatesHandler}
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
