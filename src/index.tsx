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
  reRender?:string;
  captureMode?:string;
  BarCodeScanHandler?: (_e: any) => void;
  OCRScanHandler?: (_e: any) => void;
  OnDetectedHandler?: (_e: any) => void;
  onError?: (e: { nativeEvent: { message: any } }) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  reRender,
  captureMode,
  BarCodeScanHandler = (_e: any) => {},
  OCRScanHandler = (_e: any) => {},
  OnDetectedHandler = (_e: any) => {},
  onError = (_e: any): void => {},
}: Props) => {
  const defaultScanMode = ScanMode.BARCODE;
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
      setEnvironment(appEnvironment?appEnvironment:environment);
      setToken(token);
      setlocationId(locationId);
      setapiKey(apiKey);
      onChangeMode(input);
      onChangeOptions(option?option:options);
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
    <>
      <VisionSdkView
      key={reRender}
        style={styles.flex}
        onBarcodeScanSuccess={BarCodeScanHandler}
        onOCRDataReceived={OCRScanHandler}
        onDetected={OnDetectedHandler}
        mode={mode}
        captureMode={captureMode}
        onError={onError}
        token={token|| 'eyJhbGciOiJSUzI1NiIsImtpZCI6ImI2NzE1ZTJmZjcxZDIyMjQ5ODk1MDAyMzY2ODMwNDc3Mjg2Nzg0ZTMiLCJ0eXAiOiJKV1QifQ.eyJvcmciOiJvcmdfZk1USDE1ZVhkWWFGUUI1WTd1UEVVZiIsInJvbGUiOiJyb2xlX293bmVyIiwic2NvcGVzIjp7Im9yZ2FuaXphdGlvbnMiOjIsInNoaXBtZW50cyI6MiwibG9jYXRpb25zIjoyLCJ1c2VycyI6MiwicGF5bWVudHMiOjIsInBheW1lbnRfbWV0aG9kcyI6MiwiZGVsaXZlcmllcyI6Miwid2ViaG9va3MiOjIsImFwaV9rZXlzIjoyLCJpdGVtcyI6MiwiYXNzZXRzIjoyLCJmdWxmaWxsbWVudHMiOjIsImNvbnRhY3RzIjoyLCJhZGRyZXNzZXMiOjIsImtpb3NrcyI6MiwibWFuaWZlc3RzIjoyLCJhdWRpdHMiOjIsInNjYW5zIjoyLCJldmVudHMiOjIsImNvbnRhaW5lcnMiOjIsInRocmVhZHMiOjIsImFuYWx5dGljcyI6Mn0sImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9weC1wbGF0Zm9ybS1kZXYtYTFlYzQiLCJhdWQiOiJweC1wbGF0Zm9ybS1kZXYtYTFlYzQiLCJhdXRoX3RpbWUiOjE2ODExMDIyNzQsInVzZXJfaWQiOiJ1c2VyX21SdGJ0aWJiWnFNWTgxTm5XMjZZcGgiLCJzdWIiOiJ1c2VyX21SdGJ0aWJiWnFNWTgxTm5XMjZZcGgiLCJpYXQiOjE2ODMxMTE4NjksImV4cCI6MTY4MzExNTQ2OSwiZW1haWwiOiJtdWhhbW1hZC5zaGVoYXJ5YXJAcGFja2FnZXguaW8iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiZmlyZWJhc2UiOnsiaWRlbnRpdGllcyI6eyJlbWFpbCI6WyJtdWhhbW1hZC5zaGVoYXJ5YXJAcGFja2FnZXguaW8iXX0sInNpZ25faW5fcHJvdmlkZXIiOiJwYXNzd29yZCJ9fQ.OxUnMrMwIdgnWf82_rqazJk6HD5DYSMQyNmYilErEWHclKAdZmx-yN8LfQbC81nQMosmNMvf6yGeKa2BoplNM7MrqUelcjFNewk5K9jIjIYdfIAeRn1gX6wA7PYgMWxeGLjkMLNwS9dChy-i4eGLaG0G9rk7Qxt8kSBmDJI9sDbsTRND2dwJ0KFfjlhxJH2NEWygNv0ApAPR1gTyngMRYH4JLapp91JKmWgKvq2q2Id_iGTdH1z9XuM51J5QPFNTJXK7lmzVmZknnHOt-B6QoXOADv2Js5jzNBwKgxPJssBT3Q2uDI73_zwuCj1kuGzdORMb8IqDFO80mjDVFJAXwA'}
        locationId={locationId|| 'loc_3LUuAHBZgSQ4t9fgMYVfyA'}
        options={Platform.OS === 'ios' ? options : JSON.stringify(options)} // ideally this should be passed from options variable, that is receiving data from ScannerContainer
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