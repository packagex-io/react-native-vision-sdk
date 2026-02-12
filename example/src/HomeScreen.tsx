import React, { useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';

import { VisionCore } from '../../src/index';

const HomeScreen = ({ navigation }) => {
  useEffect(() => {
    VisionCore.setEnvironment('staging');
  }, []);

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scrollContainer}>
      <View style={styles.headerContainer}>
        <Text style={styles.title}>Vision SDK</Text>
        <Text style={styles.subtitle}>React Native Example</Text>
        <Text style={styles.description}>
          Explore barcode scanning, OCR predictions, and model management capabilities.
        </Text>
      </View>

      <View style={styles.navigationContainer}>
        <TouchableOpacity
          style={styles.primaryButton}
          onPress={() => navigation.navigate("ModelManagementScreen")}
        >
          <Text style={styles.primaryButtonText}>Model Management</Text>
          <Text style={styles.buttonDescription}>
            Download, load, and test on-device & cloud predictions
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.primaryButton}
          onPress={() => navigation.navigate("VisionCameraExample")}
        >
          <Text style={styles.primaryButtonText}>Vision Camera</Text>
          <Text style={styles.buttonDescription}>
            Camera-based barcode scanning and OCR
          </Text>
        </TouchableOpacity>
      </View>

      <View style={styles.footerContainer}>
        <Text style={styles.footerText}>
          Vision SDK v3.0.0
        </Text>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollContainer: {
    flexGrow: 1,
    padding: 20,
    justifyContent: 'center',
  },
  headerContainer: {
    alignItems: 'center',
    marginBottom: 40,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#333',
    textAlign: 'center',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 18,
    color: '#666',
    textAlign: 'center',
    marginBottom: 16,
  },
  description: {
    fontSize: 14,
    color: '#888',
    textAlign: 'center',
    paddingHorizontal: 20,
    lineHeight: 20,
  },
  navigationContainer: {
    gap: 16,
    marginBottom: 40,
  },
  primaryButton: {
    backgroundColor: '#007bff',
    paddingVertical: 20,
    paddingHorizontal: 24,
    borderRadius: 12,
    shadowColor: '#007bff',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  primaryButtonText: {
    fontSize: 18,
    color: '#fff',
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 4,
  },
  buttonDescription: {
    fontSize: 13,
    color: 'rgba(255, 255, 255, 0.8)',
    textAlign: 'center',
  },
  footerContainer: {
    alignItems: 'center',
    paddingTop: 20,
  },
  footerText: {
    fontSize: 12,
    color: '#aaa',
  },
});

export default HomeScreen;
