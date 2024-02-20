import React, { useEffect, useImperativeHandle, useRef, useState } from 'react';
import {
  UIManager,
  findNodeHandle,
  StyleSheet,
  DeviceEventEmitter,
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';

enum ScanMode {
  OCR = 'ocr',
  BARCODE = 'barcode',
  QRCODE = 'qrcode',
}

type Props = {
  children?: React.ReactNode;
  refProp?: any;
  key?: string;
  reRender?: string;
  delayTime?: number;
  height?: number;
  showScanFrame?: boolean;
  captureWithScanFrame?: boolean;
  BarCodeScanHandler?: (_e: any) => void;
  ImageCaptured?: (_e: any) => void;
  OCRScanHandler?: (_e: any) => void;
  OnDetectedHandler?: (_e: any) => void;
  onError?: (e: { nativeEvent: { message: any } }) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  // key,
  reRender,
  delayTime = 100,
  showScanFrame = true,
  captureWithScanFrame = true,
  BarCodeScanHandler = (_e: any) => {},
  ImageCaptured = (_e: any) => {},
  OCRScanHandler = (_e: any) => {},
  OnDetectedHandler = (_e: any) => {},
  onError = (_e: any): void => {},
}: Props) => {
  const defaultScanMode = ScanMode.BARCODE;
  const [mode, setMode] = useState<ScanMode>(defaultScanMode);
  const [token, setToken] = useState('');
  const [height, setHeight] = useState(1.0);
  const [apiKey, setApiKey] = useState('');
  const [cameraCaptureMode, setCameraCaptureMode] = useState('auto');
  const [environment, setEnvironment] = useState('staging');
  // const [delayTime, setDelay] = useState(100);
  // const [capture, setCapture] = useState('auto');
  // const [capture, setCapture] = useState(Platform?.OS =='android'  ? 'manual':'auto');
  const [locationId, setLocationId] = useState('');
  const [options, setOptions] = useState({
    match: { location: true, search: ['recipients'] },
    postprocess: { require_unique_hash: false },
    transform: { tracker: 'inbound', use_existing_tracking_number: false },
  });
  const [metaData, setMetaData] = useState({ service: 'inbound' });
  const [recipient, setRecipient] = useState({ '': '' });
  const VisionSDKViewRef = useRef(null);

  useImperativeHandle(refProp, () => ({
    cameraCaptureHandler: () => {
      onPressCaptures();
    },
    stopRunningHandler: () => {
      onPressStopRunning();
    },
    startRunningHandler: () => {
      onPressStartRunning();
    },
    onPressToggleTorchHandler: (val: any) => {
      onPressToggleTorch(val);
    },
    setToDefaultZoom: (val: any) => {
      onPressZoom(val);
    },
    // setCaptureMode: (vale) => {
    //   setCapture(vale?vale:'auto')
    // },
    changeModeHandler: (
      c_mode: React.SetStateAction<any>,
      input: React.SetStateAction<ScanMode>,
      token: React.SetStateAction<string>,
      height: React.SetStateAction<number>,
      locationId: React.SetStateAction<string>,
      option: React.SetStateAction<any>,
      appEnvironment: React.SetStateAction<string>,
      metaDataValue: React.SetStateAction<any>,
      recipient: React.SetStateAction<any>
    ) => {
      setEnvironment(appEnvironment ? appEnvironment : environment);
      setToken(token);
      setLocationId(locationId);
      setApiKey(apiKey);
      onChangeCaptureMode(c_mode);
      onChangeMode(input);
      onChangeOptions(option ? option : options);
      onChangeHeight(height);
      onChangeMetaData(metaDataValue ? metaDataValue : metaData);
      onChangeRecipient(recipient ? recipient : '');
    },
  }));

  const onPressCaptures = () => {
    console.log('Image Captured');
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
    console.log('onPressStopRunning');
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.stopRunning) ||
        1,
      []
    );
  };

  const onPressStartRunning = () => {
    console.log('onPressStartRunning');
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .startRunning) ||
        2,
      []
    );
  };

  const onPressToggleTorch = (value: any) => {
    console.log('Toggle Torch', value);
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.toggleTorch) ||
        3,
      [value]
    );
  };
  const onPressZoom = (value: any) => {
    console.log('Zoom value', value);
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.setZoomTo) ||
        4,
      [value]
    );
  };

  const onChangeCaptureMode = (c_mode: React.SetStateAction<any>) => {
    setCameraCaptureMode(c_mode);
  };
  const onChangeMode = (input: React.SetStateAction<ScanMode>) => {
    setMode(input);
  };
  const onChangeOptions = (input: React.SetStateAction<any>) => {
    setOptions(input);
  };
  const onChangeHeight = (input: React.SetStateAction<any>) => {
    setHeight(input);
  };
  const onChangeMetaData = (input: React.SetStateAction<any>) => {
    setMetaData(input);
  };
  const onChangeRecipient = (input: React.SetStateAction<any>) => {
    setRecipient(input);
  };
  useEffect(() => {
    DeviceEventEmitter.addListener('onBarcodeScanSuccess', BarCodeScanHandler);
    DeviceEventEmitter.addListener('onImageCaptured', ImageCaptured);
    DeviceEventEmitter.addListener('onOCRDataReceived', OCRScanHandler);
    DeviceEventEmitter.addListener('onDetected', OnDetectedHandler);

    return () => {
      DeviceEventEmitter.removeAllListeners('onBarcodeScanSuccess');
      DeviceEventEmitter.removeAllListeners('onOCRDataReceived');
      DeviceEventEmitter.removeAllListeners('onDetected');
      DeviceEventEmitter.removeAllListeners('onImageCaptured');
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    <>
      <VisionSdkView
        key={reRender}
        style={styles.flex}
        showScanFrame={showScanFrame}
        captureWithScanFrame={captureWithScanFrame}
        onBarcodeScanSuccess={BarCodeScanHandler}
        onImageCaptured={ImageCaptured}
        onOCRDataReceived={OCRScanHandler}
        onDetected={OnDetectedHandler}
        mode={mode}
        captureMode={cameraCaptureMode}
        delayTime={delayTime ? delayTime : 100}
        height={height}
        onError={onError}
        token={token}
        locationId={locationId}
        options={JSON.stringify(options)} // ideally this should be passed from variable, that is receiving data from ScannerContainer
        metaData={JSON.stringify(metaData)}
        recipient={JSON.stringify(recipient)}
        environment={environment}
        ref={VisionSDKViewRef}
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
