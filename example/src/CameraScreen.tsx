import React, { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, Platform, Alert, Vibration, useWindowDimensions, Text, View, Image, TouchableOpacity, InteractionManager } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
// import { GestureHandlerRootView } from 'react-native-gesture-handler'
import VisionSdkView, {
  VisionSdkRefProps,
  ModuleType,
  ModuleSize,
  OCRConfig,
  BoundingBoxesDetectedResult,
  VisionCore,
  TemplateData
} from '../../src/index';
import CameraFooterView from './Components/CameraFooterView';
import DownloadingProgressView from './Components/DownloadingProgressView';
import CameraHeaderView from './Components/CameraHeaderView';
import LoaderView from './Components/LoaderView';
import { PERMISSIONS, RESULTS, request, check } from 'react-native-permissions';
import ResultViewOCR from './Components/ResultViewOCR';
import { useFocusEffect } from '@react-navigation/native';


const apiKey = "" // Add your PackageX API key here

// Template storage key
const TEMPLATES_STORAGE_KEY = '@vision_sdk_templates';

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


const CameraScreenComponent: React.FC<{ route: any }> = ({ route }) => {
  // console.log('🏁 CameraScreen: Component render START', Date.now());

  const visionSdk = useRef<VisionSdkRefProps>(null);
  const [captureMode, setCaptureMode] = useState<'manual' | 'auto'>('manual');
  const timeoutRef = useRef<any>(null);
  const boundingBoxTimeoutRef = useRef<any>(null);
  const BOUNDING_BOX_AUTO_CLEAR_DELAY = 2000; // 2 seconds
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
  const [reportErrorStatus, setReportErrorStatus] = useState<'idle' | 'success' | 'error'>('idle')
  const [isCameraRunning, setIsCameraRunning] = useState(true)

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
  const [cameraFacing, setCameraFacing] = useState<'back' | 'front'>('back');
  const [availableTemplates, setAvailableTemplates] = useState<TemplateData[]>([])
  const [selectedTemplate, setSelectedTemplate] = useState<TemplateData | null>(null)
  const [detectedData, setDetectedData] = useState<DetectedDataProps>({
    barcode: false,
    qrcode: false,
    text: false,
    document: false,
  });
  const [sharpnessScore, setSharpnessScore] = useState<number>(0);

  // Throttle sharpness updates (update at most every 200ms)
  const lastSharpnessUpdate = useRef<number>(0);
  const sharpnessThrottleMs = 200;

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

  // Template storage helper functions
  const loadTemplatesFromStorage = useCallback(async () => {
    try {
      const stored = await AsyncStorage.getItem(TEMPLATES_STORAGE_KEY);
      if (stored) {
        const templates = JSON.parse(stored);
        setAvailableTemplates(templates);
      }
    } catch (error) {
      console.error('Failed to load templates from storage:', error);
    }
  }, []);

  const saveTemplatesToStorage = useCallback(async (templates: TemplateData[]) => {
    try {
      await AsyncStorage.setItem(TEMPLATES_STORAGE_KEY, JSON.stringify(templates));
    } catch (error) {
      console.error('Failed to save templates to storage:', error);
    }
  }, []);

  const addTemplate = useCallback(async (templateJson: string) => {
    try {
      const template: TemplateData = JSON.parse(templateJson);
      const updatedTemplates = [...availableTemplates, template];
      // Remove duplicates by ID
      const uniqueTemplates = updatedTemplates.filter(
        (t, index, self) => index === self.findIndex((temp) => temp.id === t.id)
      );
      setAvailableTemplates(uniqueTemplates);
      await saveTemplatesToStorage(uniqueTemplates);
      return template;
    } catch (error) {
      console.error('Failed to add template:', error);
      throw error;
    }
  }, [availableTemplates, saveTemplatesToStorage]);

  const deleteTemplate = useCallback(async (templateId: string) => {
    try {
      const updatedTemplates = availableTemplates.filter((t) => t.id !== templateId);
      setAvailableTemplates(updatedTemplates);
      await saveTemplatesToStorage(updatedTemplates);

      // If deleted template was selected, clear selection
      if (selectedTemplate?.id === templateId) {
        setSelectedTemplate(null);
      }
    } catch (error) {
      console.error('Failed to delete template:', error);
    }
  }, [availableTemplates, selectedTemplate, saveTemplatesToStorage]);

  const deleteAllTemplates = useCallback(async () => {
    try {
      setAvailableTemplates([]);
      setSelectedTemplate(null);
      await AsyncStorage.removeItem(TEMPLATES_STORAGE_KEY);
    } catch (error) {
      console.error('Failed to delete all templates:', error);
    }
  }, []);

  // Helper function to clear bounding boxes
  const clearBoundingBoxes = useCallback(() => {
    // Cancel any pending auto-clear timeout
    if (boundingBoxTimeoutRef.current) {
      clearTimeout(boundingBoxTimeoutRef.current);
      boundingBoxTimeoutRef.current = null;
    }

    setDetectedBoundingBoxes({
      barcodeBoundingBoxes: [],
      qrCodeBoundingBoxes: [],
      documentBoundingBox: { x: 0, y: 0, width: 0, height: 0 }
    });
  }, []);

  const requestCameraPermission = useCallback(async () => {
    console.log('⏱️ requestCameraPermission: START', Date.now());
    const cameraPermission =
      Platform.OS === 'ios'
        ? PERMISSIONS.IOS.CAMERA
        : PERMISSIONS.ANDROID.CAMERA;

    // First check if we already have permission (faster than request)
    let result = await check(cameraPermission);
    console.log('⏱️ requestCameraPermission: Check complete, result:', result, Date.now());

    // Only request if we don't have permission yet
    if (result !== RESULTS.GRANTED && result !== RESULTS.LIMITED) {
      result = await request(cameraPermission);
      console.log('⏱️ requestCameraPermission: Request complete, result:', result, Date.now());
    }

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

      console.log('⏱️ requestCameraPermission: Setting focus/detection/camera settings', Date.now());
      visionSdk?.current?.setFocusSettings(focusSettings);
      visionSdk?.current?.setObjectDetectionSettings(detectionSettings);
      visionSdk?.current?.setCameraSettings({
        nthFrameToProcess: 10
      });

      // Start camera immediately
      visionSdk?.current?.startRunningHandler();
      console.log('⏱️ requestCameraPermission: Camera started', Date.now());

      // Load templates from AsyncStorage
      console.log('⏱️ requestCameraPermission: Loading templates from storage', Date.now());
      loadTemplatesFromStorage();

      setLoading(false);
    }
  }, [scannerX, scannerY, scannerWidth, scannerHeight, detectionSettings, loadTemplatesFromStorage]);



  useFocusEffect(useCallback(() => {
    console.log('🎯 useFocusEffect: Called', Date.now());

    // Use InteractionManager to wait for animations/transitions to complete
    // This prevents camera init from blocking navigation animations
    const interactionPromise = InteractionManager.runAfterInteractions(() => {
      console.log('🎬 InteractionManager: Animations complete, starting camera', Date.now());
      requestCameraPermission()
    });

    return () => {
      // IMPORTANT: Cancel the interaction promise first
      interactionPromise.cancel();

      // Stop camera immediately - don't wait for anything
      visionSdk?.current?.stopRunningHandler();

      // Clear bounding boxes when navigating away
      clearBoundingBoxes();

      // Clear all timeouts
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
      if (boundingBoxTimeoutRef.current) {
        clearTimeout(boundingBoxTimeoutRef.current);
        boundingBoxTimeoutRef.current = null;
      }
    }
  }, [requestCameraPermission, clearBoundingBoxes]))


  useEffect(() => {
    if (modelDownloadingProgress.isReady) {
      setLoading(false);
    }
  }, [modelDownloadingProgress]);

  // Clear bounding boxes when mode changes
  useEffect(() => {
    console.log('📸 Mode changed to:', mode);
    clearBoundingBoxes();
  }, [mode, clearBoundingBoxes]);

  useEffect(() => {
    if (['on-device', 'on_device'].includes(ocrConfig.mode)) {
      handlePressOnDeviceOcr(ocrConfig.type, ocrConfig.size)
    }
  }, [ocrConfig])

  useEffect(() => {
    // Apply selected template to detection settings
    const updatedDetectionSettings: any = { ...detectionSettings }
    const newTemplateJson = selectedTemplate ? JSON.stringify(selectedTemplate) : '';
    const currentTemplateJson = (detectionSettings as any).selectedTemplate || '';

    updatedDetectionSettings.selectedTemplate = newTemplateJson;

    // Check if there's an actual change by comparing JSON strings
    const hasChanged = newTemplateJson !== currentTemplateJson;

    if (hasChanged) {
      // Clear old bounding boxes when template changes
      clearBoundingBoxes();

      setDetectionSettings(updatedDetectionSettings)
      visionSdk.current?.setObjectDetectionSettings(updatedDetectionSettings)
      console.log("✅ Template applied:", selectedTemplate?.id || 'none');
    } else {
      console.log("⏭️ Template unchanged, skipping update");
    }
  }, [selectedTemplate, clearBoundingBoxes])

  // Capture photo when the button is pressed
  const handlePressCapture = useCallback(() => {
    visionSdk?.current?.cameraCaptureHandler();
  }, [])





  const testReportError = (type: String) => {
    try {
      setReportErrorStatus('idle');
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

      // Set success status
      setReportErrorStatus('success');

      // Reset status after 3 seconds
      setTimeout(() => {
        setReportErrorStatus('idle');
      }, 3000);

    } catch (error) {
      console.error('Error calling reportError:', error);
      setReportErrorStatus('error');

      // Reset status after 3 seconds
      setTimeout(() => {
        setReportErrorStatus('idle');
      }, 3000);
    }
  }




  // Toggle flash functionality
  const toggleFlash = (val: boolean) => {
    setFlash(val);
  };

  // Toggle camera facing
  const toggleCameraFacing = () => {
    const newFacing = cameraFacing === 'back' ? 'front' : 'back';
    setCameraFacing(newFacing);
    visionSdk?.current?.setCameraSettings({
      nthFrameToProcess: 10,
      cameraPosition: newFacing === 'front' ? 2 : 1 // 1 = back, 2 = front based on VisionSDK.CameraPosition enum
    });
    visionSdk?.current?.startRunningHandler()
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
    // Alert.alert('ERROR', error?.message);
    setLoading(false);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleDetected = useCallback((event) => {
    // Only update if the data actually changed
    setDetectedData(prev => {
      const hasChanged = prev.barcode !== event.barcode ||
        prev.qrcode !== event.qrcode ||
        prev.text !== event.text ||
        prev.document !== event.document;

      if (hasChanged) {
        return event;
      }

      // No change, return previous state to prevent re-render
      return prev;
    });
  }, [])

  const handleBarcodeScan = useCallback((event) => {
    setLoading(false);
    console.log("BARCODE SCAN RESULT: ", JSON.stringify(event))

    // Show alert with barcode scan results
    const codes = event.codes || [];
    if (codes.length > 0) {
      const barcodeInfo = codes.map((code: any, index: number) =>
        `${index + 1}. ${code.value || code.scannedCode || 'Unknown'} (${code.type || code.symbology || 'Unknown type'})`
      ).join('\n');

      Alert.alert(
        'Barcode Scanned',
        `Found ${codes.length} barcode${codes.length > 1 ? 's' : ''}:\n\n${barcodeInfo}`,
        [{ text: 'OK', onPress: () => visionSdk.current?.restartScanningHandler() }]
      );
    } else {
      Alert.alert("No Barcode Found")
      visionSdk.current?.restartScanningHandler();
    }
  }, [])


  const syncSLpx = async (ocrEvent) => {
    try {
      VisionCore.setEnvironment("staging")
      const r = await VisionCore.logShippingLabelDataToPx(
        Platform.OS === 'android' ? `file://${ocrEvent.imagePath}` : ocrEvent.imagePath,
        ocrEvent?.data?.barcode_values || [],
        { data: ocrEvent.data },
        null,
        apiKey,
        null,
        null,
        { meta1: 'metaval1', meta2: 'metaval2' },
        null,
        null,
        true
      )

      console.log("SYNC SL PX SUCCESSFUL: ", r)
    } catch (err) {
      console.log("AN ERROR OCCURED: [SYNC SL PX]", err.message)
    }
  }

  const syncILpx = async (ocrEvent) => {
    try {
      VisionCore.setEnvironment("staging")
      const r = await VisionCore.logItemLabelDataToPx(
        Platform.OS === 'android' ? `file://${ocrEvent.imagePath}` : ocrEvent.imagePath,
        ocrEvent.data.inference.barcode_values || [],
        { data: ocrEvent.data },
        null,
        apiKey,
        true,
        { meta1: 'metaval1', meta2: 'metaval2' }
      )

      console.log("IL SYNC SUCCESSFUL: ", r)
    } catch (err) {
      console.log("AN ERROR OCCURED IL SYNC: ", err.message)
    }
  }

  const handleOcrScan = useCallback((event) => {
    console.log("OCR SCAN IS: \n", JSON.stringify(event.data))
    setLoading(false);
    setResult(event.data);
    // onReportError(event.data);
    Vibration.vibrate(100);
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleImageCaptured = useCallback((event) => {
    console.log("ON IMAGE CAPTURED: ", JSON.stringify(event))
    visionSdk.current?.restartScanningHandler();
  }, [])

  const handleSharpnessScore = useCallback((event) => {
    const now = Date.now();
    if (now - lastSharpnessUpdate.current >= sharpnessThrottleMs) {
      lastSharpnessUpdate.current = now;
      const sharpness = event.sharpnessScore;
      setSharpnessScore(sharpness);
    }
  }, [sharpnessThrottleMs])

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

  const handleCreateTemplate = useCallback(async (event) => {
    try {
      const templateJson = event.data;
      console.log('📝 Template created:', templateJson);

      // Filter out old API format (just template ID string without JSON structure)
      // Only process new API format (full JSON with templateCodes array)
      try {
        const parsed = JSON.parse(templateJson);
        if (!parsed.templateCodes || !Array.isArray(parsed.templateCodes)) {
          console.log('⏭️ Ignoring old API format (no templateCodes array)');
          return;
        }
      } catch (e) {
        console.log('⏭️ Ignoring invalid JSON format');
        return;
      }

      const template = await addTemplate(templateJson);
      Alert.alert(
        'Template Created',
        `Template "${template.id}" has been created successfully.`,
        [{ text: 'OK' }]
      );
    } catch (error) {
      console.error('Failed to create template:', error);
      Alert.alert('Error', 'Failed to create template. Please try again.');
    }
  }, [addTemplate])

  const handlePressCreateTemplate = useCallback(() => {
    visionSdk.current?.createTemplate()
  }, [])

  const handlePressDeleteTemplateById = useCallback((templateId: string) => {
    Alert.alert(
      'Delete Template',
      `Are you sure you want to delete "${templateId}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => deleteTemplate(templateId)
        }
      ]
    );
  }, [deleteTemplate])

  const handlePressDeleteAllTemplates = useCallback(() => {
    Alert.alert(
      'Delete All Templates',
      'Are you sure you want to delete all templates?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete All',
          style: 'destructive',
          onPress: () => deleteAllTemplates()
        }
      ]
    );
  }, [deleteAllTemplates])


  const handleBoundingBoxesDetected = useCallback((args) => {
    // Cancel any existing auto-clear timeout
    if (boundingBoxTimeoutRef.current) {
      clearTimeout(boundingBoxTimeoutRef.current);
    }

    // Data is already parsed by the wrapper layer
    // Only update if the data actually changed
    setDetectedBoundingBoxes(prev => {
      const barcodesChanged = JSON.stringify(prev.barcodeBoundingBoxes) !== JSON.stringify(args.barcodeBoundingBoxes);
      const qrCodesChanged = JSON.stringify(prev.qrCodeBoundingBoxes) !== JSON.stringify(args.qrCodeBoundingBoxes);
      const documentChanged = JSON.stringify(prev.documentBoundingBox) !== JSON.stringify(args.documentBoundingBox);

      if (barcodesChanged || qrCodesChanged || documentChanged) {
        return {
          barcodeBoundingBoxes: args.barcodeBoundingBoxes || [],
          qrCodeBoundingBoxes: args.qrCodeBoundingBoxes || [],
          documentBoundingBox: args.documentBoundingBox || { x: 0, y: 0, width: 0, height: 0 }
        };
      }

      // No change, return previous state to prevent re-render
      return prev;
    });

    // Set up auto-clear timeout - clear bounding boxes after delay if no new detections
    boundingBoxTimeoutRef.current = setTimeout(() => {
      clearBoundingBoxes();
    }, BOUNDING_BOX_AUTO_CLEAR_DELAY);
  }, [clearBoundingBoxes, BOUNDING_BOX_AUTO_CLEAR_DELAY])

  const handlePriceTagDetected = useCallback((args) => {
    console.log('🏷️ handlePriceTagDetected: Setting detectedPriceTag', Date.now());
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

  const handlePauseResume = useCallback(() => {
    if (isCameraRunning) {
      visionSdk?.current?.stopRunningHandler();
      setIsCameraRunning(false);
    } else {
      visionSdk?.current?.startRunningHandler();
      setIsCameraRunning(true);
    }
  }, [isCameraRunning])

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
        token={undefined}
        apiKey={apiKey}
        flash={flash}
        zoomLevel={zoomLevel}
        modelExecutionProviderAndroid='CPU'
        onDetected={handleDetected}
        onBarcodeScan={handleBarcodeScan}
        onCreateTemplate={handleCreateTemplate}
        onOCRScan={handleOcrScan}
        onImageCaptured={handleImageCaptured}
        onSharpnessScore={handleSharpnessScore}
        onModelDownloadProgress={handleModelDownloadProgress}
        onBoundingBoxesDetected={handleBoundingBoxesDetected}
        onPriceTagDetected={handlePriceTagDetected}
        onError={handleError}
      />

      {/* Pause/Resume Button for testing stop/start behavior */}
      <TouchableOpacity
        onPress={handlePauseResume}
        style={styles.pauseResumeButton}
      >
        <Text style={styles.pauseResumeText}>
          {isCameraRunning ? '⏸️ PAUSE' : '▶️ RESUME'}
        </Text>
      </TouchableOpacity>

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
        sharpnessScore={sharpnessScore}
        toggleFlash={toggleFlash}
        toggleCameraFacing={toggleCameraFacing}
        cameraFacing={cameraFacing}
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

      {/* Test Report Error Button */}
      <TouchableOpacity
        style={{
          position: 'absolute',
          bottom: 150,
          right: 20,
          backgroundColor: reportErrorStatus === 'success' ? '#4CAF50' :
            reportErrorStatus === 'error' ? '#f44336' : '#2196F3',
          padding: 15,
          borderRadius: 10,
          elevation: 5,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: 2 },
          shadowOpacity: 0.25,
          shadowRadius: 3.84,
        }}
        onPress={() => testReportError('shipping_label')}
      >
        <Text style={{ color: 'white', fontWeight: 'bold', fontSize: 12 }}>
          Test Report Error
          {reportErrorStatus === 'success' && ' ✓'}
          {reportErrorStatus === 'error' && ' ✗'}
        </Text>
      </TouchableOpacity>

      {detectedBoundingBoxes.barcodeBoundingBoxes?.length > 0 ?
        <>
          {detectedBoundingBoxes.barcodeBoundingBoxes.map((code, index) => (
            <View
              key={index}
              pointerEvents="none"
              style={{
                position: 'absolute',
                left: code.boundingBox.x,
                top: code.boundingBox.y,
                width: code.boundingBox.width,
                height: code.boundingBox.height,
                borderColor: '#00ff00',
                borderWidth: 2,
              }}
            >
              <Text style={{
                color: '#FFFFFF',
                fontSize: 10,
                fontWeight: 'bold',
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                padding: 4,
                borderRadius: 3,
                position: 'absolute',
                top: -20,
                left: 0,
              }}>
                {code.scannedCode} ({code.symbology})
              </Text>
            </View>
          ))}
        </> : null}

      {['qrcode', 'barCodeOrQRCode'].includes(mode) && detectedBoundingBoxes.qrCodeBoundingBoxes?.length > 0 ?
        <>
          {detectedBoundingBoxes.qrCodeBoundingBoxes.map((code, index) => (
            <View
              key={index}
              pointerEvents="none"
              style={{
                position: 'absolute',
                left: code.boundingBox.x,
                top: code.boundingBox.y,
                width: code.boundingBox.width,
                height: code.boundingBox.height,
                borderColor: '#00ff00',
                borderWidth: 2,
              }}
            >
              <Text style={{
                color: '#FFFFFF',
                fontSize: 10,
                fontWeight: 'bold',
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                padding: 4,
                borderRadius: 3,
                position: 'absolute',
                top: -20,
                left: 0,
              }}>
                {code.scannedCode}
              </Text>
            </View>
          ))}
        </> : null}


      {['ocr', 'photo'].includes(mode) && detectedBoundingBoxes.documentBoundingBox?.width > 0 ?
        <View
          pointerEvents="none"
          style={{
            position: 'absolute',
            left: detectedBoundingBoxes.documentBoundingBox.x,
            top: detectedBoundingBoxes.documentBoundingBox.y,
            width: detectedBoundingBoxes.documentBoundingBox.width,
            height: detectedBoundingBoxes.documentBoundingBox.height,
            borderColor: '#ff0000',
            borderWidth: 2,
            backgroundColor: 'rgba(255, 0, 0, 0.1)',
          }}
        >
          <Text style={{
            color: '#FFFFFF',
            fontSize: 10,
            fontWeight: 'bold',
            backgroundColor: 'rgba(255, 0, 0, 0.8)',
            padding: 4,
            borderRadius: 3,
            position: 'absolute',
            top: -20,
            left: 0,
          }}>
            Document
          </Text>
        </View>
        : null}

      {isPriceTagBoundingBoxVisible && detectedPriceTag.boundingBox.width > 0 ?
        <>

          <View
            pointerEvents="none"
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
  pauseResumeButton: {
    position: 'absolute',
    top: 100,
    right: 20,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 25,
    zIndex: 1000,
  },
  pauseResumeText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

// Memoize component to prevent unnecessary re-renders when parent re-renders
const App = React.memo(CameraScreenComponent);

export default App;
