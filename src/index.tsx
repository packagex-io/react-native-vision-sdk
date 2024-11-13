import React, {
  useEffect,
  useImperativeHandle,
  useRef,
  forwardRef,
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
} from './types';

export * from './types';
// Default SDK options
const sdkOptions = {
  tracker: {
    type: 'inbound',
    create_automatically: false,
    status: 'pickup_available',
  },
  transform: {
    use_existing_tracking_number: true,
    tracker: null,
  },
  match: {
    location: true,
    use_best_match: true,
    search: ['recipient'],
  },
  postprocess: {
    require_unique_hash: true,
    parse_addresses: ['sender', 'recipient'],
  },
};

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
      options = sdkOptions,
      environment = 'prod',
      flash = false,
      zoomLevel = 1.8,
      ocrMode = 'cloud',
      onModelDownloadProgress = () => {},
      onBarcodeScan = () => {},
      onImageCaptured = () => {},
      onOCRScan = () => {},
      onDetected = () => {},
      onError = () => {},
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
    }

    /* Command functions using dispatchCommand helper with name and enum fallback */
    const dispatchCommand = (
      commandName: keyof typeof Commands,
      params: any[] = []
    ) => {
      try {
        // Check if the commandName is valid (exists in the Commands enum)
        if (!(commandName in Commands)) {
          throw new Error(`"${commandName}" is not a valid command name.`);
        }
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
        // Dispatch the command with the provided parameters to the native module (VisionSDKView).
        UIManager.dispatchViewManagerCommand(
          findNodeHandle(VisionSDKViewRef.current), // Find the native view reference
          command, // The command to dispatch
          params // Parameters to pass with the command
        );
      } catch (error: any) {
        console.error(error.message);
        onError({ message: error.message });
      }
    };

    // Expose handlers via ref to parent components
    // This allows the parent component to call functions like cameraCaptureHandler, stopRunningHandler, etc.
    useImperativeHandle(ref, () => ({
      cameraCaptureHandler,
      stopRunningHandler,
      restartScanningHandler,
      startRunningHandler,
      setMetadata,
      setRecipient,
      setSender,
      configureOnDeviceModel,
      setFocusSettings,
      setObjectDetectionSettings,
      setCameraSettings,
      getPrediction,
      getPredictionWithCloudTransformations,
      getPredictionShippingLabelCloud,
      getPredictionBillOfLadingCloud,
    }));

    // Captures an image using the 'captureImage' command
    const cameraCaptureHandler = () => dispatchCommand('captureImage');

    // Stops the running process using the 'stopRunning' command
    const stopRunningHandler = () => dispatchCommand('stopRunning');

    // Starts the running process using the 'startRunning' command
    const startRunningHandler = () => dispatchCommand('startRunning');

    // Sets metadata using the 'setMetaData' command
    const setMetadata = (value: any) => dispatchCommand('setMetaData', [value]);

    // Sets the recipient information using the 'setRecipient' command
    const setRecipient = (value: any) =>
      dispatchCommand('setRecipient', [value]);

    // Sets the sender information using the 'setSender' command
    const setSender = (value: any) => dispatchCommand('setSender', [value]);

    // Configures on-device model using the 'configureOnDeviceModel' command
    const configureOnDeviceModel = (val: any) =>
      dispatchCommand('configureOnDeviceModel', [val]);

    // Restarts the scanning process using the 'restartScanning' command
    const restartScanningHandler = () => dispatchCommand('restartScanning');

    // Sets focus settings using the 'setFocusSettings' command
    const setFocusSettings = (val: any) =>
      dispatchCommand('setFocusSettings', [val]);

    // Sets object detection settings using the 'setObjectDetectionSettings' command
    const setObjectDetectionSettings = (val: any) =>
      dispatchCommand('setObjectDetectionSettings', [val]);

    // Sets camera settings using the 'setCameraSettings' command
    const setCameraSettings = (val: any) =>
      dispatchCommand('setCameraSettings', [val]);

    // Retrieves prediction using the 'getPrediction' command with image and barcode parameters
    const getPrediction = (image: string, barcode: string[]) =>
      dispatchCommand('getPrediction', [image, barcode]);

    // Retrieves prediction with cloud transformations using the 'getPredictionWithCloudTransformations' command
    const getPredictionWithCloudTransformations = (
      image: string,
      barcode: string[]
    ) =>
      dispatchCommand('getPredictionWithCloudTransformations', [
        image,
        barcode,
      ]);

    // Retrieves prediction for shipping label cloud using the 'getPredictionShippingLabelCloud' command
    const getPredictionShippingLabelCloud = (
      image: string,
      barcode: string[]
    ) => dispatchCommand('getPredictionShippingLabelCloud', [image, barcode]);

    // Retrieves prediction for Bill of Lading cloud using the 'getPredictionBillOfLadingCloud' command
    const getPredictionBillOfLadingCloud = (
      image: string,
      barcode: string[],
      withImageResizing: boolean = true
    ) =>
      dispatchCommand('getPredictionBillOfLadingCloud', [
        image,
        barcode,
        withImageResizing,
      ]);

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
      ];
      // Add listeners
      eventListeners.forEach(([event, handler]) =>
        DeviceEventEmitter.addListener(
          event as string,
          handler as (event: any) => void
        )
      );

      // Cleanup listeners on unmount
      return () => {
        eventListeners.forEach(([event]) =>
          DeviceEventEmitter.removeAllListeners(event as string)
        );
      };
    }, [mode, ocrMode]);

    // Helper function to handle events
    const parseNativeEvent = <T,>(event: any): T =>
      'nativeEvent' in event ? event.nativeEvent : event;

    const onBarcodeScanHandler = (event: any) =>
      onBarcodeScan(parseNativeEvent<BarcodeScanResult>(event));

    const onModelDownloadProgressHandler = (event: any) =>
      onModelDownloadProgress(parseNativeEvent<ModelDownloadProgress>(event));

    const onImageCapturedHandler = (event: any) =>
      onImageCaptured(parseNativeEvent<ImageCaptureEvent>(event));

    const onDetectedHandler = (event: any) =>
      onDetected(parseNativeEvent<DetectionResult>(event));

    const onErrorHandler = (event: any) =>
      onError(parseNativeEvent<ErrorResult>(event));

    const onOCRScanHandler = (event: any) => {
      console.log('OCR Scan Event:', event);
      let ocrEvent = parseNativeEvent<OCRScanResult>(event);
      // Parse data only if on Android and the data is a JSON string
      if (Platform.OS === 'android' && typeof ocrEvent.data === 'string') {
        try {
          ocrEvent.data = JSON.parse(ocrEvent.data)?.data ?? ocrEvent.data;
        } catch (error) {
          ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
        }
      } else {
        ocrEvent.data = ocrEvent.data?.data ?? ocrEvent.data ?? null;
      }
      onOCRScan(ocrEvent);
    };

    return (
      <>
        <VisionSdkView
          ref={VisionSDKViewRef}
          key={reRender}
          style={styles.flex}
          apiKey={apiKey}
          mode={mode}
          captureMode={captureMode}
          ocrMode={ocrMode}
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
