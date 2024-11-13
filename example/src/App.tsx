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
        environment="staging"
        locationId="loc_w7rRdYB4zjC6PTXnqnLEtF"
        token="eyJhbGciOiJSUzI1NiIsImtpZCI6IjFlNTIxYmY1ZjdhNDAwOGMzYmQ3MjFmMzk2OTcwOWI1MzY0MzA5NjEiLCJ0eXAiOiJKV1QifQ.eyJvcmciOiJvcmdfMTg2V3h5WGU0WVFSQ3FaVmFiYzlwaCIsInJvbGUiOiJyb2xlX293bmVyIiwic2NvcGVzIjp7Im9yZ2FuaXphdGlvbnMiOjIsInNoaXBtZW50cyI6MiwibG9jYXRpb25zIjoyLCJ1c2VycyI6MiwicGF5bWVudHMiOjIsInBheW1lbnRfbWV0aG9kcyI6MiwiZGVsaXZlcmllcyI6Miwid2ViaG9va3MiOjIsImFwaV9rZXlzIjoyLCJpdGVtcyI6MiwiYXNzZXRzIjoyLCJmdWxmaWxsbWVudHMiOjIsImNvbnRhY3RzIjoyLCJhZGRyZXNzZXMiOjIsIm1hbmlmZXN0cyI6MiwiaW5mZXJlbmNlcyI6MiwiYXVkaXRzIjoyLCJzY2FucyI6MiwiZXZlbnRzIjoyLCJjb250YWluZXJzIjoyLCJ0aHJlYWRzIjoyLCJhbmFseXRpY3MiOjIsInRyYWNrZXJzIjoyLCJncm91cHMiOjIsImFkbWluX2FpIjowLCJhaSI6MCwic2RrIjowLCJsb3RzIjowLCJzZXF1ZW5jZXMiOjJ9LCJuYW1lIjoiWmFoZWVyIiwicHJvZmlsZV9pZCI6InByb2ZfclF1WkFFcTFZMUQyTDNjSnNvcFZ0RSIsImxvZ2luX2NvZGUiOnRydWUsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9weC1wbGF0Zm9ybS1zdGFnaW5nIiwiYXVkIjoicHgtcGxhdGZvcm0tc3RhZ2luZyIsImF1dGhfdGltZSI6MTczMDE5NzgwOCwidXNlcl9pZCI6InVzZXJfaTNvbU1YemR2WFpOR00yNTRuQ1F4MiIsInN1YiI6InVzZXJfaTNvbU1YemR2WFpOR00yNTRuQ1F4MiIsImlhdCI6MTczMTQ5NTEyNCwiZXhwIjoxNzMxNDk4NzI0LCJlbWFpbCI6InphaGVlci5yYXNoZWVkQHBhY2thZ2V4LmlvIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsic2FtbC5zc29fNjJrQzhXOTRITVR1Qkt3aUplc0RhNyI6WyJ6YWhlZXIucmFzaGVlZEBwYWNrYWdleC5pbyJdLCJlbWFpbCI6WyJ6YWhlZXIucmFzaGVlZEBwYWNrYWdleC5pbyJdfSwic2lnbl9pbl9wcm92aWRlciI6ImN1c3RvbSJ9fQ.Um4UYvOAEJGbPnOiNApB8Cobwkw4WRdrmC0v5fAnbj8tc8OhOTegDJ4pE-3B0MFuZSuboPe9NTKPUT6pWAvsiC9QzR-7Tlq9E8XOHaePCl6LcOAsQKxExBxhENguyg9vhzGvzsXP4r6UfipfQkn5sxLGCoQmMqt80V4Zz8QoG5aOA7HasbOf-3b2TeaWKZDq9DL2ftJAV5zTbBIl7uG7vDWxAdayaDgWjOxZy1WYWP79eROG5q8oOyzm-7x66fu9SwHwiMyl6vF53mNpFmuaTY_idmCIYcAN0mZKZ5qHbguBxjOoJ4um6mkPS59zwF17y6Ae0bpfzYdw44maZ-2qsg"
        // apiKey="key_141b2eda27Z0Cm2y0h0P6waB3Z6pjPgrmGAHNSU62rZelUthBEOOdsVTqZQCRVgPLqI5yMPqpw2ZBy2z" // sandbox
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
