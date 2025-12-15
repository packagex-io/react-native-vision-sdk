import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
  ScrollView,
  Image,
  Clipboard
} from 'react-native';

import { VisionCore } from '../../src/index';
import { useFocusEffect } from '@react-navigation/native';


const api_key = "" // Add your PackageX API key here
const HomeScreen = ({ navigation }) => {
  const [selectedModelType, setSelectedModelType] = useState<string>('shipping_label');
  const [selectedModelSize, setSelectedModelSize] = useState<string>('large');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [downloadProgress, setDownloadProgress] = useState<number>(0);
  const [isModelReady, setIsModelReady] = useState<boolean>(false);
  const [isPredicting, setIsPredicting] = useState<boolean>(false);
  const [predictionResult, setPredictionResult] = useState<any>(null);
  const [showPredictionExample, setShowPredictionExample] = useState<boolean>(false);

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

  // Helper function to get sample image for model type
  const getSampleImageForModelType = (modelType: string) => {
    switch (modelType) {
      case 'shipping_label':
        return 'https://cdn.shopify.com/s/files/1/0070/7032/files/image1_a462b651-c72f-4e21-8048-35763b21eef1.png?v=1671219333';
      case 'item_label':
        return 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ8Jmenckba1oRIkRgKXLEDBjzcohm028JTsg&s'; // Replace with actual item label image
      case 'bill_of_lading':
        return 'https://www.freightera.com/blog/wp-content/uploads/2022/09/bol-basic-info-904x1024.jpg'; // Replace with actual BOL image
      case 'document_classification':
        return 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ8Jmenckba1oRIkRgKXLEDBjzcohm028JTsg&s'; // Replace with actual document image
      default:
        return 'https://cdn.shopify.com/s/files/1/0070/7032/files/image1_a462b651-c72f-4e21-8048-35763b21eef1.png?v=1671219333';
    }
  };
  // Function to copy prediction results to clipboard
  const copyResults = () => {
    if (predictionResult) {
      const resultText = typeof predictionResult === 'string'
        ? predictionResult
        : JSON.stringify(predictionResult, null, 2);

      Clipboard.setString(resultText);
      Alert.alert('Copied! 📋', 'Prediction results copied to clipboard');
    }
  };


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
        api_key, // Use your PackageX API key
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

  const handleCompleteWorkflow = async () => {
    try {
      setIsPredicting(true);
      setPredictionResult(null);
      setIsLoading(true);
      setDownloadProgress(0);

      // Step 1: Set environment and load selected model
      VisionCore.setEnvironment('staging');

      const modelTypeLabel = modelTypes.find(t => t.value === selectedModelType)?.label || selectedModelType;
      Alert.alert('Starting Complete Workflow', `Step 1: Downloading ${modelTypeLabel} ${selectedModelSize} model for on-device prediction...`);

      // Wait for model to download with progress tracking
      await VisionCore.loadModel(
        null,
        api_key, // Use your PackageX API key
        selectedModelType,
        selectedModelSize
      );

      setIsLoading(false);
      setIsModelReady(true);

      Alert.alert('Model Downloaded! 📱', `Step 2: Making ON-DEVICE ${modelTypeLabel} prediction...`);

      // Step 2: Use appropriate sample image and barcodes for selected model type
      const sampleImageUrl = getSampleImageForModelType(selectedModelType);


      // Step 3: Make ON-DEVICE prediction using our new standalone method
      const result = await VisionCore.predict(sampleImageUrl, []);

      setPredictionResult(result);
      setShowPredictionExample(true);

      Alert.alert('On-Device Success! 🎉', `${modelTypeLabel} on-device prediction completed successfully! Check the results below.`);

    } catch (error) {
      Alert.alert('Error', `Workflow failed: ${error.message}`);
    } finally {
      setIsPredicting(false);
      setIsLoading(false);
    }
  };

  const handleOnDevicePrediction = async () => {
    try {
      setIsPredicting(true);
      setPredictionResult(null);

      const modelTypeLabel = modelTypes.find(t => t.value === selectedModelType)?.label || selectedModelType;
      Alert.alert('Starting On-Device Prediction', `Using pre-loaded ${modelTypeLabel} model for prediction...`);

      // Use appropriate sample image and barcodes for selected model type
      const sampleImageUrl = getSampleImageForModelType(selectedModelType);


      const result = await VisionCore.predict(sampleImageUrl, []);

      setPredictionResult(result);
      setShowPredictionExample(true);

      Alert.alert('On-Device Success! 🎉', `${modelTypeLabel} on-device prediction completed! Check results below.`);

    } catch (error) {
      Alert.alert('Error', `On-device prediction failed: ${error.message}`);
    } finally {
      setIsPredicting(false);
    }
  };

  const handleCloudOnlyPrediction = async () => {
    try {
      setIsPredicting(true);
      setPredictionResult(null);

      const modelTypeLabel = modelTypes.find(t => t.value === selectedModelType)?.label || selectedModelType;
      Alert.alert('Starting Cloud Prediction', `Making cloud ${modelTypeLabel} prediction without model download...`);

      const sampleImageUrl = getSampleImageForModelType(selectedModelType);

      let result;

      // Call appropriate cloud prediction method based on selected model type
      // Each method has different parameters based on VisionCoreWrapper signatures
      switch (selectedModelType) {
        case 'shipping_label':
          result = await VisionCore.predictShippingLabelCloud(
            sampleImageUrl,
            [], // barcodes
            null, // token
            api_key, // apiKey
            null, // locationId
            null, // options
            null, // metadata
            null, // recipient
            null, // sender
            true // shouldResizeImage
          );
          break;
        case 'item_label':
          result = await VisionCore.predictItemLabelCloud(
            sampleImageUrl,
            null, // token
            api_key, // apiKey
            true // shouldResizeImage
          );
          break;
        case 'bill_of_lading':
          result = await VisionCore.predictBillOfLadingCloud(
            sampleImageUrl,
            [], // barcodes
            null, // token
            api_key, // apiKey
            null, // locationId
            null, // options
            true // shouldResizeImage
          );
          break;
        case 'document_classification':
          result = await VisionCore.predictDocumentClassificationCloud(
            sampleImageUrl,
            null, // token
            api_key, // apiKey
            true // shouldResizeImage
          );
          break;
        default:
          result = await VisionCore.predictShippingLabelCloud(
            sampleImageUrl,
            [],
            null,
            api_key,
            null,
            null,
            null,
            null,
            null,
            true
          );
      }

      setPredictionResult(result);
      setShowPredictionExample(true);

      Alert.alert('Cloud Success! ☁️', `${modelTypeLabel} cloud prediction completed! Check results below.`);

    } catch (error) {
      Alert.alert('Error', `Cloud prediction failed: ${error.message}`);
    } finally {
      setIsPredicting(false);
    }
  };

  const handleHybridPrediction = async () => {
    try {
      setIsPredicting(true);
      setPredictionResult(null);

      const modelTypeLabel = modelTypes.find(t => t.value === selectedModelType)?.label || selectedModelType;
      Alert.alert('Starting Hybrid Prediction', `Using on-device ${modelTypeLabel} model + cloud transformations...`);

      const sampleImageUrl = getSampleImageForModelType(selectedModelType);

      const result = await VisionCore.predictWithCloudTransformations(sampleImageUrl, [], {
        apiKey: api_key,
        shouldResizeImage: true
      });

      setPredictionResult(result);
      setShowPredictionExample(true);

      Alert.alert('Hybrid Success! 🎉', `${modelTypeLabel} hybrid prediction completed! Check results below.`);

    } catch (error) {
      Alert.alert('Error', `Hybrid prediction failed: ${error.message}`);
    } finally {
      setIsPredicting(false);
    }
  };

  const handleUnloadCurrentModel = async () => {
    try {
      const modelTypeLabel = modelTypes.find(t => t.value === selectedModelType)?.label || selectedModelType;
      const unloaded = await VisionCore.unloadModel({
        type: selectedModelType as any,
        size: selectedModelSize as any,
      });

      if (unloaded) {
        setIsModelReady(false);
        Alert.alert('Success! ✅', `${modelTypeLabel} (${selectedModelSize}) unloaded from memory`);
      } else {
        Alert.alert('Notice ⚠️', `${modelTypeLabel} (${selectedModelSize}) was not loaded`);
      }
    } catch (error) {
      Alert.alert('Error', `Failed to unload model: ${error.message}`);
    }
  };

  const handleDeleteCurrentModel = async () => {
    const modelTypeLabel = modelTypes.find(t => t.value === selectedModelType)?.label || selectedModelType;

    Alert.alert(
      'Confirm Delete',
      `Delete ${modelTypeLabel} (${selectedModelSize}) from disk?\n\nThis will remove the model file permanently.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              const deleted = await VisionCore.deleteModel({
                type: selectedModelType as any,
                size: selectedModelSize as any,
              });

              if (deleted) {
                setIsModelReady(false);
                Alert.alert('Success! 🗑️', `${modelTypeLabel} (${selectedModelSize}) deleted from disk`);
              } else {
                Alert.alert('Notice ⚠️', `${modelTypeLabel} (${selectedModelSize}) was not found on disk`);
              }
            } catch (error) {
              Alert.alert('Error', `Failed to delete model: ${error.message}`);
            }
          },
        },
      ]
    );
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scrollContainer}>
      <Text style={styles.title}>VisionCore Standalone Prediction Examples</Text>
      <Text style={styles.subtitle}>Demonstrate headless OCR predictions without camera</Text>

      {/* Model Configuration */}
      <View style={styles.configContainer}>
        <Text style={styles.sectionTitle}>Model Configuration:</Text>

        <Text style={styles.label}>Model Type:</Text>
        <View style={styles.chipContainer}>
          {modelTypes.map((type) => (
            <TouchableOpacity
              key={type.value}
              style={[
                styles.chip,
                selectedModelType === type.value && styles.selectedChip
              ]}
              onPress={() => {
                setSelectedModelType(type.value);
                setIsModelReady(false);
                setPredictionResult(null);
                setShowPredictionExample(false);
              }}
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
            const isDisabled = (selectedModelType === 'shipping_label' && !['large', 'micro'].includes(size.value)) ||
                              (selectedModelType !== 'shipping_label' && size.value !== 'large');
            return (
              <TouchableOpacity
                key={size.value}
                style={[
                  styles.chip,
                  selectedModelSize === size.value && styles.selectedChip,
                  isDisabled && styles.disabledChip
                ]}
                onPress={() => {
                  if (!isDisabled) {
                    setSelectedModelSize(size.value);
                    setIsModelReady(false);
                    setPredictionResult(null);
                    setShowPredictionExample(false);
                  }
                }}
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
      </View>

      {/* Sample Image Preview */}
      <View style={styles.imageContainer}>
        <Text style={styles.sectionTitle}>Sample {modelTypes.find(t => t.value === selectedModelType)?.label}:</Text>
        <Image
          source={{ uri: getSampleImageForModelType(selectedModelType) }}
          style={styles.sampleImage}
          resizeMode="contain"
        />
      </View>

      {/* Prediction Buttons */}
      <View style={styles.buttonContainer}>
        <TouchableOpacity
          style={[styles.workflowButton, isPredicting && styles.disabledButton]}
          onPress={handleCompleteWorkflow}
          disabled={isPredicting}
        >
          <Text style={styles.workflowButtonText}>
            🚀 Complete On-Device Workflow (Download Model + On-Device Prediction)
          </Text>
        </TouchableOpacity>

        {/* Model Management Buttons */}
        <View style={styles.unloadButtonsRow}>
          <TouchableOpacity
            style={[styles.unloadButton]}
            onPress={handleUnloadCurrentModel}
          >
            <Text style={styles.unloadButtonText}>
              ⚪ Unload Current Model
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.deleteButton]}
            onPress={handleDeleteCurrentModel}
          >
            <Text style={styles.deleteButtonText}>
              🗑️ Delete Current Model
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={[styles.cloudButton, isPredicting && styles.disabledButton]}
          onPress={handleCloudOnlyPrediction}
          disabled={isPredicting}
        >
          <Text style={styles.buttonText}>
            ☁️ Cloud-Only Prediction (No Model Download)
          </Text>
        </TouchableOpacity>

        {isModelReady && (
          <>
          {/* 
            <TouchableOpacity
              style={[styles.predictionButton, isPredicting && styles.disabledButton]}
              onPress={handleOnDevicePrediction}
              disabled={isPredicting}
            >
              <Text style={styles.buttonText}>
                📱 On-Device Prediction Only
              </Text>
            </TouchableOpacity>
             */}

         {/*   

            <TouchableOpacity
              style={[styles.hybridButton, isPredicting && styles.disabledButton]}
              onPress={handleHybridPrediction}
              disabled={isPredicting}
            >
              <Text style={styles.buttonText}>
                🔄 Hybrid Prediction (On-Device + Cloud)
              </Text>
            </TouchableOpacity>
             */}
          </>
        )}
      </View>
      

      {/* Progress Indicator */}
      {(isLoading || isPredicting) && (
        <View style={styles.progressContainer}>
          <Text style={styles.progressText}>
            {isLoading
              ? `Downloading Model: ${downloadProgress.toFixed(1)}%`
              : 'Making Prediction...'
            }
          </Text>
          <ActivityIndicator size="large" color="#007bff" />
        </View>
      )}

      {/* Status Indicators */}
      {isModelReady && (
        <Text style={styles.successText}>✅ Model is ready for predictions!</Text>
      )}

      {/* Results Display */}
      {showPredictionExample && predictionResult && (
        <View style={styles.resultsContainer}>
          <View style={styles.resultsHeader}>
            <Text style={styles.resultsTitle}>🎯 Prediction Results:</Text>
            <TouchableOpacity
              style={styles.copyButton}
              onPress={copyResults}
            >
              <Text style={styles.copyButtonText}>📋 Copy</Text>
            </TouchableOpacity>
          </View>
          <ScrollView style={styles.resultsScroll} nestedScrollEnabled>
            <Text style={styles.resultsText}>
              {typeof predictionResult === 'string'
                ? predictionResult
                : JSON.stringify(predictionResult, null, 2)
              }
            </Text>
          </ScrollView>
        </View>
      )}

      {/* Navigation */}
      <TouchableOpacity
        style={[styles.primaryButton, { marginBottom: 12 }]}
        onPress={() => {
          navigation.navigate("ModelManagementScreen");
        }}
      >
        <Text style={styles.primaryButtonText}>🔧 Model Management API (New)</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.secondaryButton]}
        onPress={() => {
          navigation.navigate("CameraScreen", {
            // modelSize: 'large',
            // modelType: 'shipping_label',
            mode: 'barcode'
          })
        }}
      >
        <Text style={styles.secondaryButtonText}>📷 Open Camera View</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={[styles.primaryButton]}
        onPress={() => navigation.navigate("VisionCameraExample")}
      >
        <Text style={styles.primaryButtonText}>🎥 Open Vision Camera (New)</Text>
      </TouchableOpacity>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa'
  },
  scrollContainer: {
    padding: 20,
    paddingBottom: 40
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    textAlign: 'center',
    marginBottom: 8
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginBottom: 24
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12
  },
  configContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  },
  label: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
    marginTop: 12
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 12
  },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 15,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#007bff',
    marginRight: 10,
    marginBottom: 8,
    backgroundColor: 'transparent'
  },
  selectedChip: {
    backgroundColor: '#007bff'
  },
  chipText: {
    fontSize: 14,
    color: '#007bff',
    fontWeight: '500'
  },
  selectedChipText: {
    color: '#fff'
  },
  disabledChip: {
    backgroundColor: '#f0f0f0',
    borderColor: '#ccc'
  },
  disabledChipText: {
    color: '#999'
  },
  imageContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  },
  sampleImage: {
    width: '100%',
    height: 200,
    borderRadius: 8,
    backgroundColor: '#f0f0f0'
  },
  imageCaption: {
    fontSize: 12,
    color: '#666',
    marginTop: 8,
    fontStyle: 'italic'
  },
  buttonContainer: {
    marginBottom: 24
  },
  workflowButton: {
    backgroundColor: '#6f42c1',
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 12,
    shadowColor: '#6f42c1',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4
  },
  workflowButtonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: 'bold',
    textAlign: 'center'
  },
  predictionButton: {
    backgroundColor: '#28a745',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 10
  },
  hybridButton: {
    backgroundColor: '#fd7e14',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 10
  },
  cloudButton: {
    backgroundColor: '#17a2b8',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 20,
    marginBottom: 10
  },
  buttonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: 'bold'
  },
  disabledButton: {
    backgroundColor: '#cccccc',
    shadowOpacity: 0
  },
  progressContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    alignItems: 'center',
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  },
  progressText: {
    fontSize: 16,
    color: '#007bff',
    marginBottom: 12,
    textAlign: 'center'
  },
  successText: {
    fontSize: 18,
    color: '#28a745',
    fontWeight: 'bold',
    textAlign: 'center',
    backgroundColor: '#d4edda',
    padding: 12,
    borderRadius: 8,
    marginBottom: 20
  },
  resultsContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3
  },
  resultsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12
  },
  resultsTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    flex: 1
  },
  copyButton: {
    backgroundColor: '#007bff',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 6,
    shadowColor: '#007bff',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
    elevation: 2
  },
  copyButtonText: {
    fontSize: 14,
    color: '#fff',
    fontWeight: '600'
  },
  resultsScroll: {
    maxHeight: 300,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    padding: 12
  },
  resultsText: {
    fontSize: 14,
    color: '#333',
    fontFamily: 'monospace',
    lineHeight: 20
  },
  secondaryButton: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: '#007bff',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 12
  },
  secondaryButtonText: {
    fontSize: 16,
    color: '#007bff',
    fontWeight: 'bold'
  },
  primaryButton: {
    backgroundColor: '#007bff',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 12,
    shadowColor: '#007bff',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4
  },
  primaryButtonText: {
    fontSize: 16,
    color: '#fff',
    fontWeight: 'bold'
  },
  unloadButtonsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
    marginTop: 12
  },
  unloadButton: {
    flex: 1,
    backgroundColor: '#fd7e14',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
    shadowColor: '#fd7e14',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 3
  },
  unloadButtonText: {
    fontSize: 14,
    color: '#fff',
    fontWeight: '600',
    textAlign: 'center'
  },
  deleteButton: {
    flex: 1,
    backgroundColor: '#dc3545',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
    shadowColor: '#dc3545',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 3
  },
  deleteButtonText: {
    fontSize: 14,
    color: '#fff',
    fontWeight: '600',
    textAlign: 'center'
  }
});

export default HomeScreen;
