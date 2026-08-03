/**
 * CameraControlsQAScreen — manual on-device test harness for the Phase-3
 * Camera Controls API (`useCameraControls()` + the new `<VisionCamera>`
 * props/commands). RN equivalent of the native `CameraCoreTestScreen`
 * (Android) / `CameraControlsQAView` (iOS) harnesses.
 *
 * Every control below drives the PUBLIC surface only:
 *   - `useCameraControls()` hook (`camera.setZoom`, `camera.setTorch`,
 *     `camera.setFocusPoint`, `camera.state`, `camera.capabilities`)
 *   - `<VisionCamera>` props (`cameraFacing`, `focusMode`, `pinnedLensId`)
 *   - the `VisionCameraRefProps` ref (`start`/`stop`/`rescan`)
 *
 * No internal wrappers are touched.
 */
import React, { useCallback, useRef, useState } from 'react';
import {
  Alert,
  Animated,
  PanResponder,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import { Platform } from 'react-native';
import { VisionCamera } from '../../../src/VisionCamera';
import type {
  CameraFacing,
  VisionCameraRefProps,
} from '../../../src/VisionCameraTypes';
import type { FocusMode } from '../../../src/types';
import { useCameraControls } from '../../../src/camera-controls/useCameraControls';
import { ZoomPills } from '../components/ZoomPills';
import { SegmentedControl } from '../components/SegmentedControl';
import { theme } from '../theme';

interface Props {
  navigation: { goBack: () => void };
}

const clamp01 = (v: number) => Math.max(0, Math.min(1, v));

// ---------------------------------------------------------------------------
// RatioSlider — bare PanResponder-driven slider (mirrors ScannerScreen's
// ZoomSlider; kept local since this screen's data flow differs — it drives
// camera.setZoom() imperatively rather than a local zoomLevel prop binding).
// ---------------------------------------------------------------------------
interface RatioSliderProps {
  value: number;
  min: number;
  max: number;
  onValueChange: (v: number) => void;
  /** Called on drag start/end so the parent ScrollView can pause scrolling —
   * without this, the ScrollView's own pan recognizer competes for the gesture
   * on iOS and the drag feels janky/steals mid-swipe. */
  onDraggingChange?: (dragging: boolean) => void;
}

function RatioSlider({ value, min, max, onValueChange, onDraggingChange }: RatioSliderProps) {
  const trackRef = useRef<View>(null);
  const widthRef = useRef(200);
  const trackLeftRef = useRef(0); // track's absolute screen-x (left edge)
  // Thumb driven by an Animated.Value updated imperatively from the PanResponder
  // — no React re-render per move.
  const thumbPct = useRef(new Animated.Value(0)).current;
  const draggingRef = useRef(false);
  // PanResponder is created once, so it MUST read the latest min/max/onValueChange
  // from a ref — they change when the camera leaves idle (1/1 -> e.g. 0.67/8).
  const cfgRef = useRef({ min, max, onValueChange, onDraggingChange });
  cfgRef.current = { min, max, onValueChange, onDraggingChange };
  // Range FROZEN at drag-start so a mid-drag lens auto-switch (which re-reports
  // min/max) can't shift the mapping under the finger.
  const dragRangeRef = useRef<{ lo: number; hi: number } | null>(null);

  // Map an ABSOLUTE screen x (gestureState.moveX) to a fraction of the track.
  // Using the absolute gesture coord + the measured track-left — NOT
  // e.nativeEvent.locationX — is the fix for the knob "jumping": locationX is
  // reported relative to whichever child (fill bar / thumb) sits under the
  // finger, so it lurches discontinuously as the finger crosses them mid-drag.
  function applyAtAbs(absX: number) {
    const r = dragRangeRef.current ?? { lo: cfgRef.current.min, hi: cfgRef.current.max };
    const span = Math.max(r.hi - r.lo, 0.001);
    const pct = clamp01((absX - trackLeftRef.current) / widthRef.current);
    thumbPct.setValue(pct); // move the thumb imperatively — no re-render
    // Continuous — no rounding — smooth zoom that reaches the exact endpoints.
    cfgRef.current.onValueChange(r.lo + pct * span);
  }

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      // NEVER yield the gesture to the enclosing ScrollView mid-drag — without
      // this the ScrollView's pan recognizer steals the responder on small
      // vertical drift and the drag stutters/dies (iOS especially).
      onPanResponderTerminationRequest: () => false,
      onPanResponderGrant: (_e, g) => {
        draggingRef.current = true;
        cfgRef.current.onDraggingChange?.(true);
        dragRangeRef.current = { lo: cfgRef.current.min, hi: cfgRef.current.max };
        applyAtAbs(g.x0);
      },
      onPanResponderMove: (_e, g) => applyAtAbs(g.moveX),
      onPanResponderRelease: () => {
        draggingRef.current = false;
        dragRangeRef.current = null;
        cfgRef.current.onDraggingChange?.(false);
      },
      onPanResponderTerminate: () => {
        draggingRef.current = false;
        dragRangeRef.current = null;
        cfgRef.current.onDraggingChange?.(false);
      },
    })
  ).current;

  // When NOT dragging, follow live state; skipped mid-drag so the finger-driven
  // thumb is never fought by the throttle-lagging state.
  React.useEffect(() => {
    if (draggingRef.current) return;
    thumbPct.setValue(clamp01((value - min) / Math.max(max - min, 0.001)));
  }, [value, min, max, thumbPct]);

  const widthStyle = thumbPct.interpolate({
    inputRange: [0, 1],
    outputRange: ['0%', '100%'],
  });

  return (
    <View
      ref={trackRef}
      style={sliderStyles.track}
      onLayout={(e) => {
        widthRef.current = e.nativeEvent.layout.width;
        // Absolute left is stable under vertical scroll, so measuring once here
        // is enough to map screen-x → track fraction.
        trackRef.current?.measureInWindow((x) => {
          trackLeftRef.current = x;
        });
      }}
      {...panResponder.panHandlers}
    >
      <Animated.View style={[sliderStyles.fill, { width: widthStyle }]} />
      <Animated.View style={[sliderStyles.thumb, { left: widthStyle }]} />
    </View>
  );
}

