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
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';
import { VisionSdkProps, VisionSdkRefProps } from './types';

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

      ...rest // Use rest props
    },
    ref
  ) => {
    console.log({ rest });

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

    const dispatchCommand = (
      commandName: keyof typeof Commands,
      params: any[] = []
    ) => {
      if (commandName in Commands) {
        // Attempt to get the command from the view manager config or fallback to Commands enum
        const command =
          UIManager.hasViewManagerConfig('VisionSDKView') &&
          UIManager.getViewManagerConfig('VisionSDKView').Commands[commandName]
            ? UIManager.getViewManagerConfig('VisionSDKView').Commands[
                commandName
              ]
            : Commands[commandName];

        // Dispatch the command if it is found
        if (command !== undefined) {
          UIManager.dispatchViewManagerCommand(
            findNodeHandle(VisionSDKViewRef.current),
            command,
            params
          );
        } else {
          console.error(
            `Command "${commandName}" not found in VisionSDKView or Commands.`
          );
          onError({
            message: `Command "${commandName}" not found in VisionSDKView or Commands.`,
          });
        }
      } else {
        console.error(`"${commandName}" is not a valid command name.`);
        onError({ message: `"${commandName}" is not a valid command name.` });
      }
    };

    // Command functions using dispatchCommand helper with name and enum fallback

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

    // Subscribe to events on mount and cleanup on unmount
    useEffect(() => {
      DeviceEventEmitter.addListener(
        'onModelDownloadProgress',
        onModelDownloadProgress
      );
      DeviceEventEmitter.addListener('onBarcodeScan', onBarcodeScan);
      DeviceEventEmitter.addListener('onImageCaptured', onImageCaptured);
      DeviceEventEmitter.addListener('onOCRScan', onOCRScan);
      DeviceEventEmitter.addListener('onDetected', onDetected);
      DeviceEventEmitter.addListener('onError', onError);

      return () => {
        DeviceEventEmitter.removeAllListeners('onModelDownloadProgress');
        DeviceEventEmitter.removeAllListeners('onBarcodeScan');
        DeviceEventEmitter.removeAllListeners('onOCRScan');
        DeviceEventEmitter.removeAllListeners('onDetected');
        DeviceEventEmitter.removeAllListeners('onImageCaptured');
        DeviceEventEmitter.removeAllListeners('onError');
      };
    }, [mode, ocrMode]);

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
          onBarcodeScan={onBarcodeScan}
          onModelDownloadProgress={onModelDownloadProgress}
          onImageCaptured={onImageCaptured}
          onOCRScan={onOCRScan}
          onDetected={onDetected}
          onError={onError}
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
