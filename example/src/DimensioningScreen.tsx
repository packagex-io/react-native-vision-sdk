import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Platform,
  ActivityIndicator,
} from 'react-native';
import {
  DimensioningView,
  VisionDimensioning,
  type DimensioningCapabilities,
  type DimensioningMeasurement,
  type DimensioningError,
} from '../../src/dimensioning';

const DimensioningScreen = () => {
  const [capabilities, setCapabilities] = useState<DimensioningCapabilities | null>(null);
  const [capError, setCapError] = useState<string | null>(null);
  const [measurement, setMeasurement] = useState<DimensioningMeasurement | null>(null);
  const [scanError, setScanError] = useState<DimensioningError | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (Platform.OS !== 'ios') {
      setCapError('Dimensioning is iOS-only.');
      setLoading(false);
      return;
    }

    VisionDimensioning.deviceCapabilities()
      .then((caps) => {
        setCapabilities(caps);
        setLoading(false);
      })
      .catch((err: Error) => {
        setCapError(err.message);
        setLoading(false);
      });
  }, []);

  const handleCapture = (m: DimensioningMeasurement) => {
    setMeasurement(m);
    setScanError(null);
  };

  const handleError = (err: DimensioningError) => {
    setScanError(err);
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
        <Text style={styles.loadingText}>Checking capabilities…</Text>
      </View>
    );
  }

  if (capError || !capabilities) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>{capError ?? 'Failed to get capabilities'}</Text>
      </View>
    );
  }

  if (!capabilities.lidar) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>
          This device does not have LiDAR. Dimensioning requires a Pro/Max iPhone with LiDAR.
        </Text>
        <Text style={styles.capText}>
          lidar: {String(capabilities.lidar)}{'\n'}
          arWorldTracking: {String(capabilities.arWorldTracking)}{'\n'}
          sceneReconstruction: {String(capabilities.sceneReconstruction)}
        </Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <DimensioningView
        style={styles.camera}
        mode="offline"
        measurementUnit="centimeters"
        maximumTrackCount={5}
        onCapture={handleCapture}
        onError={handleError}
      />

      <ScrollView style={styles.resultsPanel}>
        {scanError ? (
          <View style={styles.errorBox}>
            <Text style={styles.errorLabel}>Error ({scanError.code})</Text>
            <Text style={styles.errorText}>{scanError.message}</Text>
            {scanError.reason ? (
              <Text style={styles.errorReason}>{scanError.reason}</Text>
            ) : null}
          </View>
        ) : null}

        {measurement ? (
          <View style={styles.resultBox}>
            <Text style={styles.resultTitle}>Last Measurement</Text>
            <Text style={styles.resultText}>
              L: {measurement.length.toFixed(1)} {measurement.lengthUnit}
            </Text>
            <Text style={styles.resultText}>
              W: {measurement.width.toFixed(1)} {measurement.widthUnit}
            </Text>
            <Text style={styles.resultText}>
              H: {measurement.height.toFixed(1)} {measurement.heightUnit}
            </Text>
            <Text style={styles.resultText}>
              Distance: {measurement.distanceFromCamera.toFixed(2)} {measurement.distanceFromCameraUnit}
            </Text>
            <Text style={styles.resultText}>
              Confidence: {(measurement.confidence * 100).toFixed(0)}%
            </Text>
            <Text style={styles.resultText}>
              Cloud SAM: {String(measurement.usedCloudSAM)}
            </Text>
          </View>
        ) : (
          <Text style={styles.hint}>
            Point camera at a box. A measurement will appear here when captured.
          </Text>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 },
  camera: { flex: 1 },
  resultsPanel: {
    maxHeight: 240,
    backgroundColor: '#111',
    padding: 12,
  },
  loadingText: { marginTop: 12, color: '#666' },
  hint: { color: '#888', textAlign: 'center', marginTop: 16 },
  errorBox: { backgroundColor: '#3a0000', borderRadius: 8, padding: 12, marginBottom: 8 },
  errorLabel: { color: '#ff6b6b', fontWeight: 'bold', marginBottom: 4 },
  errorText: { color: '#ff6b6b' },
  errorReason: { color: '#ff9999', marginTop: 4, fontSize: 12 },
  resultBox: { backgroundColor: '#1a2a1a', borderRadius: 8, padding: 12, marginBottom: 8 },
  resultTitle: { color: '#4caf50', fontWeight: 'bold', marginBottom: 8 },
  resultText: { color: '#ddd', marginBottom: 2 },
  capText: { color: '#888', marginTop: 12, fontSize: 13, lineHeight: 20 },
});

export default DimensioningScreen;
