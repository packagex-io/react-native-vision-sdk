import React, { useEffect, useState } from 'react';
import { View, StyleSheet, Platform, Alert, Vibration } from 'react-native';
import VisionSdkView from 'react-native-vision-sdk';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';
import LoaderView from './Components/LoaderView';
import ResultView from './Components/ResultView';
interface downloadingProgress {
  downloadStatus: boolean;
  progress: number;
}
interface detectedDataProps {
  barcode: boolean;
  qrcode: boolean;
  text: boolean;
  document: boolean;
}
export default function App() {
  const visionSdk = React.useRef<any>(null);
  const [captureMode, setCaptureMode] = useState<'manual' | 'auto'>('manual');
  const [isOnDeviceOCR, setIsOnDeviceOCR] = useState<boolean>(false);
  const [modelSize, setModelSize] = useState<string>('large');
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<any>('');
  const [detectedData, setDeectedData] = useState<detectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
    document: false,
  });

  const [modelDownloadingProgress, setModelDownloadingProgress] =
    useState<downloadingProgress>({
      downloadStatus: true,
      progress: 0,
    });
  React.useEffect(() => {
    visionSdk?.current?.setHeight(1);
    visionSdk?.current?.startRunningHandler();
    setLoading(false);
  }, [captureMode]);
  const onPressCapture = () => {
    if (Platform.OS === 'android') {
      setLoading(true);
    }
    visionSdk?.current?.cameraCaptureHandler();
  };
  const toggleTorch = (val: boolean) => {
    visionSdk?.current?.onPressToggleTorchHandler(val);
  };
  function isMultipleOfTen(number: any) {
    return number % 1 === 0;
  }
  useEffect(() => {
    if (isOnDeviceOCR) {
      onPressOnDeviceOcr();
    }
  }, [isOnDeviceOCR]);
  const onPressOnDeviceOcr = (type = 'shipping_label', size = 'large') => {
    visionSdk?.current?.stopRunningHandler();
    setLoading(true);
    visionSdk?.current?.configureOnDeviceModel({
      type: type,
      size: size,
    });
  };
  return (
    <View style={styles.mainContainer}>
      <VisionSdkView
        refProp={visionSdk}
        isOnDeviceOCR={isOnDeviceOCR}
        showScanFrame={true}
        showDocumentBoundaries={true}
        captureWithScanFrame={true}
        captureMode={captureMode}
        mode="ocr"
        environment="sandbox"
        apiKey="key_25b25115f9gJMn8ChDqw4RrE7n6rYw06v9tD4moEUDpE9C6eKP3rIsXU1rFwWZufmYNLudaFQPMuf3ym"
        onDetected={(e: any) => {
          setDeectedData(Platform.OS === 'android' ? e : e.nativeEvent);
        }}
        onBarcodeScan={(e: any) => console.log('BarCodeScanHandler', e)}
        onOCRScan={(e: any) => {
          let scanRes = Platform.OS === 'ios' ? e.nativeEvent : e;
          if (Platform.OS === 'android') {
            const parsedOuterJson = JSON.parse(scanRes.data);
            scanRes = parsedOuterJson.data;
          }
          setResult(
            Platform.OS === 'android'
              ? scanRes
              : isOnDeviceOCR
                ? scanRes.data
                : scanRes.data.data
          );
          setLoading(false);
          Vibration.vibrate(100);
        }}
        onModelDownloadProgress={(e: any) => {
          let response = Platform.OS === 'android' ? e : e.nativeEvent;
          console.log(
            'ModelDownloadProgress==------>>',
            Math.floor(response.progress * 100)
          );
          if (isMultipleOfTen(Math.floor(response.progress * 100))) {
            setModelDownloadingProgress(response);
            if (response.downloadStatus) {
              visionSdk?.current?.startRunningHandler();
            }
          }
          setLoading(false);
        }}
        onError={(e: any) => {
          let error = Platform.OS === 'android' ? e : e.nativeEvent;
          console.log('onError', error);
          Alert.alert('ERROR', error?.message);
          setLoading(false);
        }}
      />
      <ResultView
        visible={result ? true : false}
        result={result}
        setResult={setResult}
      />
      <LoaderView visible={loading} />
      <CameraHeaderView detectedData={detectedData} toggleTorch={toggleTorch} />

      <DownloadingProgressView
        visible={!modelDownloadingProgress.downloadStatus}
        progress={modelDownloadingProgress?.progress}
      />
      <CameraFooterView
        setCaptureMode={setCaptureMode}
        captureMode={captureMode}
        setIsOnDeviceOCR={setIsOnDeviceOCR}
        isOnDeviceOCR={isOnDeviceOCR}
        onPressCapture={onPressCapture}
        onPressOnDeviceOcr={onPressOnDeviceOcr}
        setModelSize={setModelSize}
        modelSize={modelSize}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
  },
});