const sliderStyles = StyleSheet.create({
  track: {
    height: 4,
    backgroundColor: theme.colors.divider,
    borderRadius: 2,
    justifyContent: 'center',
    marginVertical: 16,
  },
  fill: {
    height: 4,
    backgroundColor: theme.colors.accent,
    borderRadius: 2,
  },
  thumb: {
    position: 'absolute',
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: '#fff',
    marginLeft: -10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.4,
    shadowRadius: 3,
    elevation: 4,
  },
});

// ---------------------------------------------------------------------------
// Small building blocks
// ---------------------------------------------------------------------------
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.row}>
      <Text style={styles.rowLabel}>{label}</Text>
      <Text style={styles.rowValue} selectable numberOfLines={1}>
        {value}
      </Text>
    </View>
  );
}

// ---------------------------------------------------------------------------
// CameraControlsQAScreen
// ---------------------------------------------------------------------------
export function CameraControlsQAScreen({ navigation }: Props) {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const camera = useCameraControls();

  // Composes cameraRef (start/stop/rescan) with camera.ref (state/capabilities
  // wiring) — same pattern as ScannerScreen's setVisionCameraRef.
  const setVisionCameraRef = useCallback(
    (instance: VisionCameraRefProps | null) => {
      (cameraRef as React.MutableRefObject<VisionCameraRefProps | null>).current = instance;
      // `camera.ref` is a real callback ref (see useCameraControls.ts).
      camera.ref(instance);
    },
    [camera.ref]
  );

  const [hasPermission, setHasPermission] = useState(false);
  // Paused while the zoom slider is being dragged so the ScrollView's pan
  // recognizer can't compete with the slider's PanResponder (iOS jank fix).
  const [scrollEnabled, setScrollEnabled] = useState(true);
  const [facing, setFacing] = useState<CameraFacing>('back');
  const [focusMode, setFocusMode] = useState<FocusMode>('continuous');
  const [pinnedLensId, setPinnedLensId] = useState<string | undefined>(undefined);
  const [lastFocusTap, setLastFocusTap] = useState<{ x: number; y: number } | null>(null);
  const previewSize = useRef({ width: 0, height: 0 });

  React.useEffect(() => {
    const perm = Platform.OS === 'ios' ? PERMISSIONS.IOS.CAMERA : PERMISSIONS.ANDROID.CAMERA;
    request(perm).then((result) => {
      setHasPermission(result === RESULTS.GRANTED);
      if (result !== RESULTS.GRANTED) {
        Alert.alert('Camera Permission Required', 'Please enable camera access in Settings.');
      }
    });
  }, []);

  const state = camera.state;
  const capabilities = camera.capabilities;

  const zoomStops = capabilities?.zoomStops?.[facing] ?? [];
  const minZoom = state?.minZoomRatio ?? 1;
  const maxZoom = state?.maxZoomRatio ?? 4;
  const currentZoom = state?.zoomRatio ?? 1;
  // Stringify only when capabilities actually change — not on every ~10Hz state
  // update. Re-serializing this on each render adds JS-thread work that competes
  // with an in-progress zoom drag.
  const capabilitiesJson = React.useMemo(
    () => (capabilities ? JSON.stringify(capabilities, null, 2) : 'Loading…'),
    [capabilities]
  );

  // Pin/facing coherence (both platforms): natively, a cross-facing pin wins and
  // switches the camera's facing out-of-band from the cameraFacing prop — so JS
  // state must follow the pin, and an explicit facing choice must drop a pin from
  // the other facing (otherwise the native reassert re-pins and yanks it back).
  const handlePinLens = useCallback((lensId: string, lensFacing: string) => {
    setPinnedLensId(lensId);
    setFacing(lensFacing === 'front' ? 'front' : 'back');
  }, []);

  const handleFacingSelect = useCallback(
    (next: CameraFacing) => {
      setFacing(next);
      const pinned = capabilities?.lenses.find((l) => l.id === pinnedLensId);
      if (pinned && pinned.facing !== next) {
        setPinnedLensId(undefined);
      }
    },
    [capabilities, pinnedLensId]
  );

  const handlePreviewTap = useCallback(
    (locationX: number, locationY: number) => {
      const { width, height } = previewSize.current;
      if (width <= 0 || height <= 0) return;
      const x = clamp01(locationX / width);
      const y = clamp01(locationY / height);
      setLastFocusTap({ x, y });
      camera.setFocusPoint(x, y);
    },
    [camera]
  );

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <MCIcon name="arrow-left" size={22} color={theme.colors.textPrimary} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Camera Controls QA</Text>
        <View style={styles.backBtn} />
      </View>

      {/* ── Camera preview (tap-to-focus surface) ── */}
      <View
        style={styles.previewContainer}
        onLayout={(e) => {
          previewSize.current = {
            width: e.nativeEvent.layout.width,
            height: e.nativeEvent.layout.height,
          };
        }}
        onStartShouldSetResponder={() => true}
        onResponderRelease={(e) =>
          handlePreviewTap(e.nativeEvent.locationX, e.nativeEvent.locationY)
        }
      >
        {hasPermission ? (
          <VisionCamera
            ref={setVisionCameraRef}
            style={StyleSheet.absoluteFill}
            scanMode="barcode"
            cameraFacing={facing}
            focusMode={focusMode}
            pinnedLensId={pinnedLensId}
            onCameraStateChanged={camera.onCameraStateChanged}
            onError={(err) => Alert.alert('Camera Error', err.message)}
          />
        ) : (
          <View style={styles.noPerm}>
            <Text style={styles.noPermText}>Camera permission required</Text>
          </View>
        )}
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        scrollEnabled={scrollEnabled}
      >
        {/* ── Live readout strip ── */}
        <Section title="Live State">
          <View style={styles.statusHeader}>
            <View
              style={[
                styles.dot,
                {
                  backgroundColor: state?.isPreviewActive
                    ? theme.colors.success
                    : theme.colors.error,
                },
              ]}
            />
            <Text style={styles.statusText}>{state?.status ?? 'unknown'}</Text>
          </View>
          <Row label="facing" value={state?.facing ?? '—'} />
          <Row label="activeLensId" value={state?.activeLensId ?? '—'} />
          <Row
            label="zoomRatio"
            value={
              state
                ? `${state.zoomRatio.toFixed(2)}  (min ${state.minZoomRatio.toFixed(2)} / max ${state.maxZoomRatio.toFixed(2)})`
                : '—'
            }
          />
          <Row label="torchEnabled" value={state ? String(state.torchEnabled) : '—'} />
          <Row label="focusMode" value={state?.focusMode ?? '—'} />
          {state?.warningCode ? (
            <Row
              label="warning"
              value={`${state.warningCode}${state.warningMessage ? `: ${state.warningMessage}` : ''}`}
            />
          ) : null}
          {state?.errorCode ? (
            <Row
              label="error"
              value={`${state.errorCode}${state.errorMessage ? `: ${state.errorMessage}` : ''}`}
            />
          ) : null}
        </Section>

        {/* ── Zoom ── */}
        <Section title="Zoom">
          <RatioSlider
            value={currentZoom}
            min={minZoom}
            max={maxZoom}
            onValueChange={(v) => camera.setZoom(v)}
            onDraggingChange={(dragging) => setScrollEnabled(!dragging)}
          />
          <Text style={styles.helperText}>{currentZoom.toFixed(2)}x</Text>
          {zoomStops.length > 0 ? (
            <ZoomPills presets={zoomStops} current={currentZoom} onSelect={camera.setZoom} />
          ) : (
            <Text style={styles.helperTextMuted}>No zoom stops reported for {facing}.</Text>
          )}
        </Section>

        {/* ── Torch ── */}
        <Section title="Torch">
          <TouchableOpacity
            style={[styles.toggleBtn, state?.torchEnabled && styles.toggleBtnActive]}
            onPress={() => camera.setTorch(!(state?.torchEnabled ?? false))}
          >
            <Text style={[styles.toggleBtnText, state?.torchEnabled && styles.toggleBtnTextActive]}>
              {state?.torchEnabled ? 'Torch ON' : 'Torch OFF'}
            </Text>
          </TouchableOpacity>
        </Section>

        {/* ── Facing ── */}
        <Section title="Facing">
          <SegmentedControl<CameraFacing>
            segments={[
              { label: 'Back', value: 'back' },
              { label: 'Front', value: 'front' },
            ]}
            selected={facing}
            onSelect={handleFacingSelect}
          />
        </Section>

        {/* ── Focus mode ── */}
        <Section title="Focus Mode">
          <SegmentedControl<FocusMode>
            segments={[
              { label: 'Continuous', value: 'continuous' },
              { label: 'Single', value: 'single' },
              { label: 'Locked', value: 'locked' },
            ]}
            selected={focusMode}
            onSelect={setFocusMode}
          />
        </Section>

        {/* ── Tap-to-focus ── */}
        <Section title="Tap-to-Focus">
          <Text style={styles.helperTextMuted}>
            Tap anywhere on the preview above to call setFocusPoint(x, y) with
            view-normalized coordinates.
          </Text>
          <Row
            label="last tap"
            value={lastFocusTap ? `(${lastFocusTap.x.toFixed(2)}, ${lastFocusTap.y.toFixed(2)})` : '—'}
          />
        </Section>

        {/* ── Lens picker ── */}
        <Section title="Lenses">
          <TouchableOpacity
            style={[styles.lensRow, pinnedLensId === undefined && styles.lensRowActive]}
            onPress={() => setPinnedLensId(undefined)}
          >
            <Text style={styles.lensRowText}>Auto (no pin)</Text>
          </TouchableOpacity>
          {(capabilities?.lenses ?? []).map((lens) => (
            <TouchableOpacity
              key={lens.id}
              style={[styles.lensRow, pinnedLensId === lens.id && styles.lensRowActive]}
              disabled={!lens.isPinnable}
              onPress={() => handlePinLens(lens.id, lens.facing)}
            >
              <Text style={[styles.lensRowText, !lens.isPinnable && styles.lensRowTextDisabled]}>
                {lens.id} · {lens.kind} · {lens.facing}
                {lens.isPinnable ? '' : ' (not pinnable)'}
                {state?.activeLensId === lens.id ? '  ← active' : ''}
              </Text>
            </TouchableOpacity>
          ))}
          {!capabilities ? (
            <Text style={styles.helperTextMuted}>Loading capabilities…</Text>
          ) : null}
        </Section>

        {/* ── Capabilities panel ── */}
        <Section title="Capabilities (raw)">
          <ScrollView horizontal>
            <Text style={styles.capabilitiesJson} selectable>
              {capabilitiesJson}
            </Text>
          </ScrollView>
        </Section>

        {/* ── Start / Stop / Rescan ── */}
        <Section title="Session">
          <View style={styles.sessionRow}>
            <TouchableOpacity
              style={styles.sessionBtn}
              onPress={() => cameraRef.current?.start()}
            >
              <Text style={styles.sessionBtnText}>Start</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.sessionBtn}
              onPress={() => cameraRef.current?.stop()}
            >
              <Text style={styles.sessionBtnText}>Stop</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.sessionBtn}
              onPress={() => cameraRef.current?.rescan()}
            >
              <Text style={styles.sessionBtnText}>Rescan</Text>
            </TouchableOpacity>
          </View>
        </Section>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: theme.spacing.md,
    paddingVertical: theme.spacing.sm,
  },
  backBtn: { width: 36, height: 36, alignItems: 'center', justifyContent: 'center' },
  headerTitle: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.lg,
    fontWeight: theme.fontWeight.bold,
  },
  previewContainer: {
    height: 220,
    backgroundColor: theme.colors.bgDeep,
    marginHorizontal: theme.spacing.md,
    borderRadius: theme.radii.lg,
    overflow: 'hidden',
  },
  noPerm: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  noPermText: { color: theme.colors.textMuted, fontSize: theme.fontSize.sm },
  scroll: { flex: 1, marginTop: theme.spacing.md },
  scrollContent: { paddingHorizontal: theme.spacing.md, paddingBottom: theme.spacing.huge },
  section: {
    backgroundColor: theme.colors.bgCard,
    borderRadius: theme.radii.lg,
    padding: theme.spacing.lg,
    marginBottom: theme.spacing.md,
  },
  sectionTitle: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.bold,
    textTransform: 'uppercase',
    letterSpacing: theme.letterSpacing.wide,
    marginBottom: theme.spacing.sm,
  },
  statusHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: theme.spacing.sm },
  dot: { width: 10, height: 10, borderRadius: 5, marginRight: 8 },
  statusText: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.md,
    fontWeight: theme.fontWeight.semibold,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },
  rowLabel: { color: theme.colors.textMuted, fontSize: theme.fontSize.sm },
  rowValue: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.medium,
    marginLeft: theme.spacing.md,
    flexShrink: 1,
    textAlign: 'right',
  },
  helperText: { color: theme.colors.textPrimary, fontSize: theme.fontSize.sm, marginBottom: 8 },
  helperTextMuted: { color: theme.colors.textMuted, fontSize: theme.fontSize.xs },
  toggleBtn: {
    alignSelf: 'flex-start',
    paddingHorizontal: theme.spacing.lg,
    paddingVertical: theme.spacing.sm,
    borderRadius: theme.radii.lg,
    backgroundColor: theme.colors.bgCardStrong,
  },
  toggleBtnActive: { backgroundColor: theme.colors.accent },
  toggleBtnText: { color: theme.colors.textPrimary, fontWeight: theme.fontWeight.semibold },
  toggleBtnTextActive: { color: theme.colors.textOnAccent },
  lensRow: {
    paddingVertical: theme.spacing.sm,
    paddingHorizontal: theme.spacing.md,
    borderRadius: theme.radii.md,
    marginBottom: 4,
  },
  lensRowActive: { backgroundColor: theme.colors.accentDim },
  lensRowText: { color: theme.colors.textPrimary, fontSize: theme.fontSize.sm },
  lensRowTextDisabled: { color: theme.colors.textMuted },
  capabilitiesJson: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xxs,
    fontFamily: 'Menlo',
  },
  sessionRow: { flexDirection: 'row', gap: 10 },
  sessionBtn: {
    flex: 1,
    paddingVertical: theme.spacing.sm,
    borderRadius: theme.radii.lg,
    backgroundColor: theme.colors.bgCardStrong,
    alignItems: 'center',
  },
  sessionBtnText: { color: theme.colors.textPrimary, fontWeight: theme.fontWeight.semibold },
});
