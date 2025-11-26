import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  Platform,
  Text,
  FlatList,
} from 'react-native';
import OCRSelectionView from './OCRSelectionView';
import CaptureModesView from './CaptureModesView';
import ModelSizeSelectionView from './ModelSizeSelectionView';
import { OCRConfig } from '../../../src/index';

// Define a type for the zoom level
interface ZoomLevel {
  id: string;
  level: number;
  label: string;
}

// Define the props type for the CameraFooterView component
interface CameraFooterViewProps {
  setCaptureMode: (mode: 'manual' | 'auto') => void;
  captureMode: string;
  setOcrConfig: (config: OCRConfig) => void;
  ocrConfig: OCRConfig;
  onPressCapture: () => void;
  onPressOnDeviceOcr: () => void;

  mode: string;
  zoomLevel: number;
  setZoomLevel: (level: number) => void;
}

const CameraFooterView = ({
  setCaptureMode,
  captureMode,




  onPressCapture,
  onPressOnDeviceOcr,


  mode,
  zoomLevel,
  setZoomLevel,

  ocrConfig,
  setOcrConfig
}: CameraFooterViewProps) => {
  // Local state to manage visibility of OCR type and size selection
  const [showOcrTypes, setShowOcrTypes] = useState<boolean>(false);
  const [showOcrSize, setShowOcrSize] = useState<boolean>(false);

  // Define the available zoom levels
  const zoomLevels: ZoomLevel[] = [
    { id: '1', level: 1, label: '1X' },
    { id: '2', level: 1.8, label: '1.8X' },
    { id: '3', level: 2, label: '2X' },
    { id: '4', level: 3, label: '3X' },
  ];

  // Render function for zoom level buttons
  const renderZoomLevelItem = ({ item }: { item: ZoomLevel }) => (
    <TouchableOpacity onPress={() => setZoomLevel(item.level)}>
      <View
        style={[
          styles.zoomCircle,
          {
            backgroundColor: zoomLevel === item.level ? '#7420E2' : '#444444', // Highlight the active zoom level
          },
        ]}
      >
        <Text style={styles.zoomText}>{item.label}</Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {/* Left side container for OCR mode selection */}
      <View style={styles.sideContainer}>
        {mode === 'ocr' && (
          <TouchableOpacity
            onPress={() => setShowOcrTypes(true)}
            style={styles.ocrModeButton}
          >
            <Text style={styles.buttonText}>{`${ocrConfig.type}-${ocrConfig.mode}`}</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Center container for capture mode and zoom controls */}
      <View style={styles.centerContainer}>
        <CaptureModesView
          setCaptureMode={setCaptureMode}
          captureMode={captureMode}
        />

        <View style={styles.zoomContainer}>
          <FlatList
            data={zoomLevels}
            horizontal
            keyExtractor={(item) => item.id}
            renderItem={renderZoomLevelItem}
            contentContainerStyle={styles.zoomList}
          />
        </View>

        {/* Capture button visible only in manual mode */}
        {captureMode === 'manual' && (
          <TouchableOpacity
            onPress={onPressCapture}
            style={styles.captureButton}
          >
            <View style={styles.innerCaptureButton} />
          </TouchableOpacity>
        )}
      </View>

      {/* Right side container for model size selection */}
      <View style={styles.sideContainer}>
        {ocrConfig.mode !== 'cloud' ? (
          <TouchableOpacity
            onPress={() => setShowOcrSize(true)}
            style={styles.sizeButton}
          >
            <Text style={styles.buttonText}>
              {ocrConfig.size === 'large' ? 'Large' : 'Micro'}
            </Text>
          </TouchableOpacity>
        ) : null}
      </View>

      {/* Additional selection views for OCR types and model size */}
      <OCRSelectionView
        setShowOcrTypes={setShowOcrTypes}
        showOcrTypes={showOcrTypes}
        ocrConfig={ocrConfig}
        setOcrConfig={setOcrConfig}

      />
      <ModelSizeSelectionView
        setShowOcrSize={setShowOcrSize}
        showOcrSize={showOcrSize}
        onPressOnDeviceOcr={onPressOnDeviceOcr}

        ocrConfig={ocrConfig}
        setOcrConfig={setOcrConfig}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor:
      Platform.OS === 'android' ? 'rgba(0, 0, 0, 0.7)' : 'rgba(0, 0, 0, 0.9)',
    height: 180,
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    position: 'absolute',
    bottom: 0,
    paddingHorizontal: 10,
  },
  sideContainer: {
    width: '30%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  centerContainer: {
    width: '40%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  ocrModeButton: {
    backgroundColor: '#7420E2',
    paddingVertical: 10,
    paddingHorizontal: 15,
    borderRadius: 10,
    alignItems: 'center',
  },
  sizeButton: {
    backgroundColor: '#7420E2',
    padding: 10,
    borderRadius: 10,
  },
  captureButton: {
    borderColor: 'white',
    borderWidth: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    width: 65,
    height: 65,
    borderRadius: 45,
    justifyContent: 'center',
    alignItems: 'center',
  },
  innerCaptureButton: {
    backgroundColor: 'white',
    width: 50,
    height: 50,
    borderRadius: 25,
  },
  zoomContainer: {
    position: 'absolute',
    top: -60,
    width: 200,
  },
  zoomList: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  zoomCircle: {
    width: 35,
    height: 30,
    marginHorizontal: 4,
    borderRadius: 25,
    justifyContent: 'center',
    alignItems: 'center',
  },
  zoomText: {
    color: 'white',
    textAlign: 'center',
    fontSize: 14,
  },
  autoOcrButton: {
    backgroundColor: '#7420E2',
    paddingVertical: 10,
    paddingHorizontal: 15,
    borderRadius: 10,
    marginTop: 10,
  },
  autoOcrActive: {
    backgroundColor: '#7420E250', // Dimmed color when active
  },
  buttonText: {
    color: 'white',
    textTransform: 'capitalize',
    textAlign: 'center',
  },
});

export default CameraFooterView;
