import React from 'react';
import { View, StyleSheet, Text, TouchableOpacity, Modal } from 'react-native';
import { AnimatedCircularProgress } from 'react-native-circular-progress';

function DownloadingProgressView({ visible, progress }: any) {
  return (
    <Modal animationType="fade" transparent visible={visible}>
      <TouchableOpacity activeOpacity={1} style={styles.centeredViewModal}>
        <View style={styles.modalView}>
          <View style={{ alignSelf: 'center' }}>
            <AnimatedCircularProgress
              size={180}
              width={3}
              rotation={0}
              fill={Math.floor(progress * 100)}
              tintColor="#00e0ff"
              backgroundColor="#3d5875"
            >
              {(fill) => (
                <Text
                  style={{ color: 'white', fontSize: 30, fontWeight: 'bold' }}
                >
                  {Number(Math.floor(fill))}%
                </Text>
              )}
            </AnimatedCircularProgress>
          </View>
          <Text style={styles.descriptionTextStyle}>
            Please wait while on-device OCR model initializes. Once ready, you
            can proceed with regular scanning. This initialization is a one-time
            process.
          </Text>
        </View>
      </TouchableOpacity>
    </Modal>
  );
}
const styles = StyleSheet.create({
  descriptionTextStyle: {
    color: 'white',
    marginHorizontal: 10,
    marginTop: 30,
    letterSpacing: 2,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: 'bold',
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
    backgroundColor: '#33343A',
    borderRadius: 12,
    paddingVertical: 10,
    width: '76%',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
    height: '50%',
    justifyContent: 'center',
    alignItems: 'center',
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

export default DownloadingProgressView;
