/**
 * DimensioningScreen — iOS-only LiDAR dimensioning.
 *
 * Shows DimensioningView (mode=offline, centimeters, maxTrackCount=5).
 * X button top-left to go back. Measurement display overlay.
 * On non-iOS or no LiDAR → informative message.
 */
import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  DimensioningView,
  VisionDimensioning,
  type DimensioningCapabilities,
  type DimensioningMeasurement,
  type DimensioningError,
  type DimensioningUpdate,
  type DimensioningOverlayFrame,
  type DimensioningOverlayMode,
  type DimensioningViewHandle,
} from 'react-native-vision-sdk';
import { theme } from '../theme';

interface Props {
  navigation: { goBack: () => void };
}

export function DimensioningScreen({ navigation }: Props) {
  const [capabilities, setCapabilities] = useState<DimensioningCapabilities | null>(null);
  const [capError, setCapError] = useState<string | null>(null);
  const [measurement, setMeasurement] = useState<DimensioningMeasurement | null>(null);
  const [scanError, setScanError] = useState<DimensioningError | null>(null);
  const [loading, setLoading] = useState(true);
  // New in VisionSDK 2.7.0 — live guidance, custom overlays and telemetry.
  const [update, setUpdate] = useState<DimensioningUpdate | null>(null);
  const [overlay, setOverlay] = useState<DimensioningOverlayFrame | null>(null);
  const [telemetry, setTelemetry] = useState<string[]>([]);
  const [overlayMode, setOverlayMode] =
    useState<DimensioningOverlayMode>('builtIn');
  const viewRef = useRef<DimensioningViewHandle>(null);

  useEffect(() => {
    if (Platform.OS !== 'ios') {
      setCapError('Dimensioning is iOS-only (requires ARKit + LiDAR).');
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

  const renderUnsupported = (msg: string) => (
    <SafeAreaView style={styles.unsupported}>
      <TouchableOpacity onPress={() => navigation.goBack()} style={styles.closeBtn}>
        <Text style={styles.closeBtnText}>X</Text>
      </TouchableOpacity>
      <Text style={styles.unsupportedText}>{msg}</Text>
    </SafeAreaView>
  );

  if (loading) {
    return (
      <SafeAreaView style={styles.center}>
        <ActivityIndicator size="large" color={theme.colors.accent} />
        <Text style={styles.hintText}>Checking capabilities…</Text>
      </SafeAreaView>
    );
  }

  if (capError || !capabilities) {
    return renderUnsupported(capError ?? 'Failed to check capabilities.');
  }

  if (!capabilities.lidar) {
    return renderUnsupported(
      'This device does not have LiDAR. Dimensioning requires an iPhone Pro/Max with LiDAR sensor.'
    );
  }

  return (
    <View style={styles.container}>
      {/* Camera */}
      <DimensioningView
        ref={viewRef}
        style={StyleSheet.absoluteFill}
        mode="offline"
        measurementUnit="centimeters"
        maximumTrackCount={5}
        overlayMode={overlayMode}
        enableTelemetry
        onCapture={(m: DimensioningMeasurement) => {
          setMeasurement(m);
          setScanError(null);
        }}
        onError={(e: DimensioningError) => setScanError(e)}
        onMeasurementUpdate={setUpdate}
        onOverlayUpdate={setOverlay}
        onTelemetry={(e) =>
          setTelemetry((prev) =>
            [
              e.type === 'measurementAborted'
                ? `aborted: ${e.reason} (${e.durationMs}ms)`
                : `captured: ${e.lengthCm.toFixed(1)}x${e.widthCm.toFixed(1)}x${e.heightCm.toFixed(1)}cm`,
              ...prev,
            ].slice(0, 5)
          )
        }
      />

      {/* X button top-left */}
      <SafeAreaView pointerEvents="box-none" style={StyleSheet.absoluteFill}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.closeBtnOverlay}>
          <Text style={styles.closeBtnText}>X</Text>
        </TouchableOpacity>

        {/* Live guidance from onMeasurementUpdate. In 'callback' mode the SDK
            draws nothing, so the HUD text comes from onOverlayUpdate instead. */}
        <View pointerEvents="none" style={styles.hudOverlay}>
          <Text style={styles.hudText}>
            {overlay?.hud.statusText ?? update?.trackingState ?? 'starting…'}
          </Text>
          {overlay?.hud.guidanceText ? (
            <Text style={styles.hudSub}>{overlay.hud.guidanceText}</Text>
          ) : null}
          {update?.tracks.length ? (
            <Text style={styles.hudSub}>
              {update.tracks.length} box(es)
              {update.tracks.some((t) => t.isStable) ? ' · stable' : ''}
            </Text>
          ) : null}
          {overlayMode === 'callback' && overlay ? (
            <Text style={styles.hudSub}>
              overlay: {overlay.boxes.length} box · {overlay.planes.length} plane
            </Text>
          ) : null}
          {telemetry.length ? (
            <Text style={styles.hudSub}>{telemetry[0]}</Text>
          ) : null}
        </View>

        {/* Exercises the overlayMode prop and the stop/start commands. */}
        <View style={styles.controlsOverlay}>
          <TouchableOpacity
            style={styles.controlBtn}
            onPress={() =>
              setOverlayMode((m) =>
                m === 'builtIn' ? 'callback' : m === 'callback' ? 'none' : 'builtIn'
              )
            }
          >
            <Text style={styles.controlText}>overlay: {overlayMode}</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.controlBtn}
            onPress={() => viewRef.current?.stop()}
          >
            <Text style={styles.controlText}>stop</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.controlBtn}
            onPress={() => viewRef.current?.start()}
          >
            <Text style={styles.controlText}>start</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>

      {/* Measurement overlay (bottom) */}
      {(measurement || scanError) ? (
        <View style={styles.resultOverlay}>
          {scanError ? (
            <Text style={styles.errorText}>
              Error ({scanError.code}): {scanError.message}
            </Text>
          ) : measurement ? (
            <>
              <Text style={styles.measureTitle}>Measurement</Text>
              <Text style={styles.measureRow}>
                L: {measurement.length.toFixed(1)} {measurement.lengthUnit}
              </Text>
              <Text style={styles.measureRow}>
                W: {measurement.width.toFixed(1)} {measurement.widthUnit}
              </Text>
              <Text style={styles.measureRow}>
                H: {measurement.height.toFixed(1)} {measurement.heightUnit}
              </Text>
              <Text style={styles.measureRow}>
                Distance: {measurement.distanceFromCamera.toFixed(2)} {measurement.distanceFromCameraUnit}
              </Text>
              <Text style={styles.measureRow}>
                Confidence: {(measurement.confidence * 100).toFixed(0)}%
              </Text>
            </>
          ) : null}
        </View>
      ) : (
        <View style={styles.hintOverlay}>
          <Text style={styles.hintText}>Point at a box to measure</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  hudOverlay: {
    position: 'absolute',
    top: 60,
    left: 16,
    right: 16,
    padding: 10,
    borderRadius: 10,
    backgroundColor: 'rgba(0,0,0,0.55)',
  },
  hudText: { color: '#fff', fontSize: 15, fontWeight: '600' },
  hudSub: { color: '#ddd', fontSize: 12, marginTop: 2 },
  controlsOverlay: {
    position: 'absolute',
    bottom: 140,
    left: 16,
    right: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  controlBtn: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    backgroundColor: 'rgba(0,0,0,0.6)',
  },
  controlText: { color: '#fff', fontSize: 12 },
  container: { flex: 1, backgroundColor: '#000' },
  center: { flex: 1, backgroundColor: '#000', justifyContent: 'center', alignItems: 'center' },
  unsupported: {
    flex: 1,
    backgroundColor: '#000',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  unsupportedText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.md,
    textAlign: 'center',
    lineHeight: 22,
  },
  closeBtn: {
    position: 'absolute',
    top: 60,
    left: 20,
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: theme.colors.btnCircle,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  closeBtnOverlay: {
    position: 'absolute',
    top: 16,
    left: 16,
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: theme.colors.btnCircle,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  closeBtnText: {
    color: theme.colors.textPrimary,
    fontSize: 14,
    fontWeight: '700',
  },
  resultOverlay: {
    position: 'absolute',
    bottom: 40,
    left: 20,
    right: 20,
    backgroundColor: theme.colors.bgOverlay,
    borderRadius: 12,
    padding: 16,
  },
  measureTitle: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.md,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  measureRow: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    marginBottom: 3,
  },
  errorText: {
    color: theme.colors.error,
    fontSize: theme.fontSize.sm,
  },
  hintOverlay: {
    position: 'absolute',
    bottom: 40,
    alignSelf: 'center',
    backgroundColor: theme.colors.bgOverlay,
    borderRadius: 12,
    padding: 12,
  },
  hintText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.sm,
    textAlign: 'center',
    marginTop: 12,
  },
});
