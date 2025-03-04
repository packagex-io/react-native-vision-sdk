import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ActivityIndicator
} from 'react-native';

import { VisionCore } from '../../src/index';
import { useFocusEffect } from '@react-navigation/native';

const HomeScreen = ({ navigation }) => {
  const [selectedModelType, setSelectedModelType] = useState<string>('shipping_label');
  const [selectedModelSize, setSelectedModelSize] = useState<string>('large');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [downloadProgress, setDownloadProgress] = useState<number>(0);
  const [isModelReady, setIsModelReady] = useState<boolean>(false);

  const modelTypes = [
    { label: 'Shipping Label', value: 'shipping_label' },
    { label: 'Item Label', value: 'item_label' },
    { label: 'Bill of Lading', value: 'bill_of_lading' },
    { label: 'Document Classification', value: 'document_classification' },
  ];

  const modelSizes = [
    { label: 'Nano', value: 'nano' },
    { label: 'Micro', value: 'micro' },
    { label: 'Small', value: 'small' },
    { label: 'Medium', value: 'medium' },
    { label: 'Large', value: 'large' },

  ];

  useEffect(() => {
    VisionCore.setEnvironment('staging')
  }, []);


  useFocusEffect(
    useCallback(() => {
      const subscription = VisionCore.onModelDownloadProgress((progress, downloadStatus, isReady) => {
        setDownloadProgress(progress * 100);
        setIsModelReady(isReady);
      });

      return () => {
        subscription.remove();
      };
    }, [])
  );




  useEffect(() => {
    setIsModelReady(false)
    setDownloadProgress(0)
    setIsLoading(false)
  }, [selectedModelSize, selectedModelType])

  useEffect(() => {
    setSelectedModelSize('large')
  }, [selectedModelType])

  const handleLoadModel = async () => {
    setIsLoading(true);
    setDownloadProgress(0);

    try {
      await VisionCore.loadModel(
        null,
        "",
        selectedModelType,
        selectedModelSize,
        // "staging"
      );

      // Alert.alert('Success', `Model '${selectedModelType}' (${selectedModelSize}) loaded successfully`);
      setIsModelReady(true)
    } catch (error) {
      Alert.alert('Error', `Failed to load model: ${error.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>Model Type:</Text>
      <View style={styles.chipContainer}>
        {modelTypes.map((type) => (
          <TouchableOpacity
            key={type.value}
            style={[
              styles.chip,
              selectedModelType === type.value && styles.selectedChip
            ]}
            onPress={() => setSelectedModelType(type.value)}
          >
            <Text style={[
              styles.chipText,
              selectedModelType === type.value && styles.selectedChipText
            ]}>
              {type.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Model Size:</Text>
      <View style={styles.chipContainer}>
        {modelSizes.map((size) => {
          const isDisabled = (selectedModelType === 'shipping_label' && !['large', 'micro'].includes(size.value)) || (selectedModelType !== 'shipping_label' && size.value !== 'large');
          return (
            <TouchableOpacity
              key={size.value}
              style={[
                styles.chip,
                selectedModelSize === size.value && styles.selectedChip,
                isDisabled && styles.disabledChip
              ]}
              onPress={() => !isDisabled && setSelectedModelSize(size.value)}
              disabled={isDisabled}
            >
              <Text style={[
                styles.chipText,
                selectedModelSize === size.value && styles.selectedChipText,
                isDisabled && styles.disabledChipText
              ]}>
                {size.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Progress Indicator */}
      {isLoading && (
        <View style={styles.progressContainer}>
          <Text style={styles.progressText}>
            Downloading: {downloadProgress.toFixed(1)}%
          </Text>
          <ActivityIndicator size="small" color="#007bff" />
        </View>
      )}

      {isModelReady && <Text style={styles.successText}>✅ Model is ready!</Text>}

      {/* Load Model Button */}
      <TouchableOpacity
        style={[styles.button, isLoading || isModelReady ? styles.disabledButton : null]}
        onPress={handleLoadModel}
        disabled={isLoading || isModelReady}
      >
        <Text style={styles.buttonText}>
          {isLoading ? 'Loading...' : 'Load Model'}
        </Text>
      </TouchableOpacity>

      {/* Navigate to Camera Screen */}
      <TouchableOpacity
        style={[styles.secondaryButton, isLoading ? styles.disabledButton : null]}
        disabled={isLoading}
        onPress={() => navigation.navigate("CameraScreen",
          isModelReady ? {
            modelSize: selectedModelSize,
            modelType: selectedModelType,
            mode: 'ocr'
          } : {})}
      >
        <Text style={[styles.secondaryButtonText]}>Open VSDK Camera View</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    padding: 20,
    backgroundColor: '#fff'
  },
  label: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 5
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 20
  },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 15,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#007bff',
    marginRight: 10,
    marginBottom: 10
  },
  selectedChip: {
    backgroundColor: '#007bff'
  },
  chipText: {
    fontSize: 14,
    color: '#007bff'
  },
  selectedChipText: {
    color: '#fff'
  },
  disabledChip: {
    backgroundColor: '#e0e0e0',
    borderColor: '#aaa'
  },
  disabledChipText: {
    color: '#aaa'
  },
  progressContainer: {
    marginTop: 15,
    alignItems: 'center'
  },
  progressText: {
    fontSize: 16,
    color: '#007bff'
  },
  successText: {
    fontSize: 16,
    color: 'green',
    marginTop: 10,
    fontWeight: 'bold'
  },
  button: {
    backgroundColor: '#007bff',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 20
  },
  buttonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: 'bold'
  },
  disabledButton: {
    backgroundColor: '#cccccc'
  },
  secondaryButton: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: '#007bff',
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 10
  },
  secondaryButtonText: {
    fontSize: 16,
    color: '#007bff',
    fontWeight: 'bold'
  }
});

export default HomeScreen;
