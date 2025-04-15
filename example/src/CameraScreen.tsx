import React, { useCallback, useEffect, useRef, useState } from 'react';
import { View, StyleSheet, Platform, Alert, Vibration, useWindowDimensions } from 'react-native';
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
import { useFocusEffect } from '@react-navigation/native';

const apiKey = ""

// Define interfaces for the state types
interface DownloadingProgress {
  downloadStatus: boolean;
  progress: number;
  isReady: boolean;
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

const App: React.FC<{ route: any }> = ({ route }) => {
  const visionSdk = useRef<VisionSdkRefProps>(null);
  const [captureMode, setCaptureMode] = useState<'manual' | 'auto'>('manual');
  const [shouldResizeImage, setShouldResizeImage] = useState(false)
  const [ocrConfig, setOcrConfig] = useState<OCRConfig>({
    mode: route.params?.modelType ? 'on-device' : 'cloud',
    type: route.params?.modelType || 'shipping-label',
    size: route.params?.modelSize || 'large'
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<any>(null);
  const [mode, setMode] = useState<'barcode' | 'qrcode' | 'ocr' | 'photo'>(
    route?.params?.mode || 'barcode'
  );
  const [flash, setFlash] = useState<boolean>(false);
  const [zoomLevel, setZoomLevel] = useState<number>(1.8);
  const [detectedData, setDetectedData] = useState<DetectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
    document: false,
  });

  const { width: screenWidth, height: screenHeight } = useWindowDimensions()
const scannerWidth = screenWidth * 0.8;
const scannerHeight = 160;
const scannerX = (screenWidth - scannerWidth) / 2;
const scannerY = (screenHeight - scannerHeight) / 2;

  const [modelDownloadingProgress, setModelDownloadingProgress] =
    useState<DownloadingProgress>({
      downloadStatus: false,
      progress: 0,
      isReady: false
    });

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
      const focusSettings = {
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
        focusImageRect: { x: scannerX, y: scannerY, width: scannerWidth, height: scannerHeight },

      }
      visionSdk?.current?.setFocusSettings(focusSettings);
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

      setTimeout(() => {
        visionSdk?.current?.startRunningHandler();
      }, 0)

      setLoading(false);
    }
  };



  useFocusEffect(useCallback(() => {
    requestCameraPermission()
    return () => {
      visionSdk?.current?.stopRunningHandler();
    }
  }, []))


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
    visionSdk?.current?.cameraCaptureHandler();
  }, [])


  const testReportError = (type: String) => {
    switch (type) {
      case 'shipping_label':
        visionSdk.current?.reportError({
          reportText: 'respose is not correct',
          type: 'shipping_label',
          size: 'large',
          response: {},
          image: '',
          errorFlags: {
            trackingNo: true,
            courierName: false,
            weight: true,
            dimensions: false,
            receiverName: true,
            receiverAddress: true,
            senderName: false,
            senderAddres: false
          }
        }, "", apiKey)
        break;

      case 'item_label':
        visionSdk.current?.reportError({
          reportText: 'respose is not correct',
          type: 'item_label',
          size: 'large',
          response: {},
          image: '',
          errorFlags: {
            supplierName: true,
            itemName: false,
            itemSKU: true,
            weight: true,
            quantity: true,
            dimensions: true,
            productionDate: false,
            supplierAddress: true
          }
        }, "", apiKey)
        break;

      case 'BOL':
        visionSdk.current?.reportError({
          reportText: 'respose is not correct',
          type: 'bill_of_lading',
          size: 'large',
          response: {},
          image: '',
          errorFlags: {
            referenceNo: true,
            loadNumber: false,
            purchaseOrderNumber: true,
            invoiceNumber: true,
            customerPurchaseOrderNumber: false,
            orderNumber: true,
            billOfLading: false,
            masterBillOfLading: false,
            lineBillOfLading: false,
            houseBillOfLading: true,
            shippingId: false,
            shippingDate: true,
            date: false
          }
        }, "", apiKey)
        break;

      case 'DC':
        visionSdk.current?.reportError({
          reportText: 'respose is not correct',
          type: 'document_classification',
          size: 'large',
          response: {},
          image: '',
          errorFlags: {
            documentClass: true
          }
        }, "", apiKey);
        break;
      default:
        break;
    }
  }




  // Toggle flash functionality
  const toggleFlash = (val: boolean) => {
    setFlash(val);
  };
  // Function to configure on-device OCR
  const handlePressOnDeviceOcr = useCallback((type: ModuleType = 'shipping_label',
    size: ModuleSize = 'large') => {
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

  const handleBarcodeScan = (event) => {
    console.log("=======================")
    console.log('onBarcodeScan', JSON.stringify(event));
    console.log("=======================")
    setLoading(false);

    visionSdk.current?.restartScanningHandler();
  }

  const handleOcrScan = useCallback((event) => {
    setLoading(false);
    setResult(event.data);
    // onReportError(event.data);
    Vibration.vibrate(100);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleImageCaptured = useCallback((event) => {

    console.log('onImageCaptured', event);
    // console.log("CALLING GET PREDICTION shiping label ")
    // visionSdk.current?.getPredictionShippingLabelCloud(event.image, event.barcodes, "", apiKey)
    // visionSdk.current?.getPredictionBillOfLadingCloud(event.image,event.barcodes,"",apiKey)
    // visionSdk?.current?.getPredictionItemLabelCloud(event.image, "", apiKey)
    // visionSdk?.current?.getPredictionDocumentClassificationCloud(event.image, "", apiKey)
    // visionSdk.current?.getPredictionWithCloudTransformations(event.image, event.barcodes, "", apiKey)
    // console.log("TESTING REPORT ERROR")
    // testReportError('shipping_label')
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleModelDownloadProgress = useCallback((event) => {
    setModelDownloadingProgress(event);
    if (event.downloadStatus && event.isReady) {
      setLoading(false);
    }
  }, [])

  const handleSetOcrConfig = useCallback((ocrConfig: OCRConfig) => {
    //the following if else block is just to check if shouldResizeImage prop is actually getting set or not
    if (ocrConfig.type === 'bill-of-lading' && ocrConfig.mode === 'cloud') {
      setShouldResizeImage(true)
    } else {
      setShouldResizeImage(false)
    }
    setOcrConfig(ocrConfig);
  }, [])


  return (
    <View style={styles.mainContainer}>
      <VisionSdkView
        environment='staging'
        ref={visionSdk}
        ocrMode={ocrConfig.mode}
        ocrType={ocrConfig.type}
        shouldResizeImage={shouldResizeImage}
        captureMode={captureMode}
        isMultipleScanEnabled={false}
        mode={mode}
        options={{}}
        isEnableAutoOcrResponseWithImage={true}
        locationId=""
        token=""
        apiKey={apiKey}
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
      {['ocr', 'photo'].includes(mode) ? (
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
      {/* <LoaderView visible={loading} /> */}
      <CameraHeaderView
        detectedData={detectedData}
        toggleFlash={toggleFlash}
        mode={mode}
        setMode={setMode}
      />
      <DownloadingProgressView
        visible={loading && !modelDownloadingProgress.isReady}
        progress={modelDownloadingProgress?.progress}
      />
      <CameraFooterView
        setCaptureMode={setCaptureMode}
        captureMode={captureMode}
        ocrConfig={ocrConfig}
        setOcrConfig={handleSetOcrConfig}
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
