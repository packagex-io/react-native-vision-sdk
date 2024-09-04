import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';

function ModeSelectionView({
  showModeTypes,
  setShowModeTypes,
  mode,
  setMode,
}: any) {
  const closeModal = () => {
    setShowModeTypes(false);
  };
  return (
    <Modal
      animationType="fade"
      transparent
      visible={showModeTypes}
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
              setMode('barcode');
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>Barcode</Text>
            {mode == 'barcode' && (
              <MaterialIcons name="done" size={20} color="white" />
            )}
          </TouchableOpacity>
          <View style={styles.horizontalLine} />
          <TouchableOpacity
            onPress={() => {
              setMode('qrcode');
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>QR Code</Text>
            {mode == 'qrcode' && (
              <MaterialIcons name="done" size={20} color="white" />
            )}
             
          </TouchableOpacity>
          <View style={styles.horizontalLine} />
          <TouchableOpacity
            onPress={() => {
              setMode('ocr');
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>OCR</Text>
            {mode == 'ocr' && (
              <MaterialIcons name="done" size={20} color="white" />
            )}
          </TouchableOpacity>
          <View style={styles.horizontalLine} />
          <TouchableOpacity
            onPress={() => {
              setMode('photo');
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>Photo</Text>
            {mode == 'photo' && (
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
    justifyContent: 'flex-start',
    left: 100,
    top: 100,
  },
  modalView: {
    backgroundColor: '#33343A',
    borderRadius: 12,
    paddingVertical: 10,
    width: '55%',
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

export default ModeSelectionView;
