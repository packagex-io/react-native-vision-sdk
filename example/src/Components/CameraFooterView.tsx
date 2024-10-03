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

function CameraFooterView({
  setCaptureMode,
  captureMode,
  setOcrMode,
  ocrMode,
  onPressCapture,
  onPressOnDeviceOcr,
  setModelSize,
  modelSize,
  mode,
  zoomLevel,
  setZoomLevel,
}: any) {
  const [showOcrTypes, setShowOcrTypes] = useState<boolean>(false);
  const [showOcrSize, setShowOcrSize] = useState<boolean>(false);

  const zoomLevels = [
    { id: '1', level: 1, label: '1X' },
    { id: '2', level: 1.8, label: '1.8X' },
    { id: '3', level: 2, label: '2X' },
    { id: '4', level: 3, label: '3X' },
  ];

  const renderItem = ({ item }) => (
    <TouchableOpacity onPress={() => setZoomLevel(item.level)}>
      <View
        style={{
          ...styles.circle,
          backgroundColor: zoomLevel === item.level ? '#7420E2' : '#000000',
        }}
      >
        <Text style={styles.zoomTextStyle}>{item.label}</Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.mainContainer}>
      <View style={[styles.sideContainer, styles.rotatedIcon]}>
        {mode === 'ocr' && (
          <TouchableOpacity
            onPress={() => setShowOcrTypes(true)}
            style={styles.switchIconContainer}
          >
            {/* <Octicons name="arrow-switch" size={30} color="white" /> */}
            <Text style={{ color: 'white', textTransform: 'capitalize' }}>
              {ocrMode}
            </Text>
          </TouchableOpacity>
        )}
      </View>
      <View style={styles.centerContainer}>
        <CaptureModesView
          setCaptureMode={setCaptureMode}
          captureMode={captureMode}
        />

        <View style={styles.zoomOuterView}>
          <FlatList
            style={styles.zoomContainer}
            data={zoomLevels}
            contentContainerStyle={{
              justifyContent: 'center',
              alignItems: 'center',
            }}
            horizontal={true} // Ensures buttons are displayed in a row
            keyExtractor={(item) => item.id}
            renderItem={renderItem}
          />
        </View>

        {captureMode === 'manual' && (
          <TouchableOpacity onPress={onPressCapture} style={styles.outerCircle}>
            <View style={styles.innerCircle} />
          </TouchableOpacity>
        )}
      </View>
      <View style={[styles.sideContainer]}>
        {ocrMode != 'cloud' && (
          <TouchableOpacity
            onPress={() => setShowOcrSize(true)}
            style={styles.sizeIconContainer}
          >
            <Text style={{ color: 'white' }}>
              {modelSize === 'large' ? 'Large' : 'Micro'}
            </Text>
          </TouchableOpacity>
        )}
      </View>
      <OCRSelectionView
        setShowOcrTypes={setShowOcrTypes}
        showOcrTypes={showOcrTypes}
        setOcrMode={setOcrMode}
        ocrMode={ocrMode}
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
    alignContent: 'space-between',
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
  circle: {
    width: 35, // Adjust the width and height as needed
    height: 30,
    marginHorizontal: 4,
    marginVertical: 1,
    borderRadius: 25, // Half of the width/height to make it a circle
    justifyContent: 'center',
    alignItems: 'center',
  },
  zoomOuterView: {
    position: 'absolute',
    top: -60,
  },
  zoomTextStyle: {
    color: 'white',
    justifyContent: 'center',
    textTransform: 'capitalize',
    fontSize: 14,
  },
  zoomContainer: {
    width: '100%',
    flexDirection: 'row',
    height: 50,
  },
});

export default CameraFooterView;
