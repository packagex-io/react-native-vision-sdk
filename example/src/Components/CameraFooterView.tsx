import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  Platform,
  Text,
} from 'react-native';
import OCRSelectionView from './OCRSelectionView';
import CaptureModesView from './CaptureModesView';
import ModelSizeSelectionView from './ModelSizeSelectionView';

function CameraFooterView({
  setCaptureMode,
  captureMode,
  setIsOnDeviceOCR,
  isOnDeviceOCR,
  onPressCapture,
  onPressOnDeviceOcr,
  setModelSize,
  modelSize,
  mode
}: any) {
  const [showOcrTypes, setShowOcrTypes] = useState<boolean>(false);
  const [showOcrSize, setShowOcrSize] = useState<boolean>(false);
  return (
    <View style={styles.mainContainer}>
      <View style={[styles.sideContainer, styles.rotatedIcon]}>
        {mode === 'ocr' && (
          <TouchableOpacity
          onPress={() => setShowOcrTypes(true)}
          style={styles.switchIconContainer}
        >
          {/* <Octicons name="arrow-switch" size={30} color="white" /> */}
          <Text style={{ color: 'white' }}>
            {isOnDeviceOCR ? 'On-Device' : 'Cloud'}
          </Text>
        </TouchableOpacity>
        )}
      </View>
      <View style={styles.centerContainer}>
        <CaptureModesView
          setCaptureMode={setCaptureMode}
          captureMode={captureMode}
        />
        {captureMode === 'manual' && (
          <TouchableOpacity onPress={onPressCapture} style={styles.outerCircle}>
            <View style={styles.innerCircle} />
          </TouchableOpacity>
        )}
      </View>
      <View style={[styles.sideContainer]}>
        {isOnDeviceOCR && (
          <TouchableOpacity
            onPress={() => setShowOcrSize(true)}
            style={styles.sizeIconContainer}
          >
            {/* <MaterialCommunityIcons name="resize" size={30} color="white" /> */}
            <Text style={{ color: 'white' }}>
              {modelSize === 'large' ? 'Large' : 'Micro'}
            </Text>
          </TouchableOpacity>
        )}
      </View>
      <OCRSelectionView
        setShowOcrTypes={setShowOcrTypes}
        showOcrTypes={showOcrTypes}
        setIsOnDeviceOCR={setIsOnDeviceOCR}
        isOnDeviceOCR={isOnDeviceOCR}
        onPressOnDeviceOcr={onPressOnDeviceOcr}
      />
      <ModelSizeSelectionView
        setShowOcrSize={setShowOcrSize}
        showOcrSize={showOcrSize}
        onPressOnDeviceOcr={onPressOnDeviceOcr}
        setModelSize={setModelSize}
        modelSize={modelSize}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  switchIconContainer: {
    backgroundColor: '#7420E2',
    paddingVertical: 8,
    paddingHorizontal: 15,
    borderRadius: 10,
  },
  sizeIconContainer: {
    backgroundColor: '#7420E2',
    paddingVertical: 10,
    paddingHorizontal: 10,
    borderRadius: 10,
  },
  rotatedIcon: {
    flexDirection: 'row',
    // transform: [{ rotate: '90deg' }],
  },
  mainContainer: {
    backgroundColor:
      Platform.OS === 'android' ? 'rgba(0, 0, 0, 0.3)' : 'rgba(0, 0, 0, 0.5)',
    height: 150,
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    position: 'absolute',
    bottom: 0,
  },
  sideContainer: {
    width: '35%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 0,
    borderColor: 'black',
  },
  centerContainer: {
    width: '30%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    top: 10,
  },
  outerCircle: {
    borderColor: 'white',
    borderWidth: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
    width: 65,
    height: 65,
    borderRadius: 45,
    overflow: 'hidden',
    justifyContent: 'center',
  },
  innerCircle: {
    alignSelf: 'center',
    backgroundColor: 'white',
    width: 50,
    height: 50,
    borderRadius: 30,
  },
});

export default CameraFooterView;
