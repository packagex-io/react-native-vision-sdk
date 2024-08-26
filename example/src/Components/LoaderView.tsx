import React from 'react';
import {
  View,
  StyleSheet,
  ActivityIndicator,
  TouchableOpacity,
  Modal,
  Text,
} from 'react-native';

function LoaderView({ visible }: any) {
  return (
    <Modal animationType="fade" transparent visible={visible}>
      <TouchableOpacity activeOpacity={1} style={styles.centeredViewModal}>
        <View style={styles.modalView}>
          <ActivityIndicator color={'white'} size={'large'} />
          <Text style={styles.descriptionTextStyle}>preparing...</Text>
        </View>
      </TouchableOpacity>
    </Modal>
  );
}
const styles = StyleSheet.create({
  descriptionTextStyle: {
    color: 'white',
    marginTop: 15,
    letterSpacing: 2,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '500',
    left: 5,
    textAlignVertical: 'center',
  },
  centeredViewModal: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderColor: 'red',
    borderWidth: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
  },
  modalView: {
    borderRadius: 12,
    paddingVertical: 10,
    height: '50%',
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default LoaderView;
