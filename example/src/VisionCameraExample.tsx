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
  const [scanMode, setScanMode] = useState<VisionCameraScanMode>('barcode');
  const [currentMode, setCurrentMode] = useState<VisionCameraScanMode | 'barcode-focused' | 'qrcode-focused'>('barcode-focused');
  const [autoCapture, setAutoCapture] = useState(false);
  const [recognitionData, setRecognitionData] = useState({ text: false, barcode: false, qrcode: false, document: false });
  const [sharpness, setSharpness] = useState(0);
  const [barcodeResults, setBarcodeResults] = useState<any[]>([]);
  const [hasPermission, setHasPermission] = useState(false);
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

  const scanModes: { label: string; value: VisionCameraScanMode | 'barcode-focused' | 'qrcode-focused' }[] = [
    { label: '📷 Photo', value: 'photo' },
    { label: '📊 Barcode', value: 'barcode' },
    { label: '🎯 Barcode (Focused)', value: 'barcode-focused' },
    { label: '🔲 QR Code', value: 'qrcode' },
    { label: '🎯 QR Code (Focused)', value: 'qrcode-focused' },
    { label: '🔍 OCR', value: 'ocr' },
  ];

  const handleScanModeChange = (mode: VisionCameraScanMode | 'barcode-focused' | 'qrcode-focused') => {
    setCurrentMode(mode);
    if (mode === 'barcode-focused') {
      setScanMode('barcode');
    } else if (mode === 'qrcode-focused') {
      setScanMode('qrcode');
    } else {
      setScanMode(mode);
    }
    setAutoCapture(false);
  };

  // State to store camera view dimensions
  const [cameraViewSize, setCameraViewSize] = useState({ width: 0, height: 0 });

  // Calculate centered scan area for barcode-focused mode
  const getCenteredScanArea = () => {
    // Use camera view dimensions if available, otherwise fallback to screen width
    const { width: screenWidth } = Dimensions.get('window');
    const viewWidth = cameraViewSize.width || screenWidth;
    const viewHeight = cameraViewSize.height || 600; // Reasonable fallback

    // Centered rectangle: 300dp wide, 100dp tall
    const width = 300;
    const height = 100;
    const x = (viewWidth - width) / 2;
    const y = (viewHeight - height) / 2;

    const scanArea = { x, y, width, height };
    return scanArea;
  };

  // Calculate centered square scan area for qrcode-focused mode
  const getQRCodeScanArea = () => {
    // Use camera view dimensions if available, otherwise fallback to screen width
    const { width: screenWidth } = Dimensions.get('window');
    const viewWidth = cameraViewSize.width || screenWidth;
    const viewHeight = cameraViewSize.height || 600; // Reasonable fallback

    // Centered square: 250dp x 250dp
    const size = 250;
    const x = (viewWidth - size) / 2;
    const y = (viewHeight - size) / 2;

    const scanArea = { x, y, width: size, height: size };
    return scanArea;
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
    console.log("HANDLE CAPTURE: ", JSON.stringify(event))
    setCapturedImage(event.image);
    Alert.alert('Success', `Image captured: ${event.image}`);
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
            scanArea={
              currentMode === 'barcode-focused'
                ? getCenteredScanArea()
                : currentMode === 'qrcode-focused'
                  ? getQRCodeScanArea()
                  : undefined
            }
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

        {/* Scan Area Overlay (for barcode-focused mode) */}
        {currentMode === 'barcode-focused' && (
          <View style={styles.scanAreaOverlay} pointerEvents="none">
            <View style={[styles.scanAreaRect, {
              left: getCenteredScanArea().x,
              top: getCenteredScanArea().y,
              width: getCenteredScanArea().width,
              height: getCenteredScanArea().height,
            }]}>
              <Text style={styles.scanAreaLabel}>Scan Area - Only barcodes in this region will be detected</Text>
            </View>
          </View>
        )}

        {/* Scan Area Overlay (for qrcode-focused mode) */}
        {currentMode === 'qrcode-focused' && (
          <View style={styles.scanAreaOverlay} pointerEvents="none">
            <View style={[styles.qrScanAreaRect, {
              left: getQRCodeScanArea().x,
              top: getQRCodeScanArea().y,
              width: getQRCodeScanArea().width,
              height: getQRCodeScanArea().height,
            }]}>
              <Text style={styles.scanAreaLabel}>Position QR Code Here</Text>
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

        {/* Recognition Status Overlay */}
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
      </View>

      {/* Scan Mode Selector */}
      <View style={styles.modeSelector}>
        <Text style={styles.modeSelectorTitle}>Scan Mode:</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.modeScrollView}>
          {scanModes.map((mode) => (
            <TouchableOpacity
              key={mode.value}
              style={[
                styles.modeButton,
                currentMode === mode.value && styles.modeButtonActive,
              ]}
              onPress={() => handleScanModeChange(mode.value)}
            >
              <Text
                style={[
                  styles.modeButtonText,
                  currentMode === mode.value && styles.modeButtonTextActive,
                ]}
              >
                {mode.label}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {capturedImage && (
        <View style={styles.previewContainer}>
          <Text style={styles.previewTitle}>Last Captured:</Text>
          <Image
            source={{ uri: `file://${capturedImage}` }}
            style={styles.previewImage}
            resizeMode="cover"
          />
        </View>
      )}

      {barcodeResults.length > 0 && (
        <View style={styles.barcodeResultsContainer}>
          <Text style={styles.barcodeResultsTitle}>Barcodes ({barcodeResults.length}):</Text>
          <ScrollView style={styles.barcodeResultsScroll}>
            {barcodeResults.map((barcode, index) => (
              <View key={index} style={styles.barcodeItem}>
                <Text style={styles.barcodeCode}>{barcode.scannedCode}</Text>
                <Text style={styles.barcodeSymbology}>{barcode.symbology}</Text>
              </View>
            ))}
          </ScrollView>
        </View>
      )}

      <View style={styles.controls}>
        <View style={styles.controlRow}>
          <TouchableOpacity
            style={[styles.controlButton, styles.flashButton]}
            onPress={onToggleFlash}
          >
            <Text style={styles.controlButtonText}>
              {flashEnabled ? '⚡ Flash ON' : '⚡ Flash OFF'}
            </Text>
          </TouchableOpacity>

          {(scanMode === 'ocr' || scanMode === 'barcode' || scanMode === 'qrcode' || scanMode === 'barcodeorqrcode') && (
            <TouchableOpacity
              style={[styles.controlButton, styles.autoCaptureButton]}
              onPress={() => setAutoCapture(!autoCapture)}
            >
              <Text style={styles.controlButtonText}>
                {autoCapture ? '🤖 Auto ON' : '👆 Manual'}
              </Text>
            </TouchableOpacity>
          )}

          <View style={styles.zoomControls}>
            <TouchableOpacity
              style={styles.zoomButton}
              onPress={onZoomOut}
              disabled={zoomLevel <= 1.0}
            >
              <Text style={styles.zoomButtonText}>-</Text>
            </TouchableOpacity>
            <Text style={styles.zoomText}>{zoomLevel.toFixed(1)}x</Text>
            <TouchableOpacity
              style={styles.zoomButton}
              onPress={onZoomIn}
              disabled={zoomLevel >= 5.0}
            >
              <Text style={styles.zoomButtonText}>+</Text>
            </TouchableOpacity>
          </View>
        </View>

        <TouchableOpacity
          style={styles.captureButton}
          onPress={onCapturePress}
        >
          <View style={styles.captureButtonInner} />
        </TouchableOpacity>
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
  modeSelector: {
    backgroundColor: '#1a1a1a',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#333',
  },
  modeSelectorTitle: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  modeScrollView: {
    flexGrow: 0,
  },
  modeButton: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    backgroundColor: '#333',
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#444',
  },
  modeButtonActive: {
    backgroundColor: '#007bff',
    borderColor: '#007bff',
  },
  modeButtonText: {
    color: '#aaa',
    fontSize: 14,
    fontWeight: '600',
  },
  modeButtonTextActive: {
    color: '#fff',
  },
  controls: {
    paddingHorizontal: 20,
    paddingVertical: 20,
    backgroundColor: '#1a1a1a',
  },
  controlRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  controlButton: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 8,
    backgroundColor: '#333',
  },
  flashButton: {
    flex: 1,
    marginRight: 10,
  },
  autoCaptureButton: {
    flex: 1,
    marginRight: 10,
  },
  controlButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
    textAlign: 'center',
  },
  zoomControls: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#333',
    borderRadius: 8,
    paddingHorizontal: 8,
  },
  zoomButton: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  zoomButtonText: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
  },
  zoomText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
    marginHorizontal: 12,
    minWidth: 40,
    textAlign: 'center',
  },
  captureButton: {
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
