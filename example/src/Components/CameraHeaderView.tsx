import React, { useState } from 'react';
import { View, StyleSheet, Platform, TouchableOpacity } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import FontAwesome from 'react-native-vector-icons/FontAwesome';
import MaterialCommunityIcons from 'react-native-vector-icons/MaterialCommunityIcons';
import ModeSelectionView from './ModeSelectionView';

function CameraHeaderView({ detectedData, toggleFlash, mode, setMode }: any) {
  const [isFlashOn, setIsFlashOn] = useState<boolean>(false);
  const [showModeTypes, setShowModeTypes] = useState<boolean>(false);
  const checkIconColor = (val: boolean) => {
    return val ? '#4FBF67' : 'white';
  };
  return (
    <View style={styles.mainContainer}>
      <View style={styles.leftIconContainer}>
        <View style={styles.itemIconContainer}>
          <Icon
            name={'text-outline'}
            size={20}
            color={checkIconColor(detectedData?.text)}
          />
        </View>
        <View style={styles.itemIconContainer}>
          <FontAwesome
            name="barcode"
            size={20}
            color={checkIconColor(detectedData?.barcode)}
          />
        </View>
        <View style={styles.itemIconContainer}>
          <Icon
            name="qr-code-outline"
            size={20}
            color={checkIconColor(detectedData?.qrcode)}
          />
        </View>
        <View style={styles.itemIconContainer}>
          <MaterialCommunityIcons
            name="file-document-outline"
            size={20}
            color={checkIconColor(detectedData?.document)}
          />
        </View>
      </View>

      <View style={[styles.sideContainer, styles.rotatedIcon]}>
        <TouchableOpacity
          onPress={() => {
            setShowModeTypes(true);
          }}
          style={styles.switchIconContainer}
        >
          <Text style={{ color: 'white' }}>
            {mode.charAt(0).toUpperCase() + mode.slice(1)}
          </Text>
        </TouchableOpacity>
      </View>

      <View>
        <TouchableOpacity
          onPress={() => {
            setIsFlashOn(!isFlashOn);
            toggleTorch(!isFlashOn);
          }}
          style={styles.rightIconContainer}
        >
          <Icon
            name={isFlashOn ? 'flash-outline' : 'flash-off-outline'}
            size={20}
            color="white"
          />
        </TouchableOpacity>
      </View>

      <ModeSelectionView
        showModeTypes={showModeTypes}
        setShowModeTypes={setShowModeTypes}
        mode={mode}
        setMode={setMode}
      />
    </View>
  );
}
const styles = StyleSheet.create({
  mainContainer: {
    position: 'absolute',
    width: '100%',
    top: Platform.OS === 'android' ? 10 : 45,
    zIndex: 1,
    paddingHorizontal: 5,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  itemIconContainer: {
    padding: 10,
  },
  rightIconContainer: {
    padding: 10,
    borderRadius: 10,
    backgroundColor:
      Platform.OS === 'android' ? 'rgba(0, 0, 0, 0.7)' : 'rgba(0, 0, 0, 0.5)',
  },
  leftIconContainer: {
    justifyContent: 'space-between',
    borderRadius: 10,
    backgroundColor:
      Platform.OS === 'android' ? 'rgba(0, 0, 0, 0.7)' : 'rgba(0, 0, 0, 0.5)',
  },
  sideContainer: {
    width: '35%',
    height: '30%',
    justifyContent: 'center',
    alignItems: 'flex-end',
    borderWidth: 0,
    borderColor: 'black',
  },
  rotatedIcon: {
    flexDirection: 'row',
  },
  switchIconContainer: {
    backgroundColor: '#7420E2',
    paddingVertical: 8,
    paddingHorizontal: 15,
    borderRadius: 10,
  },
});
export default CameraHeaderView;
