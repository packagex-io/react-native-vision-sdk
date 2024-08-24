import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';

function OCRSelectionView({
  setShowOcrTypes,
  showOcrTypes,
  setIsOnDeviceOCR,
  isOnDeviceOCR,
}: any) {
  const closeModal = () => {
    setShowOcrTypes(false);
  };
  return (
    <Modal
      animationType="fade"
      transparent
      visible={showOcrTypes}
      onRequestClose={closeModal}
    >
      <TouchableOpacity
        activeOpacity={1}
        onPress={closeModal}
        style={styles.centeredViewModal}
      >
        <View style={styles.modalView}>
          <TouchableOpacity
            onPress={() => {
              setIsOnDeviceOCR(!isOnDeviceOCR);
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>SL Cloud OCR</Text>
            {!isOnDeviceOCR && (
              <MaterialIcons name="done" size={20} color="white" />
            )}
          </TouchableOpacity>
          <View style={styles.horizontalLine} />
          <TouchableOpacity
            onPress={() => {
              setIsOnDeviceOCR(!isOnDeviceOCR);
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>SL On-Device OCR</Text>
            {isOnDeviceOCR && (
              <MaterialIcons name="done" size={20} color="white" />
            )}
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    </Modal>
  );
}
const styles = StyleSheet.create({
  centeredViewModal: {
    flex: 1,
    marginRight: 10,
    alignItems: 'flex-start',
    justifyContent: 'flex-end',
    left: 20,
    bottom: 100,
  },
  modalView: {
    backgroundColor: '#33343A',
    borderRadius: 12,
    paddingVertical: 10,
    width: '48%',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  horizontalLine: {
    height: 1,
    width: '100%',
    backgroundColor: '#4D4D57',
    marginVertical: 5,
  },
  rowStyle: {
    height: 48,
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
  },
  textStyle: {
    fontSize: 14,
    color: 'white',
  },
});

export default OCRSelectionView;
