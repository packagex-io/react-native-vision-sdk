import React, { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, Platform, Alert, Vibration, useWindowDimensions, Text, View } from 'react-native';
// import { GestureHandlerRootView } from 'react-native-gesture-handler'
import VisionSdkView, {
  VisionSdkRefProps,
  ModuleType,
  ModuleSize,
  OCRConfig,
  BoundingBoxesDetectedResult
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
  const timeoutRef = useRef<any>(null)
  const [detectionSettings, setDetectionSettings] = useState({
    isTextIndicationOn: true,
    isBarCodeOrQRCodeIndicationOn: true,
    isDocumentIndicationOn: true,
    codeDetectionConfidence: 0.5,
    documentDetectionConfidence: 0.5,
    secondsToWaitBeforeDocumentCapture: 2.0,
    selectedTemplate: ''
  })
  const [detectedBoundingBoxes, setDetectedBoundingBoxes] = useState<BoundingBoxesDetectedResult>({
    barcodeBoundingBoxes: [],
    qrCodeBoundingBoxes: [],
    documentBoundingBox: {
      x: 0,
      y: 0,
      width: 0,
      height: 0
    }
  })

  const [detectedPriceTag, setDetectedPriceTag] = useState({
    price: "",
    sku: "",
    boundingBox: {
      x: 0,
      y: 0,
      width: 0,
      height: 0
    }
  })

  const [isPriceTagBoundingBoxVisible, setIsPriceTagBoundingBoxVisible] = useState(false)

  const [shouldResizeImage, setShouldResizeImage] = useState(false)
  const [ocrConfig, setOcrConfig] = useState<OCRConfig>({
    mode: route.params?.modelType ? 'on-device' : 'cloud',
    type: route.params?.modelType || 'shipping-label',
    size: route.params?.modelSize || 'large'
  });
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<any>(null);
  const [mode, setMode] = useState<'barcode' | 'qrcode' | 'ocr' | 'photo' | 'barCodeOrQRCode'>(
    route?.params?.mode || 'barcode'
  );

  const [flash, setFlash] = useState<boolean>(false);
  const [zoomLevel, setZoomLevel] = useState<number>(1.8);
  const [availableTemplates, setAvailableTemplates] = useState([])
  const [selectedTemplate, setSelectedTemplate] = useState({})
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
        showCodeBoundariesInMultipleScan: false,
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
      // console.log("SETTING DETECTION SETTING TO: ", detectionSettings)
      visionSdk?.current?.setObjectDetectionSettings(detectionSettings);
      visionSdk?.current?.setCameraSettings({
        nthFrameToProcess: 10,
      });

      setTimeout(() => {
        visionSdk?.current?.startRunningHandler();
        console.log("GETTING ALL TEMPLATES")
        visionSdk?.current?.getAllTemplates()
      }, 0)

      setLoading(false);
    }
  };



  useFocusEffect(useCallback(() => {
    requestCameraPermission()
    return () => {
      visionSdk?.current?.stopRunningHandler();
      if(timeoutRef.current){
        clearTimeout(timeoutRef.current)
      }
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

  useEffect(() => {
    let updatedDetectionSettings = detectionSettings
    if (selectedTemplate?.name) {
      updatedDetectionSettings = {
        ...detectionSettings,
        selectedTemplateId: selectedTemplate.name
      }
    } else {
      const { selectedTemplateId, ...rest } = detectionSettings
      updatedDetectionSettings = rest
    }
    setDetectionSettings(updatedDetectionSettings)
  }, [selectedTemplate])

  useEffect(() => {
    visionSdk.current?.setObjectDetectionSettings(detectionSettings);
  }, [detectionSettings])

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

  const handleBarcodeScan = useCallback( (event) => {
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

  const handleCreateTemplate = useCallback((event) => {
    console.log("HANDLE CREATE TEMPLATE: ", JSON.stringify(event))
    const templates = [...availableTemplates, { name: event.data }]
    const uniqueTemplates = [...new Map(templates.map(item => [item.name, item])).values()]
    setAvailableTemplates(uniqueTemplates)

  }, [availableTemplates])

  const onDeleteTemplateById = (event) => {
    console.log("ON DELETE TEMPLATE BY ID: ", JSON.stringify(event))
    const updatedTemplates = availableTemplates.filter((item) => item.name !== event.data)
    setAvailableTemplates(updatedTemplates)
    // visionSdk.current?.stopRunningHandler()
    // visionSdk.current?.startRunningHandler()
  }


  const onDeleteAllTemplates = (event) => {

    setSelectedTemplate({})
    setAvailableTemplates([])
  }

  const handleGetTemplates = useCallback((args) => {
    setAvailableTemplates(args?.data?.map(item => ({ name: item })))
  }, [])

  const handlePressCreateTemplate = () => {

    visionSdk.current?.createTemplate()

  }

  const handlePressDeleteTemplateById = (templateId) => {
    visionSdk?.current?.deleteTemplateWithId(templateId)
  }

  const handlePressDeleteAllTemplates = () => {
    visionSdk?.current?.deleteAllTemplates()
  }

  const handleBoundingBoxesDetected = (args) => {
    // console.log("BOUNDING BOXES DETECTED: ", args)
    setDetectedBoundingBoxes(args)
  }

  const handlePriceTagDetected = useCallback((args) => {
    setDetectedPriceTag({
      price: args.price,
      sku: args.sku,
      boundingBox: args.boundingBox
    })

    setIsPriceTagBoundingBoxVisible(true)
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }
    timeoutRef.current = setTimeout(() => {
      setIsPriceTagBoundingBoxVisible(false)
      timeoutRef.current = null
    }, 1500)

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
        isMultipleScanEnabled={true}
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
        onCreateTemplate={handleCreateTemplate}
        onDeleteTemplateById={onDeleteTemplateById}
        onDeleteTemplates={onDeleteAllTemplates}
        onOCRScan={handleOcrScan}
        onImageCaptured={handleImageCaptured}
        onModelDownloadProgress={handleModelDownloadProgress}
        onGetTemplates={handleGetTemplates}
        onBoundingBoxesDetected={handleBoundingBoxesDetected}
        onPriceTagDetected={handlePriceTagDetected}
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
        templates={availableTemplates}
        selectedTemplate={selectedTemplate}
        setSelectedTemplate={setSelectedTemplate}
        onPressCreateTemplate={handlePressCreateTemplate}
        onPressDeleteTemplateById={handlePressDeleteTemplateById}
        onPressDeleteAllTemplates={handlePressDeleteAllTemplates}
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
      { ['barcode', 'barCodeOrQRCode'].includes(mode) &&  detectedBoundingBoxes.barcodeBoundingBoxes?.length > 0 ?
        <>
          {detectedBoundingBoxes.barcodeBoundingBoxes.map((item, index) => (
            <View
              key={index}
              style={{
                position: 'absolute',
                left: item.x,
                top: item.y,
                width: item.width,
                height: item.height,
                borderColor: '#00ff00',
                borderWidth: 2,
              }}
            />
          ))}
        </> : null}

      {['qrcode', 'barCodeOrQRCode'].includes(mode) &&  detectedBoundingBoxes.qrCodeBoundingBoxes?.length > 0 ?
        <>
          {detectedBoundingBoxes.qrCodeBoundingBoxes.map((item, index) => (
            <View
              key={index}
              style={{
                position: 'absolute',
                left: item.x,
                top: item.y,
                width: item.width,
                height: item.height,
                borderColor: '#00ff00',
                borderWidth: 2,
              }}
            />
          ))}
        </> : null}


      {isPriceTagBoundingBoxVisible && detectedPriceTag.boundingBox.width > 0 ?
        <>

          <View

            style={{
              position: 'absolute',
              left: detectedPriceTag.boundingBox.x,
              top: detectedPriceTag.boundingBox.y,
              width: detectedPriceTag.boundingBox.width,
              height: detectedPriceTag.boundingBox.height,
              borderColor: 'green',
              borderWidth: 2,
            }}
          />

        </> : null}


    </View>
  );
};
const styles = StyleSheet.create({
  mainContainer: {
    flex: 1,
    position: 'relative'
  },
});
export default App;
