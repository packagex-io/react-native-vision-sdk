import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  StyleSheet,
  Platform,
  Alert,
  Vibration,
  Text,
} from 'react-native';
import VisionSdkView, { VisionSdkRefProps } from '../../src/index';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';
import LoaderView from './Components/LoaderView';
import ResultView from './Components/ResultView';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import ResultViewBillOfLading from './Components/ResultViewBillOfLading';

// Define interfaces for the state types
interface DownloadingProgress {
  downloadStatus: boolean;
  progress: number;
}

interface DetectedDataProps {
  barcode: boolean;
  qrcode: boolean;
  text: boolean;
  document: boolean;
}

const App: React.FC = () => {
  const visionSdk = useRef<VisionSdkRefProps>(null);
  const [captureMode, setCaptureMode] = useState<'manual' | 'auto'>('manual');
  const [ocrMode, setOcrMode] = useState<
    'cloud' | 'on-device' | 'on-device-with-translation' | 'bill-of-lading'
  >('cloud');
  const [modelSize, setModelSize] = useState<string>('large');
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<string>('');
  const [mode, setMode] = useState<'barcode' | 'qrcode' | 'ocr' | 'photo'>(
    'barcode'
  );
  const [flash, setFlash] = useState<boolean>(false);
  const [zoomLevel, setZoomLevel] = useState<number>(1.8);
  const [detectedData, setDetectedData] = useState<DetectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
    document: false,
  });
  const [modelDownloadingProgress, setModelDownloadingProgress] =
    useState<DownloadingProgress>({
      downloadStatus: true,
      progress: 0,
    });

  // Request camera permission on component mount (for Android)
  useEffect(() => {
    const requestCameraPermission = async () => {
      const cameraPermission =
        Platform.OS === 'ios'
          ? PERMISSIONS.IOS.CAMERA
          : PERMISSIONS.ANDROID.CAMERA;

      const result = await request(cameraPermission);
      if (result !== RESULTS.GRANTED) {
        Alert.alert(
          'Camera Permission Error',
          'App needs camera permission to take pictures. Please go to app settings and enable camera permission.'
        );
      }
    };

    if (Platform.OS === 'android') {
      requestCameraPermission();
    }
  }, []);

  // Configure Vision SDK settings
  useEffect(() => {
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

  // Capture photo when the button is pressed
  const onPressCapture = () => {
    if (mode === 'ocr') {
      setLoading(true);
      visionSdk?.current?.cameraCaptureHandler();
      return;
    }

    visionSdk.current?.restartScanningHandler();

    visionSdk?.current?.cameraCaptureHandler();
  };
  // Toggle flash functionality
  const toggleFlash = (val: boolean) => {
    setFlash(val);
  };
  // Function to configure on-device OCR
  const onPressOnDeviceOcr = (type = 'shipping_label', size = 'large') => {
    visionSdk?.current?.stopRunningHandler();
    setLoading(true);
    visionSdk?.current?.configureOnDeviceModel({
      type,
      size,
    });
  };
  return (
    <View style={styles.mainContainer}>
      <VisionSdkView
        ref={visionSdk}
        ocrMode={ocrMode}
        captureMode={captureMode}
        mode={mode}
        environment="sandbox"
        locationId="loc_w7rRdYB4zjC6PTXnqnLEtF"
        apiKey="key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z" // sandbox
        // apiKey="key_89a819bbe4eMsZqh3lU3QJ4iKH8YtFA0J9Muee6I7Ss3VL3sgu99mRStS5hmol0Xd0ow9UMdvVjTXjg5" //dev
        flash={flash}
        zoomLevel={zoomLevel}
        onDetected={(event) => {
          console.log('onDetected', event);
          setDetectedData(event);
        }}
        onBarcodeScan={(event) => {
          console.log('onBarcodeScan', event);
          setLoading(false);
          visionSdk.current?.restartScanningHandler();
        }}
        onOCRScan={(event) => {
          console.log('onOCRScan', event?.data);
          setLoading(false);
          setResult(event.data);
          Vibration.vibrate(100);
          visionSdk.current?.restartScanningHandler();
        }}
        onImageCaptured={(event) => {
          console.log('onImageCaptured', event);
          visionSdk.current?.restartScanningHandler();
        }}
        onModelDownloadProgress={(event) => {
          console.log('onModelDownloadProgress', event);
          setModelDownloadingProgress(event);
          if (event.downloadStatus) {
            visionSdk.current?.startRunningHandler();
            setLoading(false);
          }
        }}
        onError={(error) => {
          console.log('onError', error);
          Alert.alert('ERROR', error?.message);
          setLoading(false);
        }}
      />
      {mode == 'ocr' && ocrMode == 'bill-of-lading' ? (
        <ResultViewBillOfLading
          visible={!!result}
          result={result}
          setResult={setResult}
        />
      ) : mode == 'ocr' ? (
        <ResultView visible={!!result} result={result} setResult={setResult} />
      ) : null}
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
};
const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
  },
});
export default App;
