import React, { useEffect, useImperativeHandle, useRef, useState } from 'react';
import {
  UIManager,
  findNodeHandle,
  StyleSheet,
  DeviceEventEmitter,
  Platform,
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
  BarCodeScanHandler?: (_e: any) => void;
  OCRScanHandler?: (_e: any) => void;
  OnDetectedHandler?: (_e: any) => void;
  onError?: (e: { nativeEvent: { message: any } }) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  BarCodeScanHandler = (_e: any) => {},
  OCRScanHandler = (_e: any) => {},
  OnDetectedHandler = (_e: any) => {},
  onError = (_e: any): void => {},
}: Props) => {
  const defaultScanMode = ScanMode.OCR;
  const [mode, setMode] = useState<ScanMode>(defaultScanMode);
  const [token, setToken] = useState('');
  const [apiKey, setapiKey] = useState('');
  const [environment, setEnvironment] = useState('dev');
  const [locationId, setlocationId] = useState('');
  const [options, setOptions] = useState({
    parse_addresses: 'true',
    match_contacts: 'true',
  });

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
    changeModeHandler: (
      input: React.SetStateAction<ScanMode>,
      token: React.SetStateAction<string>,
      locationId: React.SetStateAction<string>,
      option: React.SetStateAction<any>,
      appEnvironment: React.SetStateAction<string>
    ) => {
      setEnvironment(appEnvironment);
      setToken(token);
      setlocationId(locationId);
      setapiKey(apiKey);
      onChangeMode(input);
      onChangeOptions(option);
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
    console.log('Image Captured');
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands.stopRunning) ||
        0,
      []
    );
  };

  const onPressStartRunning = () => {
    console.log('Image Captured');
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .startRunning) ||
        0,
      []
    );
  };

  const onChangeMode = (input: React.SetStateAction<ScanMode>) => {
    setMode(input);
  };
  const onChangeOptions = (input: React.SetStateAction<any>) => {
    setOptions(input);
  };
  useEffect(() => {
    DeviceEventEmitter.addListener('onBarcodeScanSuccess', BarCodeScanHandler);
    DeviceEventEmitter.addListener('onOCRDataReceived', OCRScanHandler);
    DeviceEventEmitter.addListener('onDetected', OnDetectedHandler);

    return () => {
      DeviceEventEmitter.removeAllListeners('onBarcodeScanSuccess');
      DeviceEventEmitter.removeAllListeners('onOCRDataReceived');
      DeviceEventEmitter.removeAllListeners('onDetected');
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return (
    // Platform.OS === 'ios' ? (
    <VisionSdkView
      style={styles.flex}
      onBarcodeScanSuccess={BarCodeScanHandler}
      onOCRDataReceived={OCRScanHandler}
      onDetected={OnDetectedHandler}
      mode={mode}
      // apiKey={
      //   'key_stag_7da7b5e917tq2eCckhc5QnTr1SfpvFGjwbTfpu1SQYy242xPjBz2mk3hbtzN6eB85MftxVw1zj5K5XBF'
      // }
      captureMode={'auto'}
      onError={onError}
      token={token}
      locationId={locationId}
      options={Platform.OS === 'ios' ? options : JSON.stringify(options)} // ideally this should be passed from options variable, that is receiving data from ScannerContainer
      environment={environment}
      ref={VisionSDKViewRef}
    >
      {children}
    </VisionSdkView>
    // ) : (
    //   <View style={styles.flex}>
    //     <Text>NOT IMPLEMENTED FOR ANDROID YET.</Text>
    //   </View>
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
