import React, { useState } from 'react';
import { View, StyleSheet, Platform, TouchableOpacity } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import FontAwesome from 'react-native-vector-icons/FontAwesome';

function CameraHeaderView({ detectedData, toggleTorch }: any) {
  const [isFlashOn, setIsFlashOn] = useState<boolean>(false);
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
            color={checkIconColor(
              Platform.OS === 'android'
                ? detectedData?.barcode
                : detectedData?.barCode
            )}
          />
        </View>
        <View style={styles.itemIconContainer}>
          <Icon
            name="qr-code-outline"
            size={20}
            color={checkIconColor(
              Platform.OS === 'android'
                ? detectedData?.qrcode
                : detectedData?.qrCode
            )}
          />
        </View>
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
});
export default CameraHeaderView;
