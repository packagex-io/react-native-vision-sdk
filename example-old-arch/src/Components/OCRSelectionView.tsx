import React, { useCallback } from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';
import { OCRConfig, OCRMode, OCRType } from '../../../src/index';

type OCRSelectionViewProps = {
  setShowOcrTypes: (show: boolean) => void;
  showOcrTypes: boolean;
  ocrConfig: OCRConfig;
  setOcrConfig: (config: OCRConfig) => void;
};

const OCRSelectionView: React.FC<OCRSelectionViewProps> = ({
  setShowOcrTypes,
  showOcrTypes,
  ocrConfig,
  setOcrConfig

}) => {
  const closeModal = () => setShowOcrTypes(false);

  const options: { label: string; mode: OCRMode; type: OCRType }[] = [
    { label: 'Shipping Label (Cloud)', mode: 'cloud', type: 'shipping-label' },
    { label: 'Shipping Label (Device)', mode: 'on-device', type: 'shipping-label' },
    { label: 'On-Device With Translation', mode: 'on-device', type: 'on-device-with-translation' },
    { label: 'Bill Of Lading (Cloud)', mode: 'cloud', type: 'bill-of-lading' },
    { label: 'Bill Of Lading (Device)', mode: 'on-device', type: 'bill-of-lading' },
    { label: 'Item Label (Cloud)', mode: 'cloud', type: 'item-label' },
    { label: 'Item Label (Device)', mode: 'on-device', type: 'item-label' },
    { label: 'Document Classification (Cloud)', mode: 'cloud', type: 'document-classification' },
    { label: 'Document Classification (Device)', mode: 'on-device', type: 'document-classification' },
  ];

  const Option = ({ label, mode, type }: { label: string; mode: OCRMode; type: OCRType; }) => {
    return (

      <View style={{ justifyContent: 'space-between', alignItems: 'center', flexDirection: 'row', width: '100%', height: 48 }}>
        <Text style={styles.textStyle}>{label}</Text>
        {ocrConfig.mode.toLowerCase().trim() === mode.toLowerCase().trim() && ocrConfig.type.toLowerCase().trim() === type.toLowerCase().trim() && (
          <MaterialIcons name="done" size={20} color="white" />
        )}
      </View>

    )
  };


  const handlePressOption = useCallback((mode, type) => {
    setOcrConfig({
      ...ocrConfig,
      mode,
      type,
      size: ocrConfig.type === type ? ocrConfig.size : 'large'
    });

    closeModal();
  }, [])

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
          {options.map((option, i) => (
            <React.Fragment key={i}>
              <TouchableOpacity style={styles.rowStyle} onPress={() => handlePressOption(option.mode, option.type)}>
                <Option
                  label={option.label}
                  mode={option.mode}
                  type={option.type}
                />


              </TouchableOpacity>
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
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
  },
  textStyle: {
    fontSize: 14,
    color: 'white',
    alignSelf: 'center'
  },
});

export default OCRSelectionView;
