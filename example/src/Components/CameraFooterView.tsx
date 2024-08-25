import React from 'react';
import { View, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import Octicons from 'react-native-vector-icons/Octicons';
import OCRSelectionView from './OCRSelectionView';
import CaptureModesView from './CaptureModesView';

function CameraFooterView({
  setCaptureMode,
  captureMode,
  setShowOcrTypes,
  showOcrTypes,
  setIsOnDeviceOCR,
  isOnDeviceOCR,
  onPressCapture,
}: any) {
  return (
    <View style={styles.mainContainer}>
      <View style={[styles.sideContainer, styles.rotatedIcon]}>
        <TouchableOpacity
          onPress={() => setShowOcrTypes(true)}
          style={styles.switchIconContainer}
        >
          <Octicons name="arrow-switch" size={35} color="white" />
        </TouchableOpacity>
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
      <View style={styles.sideContainer} />
      <OCRSelectionView
        setShowOcrTypes={setShowOcrTypes}
        showOcrTypes={showOcrTypes}
        setIsOnDeviceOCR={setIsOnDeviceOCR}
        isOnDeviceOCR={isOnDeviceOCR}
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
  rotatedIcon: {
    flexDirection: 'row',
    transform: [{ rotate: '90deg' }],
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
    width: '30%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  centerContainer: {
    width: '40%',
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
