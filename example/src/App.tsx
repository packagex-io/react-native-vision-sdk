import React, { useCallback, useEffect, useRef, useState } from 'react';
import { View, StyleSheet, Platform, Alert, Vibration } from 'react-native';
import VisionSdkView, {
  VisionSdkRefProps,
  ModuleType,
  ModuleSize,
  OCRConfig,
} from '../../src/index';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';
import LoaderView from './Components/LoaderView';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import ResultViewOCR from './Components/ResultViewOCR';

// Define interfaces for the state types
interface DownloadingProgress {
  downloadStatus: boolean;
  progress: number;
}

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
  }
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
  const [ocrConfig, setOcrConfig] = useState<OCRConfig>({
    mode: 'cloud',
    type: 'shipping-label',
    size: 'large'
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<any>(null);
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
      progress: 1,
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
      } else {
        console.log("CAMERA PERMISSION IS GRANTED")
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
      }
    };

    // if (Platform.OS === 'android') {
    requestCameraPermission();
    // }

    return () => {
      visionSdk?.current?.stopRunningHandler();
    };
  }, []);

  // Configure Vision SDK settings
  // useEffect(() => {



  // }, []);

  useEffect(() => {
    if (modelDownloadingProgress.downloadStatus) {
      setLoading(false);
    }
  }, [modelDownloadingProgress]);

  useEffect(() => {
    if (['on-device', 'on_device'].includes(ocrConfig.mode)) {
      handlePressOnDeviceOcr(ocrConfig.type, ocrConfig.size)
    }
  }, [ocrConfig])

  // Capture photo when the button is pressed
  const handlePressCapture = useCallback(() => {
    // if (mode === 'ocr') {
    //   visionSdk?.current?.cameraCaptureHandler();
    //   return;
    // }

    // visionSdk.current?.restartScanningHandler();

    visionSdk?.current?.cameraCaptureHandler();
  }, [])

  // Toggle flash functionality
  const toggleFlash = (val: boolean) => {
    setFlash(val);
  };
  // Function to configure on-device OCR
  const handlePressOnDeviceOcr = useCallback((type: ModuleType = 'shipping_label',
    size: ModuleSize = 'large') => {
    visionSdk?.current?.stopRunningHandler();
    setLoading(true);
    visionSdk?.current?.configureOnDeviceModel({
      type,
      size,
    });
  }, [])

  const onReportError = useCallback((response) => {
    visionSdk.current?.reportError({
      reportText: 'respose is not correct',
      type: 'document_classification',
      size: 'large',
      response: response,
      image: response?.image_url ?? ''
    });
  }, [])


  const handleError = useCallback((error) => {
    console.log('onError', error);
    Alert.alert('ERROR', error?.message);
    setLoading(false);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleDetected = useCallback((event) => {
    // console.log('onDetected', event);
    setDetectedData(event);
  }, [])

  const handleBarcodeScan = useCallback((event) => {
    console.log("=======================")
    console.log('onBarcodeScan', JSON.stringify(event));
    console.log("=======================")
    setLoading(false);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleOcrScan = useCallback((event) => {
    setLoading(false);
    setResult(event.data);
    // onReportError(event.data);
    Vibration.vibrate(100);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleImageCaptured = useCallback((event) => {
    console.log('onImageCaptured', event);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleModelDownloadProgress = useCallback((event) => {
    // console.log('onModelDownloadProgress', event);
    setModelDownloadingProgress(event);
    if (event.downloadStatus) {
      visionSdk.current?.startRunningHandler();
      setLoading(false);
    }
  }, [])

  return (
    <View style={styles.mainContainer}>
      <VisionSdkView
        environment='staging'
        ref={visionSdk}
        ocrMode={ocrConfig.mode}
        ocrType={ocrConfig.type}
        captureMode={captureMode}
        isMultipleScanEnabled={true}
        mode={mode}
        options={{}}
        isEnableAutoOcrResponseWithImage={true}
        locationId=""
        token=""
        apiKey="key_00203c5642F9SYnJkKyi9dRw1eeteeUwXhbEfGuPZ4NML8l2bAfysni4ZpcZEBKn0gnbcOZYwIaJnOyp"
        flash={flash}
        zoomLevel={zoomLevel}
        onDetected={handleDetected}
        onBarcodeScan={handleBarcodeScan}
        onCreateTemplate={(event) => console.log(event)}
        onOCRScan={handleOcrScan}
        onImageCaptured={handleImageCaptured}
        onModelDownloadProgress={handleModelDownloadProgress}
        onError={handleError}
      />
      {mode == 'ocr' ? (
        <ResultViewOCR
          mode={ocrConfig.mode}
          visible={!!result}
          result={result}
          setResult={setResult}
          onReportError={(respose) => {
            onReportError(respose);
          }}
        />
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

        ocrConfig={ocrConfig}
        setOcrConfig={setOcrConfig}



        onPressCapture={handlePressCapture}
        onPressOnDeviceOcr={handlePressOnDeviceOcr}


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
