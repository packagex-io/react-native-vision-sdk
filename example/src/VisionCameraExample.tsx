import React, { useRef, useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  SafeAreaView,
  Image,
  ScrollView,
  Platform,
  Dimensions,
} from 'react-native';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import { VisionCamera, VisionCameraRefProps, VisionCameraCaptureEvent, VisionCameraScanMode } from '../../src/VisionCamera';
import { VisionCore } from '../../src';

const VisionCameraExample = ({ navigation }) => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [flashEnabled, setFlashEnabled] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1.0);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [scanMode, setScanMode] = useState<VisionCameraScanMode>('photo');
  const [scanAreaEnabled, setScanAreaEnabled] = useState(false);
  const [autoCapture, setAutoCapture] = useState(false);
  const [recognitionData, setRecognitionData] = useState({ text: false, barcode: false, qrcode: false, document: false });
  const [sharpness, setSharpness] = useState(0);
  const [modeDropdownOpen, setModeDropdownOpen] = useState(false);
  const [barcodeResults, setBarcodeResults] = useState<any[]>([]);
  const [hasPermission, setHasPermission] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'back' | 'front'>('back');
  const [boundingBoxes, setBoundingBoxes] = useState<{
    barcodeBoundingBoxes: any[];
    qrCodeBoundingBoxes: any[];
    documentBoundingBox: any;
  }>({
    barcodeBoundingBoxes: [],
    qrCodeBoundingBoxes: [],
    documentBoundingBox: null,
  });

  // Throttle sharpness updates (update at most every 200ms)
  const lastSharpnessUpdate = useRef<number>(0);
  const sharpnessThrottleMs = 200;

  // Throttle bounding boxes updates (update at most every 300ms)
  const lastBoundingBoxUpdate = useRef<number>(0);
  const boundingBoxThrottleMs = 300;

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

    return () => {
      cameraRef.current?.stop();
    };
  }, []);

  const handleCapture = (event: VisionCameraCaptureEvent) => {
    // console.log("HANDLE CAPTURE: ", JSON.stringify(event))
    setCapturedImage(event.image);
    // Alert.alert('Success', `Image captured: ${event.image}`);
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
    console.log("DETECTED BARCODES ARE: ", event.codes);
    setBarcodeResults(event.codes);
    Alert.alert('Barcode Detected', `Found ${event.codes.length} barcode(s)`);
  };

  const handleBoundingBoxesUpdate = (event: any) => {
    // console.log("BOUNDING BOXES UPDATE EVENT: ", event)
    const now = Date.now();
    if (now - lastBoundingBoxUpdate.current >= boundingBoxThrottleMs) {
      lastBoundingBoxUpdate.current = now;
      // console.log('📦 Bounding Boxes Update:', {
      //   barcodes: event.barcodeBoundingBoxes?.length || 0,
      //   qrCodes: event.qrCodeBoundingBoxes?.length || 0,
      //   document: event.documentBoundingBox ? 'detected' : 'none',
      //   data: event
      // });
      setBoundingBoxes({
        barcodeBoundingBoxes: event.barcodeBoundingBoxes || [],
        qrCodeBoundingBoxes: event.qrCodeBoundingBoxes || [],
        documentBoundingBox: event.documentBoundingBox || null,
      });
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

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Text style={styles.backButtonText}>← Back</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Vision Camera</Text>
        <View style={styles.placeholder} />
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
            onCapture={handleCapture}
            onError={handleError}
            onRecognitionUpdate={handleRecognitionUpdate}
            onSharpnessScoreUpdate={handleSharpnessScoreUpdate}
            onBarcodeDetected={handleBarcodeDetected}
            detectionConfig={{
              text: true,
              barcode: true,
              document: true,
            }}
            frameSkip={15}
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
        <View style={styles.boundingBoxesContainer} pointerEvents="none">
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
              {/* Barcode Bounding Boxes - Yellow */}
              {boundingBoxes.barcodeBoundingBoxes.map((code, index) => (
                <View
                  key={`barcode-${index}`}
                  style={[
                    styles.boundingBox,
                    {
                      left: code.boundingBox.x,
                      top: code.boundingBox.y,
                      width: code.boundingBox.width,
                      height: code.boundingBox.height,
                      borderColor: '#FFEB3B', // Yellow for barcodes
                    },
                  ]}
                >
                  <Text style={styles.boundingBoxLabel}>
                    {code.scannedCode} ({code.symbology})
                  </Text>
                </View>
              ))}

              {/* QR Code Bounding Boxes - Cyan */}
              {boundingBoxes.qrCodeBoundingBoxes.map((code, index) => (
                <View
                  key={`qrcode-${index}`}
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

              {/* Document Bounding Box - Green */}
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

        {/* Scan Mode Dropdown - Below Recognition Status */}
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

        {/* Control Buttons - Top Right */}
        <View style={styles.controlsOverlay}>
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

        {/* Bottom Controls - Auto Capture & Scan Area */}
        {scanMode !== 'photo' && (
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

        {/* Capture Button - Bottom Center */}
        <TouchableOpacity
          style={styles.captureButtonOverlay}
          onPress={onCapturePress}
        >
          <View style={styles.captureButtonInner} />
        </TouchableOpacity>

        {/* Results Overlay - Bottom Left */}
        {(capturedImage || barcodeResults.length > 0) && (
          <View style={styles.resultsOverlay}>
            {capturedImage && (
              <View style={styles.previewOverlayContainer}>
                <Text style={styles.previewOverlayTitle}>Last:</Text>
                <Image
                  source={{ uri: `file://${capturedImage}` }}
                  style={styles.previewOverlayImage}
                  resizeMode="cover"
                />
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
      </View>
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
});

export default VisionCameraExample;
