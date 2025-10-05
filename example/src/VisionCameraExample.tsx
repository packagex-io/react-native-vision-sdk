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
} from 'react-native';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import { VisionCamera, VisionCameraRefProps, VisionCameraCaptureEvent, VisionCameraScanMode } from '../../src/VisionCamera';

const VisionCameraExample = ({ navigation }) => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [flashEnabled, setFlashEnabled] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(1.0);
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [scanMode, setScanMode] = useState<VisionCameraScanMode>('ocr');
  const [autoCapture, setAutoCapture] = useState(true);
  const [recognitionData, setRecognitionData] = useState({ text: false, barcode: false, qrcode: false, document: false });
  const [sharpness, setSharpness] = useState(0);
  const [barcodeResults, setBarcodeResults] = useState<any[]>([]);
  const [hasPermission, setHasPermission] = useState(false);

  // Throttle sharpness updates (update at most every 200ms)
  const lastSharpnessUpdate = useRef<number>(0);
  const sharpnessThrottleMs = 200;

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
    setBarcodeResults(event.codes);
    Alert.alert('Barcode Detected', `Found ${event.codes.length} barcode(s)`);
  };

  const onCapturePress = () => {
    cameraRef.current?.capture();
  };

  const onToggleFlash = () => {
    setFlashEnabled(!flashEnabled);
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

      <View style={styles.cameraContainer}>
        {hasPermission ? (
          <VisionCamera
            ref={cameraRef}
            enableFlash={flashEnabled}
            zoomLevel={zoomLevel}
            scanMode={scanMode}
            autoCapture={autoCapture}
            onCapture={handleCapture}
            onError={handleError}
            onRecognitionUpdate={handleRecognitionUpdate}
            onSharpnessScoreUpdate={handleSharpnessScoreUpdate}
            onBarcodeDetected={handleBarcodeDetected}
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
                scanMode === mode.value && styles.modeButtonActive,
              ]}
              onPress={() => handleScanModeChange(mode.value)}
            >
              <Text
                style={[
                  styles.modeButtonText,
                  scanMode === mode.value && styles.modeButtonTextActive,
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
    bottom:300,
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
});

export default VisionCameraExample;
