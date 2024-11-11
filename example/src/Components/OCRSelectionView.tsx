import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';

type OCRSelectionViewProps = {
  setShowOcrTypes: (show: boolean) => void;
  showOcrTypes: boolean;
  setOcrMode: (mode:  "cloud" | "on-device" | "on-device-with-translation" | 'bill-of-lading') => void;
  ocrMode: string;
};

const OCRSelectionView: React.FC<OCRSelectionViewProps> = ({
  setShowOcrTypes,
  showOcrTypes,
  setOcrMode,
  ocrMode,
}) => {
  const closeModal = () => setShowOcrTypes(false);

  const options = [
    { label: 'Cloud', mode: 'cloud' },
    { label: 'On-Device', mode: 'on-device' },
    { label: 'On-Device With Translation', mode: 'on-device-with-translation' },
    { label: 'Bill Of Lading', mode: 'bill-of-lading' },
  ];

  const renderOption = ({ label, mode }: { label: string; mode: string }) => (
    <TouchableOpacity
      key={mode}
      onPress={() => {
        setOcrMode(mode);
        closeModal();
      }}
      style={styles.rowStyle}
    >
      <Text style={styles.textStyle}>{label}</Text>
      {ocrMode === mode && <MaterialIcons name="done" size={20} color="white" />}
    </TouchableOpacity>
  );

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
          {options.map((option) => (
            <React.Fragment key={option.mode}>
              {renderOption(option)}
              <View style={styles.horizontalLine} />
            </React.Fragment>
          ))}
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

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
    width: '55%',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
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
