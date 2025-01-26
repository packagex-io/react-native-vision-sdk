import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';

function ModelSizeSelectionView({
  setShowOcrSize,
  showOcrSize,
  setModelSize,
  modelSize,
  ocrType
}: any) {
  const closeModal = () => {
    setShowOcrSize(false);
  };

  const modelSizeOptions = [{
    id: 1,
    label: 'Nano',
    size: 'nano',
    disabled: true,
  },{
    id: 2,
    label: 'Micro',
    size: 'micro',
    disabled: !['shipping-label', 'shipping_label'].includes(ocrType) ,
  }, {
    id: 3,
    label: 'Small',
    size: 'small',
    disabled: true,
  },{
    id: 4,
    label: 'Medium',
    size: 'medium',
    disabled: true,
  },{
    id: 5,
    label: 'Large',
    size: 'large',
    disabled: false
  }]

  return (
    <Modal
      animationType="fade"
      transparent
      visible={showOcrSize}
      onRequestClose={closeModal}
    >
      <TouchableOpacity
        activeOpacity={1}
        onPress={closeModal}
        style={styles.centeredViewModal}
      >
        <View style={styles.modalView}>
          {modelSizeOptions.map((option, i) => (
            <React.Fragment key={i}>
              <TouchableOpacity
                disabled={option.disabled}
                onPress={() => {
                  if (modelSize !== option.size) {
                    setModelSize(option.size);
                  }
                  closeModal();
                }}
                style={{...styles.rowStyle, opacity: option.disabled ? 0.5 : 1}}
              >
                <Text style={styles.textStyle}>{option.label}</Text>
                {modelSize === option.size && (
                  <MaterialIcons name="done" size={20} color="white" />
                )}
              </TouchableOpacity>
              <View style={styles.horizontalLine} />
            </React.Fragment>
          ))}
          {/* {['shipping_label', 'shipping-label'].includes(ocrType) ?
            <>
              <TouchableOpacity
                onPress={() => {
                  if (modelSize !== 'micro') {
                    setModelSize('micro');
                  }
                  closeModal();
                }}
                style={styles.rowStyle}
              >
                <Text style={styles.textStyle}>Micro</Text>
                {modelSize === 'micro' && (
                  <MaterialIcons name="done" size={20} color="white" />
                )}
              </TouchableOpacity>

              <View style={styles.horizontalLine} />
            </>
            : null}
          <TouchableOpacity
            onPress={() => {
              if (modelSize !== 'large') {
                setModelSize('large');
              }
              closeModal();
            }}
            style={styles.rowStyle}
          >
            <Text style={styles.textStyle}>Large</Text>
            {modelSize === 'large' && (
              <MaterialIcons name="done" size={20} color="white" />
            )}
          </TouchableOpacity> */}
        </View>
      </TouchableOpacity>
    </Modal>
  );
}
const styles = StyleSheet.create({
  centeredViewModal: {
    flex: 1,
    marginRight: 10,
    alignItems: 'flex-end',
    justifyContent: 'flex-end',
    right: 20,
    bottom: 100,
  },
  modalView: {
    backgroundColor: '#33343A',
    borderRadius: 12,
    paddingVertical: 10,
    width: '35%',
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

export default ModelSizeSelectionView;
