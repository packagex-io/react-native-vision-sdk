import React, { useEffect, useState } from 'react';
import {
  View,
  StyleSheet,
  Platform,
  Alert,
  Vibration,
  Text,
  Button,
} from 'react-native';
import VisionSdkView from 'react-native-vision-sdk';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';
import LoaderView from './Components/LoaderView';
import ResultView from './Components/ResultView';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import { Float } from 'react-native/Libraries/Types/CodegenTypes';

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
  const [ocrMode, setOcrMode] = useState<
    'cloud' | 'on-device' | 'on-device-with-translation'
  >('cloud');
  const [modelSize, setModelSize] = useState<string>('large');
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<any>('');

  const [mode, setMode] = useState<'barcode' | 'qrcode' | 'ocr' | 'photo'>(
    'barcode'
  );
  const [flash, setFlash] = useState<boolean>(false);
  const [zoomLevel, setZoomLevel] = useState<Float>(1.8);
  const [detectedData, setDetectedData] = useState<detectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
    document: false,
  });

  const handleCameraPress = async () => {
    try {
      let cameraPermission;
      if (Platform.OS === 'ios') {
        cameraPermission = PERMISSIONS.IOS.CAMERA;
      } else {
        cameraPermission = PERMISSIONS.ANDROID.CAMERA;
      }

      const result = await request(cameraPermission);

      if (result === RESULTS.GRANTED) {
        return true;
      } else {
        console.log('Camera Permission Error');

        Alert.alert(
          'Camera Permission Error',
          'App needs camera permission to take pictures. Please go to app setting and enable camera permission.'
        );
        return false;
      }
    } catch (error) {
      console.log('Error asking for camera permission', error);
    }
  };

  const [modelDownloadingProgress, setModelDownloadingProgress] =
    useState<downloadingProgress>({
      downloadStatus: true,
      progress: 0,
    });

  useEffect(() => {
    if (Platform.OS === 'android') {
      handleCameraPress();
    }
  }, []);

  React.useEffect(() => {
    visionSdk?.current?.setFocusSettings({
      shouldDisplayFocusImage: true,
      shouldScanInFocusImageRect: true,
      showCodeBoundariesInMultipleScan: true,
      validCodeBoundaryBorderColor: '#2abd51',
      validCodeBoundaryBorderWidth: 2,
      validCodeBoundaryFillColor: '#2abd51',
      inValidCodeBoundaryBorderColor: '#cc0829',
      inValidCodeBoundaryBorderWidth: 2,
      inValidCodeBoundaryFillColor: '#cc0829',
      showDocumentBoundaries: true,
      documentBoundaryBorderColor: '#241616',
      documentBoundaryFillColor: '#e3000080',
      focusImageTintColor: '#ffffff',
      focusImageHighlightedColor: '#e30000',
    });
    visionSdk?.current?.setObjectDetectionSettings({
      isTextIndicationOn: true,
      isBarCodeOrQRCodeIndicationOn: true,
      isDocumentIndicationOn: true,
      codeDetectionConfidence: 0.5,
      documentDetectionConfidence: 0.5,
      secondsToWaitBeforeDocumentCapture: 2.0,
    });
    visionSdk?.current?.setCameraSettings({
      nthFrameToProcess: 10,
    });
    visionSdk?.current?.startRunningHandler();
    setLoading(false);
  }, []);

  const onPressCapture = () => {
    if (mode === 'ocr') setLoading(true);
    visionSdk?.current?.cameraCaptureHandler();
  };
  const toggleFlash = (val: boolean) => {
    setFlash(val);
  };
  function isMultipleOfTen(number: any) {
    return number % 1 === 0;
  }
  useEffect(() => {
    switch (ocrMode) {
      case 'on-device':
      case 'on-device-with-translation': {
        onPressOnDeviceOcr();
      }
    }
  }, [ocrMode]);

  useEffect(() => {
    if (flash) {
      toggleFlash(flash);
    }
  }, [flash]);
  useEffect(() => {
    console.log('useEffect mode==== > ', mode);
  }, [mode]);
  const getMode = () => mode;
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
        ocrMode={ocrMode}
        captureMode={captureMode}
        mode={mode}
        environment="sandbox"
        apiKey="key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z"
        flash={flash}
        zoomLevel={zoomLevel}
        onDetected={(e: any) => {
          setDetectedData(Platform.OS === 'android' ? e : e.nativeEvent);
        }}
        onBarcodeScan={(e: any) => {
          console.log('BarCodeScanHandler', e);
          setLoading(false);
          visionSdk?.current?.restartScanningHandler();
        }}
        onOCRScan={(e: any) => {
          let scanRes = Platform.OS === 'ios' ? e.nativeEvent.data.data : e;
          if (Platform.OS === 'android') {
            const parsedOuterJson = JSON.parse(scanRes.data);
            scanRes = parsedOuterJson.data;
          }
          setResult(scanRes);
          setLoading(false);
          Vibration.vibrate(100);
          // setTimeout(() => {
          visionSdk?.current?.restartScanningHandler();
          // }, 200);
        }}
        onImageCaptured={(e: any) => {
          console.log('onImageCaptured==------>>', e);
        }}
        onModelDownloadProgress={(e: any) => {
          let response = Platform.OS === 'android' ? e : e.nativeEvent;
          console.log('ModelDownloadProgress==------>>', response.progress);
          // if (isMultipleOfTen(Math.floor(response.progress * 100))) {
          // if (response.progress !== modelDownloadingProgress.progress) {
          setModelDownloadingProgress(response);
          // }

          if (response.downloadStatus) {
            visionSdk?.current?.startRunningHandler();
          }
          // }
          setTimeout(() => {
            setLoading(false);
          }, 1000);
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
      <CameraHeaderView
        detectedData={detectedData}
        toggleFlash={toggleFlash}
        mode={mode}
        setMode={setMode}
      />

      <DownloadingProgressView
        visible={!modelDownloadingProgress.downloadStatus}
        progress={modelDownloadingProgress?.progress}
      />
      <CameraFooterView
        setCaptureMode={setCaptureMode}
        captureMode={captureMode}
        setOcrMode={setOcrMode}
        ocrMode={ocrMode}
        onPressCapture={onPressCapture}
        onPressOnDeviceOcr={onPressOnDeviceOcr}
        setModelSize={setModelSize}
        modelSize={modelSize}
        mode={mode}
        zoomLevel={zoomLevel}
        setZoomLevel={setZoomLevel}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
  },
});
