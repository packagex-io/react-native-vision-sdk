import React, { useRef, useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Pressable,
  Alert,
  SafeAreaView,
  Image,
  ScrollView,
  Platform,
  Dimensions,
  Modal,
  FlatList,
} from 'react-native';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import { VisionCamera, VisionCameraRefProps, VisionCameraCaptureEvent, VisionCameraScanMode } from '../../src/VisionCamera';
import { VisionCore } from '../../src';
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { TemplateCode, TemplateData } from '../../src';

const TEMPLATES_STORAGE_KEY = '@vision_sdk_templates';
const CAPTURED_IMAGE_STORAGE_KEY = '@vision_sdk_captured_image';

const VisionCameraExample = ({ navigation }) => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [flashEnabled, setFlashEnabled] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1.0);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [lastCaptureEvent, setLastCaptureEvent] = useState<VisionCameraCaptureEvent | null>(null);
  const [scanMode, setScanMode] = useState<VisionCameraScanMode>('barcode');
  const [scanAreaEnabled, setScanAreaEnabled] = useState(false);
  const [autoCapture, setAutoCapture] = useState(false);
  const [recognitionData, setRecognitionData] = useState({ text: false, barcode: false, qrcode: false, document: false });
  const [sharpness, setSharpness] = useState(0);
  const [modeDropdownOpen, setModeDropdownOpen] = useState(false);
  const [barcodeResults, setBarcodeResults] = useState<any[]>([]);
  const [hasPermission, setHasPermission] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'back' | 'front'>('back');
  const [isCameraRunning, setIsCameraRunning] = useState(true);
  const [boundingBoxes, setBoundingBoxes] = useState<{
    barcodeBoundingBoxes: any[];
    qrCodeBoundingBoxes: any[];
    documentBoundingBox: any;
  }>({
    barcodeBoundingBoxes: [],
    qrCodeBoundingBoxes: [],
    documentBoundingBox: null,
  });

  // Template creation state
  const [isTemplateMode, setIsTemplateMode] = useState(false);
  const [templateCodes, setTemplateCodes] = useState<TemplateCode[]>([]);
  const [savedTemplates, setSavedTemplates] = useState<TemplateData[]>([]);
  const [showTemplateManager, setShowTemplateManager] = useState(false);
  const [activeTemplate, setActiveTemplate] = useState<TemplateData | null>(null);

  // Throttle sharpness updates (update at most every 200ms)
  const lastSharpnessUpdate = useRef<number>(0);
  const sharpnessThrottleMs = 200;

  // Throttle bounding boxes updates (update at most every 300ms)
  const lastBoundingBoxUpdate = useRef<number>(0);
  const boundingBoxThrottleMs = 300;

  // Clear stale bounding boxes if no update received within timeout
  const boundingBoxTimeoutMs = 1000;
  const boundingBoxTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const scanModes: { label: string; value: VisionCameraScanMode }[] = [
    { label: '📷 Photo', value: 'photo' },
    { label: '📊 Barcode', value: 'barcode' },
    { label: '🔲 QR Code', value: 'qrcode' },
    { label: '🔍 OCR', value: 'ocr' },
  ];

  const handleScanModeChange = (mode: VisionCameraScanMode) => {
    setScanMode(mode);
    setAutoCapture(false);
  };

  // --- Template Storage Helpers ---
  const loadTemplates = useCallback(async () => {
    try {
      const json = await AsyncStorage.getItem(TEMPLATES_STORAGE_KEY);
      if (json) {
        const templates = JSON.parse(json);
        setSavedTemplates(templates);
        console.log('=== All Available Templates ===');
        console.log('Count:', templates.length);
        console.log('Templates:', JSON.stringify(templates, null, 2));
      } else {
        console.log('=== All Available Templates ===');
        console.log('No templates found');
      }
    } catch (e) {
      console.error('Failed to load templates', e);
    }
  }, []);

  const persistTemplates = useCallback(async (templates: TemplateData[]) => {
    try {
      await AsyncStorage.setItem(TEMPLATES_STORAGE_KEY, JSON.stringify(templates));
      setSavedTemplates(templates);
    } catch (e) {
      console.error('Failed to save templates', e);
    }
  }, []);

  // --- Template Handlers ---
  const handleAddBarcodeToTemplate = useCallback((code: { scannedCode: string; symbology: string; boundingBox: { x: number; y: number; width: number; height: number } }) => {
    setTemplateCodes(prev => {
      const alreadyExists = prev.some(
        c => c.codeString === code.scannedCode && c.codeSymbology === code.symbology
      );
      if (alreadyExists) return prev;
      return [...prev, { codeString: code.scannedCode, codeSymbology: code.symbology, boundingBox: code.boundingBox }];
    });
  }, []);

  const handleRemoveCodeFromTemplate = useCallback((index: number) => {
    setTemplateCodes(prev => prev.filter((_, i) => i !== index));
  }, []);

  const handleSaveTemplate = useCallback(async () => {
    if (templateCodes.length === 0) return;
    const newTemplate: TemplateData = {
      id: `template_${Date.now()}`,
      templateCodes,
    };
    console.log('=== Template Created ===');
    console.log('Template ID:', newTemplate.id);
    console.log('Template Codes Count:', newTemplate.templateCodes.length);
    console.log('Template Data:', JSON.stringify(newTemplate, null, 2));
    const updated = [...savedTemplates, newTemplate];
    await persistTemplates(updated);
    setTemplateCodes([]);
    setIsTemplateMode(false);
    Alert.alert('Template Saved', `Template saved with ${newTemplate.templateCodes.length} code(s).`);
  }, [templateCodes, savedTemplates, persistTemplates]);

  const handleCancelTemplate = useCallback(() => {
    if (templateCodes.length > 0) {
      Alert.alert(
        'Discard Template?',
        `You have ${templateCodes.length} code(s) added. Discard?`,
        [
          { text: 'Keep Editing', style: 'cancel' },
          {
            text: 'Discard',
            style: 'destructive',
            onPress: () => {
              setTemplateCodes([]);
              setIsTemplateMode(false);
            },
          },
        ]
      );
    } else {
      setTemplateCodes([]);
      setIsTemplateMode(false);
    }
  }, [templateCodes]);

  const handleApplyTemplate = useCallback((template: TemplateData) => {
    if (activeTemplate?.id === template.id) {
      setActiveTemplate(null);
    } else {
      setActiveTemplate(template);
    }
  }, [activeTemplate]);

  const handleDeleteTemplate = useCallback(async (id: string) => {
    if (activeTemplate?.id === id) {
      setActiveTemplate(null);
    }
    const updated = savedTemplates.filter(t => t.id !== id);
    await persistTemplates(updated);
  }, [savedTemplates, persistTemplates, activeTemplate]);

  const handleDeleteAllTemplates = useCallback(() => {
    if (savedTemplates.length === 0) return;
    Alert.alert(
      'Delete All Templates?',
      `This will delete ${savedTemplates.length} template(s). This cannot be undone.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete All',
          style: 'destructive',
          onPress: () => {
            setActiveTemplate(null);
            persistTemplates([]);
          },
        },
      ]
    );
  }, [savedTemplates, persistTemplates]);

  const handleEnterTemplateMode = useCallback(() => {
    setShowTemplateManager(false);
    setTemplateCodes([]);
    setScanMode('barcode');
    setAutoCapture(false);
    setIsTemplateMode(true);
  }, []);

  const isCodeInTemplate = useCallback((scannedCode: string, symbology: string) => {
    return templateCodes.some(
      c => c.codeString === scannedCode && c.codeSymbology === symbology
    );
  }, [templateCodes]);

  // State to store camera view dimensions
  const [cameraViewSize, setCameraViewSize] = useState({ width: 0, height: 0 });

  // Get scan area based on current scan mode (when enabled)
  const getScanArea = () => {
    if (!scanAreaEnabled) return undefined;

    const { width: screenWidth } = Dimensions.get('window');
    const viewWidth = cameraViewSize.width || screenWidth;
    const viewHeight = cameraViewSize.height || 600;

    if (scanMode === 'barcode') {
      // Barcode: horizontal rectangle 300dp x 100dp
      const width = 300;
      const height = 100;
      const x = (viewWidth - width) / 2;
      const y = (viewHeight - height) / 2;
      return { x, y, width, height };
    } else if (scanMode === 'qrcode') {
      // QR Code: square 250dp x 250dp
      const size = 250;
      const x = (viewWidth - size) / 2;
      const y = (viewHeight - size) / 2;
      return { x, y, width: size, height: size };
    }

    return undefined;
  };

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
      setHasPermission(false);
    } else {
      setHasPermission(true);
    }
  };

  useEffect(() => {
    requestCameraPermission();
    loadTemplates();

    return () => {
      cameraRef.current?.stop();
      if (boundingBoxTimeoutRef.current) {
        clearTimeout(boundingBoxTimeoutRef.current);
      }
    };
  }, [loadTemplates]);

  const handleCapture = (event: VisionCameraCaptureEvent) => {
    setCapturedImage(event.image);
    setLastCaptureEvent(event);
  };

  const handleUseForModelTesting = async () => {
    if (!lastCaptureEvent) {
      Alert.alert('No Image', 'Please capture an image first.');
      return;
    }
    try {
      await AsyncStorage.setItem(CAPTURED_IMAGE_STORAGE_KEY, JSON.stringify(lastCaptureEvent));
      Alert.alert(
        'Image Saved',
        'Captured image saved for model testing. Navigate to Model Management to use it.',
        [
          { text: 'Stay Here', style: 'cancel' },
          { text: 'Go to Model Management', onPress: () => navigation.navigate('ModelManagementScreen') },
        ]
      );
    } catch (e) {
      console.error('Failed to save captured image', e);
      Alert.alert('Error', 'Failed to save captured image.');
    }
  };

  const handleError = (error: any) => {
    Alert.alert('Error', error.message);
  };

  const handleRecognitionUpdate = (event: any) => {
    setRecognitionData(event);
  };

  const handleSharpnessScoreUpdate = (event: any) => {
    const now = Date.now();
    if (now - lastSharpnessUpdate.current >= sharpnessThrottleMs) {
      lastSharpnessUpdate.current = now;
      setSharpness(event.sharpnessScore);
    }
  };

  const handleBarcodeDetected = (event: any) => {
    setBarcodeResults(event.codes);
    if (!isTemplateMode) {
      Alert.alert('Barcode Detected', `Found ${event.codes.length} barcode(s)`);
    }
  };

  const handleBoundingBoxesUpdate = (event: any) => {
    const now = Date.now();
    if (now - lastBoundingBoxUpdate.current >= boundingBoxThrottleMs) {
      lastBoundingBoxUpdate.current = now;
      setBoundingBoxes({
        barcodeBoundingBoxes: event.barcodeBoundingBoxes || [],
        qrCodeBoundingBoxes: event.qrCodeBoundingBoxes || [],
        documentBoundingBox: event.documentBoundingBox || null,
      });

      // Clear any existing timeout and set a new one to clear stale boxes
      if (boundingBoxTimeoutRef.current) {
        clearTimeout(boundingBoxTimeoutRef.current);
      }
      boundingBoxTimeoutRef.current = setTimeout(() => {
        setBoundingBoxes({
          barcodeBoundingBoxes: [],
          qrCodeBoundingBoxes: [],
          documentBoundingBox: null,
        });
      }, boundingBoxTimeoutMs);
    }
  };

  const onCapturePress = () => {
    cameraRef.current?.capture();
  };

  const onToggleFlash = async () => {

    try {

      setFlashEnabled(!flashEnabled);
    } catch (error) {
      console.error("error:", error);
    }
  };

  const onZoomIn = () => {
    setZoomLevel(Math.min(zoomLevel + 0.5, 5.0));
  };

  const onZoomOut = () => {
    setZoomLevel(Math.max(zoomLevel - 0.5, 1.0));
  };

  const onToggleCameraFacing = () => {
    setCameraFacing(prevFacing => prevFacing === 'back' ? 'front' : 'back');
  };

  const onStartCamera = () => {
    setIsCameraRunning(true);
    cameraRef.current?.start();
  };

  const onStopCamera = () => {
    setIsCameraRunning(false);
    cameraRef.current?.stop();
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={[styles.header, isTemplateMode && styles.headerTemplateMode]}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backButtonText}>← Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>{isTemplateMode ? 'Creating Template' : 'Vision Camera'}</Text>
        <TouchableOpacity
          style={styles.templateHeaderButton}
          onPress={() => setShowTemplateManager(true)}
          disabled={isTemplateMode}
        >
          <Text style={[styles.templateHeaderButtonText, isTemplateMode && { opacity: 0.4 }]}>Templates</Text>
        </TouchableOpacity>
      </View>

      <View
        style={styles.cameraContainer}
        onLayout={(event) => {
          const { width, height } = event.nativeEvent.layout;
          setCameraViewSize({ width, height });
        }}
      >
        {hasPermission ? (
          <VisionCamera
            ref={cameraRef}
            enableFlash={flashEnabled}
            zoomLevel={zoomLevel}
            scanMode={scanMode}
            autoCapture={autoCapture}
            cameraFacing={cameraFacing}
            scanArea={getScanArea()}
            template={activeTemplate}
            onCapture={handleCapture}
            onError={handleError}
            onRecognitionUpdate={handleRecognitionUpdate}
            onSharpnessScoreUpdate={handleSharpnessScoreUpdate}
            onBarcodeDetected={handleBarcodeDetected}
            detectionConfig={{
              text: true,
              barcode: true,
              document: true,
              documentConfidence: 0.4,  // Lower threshold for easier detection
              documentCaptureDelay: 0.5,  // Reduce delay from 2s to 0.5s for faster capture
            }}
            // frameSkip={15}
            onBoundingBoxesUpdate={handleBoundingBoxesUpdate}
          />
        ) : (
          <View style={styles.permissionContainer}>
            <Text style={styles.permissionText}>Camera permission required</Text>
            <TouchableOpacity
              style={styles.permissionButton}
              onPress={requestCameraPermission}
            >
              <Text style={styles.permissionButtonText}>Grant Permission</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Scan Area Overlay */}
        {scanAreaEnabled && getScanArea() && (
          <View style={styles.scanAreaOverlay} pointerEvents="none">
            <View style={[
              scanMode === 'qrcode' ? styles.qrScanAreaRect : styles.scanAreaRect,
              {
                left: getScanArea()!.x,
                top: getScanArea()!.y,
                width: getScanArea()!.width,
                height: getScanArea()!.height,
              }
            ]}>
              <Text style={styles.scanAreaLabel}>
                {scanMode === 'barcode' ? 'Barcode Scan Area' : 'QR Code Scan Area'}
              </Text>
            </View>
          </View>
        )}

        {/* Bounding Boxes Overlay */}
        <View style={[styles.boundingBoxesContainer, isTemplateMode && styles.boundingBoxesContainerTemplateMode]} pointerEvents={isTemplateMode ? 'box-none' : 'none'}>
          {/* For OCR mode with autoCapture, only show document box with translucent fill */}
          {scanMode === 'ocr' && autoCapture ? (
            <>
              {/* Document Bounding Box - Translucent fill */}
              {boundingBoxes.documentBoundingBox && boundingBoxes.documentBoundingBox.width > 0 && (
                <View
                  style={[
                    styles.boundingBox,
                    {
                      left: boundingBoxes.documentBoundingBox.x,
                      top: boundingBoxes.documentBoundingBox.y,
                      width: boundingBoxes.documentBoundingBox.width,
                      height: boundingBoxes.documentBoundingBox.height,
                      borderColor: '#4CAF50', // Green for documents
                      backgroundColor: 'rgba(76, 175, 80, 0.2)', // Translucent green fill
                    },
                  ]}
                />
              )}
            </>
          ) : (
            <>
              {/* Barcode Bounding Boxes */}
              {boundingBoxes.barcodeBoundingBoxes.map((code, index) => {
                const added = isTemplateMode && isCodeInTemplate(code.scannedCode, code.symbology);
                const boxContent = (
                  <View
                    key={`barcode-${index}`}
                    style={[
                      styles.boundingBox,
                      {
                        left: code.boundingBox.x,
                        top: code.boundingBox.y,
                        width: code.boundingBox.width,
                        height: code.boundingBox.height,
                        borderColor: added ? '#4CAF50' : '#FFEB3B',
                        borderWidth: added ? 3 : 2,
                      },
                    ]}
                  >
                    <Text style={styles.boundingBoxLabel}>
                      {code.scannedCode} ({code.symbology})
                    </Text>
                    {added && (
                      <View style={styles.addedBadge}>
                        <Text style={styles.addedBadgeText}>ADDED</Text>
                      </View>
                    )}
                  </View>
                );

                if (isTemplateMode) {
                  return (
                    <Pressable
                      key={`barcode-tap-${index}`}
                      style={({ pressed }) => [
                        {
                          position: 'absolute',
                          left: code.boundingBox.x,
                          top: code.boundingBox.y,
                          width: Math.max(code.boundingBox.width, 60),
                          height: Math.max(code.boundingBox.height, 40),
                          opacity: pressed ? 0.7 : 1,
                        },
                      ]}
                      onPress={() => handleAddBarcodeToTemplate(code)}
                    >
                      <View
                        style={[
                          styles.boundingBoxTappable,
                          {
                            borderColor: added ? '#4CAF50' : '#FFEB3B',
                            borderWidth: added ? 3 : 2,
                          },
                        ]}
                      >
                        <Text style={styles.boundingBoxLabel}>
                          {code.scannedCode} ({code.symbology})
                        </Text>
                        {added && (
                          <View style={styles.addedBadge}>
                            <Text style={styles.addedBadgeText}>ADDED</Text>
                          </View>
                        )}
                      </View>
                    </Pressable>
                  );
                }
                return boxContent;
              })}

              {/* QR Code Bounding Boxes - Cyan (pointerEvents="none" so they don't block touches) */}
              {boundingBoxes.qrCodeBoundingBoxes.map((code, index) => (
                <View
                  key={`qrcode-${index}`}
                  pointerEvents="none"
                  style={[
                    styles.boundingBox,
                    {
                      left: code.boundingBox.x,
                      top: code.boundingBox.y,
                      width: code.boundingBox.width,
                      height: code.boundingBox.height,
                      borderColor: '#00E5FF', // Cyan for QR codes
                    },
                  ]}
                >
                  <Text style={styles.boundingBoxLabel}>
                    {code.scannedCode}
                  </Text>
                </View>
              ))}

              {/* Document Bounding Box - Green (hidden in template mode, pointerEvents="none" so it doesn't block touches) */}
              {!isTemplateMode && boundingBoxes.documentBoundingBox && boundingBoxes.documentBoundingBox.width > 0 && (
                <View
                  pointerEvents="none"
                  style={[
                    styles.boundingBox,
                    {
                      left: boundingBoxes.documentBoundingBox.x,
                      top: boundingBoxes.documentBoundingBox.y,
                      width: boundingBoxes.documentBoundingBox.width,
                      height: boundingBoxes.documentBoundingBox.height,
                      borderColor: '#4CAF50', // Green for documents
                    },
                  ]}
                />
              )}
            </>
          )}
        </View>

        {/* Recognition Status Overlay - Top Left */}
        <View style={styles.recognitionOverlay}>
          <View style={styles.recognitionItem}>
            <Text style={styles.recognitionLabel}>📝 Text: </Text>
            <View style={[styles.indicator, recognitionData.text && styles.indicatorActive]} />
          </View>
          <View style={styles.recognitionItem}>
            <Text style={styles.recognitionLabel}>📊 Barcode: </Text>
            <View style={[styles.indicator, recognitionData.barcode && styles.indicatorActive]} />
          </View>
          <View style={styles.recognitionItem}>
            <Text style={styles.recognitionLabel}>🔲 QR: </Text>
            <View style={[styles.indicator, recognitionData.qrcode && styles.indicatorActive]} />
          </View>
          <View style={styles.recognitionItem}>
            <Text style={styles.recognitionLabel}>📄 Doc: </Text>
            <View style={[styles.indicator, recognitionData.document && styles.indicatorActive]} />
          </View>
          <View style={styles.recognitionItem}>
            <Text style={styles.recognitionLabel}>🎯 Sharpness: </Text>
            <Text style={styles.sharpnessValue}>{sharpness.toFixed(2)}</Text>
          </View>
        </View>

        {/* Scan Mode Dropdown - Below Recognition Status (hidden in template mode) */}
        {!isTemplateMode && (
          <View style={styles.modeDropdownOverlay}>
            <TouchableOpacity
              style={styles.modeDropdownButton}
              onPress={() => setModeDropdownOpen(!modeDropdownOpen)}
            >
              <Text style={styles.modeDropdownButtonText}>
                {scanModes.find(m => m.value === scanMode)?.label || 'Photo'}
              </Text>
              <Text style={styles.modeDropdownArrow}>{modeDropdownOpen ? '▲' : '▼'}</Text>
            </TouchableOpacity>

            {modeDropdownOpen && (
              <View style={styles.modeDropdownMenu}>
                {scanModes.map((mode) => (
                  <TouchableOpacity
                    key={mode.value}
                    style={[
                      styles.modeDropdownItem,
                      scanMode === mode.value && styles.modeDropdownItemActive,
                    ]}
                    onPress={() => {
                      handleScanModeChange(mode.value);
                      setModeDropdownOpen(false);
                    }}
                  >
                    <Text
                      style={[
                        styles.modeDropdownItemText,
                        scanMode === mode.value && styles.modeDropdownItemTextActive,
                      ]}
                    >
                      {mode.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            )}
          </View>
        )}

        {/* Control Buttons - Top Right */}
        <View style={styles.controlsOverlay}>
          <TouchableOpacity
            style={[
              styles.controlButtonOverlay,
              isCameraRunning ? styles.stopButton : styles.startButton
            ]}
            onPress={isCameraRunning ? onStopCamera : onStartCamera}
          >
            <Text style={styles.controlButtonTextOverlay}>
              {isCameraRunning ? '⏸️' : '▶️'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.controlButtonOverlay}
            onPress={onToggleFlash}
          >
            <Text style={styles.controlButtonTextOverlay}>
              {flashEnabled ? '⚡' : '⚡'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.controlButtonOverlay}
            onPress={onToggleCameraFacing}
          >
            <Text style={styles.controlButtonTextOverlay}>
              {cameraFacing === 'back' ? '📷' : '🤳'}
            </Text>
          </TouchableOpacity>

          <View style={styles.zoomControlsOverlay}>
            <TouchableOpacity
              style={styles.zoomButtonOverlay}
              onPress={onZoomIn}
              disabled={zoomLevel >= 5.0}
            >
              <Text style={[styles.zoomButtonTextOverlay, zoomLevel >= 5.0 && styles.zoomButtonDisabled]}>+</Text>
            </TouchableOpacity>
            <Text style={styles.zoomTextOverlay}>{zoomLevel.toFixed(1)}x</Text>
            <TouchableOpacity
              style={styles.zoomButtonOverlay}
              onPress={onZoomOut}
              disabled={zoomLevel <= 1.0}
            >
              <Text style={[styles.zoomButtonTextOverlay, zoomLevel <= 1.0 && styles.zoomButtonDisabled]}>-</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Bottom Controls - Auto Capture & Scan Area (hidden in template mode) */}
        {scanMode !== 'photo' && !isTemplateMode && (
          <View style={styles.bottomControlsOverlay}>
            {(scanMode === 'barcode' || scanMode === 'qrcode') && (
              <TouchableOpacity
                style={styles.bottomControlButton}
                onPress={() => setScanAreaEnabled(!scanAreaEnabled)}
              >
                <Text style={styles.bottomControlButtonText}>
                  {scanAreaEnabled ? '🎯 Area' : '📍 Area'}
                </Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity
              style={styles.bottomControlButton}
              onPress={() => setAutoCapture(!autoCapture)}
            >
              <Text style={styles.bottomControlButtonText}>
                {autoCapture ? '🤖 Auto' : '👆 Manual'}
              </Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Capture Button - Bottom Center (hidden in template mode) */}
        {!isTemplateMode && (
          <TouchableOpacity
            style={styles.captureButtonOverlay}
            onPress={onCapturePress}
          >
            <View style={styles.captureButtonInner} />
          </TouchableOpacity>
        )}

        {/* Results Overlay - Bottom Left (hidden in template mode) */}
        {!isTemplateMode && (capturedImage || barcodeResults.length > 0) && (
          <View style={styles.resultsOverlay}>
            {capturedImage && (
              <View style={styles.previewOverlayContainer}>
                <Text style={styles.previewOverlayTitle}>Last:</Text>
                <Image
                  source={{ uri: `file://${capturedImage}` }}
                  style={styles.previewOverlayImage}
                  resizeMode="cover"
                />
                <TouchableOpacity
                  style={styles.useForTestingButton}
                  onPress={handleUseForModelTesting}
                >
                  <Text style={styles.useForTestingButtonText}>Use for Testing</Text>
                </TouchableOpacity>
              </View>
            )}

            {barcodeResults.length > 0 && (
              <View style={styles.barcodeOverlayContainer}>
                <Text style={styles.barcodeOverlayTitle}>Codes: {barcodeResults.length}</Text>
                <ScrollView style={styles.barcodeOverlayScroll} nestedScrollEnabled>
                  {barcodeResults.slice(0, 3).map((barcode, index) => (
                    <Text key={index} style={styles.barcodeOverlayText} numberOfLines={1}>
                      {barcode.scannedCode}
                    </Text>
                  ))}
                </ScrollView>
              </View>
            )}
          </View>
        )}

        {/* Template Creation Panel - Bottom */}
        {isTemplateMode && (
          <View style={styles.templatePanel}>
            <View style={styles.templatePanelHeader}>
              <Text style={styles.templatePanelTitle}>Template ({templateCodes.length} codes)</Text>
              <Text style={styles.templatePanelHint}>Tap barcodes on camera to add</Text>
            </View>

            {templateCodes.length > 0 && (
              <ScrollView style={styles.templateCodesList} nestedScrollEnabled>
                {templateCodes.map((code, index) => (
                  <View key={`tpl-${index}`} style={styles.templateCodeItem}>
                    <View style={styles.templateCodeInfo}>
                      <Text style={styles.templateCodeString} numberOfLines={1}>{code.codeString}</Text>
                      <Text style={styles.templateCodeSymbology}>{code.codeSymbology}</Text>
                    </View>
                    <TouchableOpacity
                      style={styles.templateCodeRemove}
                      onPress={() => handleRemoveCodeFromTemplate(index)}
                    >
                      <Text style={styles.templateCodeRemoveText}>X</Text>
                    </TouchableOpacity>
                  </View>
                ))}
              </ScrollView>
            )}

            <View style={styles.templatePanelActions}>
              <TouchableOpacity style={styles.templateCancelButton} onPress={handleCancelTemplate}>
                <Text style={styles.templateCancelButtonText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.templateSaveButton, templateCodes.length === 0 && styles.templateSaveButtonDisabled]}
                onPress={handleSaveTemplate}
                disabled={templateCodes.length === 0}
              >
                <Text style={styles.templateSaveButtonText}>Save Template</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      </View>

      {/* Template Manager Modal */}
      <Modal
        visible={showTemplateManager}
        animationType="slide"
        transparent
        onRequestClose={() => setShowTemplateManager(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Templates</Text>
              <TouchableOpacity onPress={() => setShowTemplateManager(false)}>
                <Text style={styles.modalClose}>Close</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity style={styles.createTemplateButton} onPress={handleEnterTemplateMode}>
              <Text style={styles.createTemplateButtonText}>+ Create New Template</Text>
            </TouchableOpacity>

            {savedTemplates.length === 0 ? (
              <View style={styles.emptyTemplates}>
                <Text style={styles.emptyTemplatesText}>No saved templates</Text>
              </View>
            ) : (
              <>
                <FlatList
                  data={savedTemplates}
                  keyExtractor={(item) => item.id}
                  style={styles.templateList}
                  renderItem={({ item }) => {
                    const isActive = activeTemplate?.id === item.id;
                    return (
                      <View style={[styles.templateListItem, isActive && styles.templateListItemActive]}>
                        <View style={styles.templateListItemInfo}>
                          <Text style={styles.templateListItemId} numberOfLines={1}>
                            {item.id}{isActive ? ' (Active)' : ''}
                          </Text>
                          <Text style={styles.templateListItemCount}>
                            {item.templateCodes.length} code(s): {item.templateCodes.map(c => c.codeString).join(', ')}
                          </Text>
                        </View>
                        <TouchableOpacity
                          style={[styles.templateApplyButton, isActive && styles.templateApplyButtonActive]}
                          onPress={() => handleApplyTemplate(item)}
                        >
                          <Text style={styles.templateApplyButtonText}>{isActive ? 'Remove' : 'Apply'}</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                          style={styles.templateDeleteButton}
                          onPress={() => handleDeleteTemplate(item.id)}
                        >
                          <Text style={styles.templateDeleteButtonText}>Delete</Text>
                        </TouchableOpacity>
                      </View>
                    );
                  }}
                />
                <TouchableOpacity style={styles.deleteAllButton} onPress={handleDeleteAllTemplates}>
                  <Text style={styles.deleteAllButtonText}>Delete All</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#1a1a1a',
  },
  backButton: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  backButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  title: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  placeholder: {
    width: 60,
  },
  cameraContainer: {
    flex: 1,
    backgroundColor: '#000',
    position: 'relative',
  },
  boundingBoxesContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 10,
  },
  boundingBoxesContainerTemplateMode: {
    // On iOS, we need to ensure the overlay is above the native camera view
    // and can receive touch events
    zIndex: 100,
    elevation: 100, // Android elevation
  },
  boundingBox: {
    position: 'absolute',
    borderWidth: 2,
    backgroundColor: 'transparent',
  },
  permissionContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#000',
  },
  permissionText: {
    color: '#fff',
    fontSize: 16,
    marginBottom: 20,
    textAlign: 'center',
  },
  permissionButton: {
    backgroundColor: '#007bff',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 8,
  },
  permissionButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  recognitionOverlay: {
    position: 'absolute',
    top: 16,
    left: 16,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    borderRadius: 8,
    padding: 12,
  },
  recognitionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  recognitionLabel: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
    minWidth: 90,
  },
  indicator: {
    width: 12,
    height: 12,
    borderRadius: 6,
    backgroundColor: '#555',
  },
  indicatorActive: {
    backgroundColor: '#4CAF50',
  },
  sharpnessValue: {
    color: '#4CAF50',
    fontSize: 12,
    fontWeight: 'bold',
  },
  previewContainer: {
    position: 'absolute',
    top: 100,
    right: 16,
    width: 100,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    borderRadius: 8,
    padding: 8,
  },
  previewTitle: {
    color: '#fff',
    fontSize: 10,
    marginBottom: 4,
    textAlign: 'center',
  },
  previewImage: {
    width: 84,
    height: 84,
    borderRadius: 4,
  },
  barcodeResultsContainer: {
    position: 'absolute',
    bottom: 300,
    left: 16,
    right: 16,
    maxHeight: 150,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    borderRadius: 8,
    padding: 12,
  },
  barcodeResultsTitle: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 8,
    textTransform: 'uppercase',
  },
  barcodeResultsScroll: {
    maxHeight: 110,
  },
  barcodeItem: {
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 4,
    padding: 8,
    marginBottom: 6,
  },
  barcodeCode: {
    color: '#4CAF50',
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 2,
  },
  barcodeSymbology: {
    color: '#aaa',
    fontSize: 11,
  },
  // Mode Dropdown (below recognition status)
  modeDropdownOverlay: {
    position: 'absolute',
    top: 160,
    left: 16,
  },
  modeDropdownButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: 'rgba(51, 51, 51, 0.9)',
    borderRadius: 8,
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: 'rgba(68, 68, 68, 0.9)',
    minWidth: 115,
  },
  modeDropdownButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  modeDropdownArrow: {
    color: '#fff',
    fontSize: 10,
    marginLeft: 8,
  },
  modeDropdownMenu: {
    backgroundColor: 'rgba(26, 26, 26, 0.95)',
    borderRadius: 8,
    marginTop: 4,
    borderWidth: 1,
    borderColor: 'rgba(68, 68, 68, 0.9)',
    overflow: 'hidden',
  },
  modeDropdownItem: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(68, 68, 68, 0.5)',
  },
  modeDropdownItemActive: {
    backgroundColor: 'rgba(0, 123, 255, 0.3)',
  },
  modeDropdownItemText: {
    color: '#aaa',
    fontSize: 12,
    fontWeight: '600',
  },
  modeDropdownItemTextActive: {
    color: '#fff',
  },
  controlsOverlay: {
    position: 'absolute',
    top: 16,
    right: 16,
    alignItems: 'flex-end',
  },
  controlButtonOverlay: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: 'rgba(51, 51, 51, 0.9)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  controlButtonTextOverlay: {
    fontSize: 22,
  },
  zoomControlsOverlay: {
    flexDirection: 'column',
    alignItems: 'center',
    backgroundColor: 'rgba(51, 51, 51, 0.9)',
    borderRadius: 24,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  zoomButtonOverlay: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  zoomButtonTextOverlay: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  zoomButtonDisabled: {
    color: '#666',
  },
  zoomTextOverlay: {
    color: '#fff',
    fontSize: 11,
    fontWeight: '600',
    marginVertical: 4,
    textAlign: 'center',
  },
  bottomControlsOverlay: {
    position: 'absolute',
    bottom: 100,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 12,
  },
  bottomControlButton: {
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 20,
    backgroundColor: 'rgba(51, 51, 51, 0.9)',
    borderWidth: 1,
    borderColor: 'rgba(68, 68, 68, 0.9)',
  },
  bottomControlButtonText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: '600',
  },
  captureButtonOverlay: {
    position: 'absolute',
    bottom: 20,
    alignSelf: 'center',
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 4,
    borderColor: '#333',
  },
  captureButtonInner: {
    width: 54,
    height: 54,
    borderRadius: 27,
    backgroundColor: '#fff',
    borderWidth: 2,
    borderColor: '#333',
  },
  resultsOverlay: {
    position: 'absolute',
    bottom: 20,
    left: 16,
    maxWidth: 180,
  },
  previewOverlayContainer: {
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    borderRadius: 8,
    padding: 8,
    marginBottom: 8,
  },
  previewOverlayTitle: {
    color: '#fff',
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 6,
  },
  previewOverlayImage: {
    width: 80,
    height: 80,
    borderRadius: 6,
  },
  useForTestingButton: {
    marginTop: 6,
    backgroundColor: '#6f42c1',
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 4,
  },
  useForTestingButtonText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: '600',
    textAlign: 'center',
  },
  barcodeOverlayContainer: {
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    borderRadius: 8,
    padding: 8,
  },
  barcodeOverlayTitle: {
    color: '#fff',
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 6,
  },
  barcodeOverlayScroll: {
    maxHeight: 60,
  },
  barcodeOverlayText: {
    color: '#4CAF50',
    fontSize: 11,
    fontWeight: '600',
    marginBottom: 3,
  },
  scanAreaOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  scanAreaRect: {
    position: 'absolute',
    borderWidth: 3,
    borderColor: '#FF9800',
    backgroundColor: 'rgba(255, 152, 0, 0.1)',
    borderStyle: 'dashed',
    justifyContent: 'center',
    alignItems: 'center',
  },
  scanAreaLabel: {
    color: '#FF9800',
    fontSize: 12,
    fontWeight: 'bold',
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    padding: 8,
    borderRadius: 4,
    textAlign: 'center',
  },
  qrScanAreaRect: {
    position: 'absolute',
    borderWidth: 4,
    borderColor: '#00E5FF',
    backgroundColor: 'rgba(0, 229, 255, 0.1)',
    borderStyle: 'solid',
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 8,
  },
  boundingBoxLabel: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: 'bold',
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    padding: 4,
    borderRadius: 3,
    position: 'absolute',
    top: -20,
    left: 0,
  },
  stopButton: {
    backgroundColor: 'rgba(220, 53, 69, 0.9)', // Red for stop
  },
  startButton: {
    backgroundColor: 'rgba(40, 167, 69, 0.9)', // Green for start
  },
  // --- Header Template Mode ---
  headerTemplateMode: {
    backgroundColor: '#6A1B9A',
  },
  templateHeaderButton: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  templateHeaderButtonText: {
    color: '#BB86FC',
    fontSize: 14,
    fontWeight: '600',
  },
  // --- Bounding Box Tappable ---
  boundingBoxTappable: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  addedBadge: {
    position: 'absolute',
    bottom: -20,
    left: 0,
    backgroundColor: '#4CAF50',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 3,
  },
  addedBadgeText: {
    color: '#fff',
    fontSize: 9,
    fontWeight: 'bold',
  },
  // --- Template Panel ---
  templatePanel: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    paddingTop: 16,
    paddingHorizontal: 16,
    paddingBottom: 24,
    maxHeight: 280,
  },
  templatePanelHeader: {
    marginBottom: 12,
  },
  templatePanelTitle: {
    color: '#BB86FC',
    fontSize: 16,
    fontWeight: 'bold',
  },
  templatePanelHint: {
    color: '#aaa',
    fontSize: 12,
    marginTop: 4,
  },
  templateCodesList: {
    maxHeight: 120,
    marginBottom: 12,
  },
  templateCodeItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 8,
    padding: 10,
    marginBottom: 6,
  },
  templateCodeInfo: {
    flex: 1,
  },
  templateCodeString: {
    color: '#fff',
    fontSize: 13,
    fontWeight: '600',
  },
  templateCodeSymbology: {
    color: '#aaa',
    fontSize: 11,
    marginTop: 2,
  },
  templateCodeRemove: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: 'rgba(220, 53, 69, 0.8)',
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 10,
  },
  templateCodeRemoveText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: 'bold',
  },
  templatePanelActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  templateCancelButton: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#666',
    alignItems: 'center',
  },
  templateCancelButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  templateSaveButton: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 8,
    backgroundColor: '#6A1B9A',
    alignItems: 'center',
  },
  templateSaveButtonDisabled: {
    backgroundColor: '#444',
    opacity: 0.6,
  },
  templateSaveButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  // --- Template Manager Modal ---
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.6)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#1a1a1a',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    paddingTop: 20,
    paddingHorizontal: 20,
    paddingBottom: 40,
    maxHeight: '70%',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  modalTitle: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  modalClose: {
    color: '#BB86FC',
    fontSize: 16,
    fontWeight: '600',
  },
  createTemplateButton: {
    backgroundColor: '#6A1B9A',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 20,
  },
  createTemplateButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  emptyTemplates: {
    alignItems: 'center',
    paddingVertical: 30,
  },
  emptyTemplatesText: {
    color: '#666',
    fontSize: 14,
  },
  templateList: {
    maxHeight: 300,
  },
  templateListItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderRadius: 10,
    padding: 14,
    marginBottom: 8,
  },
  templateListItemActive: {
    borderWidth: 1,
    borderColor: '#4CAF50',
    backgroundColor: 'rgba(76, 175, 80, 0.1)',
  },
  templateListItemInfo: {
    flex: 1,
  },
  templateListItemId: {
    color: '#fff',
    fontSize: 13,
    fontWeight: '600',
  },
  templateListItemCount: {
    color: '#aaa',
    fontSize: 11,
    marginTop: 4,
  },
  templateApplyButton: {
    paddingVertical: 6,
    paddingHorizontal: 14,
    backgroundColor: 'rgba(76, 175, 80, 0.8)',
    borderRadius: 6,
    marginLeft: 10,
  },
  templateApplyButtonActive: {
    backgroundColor: 'rgba(158, 158, 158, 0.8)',
  },
  templateApplyButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  templateDeleteButton: {
    paddingVertical: 6,
    paddingHorizontal: 14,
    backgroundColor: 'rgba(220, 53, 69, 0.8)',
    borderRadius: 6,
    marginLeft: 10,
  },
  templateDeleteButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  deleteAllButton: {
    marginTop: 12,
    paddingVertical: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#dc3545',
    alignItems: 'center',
  },
  deleteAllButtonText: {
    color: '#dc3545',
    fontSize: 14,
    fontWeight: '600',
  },
});

export default VisionCameraExample;
