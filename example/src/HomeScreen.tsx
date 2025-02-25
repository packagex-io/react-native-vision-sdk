import React, { useState, useEffect } from 'react';
import { View, Text, Button, ActivityIndicator, Alert, StyleSheet } from 'react-native';
import VisionSdk from '../../src/VisionSdk'; // Import the Vision SDK wrapper
import { useNavigation } from '@react-navigation/native';

const HomeScreen = () => {
  const [progress, setProgress] = useState(0);
  const [isReady, setIsReady] = useState(false);
  const navigation = useNavigation();

  useEffect(() => {
    // Subscribe to model download progress
    const subscription = VisionSdk.onModelDownloadProgress((progress, status, ready) => {
      console.log("LOADING PROGRESS: ", {progress, status, ready})
      setProgress(progress * 100); // Convert to percentage
      setIsReady(ready);
    });

    return () => subscription.remove(); // Cleanup on unmount
  }, []);

  const handleLoadModels = async () => {
    try {
      setProgress(0);
      setIsReady(false);
      await VisionSdk.loadModels(
        "",
        "key_00203c5642F9SYnJkKyi9dRw1eeteeUwXhbEfGuPZ4NML8l2bAfysni4ZpcZEBKn0gnbcOZYwIaJnOyp",
        "shipping_label",
        "large"
      );
    } catch (error) {
      Alert.alert("Error", "Failed to load models: " + error.message);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Vision SDK Example</Text>

      {/* Background Model Loading Button */}
      <Button title="Load On-Device Models" onPress={handleLoadModels} />

      {/* Progress Indicator */}
      <View style={styles.progressContainer}>
        {progress > 0 && !isReady && (
          <>
            <ActivityIndicator size="large" color="blue" />
            <Text style={styles.progressText}>Loading Models... {progress.toFixed(1)}%</Text>
          </>
        )}
        {isReady && <Text style={styles.readyText}>✅ Model is Ready!</Text>}
      </View>

      {/* Navigate to Camera */}
      <Button title="Open Camera" onPress={() => navigation.navigate("CameraScreen")} disabled={!isReady} />
    </View>
  );
};

export default HomeScreen;

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 20 },
  progressContainer: { marginVertical: 20, alignItems: 'center' },
  progressText: { fontSize: 16, marginTop: 10, color: 'blue' },
  readyText: { fontSize: 18, fontWeight: 'bold', color: 'green' },
});
