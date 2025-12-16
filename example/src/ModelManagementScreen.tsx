import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  ActivityIndicator,
  Image,
  Clipboard,
} from 'react-native';
import { VisionCore } from '../../src/index';
import { useFocusEffect } from '@react-navigation/native';

const api_key = ""; // Add your PackageX API key here

const ModelManagementScreen = ({ navigation }) => {
  // State management
  const [isInitialized, setIsInitialized] = useState<boolean>(false);
  const [selectedModelType, setSelectedModelType] = useState<string>('shipping_label');
  const [selectedModelSize, setSelectedModelSize] = useState<string>('micro');
  const [selectedExecutionProvider, setSelectedExecutionProvider] = useState<string>('CPU');
  const [downloadProgress, setDownloadProgress] = useState<number>(0);
  // NOT AVAILABLE - State commented out as method is no longer available
  // const [activeDownloads, setActiveDownloads] = useState<number>(0);
  const [loadedModelsCount, setLoadedModelsCount] = useState<number>(0);
  const [isModelLoaded, setIsModelLoaded] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [results, setResults] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [concurrentLoadingResults, setConcurrentLoadingResults] = useState<any[]>([]);
  const [downloadProgressMap, setDownloadProgressMap] = useState<{[key: string]: number}>({});

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
    { label: 'XLarge', value: 'xlarge' },
  ];

  const executionProviders = [
    { label: 'CPU', value: 'CPU' },
    { label: 'NNAPI', value: 'NNAPI' },
    { label: 'XNNPACK', value: 'XNNPACK' },
  ];

  // Helper function to get sample image
  const getSampleImageForModelType = (modelType: string) => {
    switch (modelType) {
      case 'shipping_label':
        return 'https://cdn.shopify.com/s/files/1/0070/7032/files/image1_a462b651-c72f-4e21-8048-35763b21eef1.png?v=1671219333';
      case 'item_label':
        return 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ8Jmenckba1oRIkRgKXLEDBjzcohm028JTsg&s';
      case 'bill_of_lading':
        return 'https://www.freightera.com/blog/wp-content/uploads/2022/09/bol-basic-info-904x1024.jpg';
      case 'document_classification':
        return 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQ8Jmenckba1oRIkRgKXLEDBjzcohm028JTsg&s';
      default:
        return 'https://cdn.shopify.com/s/files/1/0070/7032/files/image1_a462b651-c72f-4e21-8048-35763b21eef1.png?v=1671219333';
    }
  };

  // Subscribe to download progress events
  useFocusEffect(
    useCallback(() => {
      const subscription = VisionCore.onModelDownloadProgress((progress) => {
        setDownloadProgress(progress * 100);
      });

      return () => {
        subscription.remove();
      };
    }, [])
  );

  // 1️⃣ INITIALIZATION METHODS
  const handleInitializeModelManager = async () => {
    try {
      setIsLoading(true);
      VisionCore.initializeModelManager({
        maxConcurrentDownloads: 3,
        enableLogging: true,
      });
      setIsInitialized(true);
      setStatusMessage('✅ ModelManager initialized successfully!');
      setResults('ModelManager initialized with:\n' + JSON.stringify({
        maxConcurrentDownloads: 3,
        enableLogging: true
      }, null, 2));
    } catch (error: any) {
      setStatusMessage('❌ Initialization failed: ' + error.message);
      Alert.alert('Error', 'Failed to initialize: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCheckInitializationStatus = () => {
    const status = VisionCore.isModelManagerInitialized();
    setIsInitialized(status);
    setStatusMessage(status ? '✅ ModelManager is initialized' : '⚠️ ModelManager is NOT initialized');
    setResults(`ModelManager initialized: ${status}`);
  };

  // 2️⃣ DOWNLOAD OPERATIONS
  const handleDownloadModel = async () => {
    try {
      setIsLoading(true);
      setDownloadProgress(0);
      setStatusMessage('📥 Downloading model...');

      await VisionCore.downloadModel(
        {
          type: selectedModelType as any,
          size: selectedModelSize as any,
        },
        api_key,
        null,
        'react_native',
        (progress) => {
          console.log("PROGRESS IS: ", progress)
          setDownloadProgress(progress.progress * 100);
        }
      );

      setStatusMessage('✅ Model downloaded successfully!');
      setResults(`Downloaded: ${selectedModelType} (${selectedModelSize})`);
      // NOT AVAILABLE - Method commented out for API consistency across platforms
      // await updateActiveDownloadsCount();
    } catch (error: any) {
      setStatusMessage('❌ Download failed: ' + error.message);
      Alert.alert('Error', 'Download failed: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancelDownload = async () => {
    try {
      const cancelled = await VisionCore.cancelDownload({
        type: selectedModelType as any,
        size: selectedModelSize as any,
      });
      setStatusMessage(cancelled ? '✅ Download cancelled' : '⚠️ No active download to cancel');
      setResults(`Cancel download result: ${cancelled}`);
      // NOT AVAILABLE - Method commented out for API consistency across platforms
      // await updateActiveDownloadsCount();
    } catch (error: any) {
      Alert.alert('Error', 'Failed to cancel: ' + error.message);
    }
  };

  // NOT AVAILABLE - Method commented out for API consistency across platforms
  // const updateActiveDownloadsCount = async () => {
  //   try {
  //     const count = VisionCore.getActiveDownloadCount();
  //     console.log("Active downloads count:", count);
  //     setActiveDownloads(count);
  //   } catch (error: any) {
  //     console.error('Failed to get active downloads:', error);
  //   }
  // };

  // 3️⃣ LOAD/UNLOAD OPERATIONS
  const handleLoadModel = async () => {
    try {
      setIsLoading(true);
      setStatusMessage('📱 Loading model into memory...');

      await VisionCore.loadOCRModel(
        {
          type: selectedModelType as any,
          size: selectedModelSize as any,
        },
        api_key,
        null,
        'react_native',
        selectedExecutionProvider
      );

      setStatusMessage('✅ Model loaded successfully!');
      setResults(`Loaded: ${selectedModelType} (${selectedModelSize}) with ${selectedExecutionProvider}`);
      await updateLoadedStatus();
    } catch (error: any) {
      setStatusMessage('❌ Load failed: ' + error.message);
      Alert.alert('Error', 'Load failed: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUnloadModel = async () => {
    try {
      const unloaded = await VisionCore.unloadModel({
        type: selectedModelType as any,
        size: selectedModelSize as any,
      });
      setStatusMessage(unloaded ? '✅ Model unloaded' : '⚠️ Model was not loaded');
      setResults(`Unload result: ${unloaded}`);
      await updateLoadedStatus();
    } catch (error: any) {
      Alert.alert('Error', 'Failed to unload: ' + error.message);
    }
  };

  const handleCheckIfModelLoaded = async () => {
    try {
      const loaded = await VisionCore.isModelLoaded({
        type: selectedModelType as any,
        size: selectedModelSize as any,
      });
      setIsModelLoaded(loaded);
      setStatusMessage(loaded ? '✅ Model is loaded' : '⚠️ Model is NOT loaded');
      setResults(`Is model loaded: ${loaded}`);
    } catch (error: any) {
      Alert.alert('Error', 'Failed to check: ' + error.message);
    }
  };

  const updateLoadedStatus = async () => {
    try {
      const count = VisionCore.getLoadedModelCount();
      setLoadedModelsCount(count);
      const loaded = await VisionCore.isModelLoaded({
        type: selectedModelType as any,
        size: selectedModelSize as any,
      });
      setIsModelLoaded(loaded);
    } catch (error: any) {
      // Silent fail - this is a background status update
    }
  };

  // 4B️⃣ CONCURRENT DOWNLOAD TEST
  const handleDownloadMultipleModelsConcurrently = async () => {
    try {
      setIsLoading(true);
      setStatusMessage('🔄 Downloading 3 large models concurrently...');
      setConcurrentLoadingResults([]);

      // Initialize progress for all models
      setDownloadProgressMap({
        'shipping_label': 0,
        'item_label': 0,
        'bill_of_lading': 0,
      });

      const startTime = Date.now();

      // Define 3 large models to download concurrently
      const modelsToDownload = [
        { type: 'shipping_label' as const, size: 'large' as const },
        { type: 'item_label' as const, size: 'large' as const },
        { type: 'bill_of_lading' as const, size: 'large' as const },
      ];

      // Download all models concurrently using Promise.all
      const downloadPromises = modelsToDownload.map(async (model) => {
        const modelStartTime = Date.now();
        try {
          await VisionCore.downloadModel(
            model,
            api_key,
            null,
            'react_native',
            (progress) => {
              // Update progress for this specific model
              setDownloadProgressMap((prev) => ({
                ...prev,
                [model.type]: progress.progress * 100,
              }));
            }
          );
          const duration = Date.now() - modelStartTime;
          return {
            model: `${model.type} (${model.size})`,
            status: 'success',
            duration: `${(duration / 1000).toFixed(2)}s`,
            icon: '✅',
          };
        } catch (error: any) {
          const duration = Date.now() - modelStartTime;
          return {
            model: `${model.type} (${model.size})`,
            status: 'failed',
            error: error.message,
            duration: `${(duration / 1000).toFixed(2)}s`,
            icon: '❌',
          };
        }
      });

      const downloadResults = await Promise.all(downloadPromises);
      const totalDuration = ((Date.now() - startTime) / 1000).toFixed(2);

      setConcurrentLoadingResults(downloadResults);

      const successCount = downloadResults.filter(r => r.status === 'success').length;
      setStatusMessage(`✅ Downloaded ${successCount}/${modelsToDownload.length} models in ${totalDuration}s`);

      setResults(JSON.stringify({
        totalDuration: `${totalDuration}s`,
        modelsDownloaded: successCount,
        totalModels: modelsToDownload.length,
        results: downloadResults
      }, null, 2));

      // NOT AVAILABLE - Method commented out for API consistency across platforms
      // await updateActiveDownloadsCount();

      // Clear progress after completion
      setDownloadProgressMap({});
    } catch (error: any) {
      setStatusMessage('❌ Concurrent download failed: ' + error.message);
      Alert.alert('Error', 'Failed to download models: ' + error.message);
      setDownloadProgressMap({});
    } finally {
      setIsLoading(false);
    }
  };

  // 4C️⃣ CONCURRENT LOADING TEST
  const handleLoadMultipleModelsConcurrently = async () => {
    try {
      setIsLoading(true);
      setStatusMessage('🔄 Loading 3 large models concurrently...');
      setConcurrentLoadingResults([]);

      const startTime = Date.now();

      // Define 3 large models to load concurrently
      const modelsToLoad = [
        { type: 'shipping_label' as const, size: 'large' as const },
        { type: 'item_label' as const, size: 'large' as const },
        { type: 'bill_of_lading' as const, size: 'large' as const },
      ];

      // Load all models concurrently using Promise.all
      const loadPromises = modelsToLoad.map(async (model) => {
        const modelStartTime = Date.now();
        try {
          await VisionCore.loadOCRModel(
            model,
            api_key,
            null,
            'react_native',
            selectedExecutionProvider
          );
          const duration = Date.now() - modelStartTime;
          return {
            model: `${model.type} (${model.size})`,
            status: 'success',
            duration: `${(duration / 1000).toFixed(2)}s`,
            icon: '✅',
          };
        } catch (error: any) {
          const duration = Date.now() - modelStartTime;
          return {
            model: `${model.type} (${model.size})`,
            status: 'failed',
            error: error.message,
            duration: `${(duration / 1000).toFixed(2)}s`,
            icon: '❌',
          };
        }
      });

      const loadResults = await Promise.all(loadPromises);
      const totalDuration = ((Date.now() - startTime) / 1000).toFixed(2);

      setConcurrentLoadingResults(loadResults);

      const successCount = loadResults.filter(r => r.status === 'success').length;
      setStatusMessage(`✅ Loaded ${successCount}/${modelsToLoad.length} models in ${totalDuration}s`);

      setResults(JSON.stringify({
        totalDuration: `${totalDuration}s`,
        modelsLoaded: successCount,
        totalModels: modelsToLoad.length,
        results: loadResults
      }, null, 2));

      await updateLoadedStatus();
    } catch (error: any) {
      setStatusMessage('❌ Concurrent loading failed: ' + error.message);
      Alert.alert('Error', 'Failed to load models: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUnloadAllModels = async () => {
    try {
      setIsLoading(true);
      setStatusMessage('🔄 Unloading all large models...');

      const modelsToUnload = [
        { type: 'shipping_label' as const, size: 'large' as const },
        { type: 'item_label' as const, size: 'large' as const },
        { type: 'bill_of_lading' as const, size: 'large' as const },
      ];

      const unloadPromises = modelsToUnload.map(async (model) => {
        try {
          const unloaded = await VisionCore.unloadModel(model);
          return {
            model: `${model.type} (${model.size})`,
            status: unloaded ? 'unloaded' : 'not loaded',
            icon: unloaded ? '✅' : '⚠️',
          };
        } catch (error: any) {
          return {
            model: `${model.type} (${model.size})`,
            status: 'error',
            error: error.message,
            icon: '❌',
          };
        }
      });

      const unloadResults = await Promise.all(unloadPromises);
      setConcurrentLoadingResults(unloadResults);

      setStatusMessage('✅ All models unloaded');
      setResults(JSON.stringify(unloadResults, null, 2));

      await updateLoadedStatus();
    } catch (error: any) {
      setStatusMessage('❌ Failed to unload: ' + error.message);
      Alert.alert('Error', 'Failed to unload models: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  // 4D️⃣ CONCURRENT PREDICTION TEST
  const handlePredictWithMultipleModelsConcurrently = async () => {
    try {
      setIsLoading(true);
      setStatusMessage('🔮 Running 3 predictions concurrently...');
      setConcurrentLoadingResults([]);

      const startTime = Date.now();

      // Define 3 large models to predict with concurrently
      const modelsToPredictWith = [
        { type: 'shipping_label' as const, size: 'large' as const },
        { type: 'item_label' as const, size: 'large' as const },
        { type: 'bill_of_lading' as const, size: 'large' as const },
      ];

      // Run predictions concurrently using Promise.all
      const predictionPromises = modelsToPredictWith.map(async (model) => {
        const modelStartTime = Date.now();
        try {
          const imagePath = getSampleImageForModelType(model.type);
          const result = await VisionCore.predictWithModule(
            model,
            imagePath,
            []
          );
          const duration = Date.now() - modelStartTime;

          // Parse result to get summary
          let resultSummary = 'Success';
          try {
            const parsed = typeof result === 'string' ? JSON.parse(result) : result;
            resultSummary = `Fields extracted: ${Object.keys(parsed).length}`;
          } catch {
            resultSummary = 'Success';
          }

          return {
            model: `${model.type} (${model.size})`,
            status: 'success',
            duration: `${(duration / 1000).toFixed(2)}s`,
            summary: resultSummary,
            icon: '✅',
            result: typeof result === 'string' ? result.substring(0, 200) : JSON.stringify(result).substring(0, 200),
          };
        } catch (error: any) {
          const duration = Date.now() - modelStartTime;
          return {
            model: `${model.type} (${model.size})`,
            status: 'failed',
            error: error.message,
            duration: `${(duration / 1000).toFixed(2)}s`,
            icon: '❌',
          };
        }
      });

      const predictionResults = await Promise.all(predictionPromises);
      const totalDuration = ((Date.now() - startTime) / 1000).toFixed(2);

      setConcurrentLoadingResults(predictionResults);

      const successCount = predictionResults.filter(r => r.status === 'success').length;
      setStatusMessage(`✅ Completed ${successCount}/${modelsToPredictWith.length} predictions in ${totalDuration}s`);

      setResults(JSON.stringify({
        totalDuration: `${totalDuration}s`,
        predictionsCompleted: successCount,
        totalPredictions: modelsToPredictWith.length,
        results: predictionResults
      }, null, 2));
    } catch (error: any) {
      setStatusMessage('❌ Concurrent prediction failed: ' + error.message);
      Alert.alert('Error', 'Failed to run predictions: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  // 4️⃣ QUERY OPERATIONS
  const handleFindDownloadedModels = async () => {
    try {
      setIsLoading(true);
      const models = await VisionCore.findDownloadedModels();
      setStatusMessage(`✅ Found ${models.length} downloaded model(s)`);
      setResults(JSON.stringify(models, null, 2));
    } catch (error: any) {
      setStatusMessage('❌ Query failed: ' + error.message);
      Alert.alert('Error', 'Failed to find models: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFindDownloadedModel = async () => {
    try {
      setIsLoading(true);
      const model = await VisionCore.findDownloadedModel({
        type: selectedModelType as any,
        size: selectedModelSize as any,
      });
      setStatusMessage(model ? '✅ Model found' : '⚠️ Model not found');
      setResults(model ? JSON.stringify(model, null, 2) : 'null');
    } catch (error: any) {
      setStatusMessage('❌ Query failed: ' + error.message);
      Alert.alert('Error', 'Failed to find model: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleFindLoadedModels = async () => {
    try {
      setIsLoading(true);
      const models = await VisionCore.findLoadedModels();
      setStatusMessage(`✅ Found ${models.length} loaded model(s)`);
      setResults(JSON.stringify(models, null, 2));
    } catch (error: any) {
      setStatusMessage('❌ Query failed: ' + error.message);
      Alert.alert('Error', 'Failed to find loaded models: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  // 5️⃣ DELETE OPERATIONS
  // NOT AVAILABLE - Method commented out for API consistency across platforms
  // const handleCheckModelUpdates = async () => {
  //   try {
  //     setIsLoading(true);
  //     setStatusMessage('🔍 Checking for updates...');
  //
  //     const updateInfo = await VisionCore.checkModelUpdates(
  //       {
  //         type: selectedModelType as any,
  //         size: selectedModelSize as any,
  //       },
  //       api_key,
  //       null,
  //       'react_native'
  //     );
  //
  //     setStatusMessage(
  //       updateInfo.updateAvailable
  //         ? '🆕 Update available!'
  //         : '✅ Model is up to date'
  //     );
  //     setResults(JSON.stringify(updateInfo, null, 2));
  //   } catch (error: any) {
  //     setStatusMessage('❌ Update check failed: ' + error.message);
  //     Alert.alert('Error', 'Failed to check updates: ' + error.message);
  //   } finally {
  //     setIsLoading(false);
  //   }
  // };

  const handleDeleteModel = async () => {
    Alert.alert(
      'Confirm Delete',
      `Delete ${selectedModelType} (${selectedModelSize}) from disk?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              setIsLoading(true);
              const deleted = await VisionCore.deleteModel({
                type: selectedModelType as any,
                size: selectedModelSize as any,
              });
              setStatusMessage(deleted ? '✅ Model deleted' : '⚠️ Model not found');
              setResults(`Delete result: ${deleted}`);
              await updateLoadedStatus();
            } catch (error: any) {
              setStatusMessage('❌ Delete failed: ' + error.message);
              Alert.alert('Error', 'Failed to delete: ' + error.message);
            } finally {
              setIsLoading(false);
            }
          },
        },
      ]
    );
  };

  // 6️⃣ PREDICTION
  const handlePredictWithModule = async () => {
    try {
      setIsLoading(true);
      setStatusMessage('🔮 Making prediction...');

      const imagePath = getSampleImageForModelType(selectedModelType);
      const result = await VisionCore.predictWithModule(
        {
          type: selectedModelType as any,
          size: selectedModelSize as any,
        },
        imagePath,
        []
      );

      setStatusMessage('✅ Prediction completed!');
      setResults(typeof result === 'string' ? result : JSON.stringify(result, null, 2));
    } catch (error: any) {
      setStatusMessage('❌ Prediction failed: ' + error.message);
      Alert.alert('Error', 'Prediction failed: ' + error.message);
    } finally {
      setIsLoading(false);
    }
  };

  // UTILITY FUNCTIONS
  const copyResults = () => {
    if (results) {
      Clipboard.setString(results);
      Alert.alert('Copied! 📋', 'Results copied to clipboard');
    }
  };

  const clearResults = () => {
    setResults('');
    setStatusMessage('');
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.scrollContainer}>
      <Text style={styles.title}>Model Management API</Text>
      <Text style={styles.subtitle}>Test all 15 methods below</Text>

      {/* Status Message Banner */}
      {statusMessage !== '' && (
        <View style={styles.statusBanner}>
          <Text style={styles.statusText}>{statusMessage}</Text>
        </View>
      )}

      {/* 1️⃣ INITIALIZATION */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>1️⃣ INITIALIZATION</Text>
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.purpleButton, styles.halfButton]}
            onPress={handleInitializeModelManager}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>Initialize</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.purpleButton, styles.halfButton]}
            onPress={handleCheckInitializationStatus}
          >
            <Text style={styles.buttonText}>Check Status</Text>
          </TouchableOpacity>
        </View>
        <Text style={styles.statusIndicator}>
          Status: {isInitialized ? '✅ Initialized' : '⚠️ Not Initialized'}
        </Text>
      </View>

      {/* 2️⃣ MODEL CONFIGURATION */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>2️⃣ MODEL CONFIGURATION</Text>

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
          {modelSizes.map((size) => (
            <TouchableOpacity
              key={size.value}
              style={[
                styles.chip,
                selectedModelSize === size.value && styles.selectedChip
              ]}
              onPress={() => setSelectedModelSize(size.value)}
            >
              <Text style={[
                styles.chipText,
                selectedModelSize === size.value && styles.selectedChipText
              ]}>
                {size.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <Text style={styles.label}>Execution Provider (Android):</Text>
        <View style={styles.chipContainer}>
          {executionProviders.map((provider) => (
            <TouchableOpacity
              key={provider.value}
              style={[
                styles.chip,
                selectedExecutionProvider === provider.value && styles.selectedChip
              ]}
              onPress={() => setSelectedExecutionProvider(provider.value)}
            >
              <Text style={[
                styles.chipText,
                selectedExecutionProvider === provider.value && styles.selectedChipText
              ]}>
                {provider.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* 3️⃣ DOWNLOAD OPERATIONS */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>3️⃣ DOWNLOAD OPERATIONS</Text>
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.blueButton, styles.halfButton]}
            onPress={handleDownloadModel}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>Download</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.blueButton, styles.halfButton]}
            onPress={handleCancelDownload}
          >
            <Text style={styles.buttonText}>Cancel</Text>
          </TouchableOpacity>
        </View>
        {downloadProgress > 0 && (
          <View style={styles.progressContainer}>
            <View style={styles.progressBar}>
              <View style={[styles.progressFill, { width: `${downloadProgress}%` }]} />
            </View>
            <Text style={styles.progressText}>{downloadProgress.toFixed(1)}%</Text>
          </View>
        )}
        {/* NOT AVAILABLE - Method commented out for API consistency across platforms */}
        {/* <TouchableOpacity
          style={[styles.button, styles.blueButton]}
          onPress={updateActiveDownloadsCount}
        >
          <Text style={styles.buttonText}>Get Active Downloads Count: {activeDownloads}</Text>
        </TouchableOpacity> */}
      </View>

      {/* 4️⃣ LOAD/UNLOAD OPERATIONS */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>4️⃣ LOAD/UNLOAD OPERATIONS</Text>
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.greenButton, styles.halfButton]}
            onPress={handleLoadModel}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>Load</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.greenButton, styles.halfButton]}
            onPress={handleUnloadModel}
          >
            <Text style={styles.buttonText}>Unload</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.greenButton, styles.halfButton]}
            onPress={handleCheckIfModelLoaded}
          >
            <Text style={styles.buttonText}>Check Loaded</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.greenButton, styles.halfButton]}
            onPress={updateLoadedStatus}
          >
            <Text style={styles.buttonText}>Count: {loadedModelsCount}</Text>
          </TouchableOpacity>
        </View>
        <Text style={styles.statusIndicator}>
          Current Model: {isModelLoaded ? '✅ Loaded' : '⚠️ Not Loaded'}
        </Text>
      </View>

      {/* 4B️⃣ CONCURRENT OPERATIONS TEST */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>4B️⃣ CONCURRENT OPERATIONS TEST</Text>
        <Text style={styles.helperText}>
          Test downloading and loading 3 LARGE models simultaneously (shipping_label, item_label, bill_of_lading)
        </Text>

        <Text style={styles.subSectionTitle}>Step 1: Download Models</Text>
        <TouchableOpacity
          style={[styles.button, styles.purpleButton]}
          onPress={handleDownloadMultipleModelsConcurrently}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>📥 Download 3 Large Models Concurrently</Text>
        </TouchableOpacity>

        {Object.keys(downloadProgressMap).length > 0 && (
          <View style={styles.multiProgressContainer}>
            {Object.entries(downloadProgressMap).map(([modelType, progress]) => (
              <View key={modelType} style={styles.individualProgressContainer}>
                <Text style={styles.progressLabel}>
                  {modelType.replace('_', ' ').toUpperCase()}
                </Text>
                <View style={styles.progressBar}>
                  <View style={[styles.progressFill, { width: `${progress}%` }]} />
                </View>
                <Text style={styles.progressText}>{progress.toFixed(1)}%</Text>
              </View>
            ))}
          </View>
        )}

        <Text style={styles.subSectionTitle}>Step 2: Load Into Memory</Text>
        <TouchableOpacity
          style={[styles.button, styles.purpleButton]}
          onPress={handleLoadMultipleModelsConcurrently}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>🚀 Load 3 Large Models Concurrently</Text>
        </TouchableOpacity>

        <Text style={styles.subSectionTitle}>Step 3: Test Predictions</Text>
        <TouchableOpacity
          style={[styles.button, styles.purpleButton]}
          onPress={handlePredictWithMultipleModelsConcurrently}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>🔮 Run 3 Predictions Concurrently</Text>
        </TouchableOpacity>

        <Text style={styles.subSectionTitle}>Step 4: Cleanup</Text>
        <TouchableOpacity
          style={[styles.button, styles.purpleButton]}
          onPress={handleUnloadAllModels}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>🗑️ Unload All 3 Large Models</Text>
        </TouchableOpacity>

        {concurrentLoadingResults.length > 0 && (
          <View style={styles.concurrentResultsContainer}>
            <Text style={styles.concurrentResultsTitle}>Results:</Text>
            {concurrentLoadingResults.map((result, index) => (
              <View key={index} style={styles.concurrentResultItem}>
                <Text style={styles.concurrentResultText}>
                  {result.icon} {result.model}
                </Text>
                <Text style={styles.concurrentResultDetail}>
                  Status: {result.status}
                  {result.duration && ` | Time: ${result.duration}`}
                </Text>
                {result.summary && (
                  <Text style={styles.concurrentResultDetail}>
                    {result.summary}
                  </Text>
                )}
                {result.error && (
                  <Text style={styles.concurrentResultError}>
                    Error: {result.error}
                  </Text>
                )}
                {result.result && (
                  <Text style={styles.concurrentResultPreview}>
                    Preview: {result.result}...
                  </Text>
                )}
              </View>
            ))}
          </View>
        )}
      </View>

      {/* 5️⃣ QUERY OPERATIONS */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>5️⃣ QUERY OPERATIONS</Text>
        <TouchableOpacity
          style={[styles.button, styles.tealButton]}
          onPress={handleFindDownloadedModels}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>Find All Downloaded Models</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.button, styles.tealButton]}
          onPress={handleFindDownloadedModel}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>Find Current Downloaded Model</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.button, styles.tealButton]}
          onPress={handleFindLoadedModels}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>Find All Loaded Models</Text>
        </TouchableOpacity>
      </View>

      {/* 6️⃣ DELETE */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>6️⃣ DELETE</Text>
        {/* NOT AVAILABLE - Method commented out for API consistency across platforms */}
        {/* <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.button, styles.orangeButton, styles.halfButton]}
            onPress={handleCheckModelUpdates}
            disabled={isLoading}
          >
            <Text style={styles.buttonText}>Check Updates</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.redButton, styles.halfButton]}
            onPress={handleDeleteModel}
          >
            <Text style={styles.buttonText}>Delete</Text>
          </TouchableOpacity>
        </View> */}
        <TouchableOpacity
          style={[styles.button, styles.redButton]}
          onPress={handleDeleteModel}
        >
          <Text style={styles.buttonText}>Delete Model</Text>
        </TouchableOpacity>
      </View>

      {/* 7️⃣ PREDICTION */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>7️⃣ PREDICTION</Text>
        <Text style={styles.label}>Sample Image:</Text>
        <Image
          source={{ uri: getSampleImageForModelType(selectedModelType) }}
          style={styles.sampleImage}
          resizeMode="contain"
        />
        <TouchableOpacity
          style={[styles.button, styles.indigoButton]}
          onPress={handlePredictWithModule}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>Predict with Module</Text>
        </TouchableOpacity>
      </View>

      {/* Loading Indicator */}
      {isLoading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#007bff" />
          <Text style={styles.loadingText}>Processing...</Text>
        </View>
      )}

      {/* RESULTS PANEL */}
      {results !== '' && (
        <View style={styles.resultsSection}>
          <View style={styles.resultsHeader}>
            <Text style={styles.sectionTitle}>📋 RESULTS</Text>
            <View style={styles.buttonRow}>
              <TouchableOpacity style={styles.smallButton} onPress={copyResults}>
                <Text style={styles.smallButtonText}>Copy</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.smallButton} onPress={clearResults}>
                <Text style={styles.smallButtonText}>Clear</Text>
              </TouchableOpacity>
            </View>
          </View>
          <ScrollView style={styles.resultsScroll} nestedScrollEnabled>
            <Text style={styles.resultsText}>{results}</Text>
          </ScrollView>
        </View>
      )}

      {/* Back Button */}
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => navigation.goBack()}
      >
        <Text style={styles.backButtonText}>← Back to Home</Text>
      </TouchableOpacity>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollContainer: {
    padding: 16,
    paddingBottom: 40,
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    color: '#333',
    textAlign: 'center',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
    marginBottom: 20,
  },
  statusBanner: {
    backgroundColor: '#d4edda',
    borderRadius: 8,
    padding: 12,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#c3e6cb',
  },
  statusText: {
    fontSize: 15,
    color: '#155724',
    textAlign: 'center',
    fontWeight: '500',
  },
  section: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  subSectionTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: '#6f42c1',
    marginTop: 12,
    marginBottom: 8,
  },
  label: {
    fontSize: 15,
    fontWeight: '600',
    color: '#333',
    marginTop: 8,
    marginBottom: 8,
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 12,
  },
  chip: {
    paddingVertical: 8,
    paddingHorizontal: 14,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#007bff',
    marginRight: 8,
    marginBottom: 8,
    backgroundColor: 'transparent',
  },
  selectedChip: {
    backgroundColor: '#007bff',
  },
  chipText: {
    fontSize: 13,
    color: '#007bff',
    fontWeight: '500',
  },
  selectedChipText: {
    color: '#fff',
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 10,
    marginBottom: 10,
  },
  button: {
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 10,
  },
  halfButton: {
    flex: 1,
  },
  purpleButton: {
    backgroundColor: '#6f42c1',
  },
  blueButton: {
    backgroundColor: '#007bff',
  },
  greenButton: {
    backgroundColor: '#28a745',
  },
  tealButton: {
    backgroundColor: '#17a2b8',
  },
  orangeButton: {
    backgroundColor: '#fd7e14',
  },
  redButton: {
    backgroundColor: '#dc3545',
  },
  indigoButton: {
    backgroundColor: '#6610f2',
  },
  buttonText: {
    fontSize: 15,
    color: '#fff',
    fontWeight: 'bold',
  },
  statusIndicator: {
    fontSize: 15,
    color: '#333',
    fontWeight: '500',
    textAlign: 'center',
    marginTop: 8,
  },
  progressContainer: {
    marginBottom: 12,
  },
  multiProgressContainer: {
    marginTop: 12,
    marginBottom: 12,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    padding: 12,
    borderWidth: 1,
    borderColor: '#dee2e6',
  },
  individualProgressContainer: {
    marginBottom: 12,
  },
  progressLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: '#6f42c1',
    marginBottom: 6,
  },
  progressBar: {
    height: 20,
    backgroundColor: '#e9ecef',
    borderRadius: 10,
    overflow: 'hidden',
    marginBottom: 4,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#007bff',
  },
  progressText: {
    fontSize: 14,
    color: '#007bff',
    textAlign: 'center',
    fontWeight: '600',
  },
  sampleImage: {
    width: '100%',
    height: 150,
    borderRadius: 8,
    backgroundColor: '#f0f0f0',
    marginBottom: 12,
  },
  loadingContainer: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    alignItems: 'center',
    marginBottom: 16,
  },
  loadingText: {
    marginTop: 12,
    fontSize: 16,
    color: '#007bff',
  },
  resultsSection: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  resultsHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  smallButton: {
    backgroundColor: '#007bff',
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 6,
    marginLeft: 8,
  },
  smallButtonText: {
    fontSize: 13,
    color: '#fff',
    fontWeight: '600',
  },
  resultsScroll: {
    maxHeight: 300,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    padding: 12,
  },
  resultsText: {
    fontSize: 13,
    color: '#333',
    fontFamily: 'monospace',
    lineHeight: 18,
  },
  backButton: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: '#007bff',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 12,
  },
  backButtonText: {
    fontSize: 16,
    color: '#007bff',
    fontWeight: 'bold',
  },
  helperText: {
    fontSize: 13,
    color: '#666',
    marginBottom: 12,
    fontStyle: 'italic',
  },
  concurrentResultsContainer: {
    marginTop: 16,
    backgroundColor: '#f8f9fa',
    borderRadius: 8,
    padding: 12,
    borderWidth: 1,
    borderColor: '#dee2e6',
  },
  concurrentResultsTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 8,
  },
  concurrentResultItem: {
    backgroundColor: '#fff',
    borderRadius: 6,
    padding: 10,
    marginBottom: 8,
    borderLeftWidth: 3,
    borderLeftColor: '#6f42c1',
  },
  concurrentResultText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  concurrentResultDetail: {
    fontSize: 13,
    color: '#666',
  },
  concurrentResultError: {
    fontSize: 12,
    color: '#dc3545',
    marginTop: 4,
    fontStyle: 'italic',
  },
  concurrentResultPreview: {
    fontSize: 11,
    color: '#6c757d',
    marginTop: 6,
    fontFamily: 'monospace',
    fontStyle: 'italic',
  },
});

export default ModelManagementScreen;
