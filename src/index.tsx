import React, { useEffect, useImperativeHandle, useRef } from 'react';
import {
  UIManager,
  findNodeHandle,
  StyleSheet,
  DeviceEventEmitter,
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';

type Props = {
  children?: React.ReactNode;
  refProp?: any;
  apiKey?: string;
  reRender?: string;
  delayTime?: number;
  captureMode?: 'manual' | 'auto';
  mode?: 'barcode' | 'qrcode' | 'ocr' | 'photo' | 'autoBarCodeOrQRCode';
  token?: string;
  locationId?: string;
  options?: any;
  environment?: 'prod' | 'sandbox';
  flash?: boolean;
  // showDocumentBoundaries?: boolean;
  isOnDeviceOCR?: boolean;
  // showScanFrame?: boolean;
  // captureWithScanFrame?: boolean;
  onModelDownloadProgress?: (_e: any) => void;
  onBarcodeScan?: (_e: any) => void;
  onImageCaptured?: (_e: any) => void;
  onOCRScan?: (_e: any) => void;
  onDetected?: (_e: any) => void;
  onError?: (e: any) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  apiKey = '',
  reRender,
  delayTime = 100,
  captureMode = 'manual',
  mode = 'barcode',
  token = '',
  locationId = '',
  options = {},
  environment = 'prod',
  flash = false,
  // showDocumentBoundaries = false,
  isOnDeviceOCR = false,
  // showScanFrame = true,
  // captureWithScanFrame = true,
  onModelDownloadProgress = (_e: any) => {},
  onBarcodeScan = (_e: any) => {},
  onImageCaptured = (_e: any) => {},
  onOCRScan = (_e: any) => {},
  onDetected = (_e: any) => {},
  onError = (_e: any) => {},
}: Props) => {
  const VisionSDKViewRef = useRef(null);
  useImperativeHandle(refProp, () => ({
    cameraCaptureHandler: () => {
      onPressCaptures();
    },
    stopRunningHandler: () => {
      onPressStopRunning();
    },
    restartScanningHandler: () => {
      onPressRestartScanning();
    },
    startRunningHandler: () => {
      onPressStartRunning();
    },
    setToDefaultZoom: (val: any) => {
      onPressZoom(val);
    },
    setMetadata: (val: any) => {
      setMetadata(val);
    },
    setHeight: (val: any) => {
      setHeight(val);
    },
    setRecipient: (val: any) => {
      setRecipient(val);
    },
    setSender: (val: any) => {
      setSender(val);
    },
    configureOnDeviceModel: (val: any) => {
      configureOnDeviceModel(val);
    },
    setFocusSettings: (val: any) => {
      setFocusSettings(val);
    },
    setObjectDetectionSettings: (val: any) => {
      setObjectDetectionSettings(val);
    },
    setCameraSettings: (val: any) => {
      setCameraSettings(val);
    },
  }));

  const onPressCaptures = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .captureImage) ||
        0,
      []
    );
  };

  const onPressStopRunning = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.stopRunning) ||
        1,
      []
    );
  };

  const onPressStartRunning = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .startRunning) ||
        2,
      []
    );
  };

  const onPressZoom = (value: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.setZoomTo) ||
        3,
      [value]
    );
  };
  const setHeight = (value: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.setHeight) ||
        4,
      [value]
    );
  };

  const setMetadata = (value: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.setMetaData) ||
        5,
      [value]
    );
  };
  const setRecipient = (value: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .setRecipient) ||
        6,
      [value]
    );
  };
  const setSender = (value: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.setSender) ||
        7,
      [value]
    );
  };
  const configureOnDeviceModel = (val: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .configureOnDeviceModel) ||
        8,
      [val]
    );
  };
  const onPressRestartScanning = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .restartScanning) ||
        9,
      []
    );
  };
  const setFocusSettings = (val: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .setFocusSettings) ||
        10,
      [val]
    );
  };
  const setObjectDetectionSettings = (val: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .setObjectDetectionSettings) ||
        11,
      [val]
    );
  };
  const setCameraSettings = (val: any) => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .setCameraSettings) ||
        12,
      [val]
    );
  };

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
  }, [mode]);

  return (
    <>
      <VisionSdkView
        ref={VisionSDKViewRef}
        key={reRender}
        style={styles.flex}
        apiKey={apiKey}
        // showScanFrame={showScanFrame}
        // captureWithScanFrame={captureWithScanFrame}
        mode={mode}
        captureMode={captureMode}
        
        delayTime={delayTime ? delayTime : 100}
        // showDocumentBoundaries={showDocumentBoundaries}
        isOnDeviceOCR={isOnDeviceOCR}
        token={token}
        locationId={locationId}
        options={JSON.stringify(options)} // ideally this should be passed from variable, that is receiving data from ScannerContainer
        environment={environment}
        flash={flash}
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
};

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
