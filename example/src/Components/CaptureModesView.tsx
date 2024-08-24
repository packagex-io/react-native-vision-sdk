import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';

function CaptureModesView({ setCaptureMode, captureMode }: any) {
  return (
    <View style={styles.mainContainer}>
      <TouchableOpacity
        onPress={() => setCaptureMode('manual')}
        style={{
          ...styles.tabItemContainer,
          backgroundColor: captureMode === 'manual' ? 'white' : 'grey',
        }}
      >
        <Text style={styles.tabItemText}>Manual</Text>
      </TouchableOpacity>
      <TouchableOpacity
        onPress={() => setCaptureMode('auto')}
        style={{
          ...styles.tabItemContainer,
          backgroundColor: captureMode === 'auto' ? 'white' : 'grey',
        }}
      >
        <Text style={styles.tabItemText}>Auto</Text>
      </TouchableOpacity>
    </View>
  );
}
const styles = StyleSheet.create({
  mainContainer: {
    flexDirection: 'row',
    position: 'absolute',
    top: 0,
    backgroundColor: 'grey',
    borderRadius: 10,
  },
  tabItemContainer: {
    backgroundColor: 'white',
    borderColor: 'red',
    borderWidth: 0,
    padding: 7,
    borderRadius: 10,
  },
  tabItemText: {
    fontSize: 12,
    color: 'black',
    fontWeight: 'bold',
  },
});
export default CaptureModesView;
