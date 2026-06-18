/**
 * ScannerScreen — redesigned modern dark camera UI.
 *
 * Layout architecture:
 *   ┌──────────────────────────────────┐
 *   │  [FPS chip] top strip  [indicators] │  ← frosted top strip
 *   │                                  │
 *   │         CAMERA PREVIEW           │  ← full bleed
 *   │                                  │
 *   │  [OCR/template controls]         │  ← floating mid-area
 *   │  [on-device hint / progress]     │
 *   │  [template create panel]         │
 *   │                                  │
 *   │  [manual/auto pill]              │  ← above control cluster
 *   ├──────────────────────────────────┤
 *   │  [flash] [tmpl] [zoom…] [expand] │  ← translucent control row
 *   │  [flip]  [ CAPTURE ]   [settings] │  ← bottom bar
 *   └──────────────────────────────────┘
 *
 * Bottom mode switcher: horizontal scrollable pill row (iOS-camera style),
 * replaced by a dropdown pill when OCR mode is selected.
 */
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  Alert,
  Animated,
  Modal,
  PanResponder,
  Platform,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  Vibration,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { PERMISSIONS, RESULTS, request } from 'react-native-permissions';
import {
  VisionCamera,
  type VisionCameraCaptureEvent,
  type VisionCameraRefProps,
  type VisionCameraScanMode,
} from '../../../src/VisionCamera';
import type {
  VisionCameraRecognitionUpdateEvent,
  VisionCameraSharpnessScoreEvent,
  VisionCameraBarcodeDetectedEvent,
  VisionCameraBoundingBoxesUpdateEvent,
} from '../../../src/VisionCameraTypes';
import { VisionCore } from '../../../src';
import { SettingsModal } from './SettingsModal';
import { ResultScreen } from './ResultScreen';
import { theme } from '../theme';
import { API_KEY, ENVIRONMENT, NO_API_KEY } from '../config';
import {
  loadSettings,
  saveSettings,
  type AppSettings,
  type SoundMode,
} from '../lib/settings';
import {
  loadTemplates,
  addTemplate,
  deleteTemplate,
  deleteAllTemplates,
  buildTemplateId,
  type TemplateCode,
  type TemplateData,
} from '../lib/templates';
import { TemplateManagerModal } from '../components/TemplateManagerModal';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------
type OCRModuleType =
  | 'shipping_label'
  | 'bill_of_lading'
  | 'item_label'
  | 'document_classification';

interface BarcodeResultItem {
  scannedCode: string;
  symbology: string;
  gs1ExtractedInfo?: Record<string, string>;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------
const SCAN_MODES: { label: string; value: VisionCameraScanMode }[] = [
  { label: 'Barcode', value: 'barcode' },
  { label: 'QR Code', value: 'qrcode' },
  { label: 'Vision', value: 'ocr' },
  { label: 'Photo', value: 'photo' },
];

const OCR_MODULE_OPTIONS: { label: string; type: OCRModuleType }[] = [
  { label: 'Shipping Label', type: 'shipping_label' },
  { label: 'Bill of Lading', type: 'bill_of_lading' },
  { label: 'Item Labels', type: 'item_label' },
  { label: 'Doc. Classification', type: 'document_classification' },
];

const ZOOM_PRESETS = [0.6, 1.0, 2.0];
const DEFAULT_ZOOM = 1.0;
const SHARPNESS_THRESHOLD = 0.5;

function vibrate(pattern?: number[]) {
  Vibration.vibrate(pattern ?? 200);
}

// ---------------------------------------------------------------------------
// Animated press wrapper
// ---------------------------------------------------------------------------
interface PressScaleProps {
  onPress: () => void;
  children: React.ReactNode;
  style?: object;
  disabled?: boolean;
  activeOpacity?: number;
}

function PressScale({ onPress, children, style, disabled }: PressScaleProps) {
  const scale = useRef(new Animated.Value(1)).current;

  const onPressIn = () => {
    Animated.spring(scale, {
      toValue: 0.88,
      useNativeDriver: true,
      speed: 40,
      bounciness: 0,
    }).start();
  };

  const onPressOut = () => {
    Animated.spring(scale, {
      toValue: 1,
      useNativeDriver: true,
      speed: 24,
      bounciness: 4,
    }).start();
  };

  return (
    <TouchableOpacity
      onPress={onPress}
      onPressIn={onPressIn}
      onPressOut={onPressOut}
      disabled={disabled}
      activeOpacity={1}
    >
      <Animated.View style={[style, { transform: [{ scale }] }]}>
        {children}
      </Animated.View>
    </TouchableOpacity>
  );
}

// ---------------------------------------------------------------------------
// BarcodeResultModal — redesigned bottom sheet
// ---------------------------------------------------------------------------
interface BarcodeResultModalProps {
  codes: BarcodeResultItem[];
  onClose: () => void;
}

function BarcodeResultModal({ codes, onClose }: BarcodeResultModalProps) {
  const slideAnim = useRef(new Animated.Value(300)).current;

  useEffect(() => {
    Animated.spring(slideAnim, {
      toValue: 0,
      useNativeDriver: true,
      damping: 22,
      stiffness: 220,
    }).start();
  }, [slideAnim]);

  const allText = codes
    .map((c) => `[${c.symbology}] ${c.scannedCode}`)
    .join('\n');

  return (
    <Modal visible animationType="none" transparent onRequestClose={onClose}>
      <View style={bStyles.overlay}>
        <Animated.View
          style={[bStyles.sheet, { transform: [{ translateY: slideAnim }] }]}
        >
          {/* Grab handle */}
          <View style={bStyles.grabber} />

          <View style={bStyles.header}>
            <Text style={bStyles.title}>Scan Result</Text>
            <TouchableOpacity
              onPress={onClose}
              style={bStyles.closeIconBtn}
              hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
            >
              <MCIcon name="close" size={20} color={theme.colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <ScrollView style={bStyles.list} showsVerticalScrollIndicator={false}>
            {codes.map((c, i) => (
              <View key={i} style={bStyles.row}>
                <View style={bStyles.rowLeft}>
                  <View style={bStyles.symBadge}>
                    <Text style={bStyles.symText}>{c.symbology}</Text>
                  </View>
                  <View style={bStyles.rowContent}>
                    <Text style={bStyles.rowCode} selectable numberOfLines={2}>
                      {c.scannedCode}
                    </Text>
                    {c.gs1ExtractedInfo &&
                    Object.keys(c.gs1ExtractedInfo).length > 0 ? (
                      <Text style={bStyles.gs1} selectable numberOfLines={1}>
                        GS1: {Object.entries(c.gs1ExtractedInfo).map(([k, v]) => `${k}=${v}`).join(', ')}
                      </Text>
                    ) : null}
                  </View>
                </View>
                <TouchableOpacity
                  onPress={() => Alert.alert('Copied', c.scannedCode)}
                  style={bStyles.copyBtn}
                  hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
                >
                  <MCIcon name="content-copy" size={16} color={theme.colors.info} />
                </TouchableOpacity>
              </View>
            ))}
          </ScrollView>

          <View style={bStyles.actions}>
            {codes.length > 1 ? (
              <TouchableOpacity
                style={[bStyles.actionBtn, bStyles.actionBtnAccent]}
                onPress={() => Alert.alert('Copied All', allText)}
              >
                <MCIcon name="content-copy" size={14} color={theme.colors.textOnAccent} style={{ marginRight: 6 }} />
                <Text style={bStyles.actionBtnTextDark}>Copy All</Text>
              </TouchableOpacity>
            ) : null}
            <TouchableOpacity
              style={[bStyles.actionBtn, bStyles.actionBtnGray]}
              onPress={onClose}
            >
              <Text style={bStyles.actionBtnText}>Dismiss</Text>
            </TouchableOpacity>
          </View>
        </Animated.View>
      </View>
    </Modal>
  );
}

const bStyles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: theme.colors.bgSheet,
    borderTopLeftRadius: theme.radii.xxl,
    borderTopRightRadius: theme.radii.xxl,
    paddingHorizontal: theme.spacing.xxl,
    paddingBottom: 32,
    maxHeight: '72%',
  },
  grabber: {
    width: 36,
    height: 4,
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: 10,
    marginBottom: 4,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
    marginBottom: theme.spacing.sm,
  },
  title: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.lg,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.tight,
  },
  closeIconBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: theme.colors.bgCard,
    alignItems: 'center',
    justifyContent: 'center',
  },
  list: { maxHeight: 280 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
  },
  rowLeft: { flex: 1, flexDirection: 'row', alignItems: 'flex-start', marginRight: 8 },
  symBadge: {
    backgroundColor: theme.colors.accentDim,
    borderRadius: theme.radii.xs,
    paddingHorizontal: 6,
    paddingVertical: 3,
    marginRight: 10,
    marginTop: 1,
  },
  symText: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.wide,
  },
  rowContent: { flex: 1 },
  rowCode: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
    lineHeight: 18,
  },
  gs1: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xxs,
    marginTop: 3,
  },
  copyBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(10,132,255,0.12)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  actions: { flexDirection: 'row', gap: 10, marginTop: theme.spacing.lg },
  actionBtn: {
    flex: 1,
    paddingVertical: 13,
    borderRadius: theme.radii.lg,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
  },
  actionBtnAccent: { backgroundColor: theme.colors.accent },
  actionBtnGray: { backgroundColor: theme.colors.bgCardStrong },
  actionBtnText: {
    color: theme.colors.textPrimary,
    fontWeight: theme.fontWeight.semibold,
    fontSize: theme.fontSize.sm,
  },
  actionBtnTextDark: {
    color: theme.colors.textOnAccent,
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.sm,
  },
});

// ---------------------------------------------------------------------------
// Generic bottom-sheet picker — redesigned
// ---------------------------------------------------------------------------
interface SheetPickerProps<T extends string> {
  visible: boolean;
  title: string;
  options: { label: string; value: T }[];
  current: T;
  onSelect: (value: T) => void;
  onClose: () => void;
}

function SheetPicker<T extends string>({
  visible,
  title,
  options,
  current,
  onSelect,
  onClose,
}: SheetPickerProps<T>) {
  const slideAnim = useRef(new Animated.Value(400)).current;

  useEffect(() => {
    if (visible) {
      Animated.spring(slideAnim, {
        toValue: 0,
        useNativeDriver: true,
        damping: 22,
        stiffness: 200,
      }).start();
    } else {
      slideAnim.setValue(400);
    }
  }, [visible, slideAnim]);

  return (
    <Modal
      visible={visible}
      animationType="none"
      transparent
      onRequestClose={onClose}
    >
      <TouchableOpacity
        style={spStyles.scrim}
        activeOpacity={1}
        onPress={onClose}
      />
      <Animated.View
        style={[spStyles.sheet, { transform: [{ translateY: slideAnim }] }]}
      >
        <View style={spStyles.grabber} />
        <Text style={spStyles.title}>{title}</Text>
        {options.map((opt) => {
          const active = opt.value === current;
          return (
            <TouchableOpacity
              key={opt.value}
              style={[spStyles.row, active && spStyles.rowActive]}
              onPress={() => {
                onSelect(opt.value);
                onClose();
              }}
            >
              <Text
                style={[spStyles.rowLabel, active && spStyles.rowLabelActive]}
              >
                {opt.label}
              </Text>
              {active ? (
                <MCIcon name="check" size={18} color={theme.colors.accent} />
              ) : null}
            </TouchableOpacity>
          );
        })}
        <TouchableOpacity style={spStyles.cancelBtn} onPress={onClose}>
          <Text style={spStyles.cancelText}>Cancel</Text>
        </TouchableOpacity>
      </Animated.View>
    </Modal>
  );
}

const spStyles = StyleSheet.create({
  scrim: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.6)',
  },
  sheet: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: theme.colors.bgModal,
    borderTopLeftRadius: theme.radii.xxl,
    borderTopRightRadius: theme.radii.xxl,
    paddingHorizontal: theme.spacing.xxl,
    paddingBottom: 28,
  },
  grabber: {
    width: 36,
    height: 4,
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: 10,
    marginBottom: 4,
  },
  title: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.bold,
    textAlign: 'center',
    marginBottom: theme.spacing.md,
    letterSpacing: theme.letterSpacing.widest,
    textTransform: 'uppercase',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 15,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
  },
  rowActive: {},
  rowLabel: {
    flex: 1,
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.md,
    fontWeight: theme.fontWeight.medium,
  },
  rowLabelActive: {
    color: theme.colors.accent,
    fontWeight: theme.fontWeight.semibold,
  },
  cancelBtn: {
    marginTop: theme.spacing.lg,
    paddingVertical: 13,
    borderRadius: theme.radii.lg,
    backgroundColor: theme.colors.bgCardStrong,
    alignItems: 'center',
  },
  cancelText: {
    color: theme.colors.textPrimary,
    fontWeight: theme.fontWeight.semibold,
    fontSize: theme.fontSize.sm,
  },
});

// ---------------------------------------------------------------------------
// ZoomSlider — PanResponder-based horizontal slider
// ---------------------------------------------------------------------------
interface ZoomSliderProps {
  value: number;
  min: number;
  max: number;
  onValueChange: (v: number) => void;
}

function ZoomSlider({ value, min, max, onValueChange }: ZoomSliderProps) {
  const sliderWidthRef = useRef(200);
  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: (e) => {
        const { locationX } = e.nativeEvent;
        const pct = Math.max(0, Math.min(1, locationX / sliderWidthRef.current));
        const z = min + pct * (max - min);
        onValueChange(Math.round(z * 10) / 10);
      },
      onPanResponderMove: (e) => {
        const { locationX } = e.nativeEvent;
        const pct = Math.max(0, Math.min(1, locationX / sliderWidthRef.current));
        const z = min + pct * (max - min);
        onValueChange(Math.round(z * 10) / 10);
      },
    })
  ).current;

  const pct = (value - min) / (max - min);

  return (
    <View
      style={zsStyles.track}
      onLayout={(e) => {
        sliderWidthRef.current = e.nativeEvent.layout.width;
      }}
      {...panResponder.panHandlers}
    >
      <View style={[zsStyles.fill, { width: `${pct * 100}%` as `${number}%` }]} />
      <View
        style={[zsStyles.thumb, { left: `${pct * 100}%` as `${number}%` }]}
      />
    </View>
  );
}

const zsStyles = StyleSheet.create({
  track: {
    flex: 1,
    height: 3,
    backgroundColor: 'rgba(255,255,255,0.15)',
    borderRadius: 2,
    marginHorizontal: 10,
    justifyContent: 'center',
    position: 'relative',
  },
  fill: {
    height: 3,
    backgroundColor: theme.colors.accent,
    borderRadius: 2,
  },
  thumb: {
    position: 'absolute',
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: '#fff',
    top: -7.5,
    marginLeft: -9,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.4,
    shadowRadius: 3,
    elevation: 4,
  },
});

// ---------------------------------------------------------------------------
// CaptureButton — ring with sharpness-reactive yellow fill
// ---------------------------------------------------------------------------
interface CaptureButtonProps {
  sharpness: number;
  onPress: () => void;
  disabled: boolean;
}

function CaptureButton({ sharpness, onPress, disabled }: CaptureButtonProps) {
  const scaleAnim = useRef(new Animated.Value(1)).current;
  const opacityAnim = useRef(new Animated.Value(1)).current;

  const isReady = sharpness >= SHARPNESS_THRESHOLD;

  const onPressIn = () => {
    Animated.parallel([
      Animated.spring(scaleAnim, {
        toValue: 0.92,
        useNativeDriver: true,
        speed: 50,
        bounciness: 0,
      }),
      Animated.timing(opacityAnim, {
        toValue: 0.75,
        duration: 80,
        useNativeDriver: true,
      }),
    ]).start();
  };

  const onPressOut = () => {
    Animated.parallel([
      Animated.spring(scaleAnim, {
        toValue: 1,
        useNativeDriver: true,
        speed: 20,
        bounciness: 6,
      }),
      Animated.timing(opacityAnim, {
        toValue: 1,
        duration: 120,
        useNativeDriver: true,
      }),
    ]).start();
  };

  const ringColor = disabled
    ? 'rgba(255,255,255,0.2)'
    : isReady
    ? theme.colors.accent
    : 'rgba(255,255,255,0.65)';

  const innerColor = disabled
    ? 'rgba(255,255,255,0.25)'
    : isReady
    ? theme.colors.accent
    : '#fff';

  return (
    <TouchableOpacity
      onPress={onPress}
      onPressIn={onPressIn}
      onPressOut={onPressOut}
      disabled={disabled}
      activeOpacity={1}
    >
      <Animated.View
        style={[
          capStyles.outer,
          { borderColor: ringColor, transform: [{ scale: scaleAnim }], opacity: opacityAnim },
        ]}
      >
        <View style={[capStyles.inner, { backgroundColor: innerColor }]} />
      </Animated.View>
    </TouchableOpacity>
  );
}

const capStyles = StyleSheet.create({
  outer: {
    width: 74,
    height: 74,
    borderRadius: 37,
    borderWidth: 3,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 8,
  },
  inner: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },
});

// ---------------------------------------------------------------------------
// ScannerScreen
// ---------------------------------------------------------------------------
interface Props {
  navigation: { navigate: (name: string) => void };
}

export function ScannerScreen({ navigation }: Props) {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const insets = useSafeAreaInsets();

  const [hasPermission, setHasPermission] = useState(false);
  const [settings, setSettings] = useState<AppSettings | null>(null);
  const [showSettings, setShowSettings] = useState(false);

  // Scanning state
  const [scanMode, setScanMode] = useState<VisionCameraScanMode>('barcode');
  const [autoCapture, setAutoCapture] = useState(false);
  const [flashEnabled, setFlashEnabled] = useState(false);
  const [cameraFacing, setCameraFacing] = useState<'back' | 'front'>('back');
  const [zoomLevel, setZoomLevel] = useState(DEFAULT_ZOOM);
  const [soundMode, setSoundMode] = useState<SoundMode>('Vibrate');
  const [frameSkip, setFrameSkip] = useState(10);

  // CB FPS counter
  const [cbFps, setCbFps] = useState<number | null>(null);
  const cbFpsFrames = useRef(0);
  const cbFpsLastTime = useRef(Date.now());
  // Tracks when the last tick arrived; used for idle-decay to '--' after 2 s of silence.
  const cbFpsLastTickTime = useRef(0);
  // Tracks the bounding-box code count for the chip label.
  const cbFpsCodeCount = useRef(0);

  // Recognition indicators
  const [recognition, setRecognition] = useState({
    text: false,
    barcode: false,
    qrcode: false,
    document: false,
  });
  const [sharpness, setSharpness] = useState(1);
  const lastSharpnessRef = useRef(0);
  const sharpnessThrottle = 200;

  // OCR
  const [ocrModuleType, setOcrModuleType] = useState<OCRModuleType>('shipping_label');
  const [ocrConnectivity, setOcrConnectivity] = useState<'cloud' | 'on_device'>('cloud');
  const [showOcrModulePicker, setShowOcrModulePicker] = useState(false);

  // Mode picker
  const [showModePicker, setShowModePicker] = useState(false);

  // Zoom slider
  const [showZoomSlider, setShowZoomSlider] = useState(false);

  // Result / capture
  const [isProcessing, setIsProcessing] = useState(false);
  const [showResult, setShowResult] = useState(false);
  const [resultImagePath, setResultImagePath] = useState('');
  const [resultResponse, setResultResponse] = useState<Record<string, unknown> | null>(null);
  const [capturedBarcodes, setCapturedBarcodes] = useState<BarcodeResultItem[]>([]);

  // Barcode result sheet
  const [showBarcodeResult, setShowBarcodeResult] = useState(false);
  const [pendingBarcodes, setPendingBarcodes] = useState<BarcodeResultItem[]>([]);

  // Template state
  const [savedTemplates, setSavedTemplates] = useState<TemplateData[]>([]);
  const [activeTemplate, setActiveTemplate] = useState<TemplateData | null>(null);
  const [showTemplateManager, setShowTemplateManager] = useState(false);
  const [isTemplateCreateMode, setIsTemplateCreateMode] = useState(false);
  const [pendingTemplateCodes, setPendingTemplateCodes] = useState<TemplateCode[]>([]);
  const isTemplateCreateModeRef = useRef(false);
  useEffect(() => {
    isTemplateCreateModeRef.current = isTemplateCreateMode;
  }, [isTemplateCreateMode]);

  // On-device model
  const [modelReady, setModelReady] = useState(false);
  const [modelDownloading, setModelDownloading] = useState(false);
  const [modelDownloadProgress, setModelDownloadProgress] = useState(0);

  // Manual entry
  const [manualEntryVisible, setManualEntryVisible] = useState(false);
  const [manualEntryValue, setManualEntryValue] = useState('');
  const [manualEntryLabel, setManualEntryLabel] = useState('Barcode');

  // ---------------------------------------------------------------------------
  // Init
  // ---------------------------------------------------------------------------
  useEffect(() => {
    VisionCore.setEnvironment(ENVIRONMENT);

    if (Platform.OS === 'android') {
      try {
        if (!VisionCore.isModelManagerInitialized()) {
          VisionCore.initializeModelManager({
            maxConcurrentDownloads: 2,
            enableLogging: true,
          });
        }
      } catch {
        // Already initialized
      }
    }

    loadSettings().then((s) => {
      setSettings(s);
      setScanMode(s.scanMode);
      setAutoCapture(s.autoCapture);
      setSoundMode(s.soundMode);
      setFrameSkip(s.frameSkip);
      setOcrModuleType(s.ocrSelection.moduleType);
      setOcrConnectivity(s.ocrSelection.connectivity);
      setZoomLevel(DEFAULT_ZOOM);
    });

    loadTemplates().then(setSavedTemplates);
    requestCameraPermission();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const requestCameraPermission = async () => {
    const perm =
      Platform.OS === 'ios'
        ? PERMISSIONS.IOS.CAMERA
        : PERMISSIONS.ANDROID.CAMERA;
    const result = await request(perm);
    setHasPermission(result === RESULTS.GRANTED);
    if (result !== RESULTS.GRANTED) {
      Alert.alert('Camera Permission Required', 'Please enable camera access in Settings.', [
        { text: 'OK' },
      ]);
    }
  };

  // ---------------------------------------------------------------------------
  // Settings save
  // ---------------------------------------------------------------------------
  const handleSaveSettings = useCallback(
    (updated: AppSettings) => {
      setSettings(updated);
      setSoundMode(updated.soundMode);
      setFrameSkip(updated.frameSkip);
      setOcrModuleType(updated.ocrSelection.moduleType);
      setOcrConnectivity(updated.ocrSelection.connectivity);
      if (updated.multipleScan && (scanMode === 'barcode' || scanMode === 'barcodesinglecapture')) {
        setScanMode('barcode');
      } else if (!updated.multipleScan && scanMode === 'barcode') {
        setScanMode('barcodesinglecapture');
      }
      saveSettings(updated);
    },
    [scanMode]
  );

  // ---------------------------------------------------------------------------
  // On-device model check
  // ---------------------------------------------------------------------------
  useEffect(() => {
    if (scanMode !== 'ocr' || ocrConnectivity !== 'on_device') {
      setModelReady(false);
      return;
    }
    const module = { type: ocrModuleType, size: settings?.modelSize ?? 'micro' };
    const isLoaded = VisionCore.isModelLoaded(module);
    setModelReady(isLoaded);
    if (!isLoaded) {
      VisionCore.findDownloadedModel(module).then((info) => {
        setModelReady(!!info);
      });
    }
  }, [scanMode, ocrModuleType, ocrConnectivity, settings?.modelSize]);

  // ---------------------------------------------------------------------------
  // Mode change
  // ---------------------------------------------------------------------------
  const handleModeSelect = useCallback(
    (mode: VisionCameraScanMode) => {
      setScanMode(mode);
      setAutoCapture(false);
      setZoomLevel(DEFAULT_ZOOM);
      if (settings) {
        const updated = { ...settings, scanMode: mode, autoCapture: false };
        setSettings(updated);
        saveSettings(updated);
      }
    },
    [settings]
  );

  // ---------------------------------------------------------------------------
  // CB FPS tracking
  // ---------------------------------------------------------------------------
  // onRecognitionUpdate fires per processed frame (throttled at 10 fps by the
  // Android bridge; unthrottled on iOS). onBoundingBoxesUpdate only fires when
  // codes are actually in frame — it is NOT a per-frame stream. So
  // onRecognitionUpdate is the tick source; onBoundingBoxesUpdate is used only
  // to keep the "codes: N" label current.
  const tickCbFps = useCallback(() => {
    const now = Date.now();
    cbFpsLastTickTime.current = now;
    cbFpsFrames.current += 1;
    const elapsed = now - cbFpsLastTime.current;
    if (elapsed >= 1000) {
      setCbFps(Math.round((cbFpsFrames.current * 1000) / elapsed));
      cbFpsFrames.current = 0;
      cbFpsLastTime.current = now;
    }
  }, []);

  // Idle-decay: reset to '--' when no tick arrives for 2 s. Interval runs at
  // 500 ms to keep latency acceptable without hammering the JS thread. The
  // cbFps !== null guard ensures no re-render when already showing '--'.
  useEffect(() => {
    const id = setInterval(() => {
      if (Date.now() - cbFpsLastTickTime.current > 2000) {
        setCbFps((prev) => (prev !== null ? null : prev));
      }
    }, 500);
    return () => clearInterval(id);
  }, []);

  // ---------------------------------------------------------------------------
  // Camera events
  // ---------------------------------------------------------------------------
  const handleRecognitionUpdate = useCallback(
    (e: VisionCameraRecognitionUpdateEvent) => {
      setRecognition(e);
      tickCbFps();
    },
    [tickCbFps]
  );

  const handleSharpnessUpdate = useCallback(
    (e: VisionCameraSharpnessScoreEvent) => {
      const now = Date.now();
      if (now - lastSharpnessRef.current >= sharpnessThrottle) {
        lastSharpnessRef.current = now;
        setSharpness(e.sharpnessScore);
      }
    },
    []
  );

  const handleBarcodeDetected = useCallback(
    (e: VisionCameraBarcodeDetectedEvent) => {
      if (!e.codes || e.codes.length === 0) return;

      if (isTemplateCreateModeRef.current) {
        setPendingTemplateCodes((prev) => {
          let updated = [...prev];
          let added = false;
          for (const c of e.codes) {
            const exists = updated.some(
              (t) => t.codeString === c.scannedCode && t.codeSymbology === c.symbology
            );
            if (!exists) {
              updated = [
                ...updated,
                { codeString: c.scannedCode, codeSymbology: c.symbology },
              ];
              added = true;
            }
          }
          if (added && soundMode === 'Vibrate') vibrate([0, 60]);
          return updated;
        });
        return;
      }

      const results: BarcodeResultItem[] = e.codes.map((c) => ({
        scannedCode: c.scannedCode,
        symbology: c.symbology,
        gs1ExtractedInfo: c.gs1ExtractedInfo,
      }));
      if (soundMode === 'Vibrate') vibrate([0, 100]);
      setPendingBarcodes(results);
      setShowBarcodeResult(true);
    },
    [soundMode]
  );

  // onBoundingBoxesUpdate fires only when codes are in frame (not per processed
  // frame). Used here only to update the code count label — not as a tick source.
  const handleBoundingBoxesUpdate = useCallback(
    (e: VisionCameraBoundingBoxesUpdateEvent) => {
      cbFpsCodeCount.current =
        (e.barcodeBoundingBoxes?.length ?? 0) +
        (e.qrCodeBoundingBoxes?.length ?? 0);
    },
    []
  );

  const handleCapture = useCallback(
    async (e: VisionCameraCaptureEvent) => {
      const imagePath = e.image ?? '';
      const barcodeData: BarcodeResultItem[] = (e.barcodes ?? []).map((b) => ({
        scannedCode: b.scannedCode,
        symbology: b.symbology ?? '',
        gs1ExtractedInfo: b.gs1ExtractedInfo,
      }));
      setCapturedBarcodes(barcodeData);

      if (scanMode === 'photo') {
        if (soundMode === 'Vibrate') vibrate([0, 100]);
        Alert.alert('Photo Captured', `Sharpness: ${(e.sharpnessScore ?? 0).toFixed(2)}`);
        cameraRef.current?.rescan();
        return;
      }

      if (
        scanMode === 'barcode' ||
        scanMode === 'qrcode' ||
        scanMode === 'barcodeorqrcode' ||
        scanMode === 'barcodesinglecapture'
      ) {
        if (barcodeData.length === 0) {
          if (soundMode === 'Vibrate') vibrate([0, 50, 50, 50]);
          const modeLabel = scanMode === 'qrcode' ? 'QR Code' : 'Barcode';
          Alert.alert(
            `No ${modeLabel} Found`,
            `Please capture when the indicator is active or manually enter the code.`,
            [
              { text: 'OK', onPress: () => cameraRef.current?.rescan() },
              {
                text: 'Enter Manually',
                onPress: () => {
                  promptManualBarcode(modeLabel);
                  cameraRef.current?.rescan();
                },
              },
            ]
          );
          return;
        }
        if (soundMode === 'Vibrate') vibrate([0, 100]);
        setPendingBarcodes(barcodeData);
        setShowBarcodeResult(true);
        return;
      }

      if (scanMode === 'ocr') {
        setResultImagePath(imagePath);
        setResultResponse(null);
        setIsProcessing(true);
        setShowResult(true);

        try {
          let response: Record<string, unknown>;
          if (settings?.wildCardScan) {
            response = await runWildCard(imagePath, barcodeData);
          } else if (ocrConnectivity === 'cloud') {
            response = await runCloudPredict(imagePath, barcodeData);
          } else {
            response = await runOnDevicePredict(imagePath, barcodeData);
          }
          if (soundMode === 'Vibrate') vibrate([0, 100]);
          setResultResponse(response);
        } catch (err: unknown) {
          const msg = err instanceof Error ? err.message : String(err);
          if (soundMode === 'Vibrate') vibrate([0, 50, 50, 50]);
          setResultResponse({ error: msg });
          Alert.alert('OCR Error', msg);
        } finally {
          setIsProcessing(false);
        }
        return;
      }

      cameraRef.current?.rescan();
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [scanMode, ocrConnectivity, ocrModuleType, settings?.wildCardScan, settings?.modelSize, soundMode]
  );

  // ---------------------------------------------------------------------------
  // Wild Card / Cloud / On-Device
  // ---------------------------------------------------------------------------
  const runWildCard = useCallback(
    async (imagePath: string, barcodes: BarcodeResultItem[]): Promise<Record<string, unknown>> => {
      const dcModule = {
        type: 'document_classification' as const,
        size: settings?.modelSize ?? ('micro' as const),
      };
      let dcResponse: Record<string, unknown>;
      const dcLoaded = VisionCore.isModelLoaded(dcModule);
      if (dcLoaded) {
        dcResponse = await VisionCore.predictWithModule(dcModule, imagePath, []);
      } else {
        dcResponse = await VisionCore.predictDocumentClassificationCloud(imagePath, null, API_KEY);
      }
      const data: Record<string, unknown> =
        (dcResponse?.data as Record<string, unknown>) ?? dcResponse;
      const docClass = String(data.document_class ?? data.documentClass ?? 'unknown')
        .toLowerCase()
        .replace(/-/g, '_');

      const barcodesArr = barcodes.map((b) => b.scannedCode);
      if (docClass.includes('shipping_label') || docClass.includes('shipping label')) {
        return VisionCore.predictShippingLabelCloud(imagePath, barcodesArr, null, API_KEY);
      } else if (docClass.includes('bill_of_lading') || docClass.includes('bill of lading')) {
        return VisionCore.predictBillOfLadingCloud(imagePath, barcodesArr, null, API_KEY);
      } else if (docClass.includes('item_label') || docClass.includes('item label')) {
        return VisionCore.predictItemLabelCloud(imagePath, null, API_KEY);
      }
      return dcResponse;
    },
    [settings?.modelSize]
  );

  const runCloudPredict = useCallback(
    async (imagePath: string, barcodes: BarcodeResultItem[]): Promise<Record<string, unknown>> => {
      const barcodesArr = barcodes.map((b) => b.scannedCode);
      switch (ocrModuleType) {
        case 'shipping_label':
          return VisionCore.predictShippingLabelCloud(imagePath, barcodesArr, null, API_KEY);
        case 'bill_of_lading':
          return VisionCore.predictBillOfLadingCloud(imagePath, barcodesArr, null, API_KEY);
        case 'item_label':
          return VisionCore.predictItemLabelCloud(imagePath, null, API_KEY);
        case 'document_classification':
          return VisionCore.predictDocumentClassificationCloud(imagePath, null, API_KEY);
      }
    },
    [ocrModuleType]
  );

  const runOnDevicePredict = useCallback(
    async (imagePath: string, barcodes: BarcodeResultItem[]): Promise<Record<string, unknown>> => {
      const module = {
        type: ocrModuleType,
        size: settings?.modelSize ?? ('micro' as const),
      };
      const isLoaded = VisionCore.isModelLoaded(module);
      if (!isLoaded) {
        await VisionCore.loadOCRModel(module, API_KEY);
      }
      return VisionCore.predictWithModule(
        module,
        imagePath,
        barcodes.map((b) => ({ scannedCode: b.scannedCode, symbology: b.symbology }))
      );
    },
    [ocrModuleType, settings?.modelSize]
  );

  // ---------------------------------------------------------------------------
  // Model download
  // ---------------------------------------------------------------------------
  const handleDownloadModel = useCallback(async () => {
    const module = {
      type: ocrModuleType,
      size: settings?.modelSize ?? ('micro' as const),
    };
    setModelDownloading(true);
    setModelDownloadProgress(0);
    try {
      await VisionCore.downloadModel(module, API_KEY, null, (p) => {
        setModelDownloadProgress(p.progress);
      });
      setModelReady(true);
    } catch (e: unknown) {
      Alert.alert('Download Failed', e instanceof Error ? e.message : String(e));
    } finally {
      setModelDownloading(false);
    }
  }, [ocrModuleType, settings?.modelSize]);

  // ---------------------------------------------------------------------------
  // Template management
  // ---------------------------------------------------------------------------
  const handleStartTemplateCreate = useCallback(() => {
    setShowTemplateManager(false);
    setPendingTemplateCodes([]);
    setScanMode('barcode');
    setIsTemplateCreateMode(true);
  }, []);

  const handleSaveTemplate = useCallback(async () => {
    if (pendingTemplateCodes.length === 0) return;
    const newTemplate: TemplateData = {
      id: buildTemplateId(),
      templateCodes: pendingTemplateCodes,
    };
    const updated = await addTemplate(newTemplate);
    setSavedTemplates(updated);
    setPendingTemplateCodes([]);
    setIsTemplateCreateMode(false);
    Alert.alert(
      'Template Saved',
      `Saved ${newTemplate.templateCodes.length} code(s). Apply it from the Templates menu.`
    );
  }, [pendingTemplateCodes]);

  const handleCancelTemplateCreate = useCallback(() => {
    if (pendingTemplateCodes.length > 0) {
      Alert.alert(
        'Discard Template?',
        `You have ${pendingTemplateCodes.length} code(s). Discard?`,
        [
          { text: 'Keep Editing', style: 'cancel' },
          {
            text: 'Discard',
            style: 'destructive',
            onPress: () => {
              setPendingTemplateCodes([]);
              setIsTemplateCreateMode(false);
            },
          },
        ]
      );
    } else {
      setIsTemplateCreateMode(false);
    }
  }, [pendingTemplateCodes.length]);

  const handleApplyTemplate = useCallback((template: TemplateData) => {
    setActiveTemplate(template);
    setShowTemplateManager(false);
  }, []);

  const handleRemoveTemplate = useCallback(() => {
    setActiveTemplate(null);
  }, []);

  const handleDeleteTemplate = useCallback(
    async (id: string) => {
      if (activeTemplate?.id === id) setActiveTemplate(null);
      const updated = await deleteTemplate(id);
      setSavedTemplates(updated);
    },
    [activeTemplate]
  );

  const handleDeleteAllTemplates = useCallback(async () => {
    setActiveTemplate(null);
    await deleteAllTemplates();
    setSavedTemplates([]);
  }, []);

  const handleRemovePendingCode = useCallback((index: number) => {
    setPendingTemplateCodes((prev) => prev.filter((_, i) => i !== index));
  }, []);

  // ---------------------------------------------------------------------------
  // Manual entry
  // ---------------------------------------------------------------------------
  const promptManualBarcode = (label: string) => {
    setManualEntryLabel(label);
    setManualEntryValue('');
    setManualEntryVisible(true);
  };

  const handleManualEntrySubmit = () => {
    if (manualEntryValue.trim()) {
      setPendingBarcodes([
        { scannedCode: manualEntryValue.trim(), symbology: 'MANUAL' },
      ]);
      setShowBarcodeResult(true);
    }
    setManualEntryVisible(false);
  };

  // ---------------------------------------------------------------------------
  // Focus settings wiring
  // ---------------------------------------------------------------------------
  useEffect(() => {
    if (!settings) return;
    cameraRef.current?.setFocusSettings({
      shouldDisplayFocusImage: settings.focusTapEnabled,
      shouldScanInFocusImageRect: false,
      showCodeBoundariesInMultipleScan: settings.multipleScan,
      documentBoundaryBorderColor: theme.colors.accent,
      // Native parses 8-digit hex as ARGB (#AARRGGBB) — alpha must lead.
      documentBoundaryFillColor: `#4D${theme.colors.accent.slice(1)}`,
    });
  }, [settings]);

  // ---------------------------------------------------------------------------
  // Zoom
  // ---------------------------------------------------------------------------
  const handleZoomSelect = useCallback((z: number) => {
    setZoomLevel(z);
    cameraRef.current?.setZoom(z);
  }, []);

  // ---------------------------------------------------------------------------
  // Flash
  // ---------------------------------------------------------------------------
  const toggleFlash = useCallback(() => {
    const next = !flashEnabled;
    setFlashEnabled(next);
    cameraRef.current?.toggleFlash(next);
  }, [flashEnabled]);

  // ---------------------------------------------------------------------------
  // Sound mode cycle
  // ---------------------------------------------------------------------------
  const cycleSoundMode = useCallback(() => {
    setSoundMode((prev) => {
      const next: SoundMode =
        prev === 'Sound' ? 'Vibrate' : prev === 'Vibrate' ? 'Silent' : 'Sound';
      if (settings) {
        const updated = { ...settings, soundMode: next };
        setSettings(updated);
        saveSettings(updated);
      }
      return next;
    });
  }, [settings]);

  // ---------------------------------------------------------------------------
  // OCR connectivity toggle
  // ---------------------------------------------------------------------------
  const handleOcrConnectivityToggle = useCallback(
    (conn: 'cloud' | 'on_device') => {
      setOcrConnectivity(conn);
      if (settings) {
        const updated = {
          ...settings,
          ocrSelection: { moduleType: ocrModuleType, connectivity: conn },
        };
        setSettings(updated);
        saveSettings(updated);
      }
    },
    [settings, ocrModuleType]
  );

  const handleOcrModuleSelect = useCallback(
    (moduleType: OCRModuleType) => {
      setOcrModuleType(moduleType);
      if (settings) {
        const updated = {
          ...settings,
          ocrSelection: { moduleType, connectivity: ocrConnectivity },
        };
        setSettings(updated);
        saveSettings(updated);
      }
    },
    [settings, ocrConnectivity]
  );

  // ---------------------------------------------------------------------------
  // Derived
  // ---------------------------------------------------------------------------
  const showOcrRow = scanMode === 'ocr' && !settings?.wildCardScan;
  const showTemplateControls =
    !isTemplateCreateMode &&
    (scanMode === 'barcode' ||
      scanMode === 'qrcode' ||
      scanMode === 'barcodeorqrcode' ||
      scanMode === 'barcodesinglecapture');
  const showCapture = (scanMode === 'ocr' || !autoCapture) && !isTemplateCreateMode;
  const captureDisabled = sharpness < SHARPNESS_THRESHOLD && !autoCapture;
  const showOnDeviceHint =
    scanMode === 'ocr' &&
    ocrConnectivity === 'on_device' &&
    !modelReady &&
    !settings?.wildCardScan;

  const effectiveScanMode: VisionCameraScanMode = useMemo(() => {
    if (scanMode === 'barcode' && settings?.multipleScan) return 'barcode';
    if (scanMode === 'barcode' && !settings?.multipleScan) return 'barcodesinglecapture';
    return scanMode;
  }, [scanMode, settings?.multipleScan]);

  const modeLabel = useMemo(() => {
    if (scanMode === 'barcodesinglecapture') return 'Barcode';
    return SCAN_MODES.find((m) => m.value === scanMode)?.label ?? scanMode;
  }, [scanMode]);

  const templatePillLabel = activeTemplate
    ? `${activeTemplate.templateCodes.length} codes`
    : 'Template';

  const cbFpsLabel = cbFps !== null ? `${cbFps}` : '--';

  // insets.top = status-bar height on Android, notch/Dynamic-Island height on iOS.
  // We add 8 px of extra breathing room between the strip and the physical edge.
  const topInset = insets.top + 8;
  // bottomOffset: home-indicator safe area on iOS (~34pt), 0 on Android.
  // All UI layers above the bottom bar shift up by this amount.
  const bottomOffset = insets.bottom;

  if (!settings) {
    return (
      <View style={styles.loading}>
        <Text style={styles.loadingText}>Loading…</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.cameraContainer}>
        {/* ── Camera fill ── */}
        {hasPermission ? (
          <VisionCamera
            ref={cameraRef}
            style={StyleSheet.absoluteFill}
            enableFlash={flashEnabled}
            zoomLevel={zoomLevel}
            scanMode={effectiveScanMode}
            autoCapture={autoCapture}
            cameraFacing={cameraFacing}
            showCodeBoundingBoxes
            barcodeBoundingBoxBorderColor={
              isTemplateCreateMode ? theme.colors.success : theme.colors.accent
            }
            barcodeBoundingBoxFillColor={
              // Native parses 8-digit hex as ARGB (#AARRGGBB), not RGBA — alpha must lead.
              isTemplateCreateMode
                ? `#33${theme.colors.success.slice(1)}`
                : `#2A${theme.colors.accent.slice(1)}`
            }
            template={activeTemplate}
            onCapture={handleCapture}
            onError={(err) => {
              if (soundMode === 'Vibrate') vibrate([0, 50, 50, 50]);
              Alert.alert('Camera Error', err.message);
            }}
            onRecognitionUpdate={handleRecognitionUpdate}
            onSharpnessScoreUpdate={handleSharpnessUpdate}
            onBarcodeDetected={handleBarcodeDetected}
            onBoundingBoxesUpdate={handleBoundingBoxesUpdate}
            detectionConfig={{
              text: true,
              barcode: true,
              document: true,
              documentConfidence: 0.8,
              documentCaptureDelay: settings.documentAutoCapture ? 3.0 : 9999,
            }}
            frameSkip={frameSkip}
          />
        ) : (
          <View style={styles.noPerm}>
            <MCIcon name="camera-off" size={48} color={theme.colors.textMuted} />
            <Text style={styles.noPermText}>Camera permission required</Text>
            <TouchableOpacity
              onPress={requestCameraPermission}
              style={styles.noPermBtn}
            >
              <Text style={styles.noPermBtnText}>Grant Access</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* ── TOP STRIP: FPS + mode label + indicators ── */}
        {/* The strip uses an absolute-center layout: pill is centered on screen;
            FPS chip and sound icon are pinned to left/right absolutely so they
            never influence the pill's horizontal position. */}
        <View style={[styles.topStrip, { top: topInset }]} pointerEvents="box-none">
          {/* Absolute center: mode pill */}
          <View style={styles.topStripCenter} pointerEvents="box-none">
            <TouchableOpacity
              style={styles.modePill}
              onPress={() => setShowModePicker(true)}
              activeOpacity={0.75}
            >
              <Text style={styles.modePillText}>{modeLabel}</Text>
              <MCIcon name="chevron-down" size={14} color={theme.colors.accent} style={{ marginLeft: 2 }} />
            </TouchableOpacity>
          </View>

          {/* Left: FPS chip (position absolute so it doesn't shift the pill) */}
          <View style={styles.fpsChip} pointerEvents="none">
            <Text style={styles.fpsText}>{cbFpsLabel}</Text>
            <Text style={styles.fpsSuffix}> fps</Text>
          </View>

          {/* Right: sound icon (position absolute) */}
          <View style={styles.topRight} pointerEvents="box-none">
            <TouchableOpacity
              style={styles.iconPill}
              onPress={cycleSoundMode}
              activeOpacity={0.7}
            >
              <MCIcon
                name={
                  soundMode === 'Sound'
                    ? 'volume-high'
                    : soundMode === 'Vibrate'
                    ? 'vibrate'
                    : 'volume-off'
                }
                size={16}
                color={theme.colors.textSecondary}
              />
            </TouchableOpacity>
          </View>
        </View>

        {/* ── VERTICAL INDICATOR COLUMN (right edge, below top strip) ── */}
        <IndicatorColumn recognition={recognition} topInset={topInset} />

        {/* ── OCR MODULE PILL — large, centered, above Online/On-Device ── */}
        {showOcrRow ? (
          <TouchableOpacity
            style={[styles.ocrModulePillLarge, { bottom: 210 + bottomOffset }]}
            onPress={() => setShowOcrModulePicker(true)}
            activeOpacity={0.8}
          >
            <Text style={styles.ocrModulePillLargeText}>
              {OCR_MODULE_OPTIONS.find((o) => o.type === ocrModuleType)?.label ?? ocrModuleType}
            </Text>
            <MCIcon name="chevron-down" size={14} color={theme.colors.accent} style={{ marginLeft: 4 }} />
          </TouchableOpacity>
        ) : null}

        {/* ── OCR CONNECTIVITY — centered, below module pill ── */}
        {showOcrRow ? (
          <View style={[styles.ocrConnectivityRow, { bottom: 162 + bottomOffset }]} pointerEvents="box-none">
            <View style={styles.ocrToggleGroup}>
              <TouchableOpacity
                style={[styles.ocrToggleBtn, ocrConnectivity === 'cloud' && styles.ocrToggleBtnActive]}
                onPress={() => handleOcrConnectivityToggle('cloud')}
                activeOpacity={0.8}
              >
                <MCIcon
                  name="cloud-outline"
                  size={12}
                  color={ocrConnectivity === 'cloud' ? theme.colors.textOnAccent : theme.colors.textSecondary}
                  style={{ marginRight: 4 }}
                />
                <Text style={[styles.ocrToggleText, ocrConnectivity === 'cloud' && styles.ocrToggleTextActive]}>
                  Online
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.ocrToggleBtn, ocrConnectivity === 'on_device' && styles.ocrToggleBtnActive]}
                onPress={() => handleOcrConnectivityToggle('on_device')}
                activeOpacity={0.8}
              >
                <MCIcon
                  name="cellphone"
                  size={12}
                  color={ocrConnectivity === 'on_device' ? theme.colors.textOnAccent : theme.colors.textSecondary}
                  style={{ marginRight: 4 }}
                />
                <Text style={[styles.ocrToggleText, ocrConnectivity === 'on_device' && styles.ocrToggleTextActive]}>
                  On-Device
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        ) : null}

        {/* ── TEMPLATE PILL — barcode/QR modes (below top strip) ── */}
        {showTemplateControls ? (
          <View style={[styles.templatePillRow, { top: topInset + 44 }]} pointerEvents="box-none">
            <TouchableOpacity
              style={[styles.templateChip, activeTemplate ? styles.templateChipActive : null]}
              onPress={() => setShowTemplateManager(true)}
              activeOpacity={0.8}
            >
              <MCIcon
                name="view-grid-outline"
                size={13}
                color={activeTemplate ? theme.colors.textOnAccent : theme.colors.textSecondary}
                style={{ marginRight: 5 }}
              />
              <Text style={[styles.templateChipText, activeTemplate ? styles.templateChipTextActive : null]}>
                {templatePillLabel}
              </Text>
              {activeTemplate ? (
                <TouchableOpacity
                  onPress={handleRemoveTemplate}
                  hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
                  style={{ marginLeft: 4 }}
                >
                  <MCIcon name="close-circle" size={14} color={theme.colors.textOnAccent} />
                </TouchableOpacity>
              ) : null}
            </TouchableOpacity>
          </View>
        ) : null}

        {/* ── ON-DEVICE HINT ── */}
        {showOnDeviceHint ? (
          <View style={[styles.onDeviceHint, { bottom: 316 + bottomOffset }]}>
            {modelDownloading ? (
              <>
                <Text style={styles.onDeviceHintText}>
                  Downloading {Math.round(modelDownloadProgress * 100)}%
                </Text>
                <View style={styles.onDeviceProgress}>
                  <View
                    style={[
                      styles.onDeviceProgressFill,
                      { width: `${Math.round(modelDownloadProgress * 100)}%` as `${number}%` },
                    ]}
                  />
                </View>
              </>
            ) : (
              <>
                <MCIcon name="cloud-download-outline" size={18} color={theme.colors.textMuted} style={{ marginBottom: 6 }} />
                <Text style={styles.onDeviceHintText}>Model not downloaded</Text>
                <TouchableOpacity onPress={handleDownloadModel} style={styles.downloadBtn}>
                  <Text style={styles.downloadBtnText}>Download</Text>
                </TouchableOpacity>
              </>
            )}
          </View>
        ) : null}

        {/* ── TEMPLATE CREATION PANEL ── */}
        {isTemplateCreateMode ? (
          <View style={[styles.templateCreatePanel, { bottom: 88 + bottomOffset }]}>
            <View style={styles.templateCreateHeader}>
              <View style={styles.templateCreateTitleRow}>
                <MCIcon name="barcode-scan" size={16} color={theme.colors.success} style={{ marginRight: 6 }} />
                <Text style={styles.templateCreateTitle}>
                  Create Template
                </Text>
                <View style={styles.templateCountBadge}>
                  <Text style={styles.templateCountText}>
                    {pendingTemplateCodes.length}
                  </Text>
                </View>
              </View>
              <Text style={styles.templateCreateHint}>
                Point camera at barcodes — codes are added automatically
              </Text>
            </View>
            {pendingTemplateCodes.length > 0 ? (
              <ScrollView
                horizontal
                showsHorizontalScrollIndicator={false}
                style={styles.templateCodeChips}
                contentContainerStyle={{ paddingHorizontal: 2 }}
              >
                {pendingTemplateCodes.map((code, i) => (
                  <TouchableOpacity
                    key={`chip-${i}`}
                    style={styles.templateCodeChip}
                    onPress={() => handleRemovePendingCode(i)}
                  >
                    <Text style={styles.templateCodeChipText} numberOfLines={1}>
                      {code.codeString}
                    </Text>
                    <MCIcon name="close-circle" size={13} color={theme.colors.success} style={{ marginLeft: 4 }} />
                  </TouchableOpacity>
                ))}
              </ScrollView>
            ) : null}
            <View style={styles.templateCreateActions}>
              <TouchableOpacity
                style={styles.templateCancelBtn}
                onPress={handleCancelTemplateCreate}
              >
                <Text style={styles.templateCancelBtnText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[
                  styles.templateSaveBtn,
                  pendingTemplateCodes.length === 0 && styles.templateSaveBtnDisabled,
                ]}
                onPress={handleSaveTemplate}
                disabled={pendingTemplateCodes.length === 0}
              >
                <MCIcon name="content-save" size={14} color="#000" style={{ marginRight: 5 }} />
                <Text style={styles.templateSaveBtnText}>Save</Text>
              </TouchableOpacity>
            </View>
          </View>
        ) : null}

        {/* ── MANUAL/AUTO TOGGLE ── */}
        {scanMode !== 'photo' && !isTemplateCreateMode ? (
          <View style={[
            styles.manualAutoRow,
            showOcrRow && styles.manualAutoRowOcr,
            { bottom: (showOcrRow ? 266 : 162) + bottomOffset },
          ]}>
            <TouchableOpacity
              style={[styles.manualAutoBtn, !autoCapture && styles.manualAutoBtnActive]}
              onPress={() => setAutoCapture(false)}
              activeOpacity={0.8}
            >
              <Text style={[styles.manualAutoBtnText, !autoCapture && styles.manualAutoBtnTextActive]}>
                Manual
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.manualAutoBtn, autoCapture && styles.manualAutoBtnActive]}
              onPress={() => setAutoCapture(true)}
              activeOpacity={0.8}
            >
              <Text style={[styles.manualAutoBtnText, autoCapture && styles.manualAutoBtnTextActive]}>
                Auto
              </Text>
            </TouchableOpacity>
          </View>
        ) : null}

        {/* ── CONTROL ROW ── */}
        {!isTemplateCreateMode ? (
          <View style={[styles.controlRow, { bottom: 90 + bottomOffset }]}>
            {/* Flash */}
            <TouchableOpacity
              style={[styles.ctrlBtn, flashEnabled && styles.ctrlBtnActive]}
              onPress={toggleFlash}
              activeOpacity={0.75}
            >
              <MCIcon
                name={flashEnabled ? 'flash' : 'flash-off'}
                size={19}
                color={flashEnabled ? theme.colors.accent : theme.colors.textSecondary}
              />
            </TouchableOpacity>

            {/* Templates / OCR options */}
            <TouchableOpacity
              style={[
                styles.ctrlBtn,
                (activeTemplate || scanMode === 'ocr') && styles.ctrlBtnActive,
              ]}
              onPress={() => {
                if (scanMode === 'ocr') setShowOcrModulePicker(true);
                else setShowTemplateManager(true);
              }}
              activeOpacity={0.75}
            >
              <MCIcon
                name={scanMode === 'ocr' ? 'tune-variant' : 'layers-outline'}
                size={19}
                color={
                  activeTemplate || scanMode === 'ocr'
                    ? theme.colors.accent
                    : theme.colors.textSecondary
                }
              />
            </TouchableOpacity>

            {/* Zoom controls */}
            {showZoomSlider ? (
              <>
                <Text style={styles.zoomValueLabel}>{zoomLevel.toFixed(1)}×</Text>
                <ZoomSlider
                  value={zoomLevel}
                  min={ZOOM_PRESETS[0]!}
                  max={ZOOM_PRESETS[ZOOM_PRESETS.length - 1]!}
                  onValueChange={handleZoomSelect}
                />
              </>
            ) : (
              <View style={styles.zoomPills}>
                {ZOOM_PRESETS.map((z) => {
                  const selected = Math.abs(zoomLevel - z) < 0.05;
                  return (
                    <TouchableOpacity
                      key={z}
                      style={[styles.zoomPill, selected && styles.zoomPillSelected]}
                      onPress={() => handleZoomSelect(z)}
                      activeOpacity={0.7}
                    >
                      <Text
                        style={[styles.zoomPillText, selected && styles.zoomPillTextSelected]}
                      >
                        {z === 1.0 ? '1×' : `${z}×`}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            )}

            {/* Zoom slider toggle */}
            <TouchableOpacity
              style={[styles.ctrlBtn, showZoomSlider && styles.ctrlBtnActive]}
              onPress={() => setShowZoomSlider((v) => !v)}
              activeOpacity={0.75}
            >
              <MCIcon
                name={showZoomSlider ? 'chevron-down' : 'chevron-up'}
                size={20}
                color={showZoomSlider ? theme.colors.accent : theme.colors.textSecondary}
              />
            </TouchableOpacity>
          </View>
        ) : null}

        {/* ── BOTTOM BAR ── */}
        <View style={[styles.bottomBar, { paddingBottom: 8 + insets.bottom, height: 90 + insets.bottom }]}>
          {/* Camera flip */}
          <PressScale onPress={() => setCameraFacing((f) => (f === 'back' ? 'front' : 'back'))}>
            <View style={styles.bottomIconBtn}>
              <MCIcon name="camera-flip-outline" size={26} color={theme.colors.textPrimary} />
            </View>
          </PressScale>

          {/* Capture button */}
          {showCapture ? (
            <CaptureButton
              sharpness={sharpness}
              onPress={() => cameraRef.current?.capture()}
              disabled={captureDisabled}
            />
          ) : (
            <View style={styles.capturePlaceholder} />
          )}

          {/* Settings gear */}
          <PressScale onPress={() => setShowSettings(true)}>
            <View style={styles.bottomIconBtn}>
              <MCIcon name="tune-vertical-variant" size={26} color={theme.colors.textSecondary} />
            </View>
          </PressScale>
        </View>
      </View>

      {/* ── MODALS ── */}
      {showSettings ? (
        <SettingsModal
          visible={showSettings}
          settings={settings}
          onClose={() => setShowSettings(false)}
          onSave={handleSaveSettings}
          noApiKey={NO_API_KEY}
        />
      ) : null}

      <SheetPicker
        visible={showModePicker}
        title="Scan Mode"
        options={
          Platform.OS === 'ios'
            ? [...SCAN_MODES, { label: 'Dimensioning', value: 'dimensioning' as VisionCameraScanMode }]
            : SCAN_MODES
        }
        current={scanMode === 'barcodesinglecapture' ? 'barcode' : scanMode}
        onSelect={(mode) => {
          if ((mode as string) === 'dimensioning') {
            setShowModePicker(false);
            navigation.navigate('DimensioningScreen');
          } else {
            handleModeSelect(mode);
          }
        }}
        onClose={() => setShowModePicker(false)}
      />

      <SheetPicker
        visible={showOcrModulePicker}
        title="OCR Type"
        options={OCR_MODULE_OPTIONS.map((o) => ({ label: o.label, value: o.type }))}
        current={ocrModuleType}
        onSelect={(type) => handleOcrModuleSelect(type as OCRModuleType)}
        onClose={() => setShowOcrModulePicker(false)}
      />

      {showBarcodeResult ? (
        <BarcodeResultModal
          codes={pendingBarcodes}
          onClose={() => {
            setShowBarcodeResult(false);
            setPendingBarcodes([]);
            cameraRef.current?.rescan();
          }}
        />
      ) : null}

      <ResultScreen
        visible={showResult}
        imagePath={resultImagePath}
        response={resultResponse}
        barcodes={capturedBarcodes.map((b) => ({
          ...b,
          boundingBox: { x: 0, y: 0, width: 0, height: 0 },
        }))}
        moduleType={ocrModuleType}
        isProcessing={isProcessing}
        onClose={() => {
          setShowResult(false);
          setResultResponse(null);
          cameraRef.current?.rescan();
        }}
      />

      <TemplateManagerModal
        visible={showTemplateManager}
        templates={savedTemplates}
        activeTemplateId={activeTemplate?.id ?? null}
        onApply={handleApplyTemplate}
        onRemove={handleRemoveTemplate}
        onDelete={handleDeleteTemplate}
        onDeleteAll={handleDeleteAllTemplates}
        onStartCreate={handleStartTemplateCreate}
        onClose={() => setShowTemplateManager(false)}
      />

      <Modal
        visible={manualEntryVisible}
        animationType="fade"
        transparent
        onRequestClose={() => setManualEntryVisible(false)}
      >
        <View style={styles.manualOverlay}>
          <View style={styles.manualCard}>
            <Text style={styles.manualTitle}>Enter {manualEntryLabel}</Text>
            <TextInput
              style={styles.manualInput}
              value={manualEntryValue}
              onChangeText={setManualEntryValue}
              autoFocus
              placeholder="Type code here…"
              placeholderTextColor={theme.colors.textMuted}
              returnKeyType="done"
              onSubmitEditing={handleManualEntrySubmit}
            />
            <View style={styles.manualActions}>
              <TouchableOpacity
                style={[styles.manualBtn, styles.manualBtnAccent]}
                onPress={handleManualEntrySubmit}
              >
                <Text style={styles.manualBtnTextDark}>Confirm</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.manualBtn, styles.manualBtnGray]}
                onPress={() => setManualEntryVisible(false)}
              >
                <Text style={styles.manualBtnText}>Cancel</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

// ---------------------------------------------------------------------------
// Indicator column — vertical, right-edge anchored
// ---------------------------------------------------------------------------
interface RecognitionState {
  text: boolean;
  barcode: boolean;
  qrcode: boolean;
  document: boolean;
}

const INDICATOR_DEFS: { key: keyof RecognitionState; icon: string; label: string }[] = [
  { key: 'text', icon: 'format-text', label: 'Aa' },
  { key: 'barcode', icon: 'barcode', label: '' },
  { key: 'qrcode', icon: 'qrcode', label: '' },
  { key: 'document', icon: 'file-document-outline', label: '' },
];

function IndicatorColumn({
  recognition,
  topInset,
}: {
  recognition: RecognitionState;
  topInset: number;
}) {
  return (
    <View
      style={[indStyles.column, { top: topInset + 48 }]}
      pointerEvents="none"
    >
      {INDICATOR_DEFS.map(({ key, icon, label }) => {
        const active = recognition[key];
        return (
          <View
            key={key}
            style={[indStyles.dot, active && indStyles.dotActive]}
          >
            {label ? (
              <Text style={[indStyles.dotLabel, active && indStyles.dotLabelActive]}>
                {label}
              </Text>
            ) : (
              <MCIcon
                name={icon}
                size={15}
                color={active ? theme.colors.indicatorOn : theme.colors.textMuted}
              />
            )}
          </View>
        );
      })}
    </View>
  );
}

const indStyles = StyleSheet.create({
  column: {
    position: 'absolute',
    right: 12,
    zIndex: 20,
    flexDirection: 'column',
    alignItems: 'center',
    gap: 8,
  },
  dot: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: theme.colors.btnCircle,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dotActive: {
    backgroundColor: theme.colors.successDim,
    borderColor: theme.colors.indicatorOn,
  },
  dotLabel: {
    color: theme.colors.textMuted,
    fontSize: 10,
    fontWeight: theme.fontWeight.bold,
  },
  dotLabelActive: {
    color: theme.colors.indicatorOn,
  },
});

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const CTRL_BTN_SIZE = 38;

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    backgroundColor: theme.colors.bgDeep,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.md,
  },
  container: {
    flex: 1,
    backgroundColor: theme.colors.bgDeep,
  },
  cameraContainer: {
    flex: 1,
    position: 'relative',
    backgroundColor: theme.colors.bgDeep,
  },
  noPerm: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#0D0D0F',
    gap: 12,
  },
  noPermText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.md,
    marginTop: 4,
  },
  noPermBtn: {
    backgroundColor: theme.colors.accent,
    paddingHorizontal: 28,
    paddingVertical: 13,
    borderRadius: theme.radii.lg,
    marginTop: 8,
  },
  noPermBtnText: {
    color: theme.colors.textOnAccent,
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.sm,
  },

  // ── Top strip ──────────────────────────────────────────────────────────────
  topStrip: {
    position: 'absolute',
    left: 12,
    right: 12,
    zIndex: 20,
    height: 36,
    // Children: topStripCenter (absolute fill for pill), fpsChip (left), topRight (right)
  },
  // Fills the strip and absolutely centers the mode pill, ignoring sibling widths.
  topStripCenter: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1,
  },
  fpsChip: {
    position: 'absolute',
    left: 0,
    top: 0,
    bottom: 0,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.55)',
    borderRadius: theme.radii.sm,
    paddingHorizontal: 7,
    paddingVertical: 4,
    alignSelf: 'center',
    zIndex: 2,
  },
  fpsText: {
    color: '#4ADE80',
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.bold,
    fontVariant: ['tabular-nums'],
  },
  fpsSuffix: {
    color: '#4ADE80',
    fontSize: 9,
    opacity: 0.7,
  },
  modePill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.bgFrosted,
    borderRadius: theme.radii.circle,
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderWidth: 1,
    borderColor: 'rgba(255,214,10,0.35)',
  },
  modePillText: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.wide,
  },
  topRight: {
    position: 'absolute',
    right: 0,
    top: 0,
    bottom: 0,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    zIndex: 2,
  },
  iconPill: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: theme.colors.btnCircle,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // ── OCR module pill — large, centered, above the Online/On-Device row ──
  // bottom applied inline to add bottomOffset
  ocrModulePillLarge: {
    position: 'absolute',
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.bgFrosted,
    borderRadius: theme.radii.circle,
    paddingHorizontal: 20,
    paddingVertical: 11,
    borderWidth: 1,
    borderColor: 'rgba(255,214,10,0.45)',
    zIndex: 20,
  },
  ocrModulePillLargeText: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.md,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.wide,
  },

  // ── OCR connectivity row — centered, below module pill ──
  // bottom applied inline to add bottomOffset
  ocrConnectivityRow: {
    position: 'absolute',
    alignSelf: 'center',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    zIndex: 20,
  },
  ocrToggleGroup: {
    flexDirection: 'row',
    backgroundColor: theme.colors.bgFrosted,
    borderRadius: theme.radii.circle,
    padding: 3,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  ocrToggleBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    paddingHorizontal: 18,
    borderRadius: theme.radii.circle,
  },
  ocrToggleBtnActive: {
    backgroundColor: theme.colors.accent,
  },
  ocrToggleText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.semibold,
  },
  ocrToggleTextActive: {
    color: theme.colors.textOnAccent,
  },

  // ── Template pill row ──────────────────────────────────────────────────────
  templatePillRow: {
    position: 'absolute',
    left: 12,
    right: 12,
    zIndex: 20,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  templateChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.bgFrosted,
    borderRadius: theme.radii.circle,
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  templateChipActive: {
    backgroundColor: theme.colors.accent,
    borderColor: theme.colors.accent,
  },
  templateChipText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.semibold,
  },
  templateChipTextActive: {
    color: theme.colors.textOnAccent,
    fontWeight: theme.fontWeight.bold,
  },

  // ── On-device hint ─────────────────────────────────────────────────────────
  // bottom applied inline to add bottomOffset
  onDeviceHint: {
    position: 'absolute',
    alignSelf: 'center',
    backgroundColor: theme.colors.bgFrostedLight,
    borderRadius: theme.radii.xl,
    padding: 16,
    alignItems: 'center',
    zIndex: 25,
    minWidth: 180,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
  },
  onDeviceHintText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xs,
    marginBottom: 10,
  },
  onDeviceProgress: {
    width: 140,
    height: 3,
    backgroundColor: 'rgba(255,255,255,0.1)',
    borderRadius: 2,
    overflow: 'hidden',
  },
  onDeviceProgressFill: {
    height: '100%',
    backgroundColor: theme.colors.accent,
  },
  downloadBtn: {
    backgroundColor: theme.colors.accent,
    paddingHorizontal: 20,
    paddingVertical: 9,
    borderRadius: theme.radii.circle,
  },
  downloadBtnText: {
    color: theme.colors.textOnAccent,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.wide,
  },

  // ── Template creation panel ────────────────────────────────────────────────
  // bottom applied inline to add bottomOffset
  templateCreatePanel: {
    position: 'absolute',
    left: 0,
    right: 0,
    backgroundColor: theme.colors.bgFrostedLight,
    borderTopLeftRadius: theme.radii.xl,
    borderTopRightRadius: theme.radii.xl,
    padding: theme.spacing.lg,
    zIndex: 30,
    borderTopWidth: 1,
    borderTopColor: theme.colors.dividerStrong,
  },
  templateCreateHeader: { marginBottom: 10 },
  templateCreateTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  templateCreateTitle: {
    color: theme.colors.success,
    fontSize: theme.fontSize.md,
    fontWeight: theme.fontWeight.bold,
  },
  templateCountBadge: {
    marginLeft: 8,
    backgroundColor: theme.colors.success,
    borderRadius: theme.radii.circle,
    width: 22,
    height: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  templateCountText: {
    color: '#000',
    fontSize: 11,
    fontWeight: theme.fontWeight.bold,
  },
  templateCreateHint: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xs,
  },
  templateCodeChips: {
    maxHeight: 42,
    marginBottom: 12,
  },
  templateCodeChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: theme.colors.successDim,
    borderRadius: theme.radii.md,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderWidth: 1,
    borderColor: theme.colors.success,
    marginRight: 6,
    maxWidth: 160,
  },
  templateCodeChipText: {
    color: theme.colors.success,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.semibold,
    flexShrink: 1,
  },
  templateCreateActions: {
    flexDirection: 'row',
    gap: 10,
  },
  templateCancelBtn: {
    flex: 1,
    paddingVertical: 11,
    borderRadius: theme.radii.lg,
    alignItems: 'center',
    backgroundColor: theme.colors.bgCardStrong,
  },
  templateCancelBtnText: {
    color: theme.colors.textPrimary,
    fontWeight: theme.fontWeight.semibold,
    fontSize: theme.fontSize.sm,
  },
  templateSaveBtn: {
    flex: 2,
    paddingVertical: 11,
    borderRadius: theme.radii.lg,
    alignItems: 'center',
    backgroundColor: theme.colors.success,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  templateSaveBtnDisabled: { opacity: 0.3 },
  templateSaveBtnText: {
    color: '#000',
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.sm,
  },

  // ── Manual/Auto toggle ─────────────────────────────────────────────────────
  // bottom is applied inline (adds bottomOffset for iOS home indicator)
  manualAutoRow: {
    position: 'absolute',
    alignSelf: 'center',
    flexDirection: 'row',
    backgroundColor: theme.colors.bgFrosted,
    borderRadius: theme.radii.circle,
    padding: 3,
    zIndex: 20,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  manualAutoRowOcr: {},
  manualAutoBtn: {
    paddingHorizontal: 26,
    paddingVertical: 8,
    borderRadius: theme.radii.circle,
  },
  manualAutoBtnActive: {
    backgroundColor: 'rgba(255,255,255,0.92)',
  },
  manualAutoBtnText: {
    color: 'rgba(255,255,255,0.55)',
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
  },
  manualAutoBtnTextActive: {
    color: '#000',
    fontWeight: theme.fontWeight.bold,
  },

  // ── Control row ────────────────────────────────────────────────────────────
  // bottom applied inline to add bottomOffset
  controlRow: {
    position: 'absolute',
    left: 0,
    right: 0,
    height: 64,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    zIndex: 20,
    backgroundColor: theme.colors.bgFrosted,
    borderTopWidth: 1,
    borderTopColor: theme.colors.divider,
  },
  ctrlBtn: {
    width: CTRL_BTN_SIZE,
    height: CTRL_BTN_SIZE,
    borderRadius: CTRL_BTN_SIZE / 2,
    backgroundColor: theme.colors.bgCard,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  ctrlBtnActive: {
    backgroundColor: theme.colors.btnCircleActive,
    borderColor: theme.colors.accent,
  },
  zoomValueLabel: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.bold,
    fontVariant: ['tabular-nums'],
    minWidth: 32,
    textAlign: 'center',
  },
  zoomPills: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
  },
  zoomPill: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: theme.radii.circle,
    backgroundColor: theme.colors.bgCard,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  zoomPillSelected: {
    backgroundColor: theme.colors.accentDim,
    borderColor: theme.colors.accent,
  },
  zoomPillText: {
    color: 'rgba(255,255,255,0.5)',
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.semibold,
  },
  zoomPillTextSelected: {
    color: theme.colors.accent,
    fontWeight: theme.fontWeight.bold,
  },

  // ── Bottom bar ─────────────────────────────────────────────────────────────
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: 90,
    backgroundColor: theme.colors.bgDeep,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 32,
    paddingBottom: 8,
    zIndex: 20,
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.04)',
  },
  bottomIconBtn: {
    width: 50,
    height: 50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  capturePlaceholder: { width: 74, height: 74 },

  // ── Manual entry ───────────────────────────────────────────────────────────
  manualOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  manualCard: {
    backgroundColor: theme.colors.bgModal,
    borderRadius: theme.radii.xl,
    padding: theme.spacing.xxl,
    width: '100%',
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
  },
  manualTitle: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.lg,
    fontWeight: theme.fontWeight.bold,
    marginBottom: 14,
    letterSpacing: theme.letterSpacing.tight,
  },
  manualInput: {
    backgroundColor: theme.colors.bgCard,
    borderRadius: theme.radii.md,
    padding: 14,
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.md,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
    marginBottom: 16,
  },
  manualActions: { flexDirection: 'row', gap: 10 },
  manualBtn: {
    flex: 1,
    paddingVertical: 13,
    borderRadius: theme.radii.lg,
    alignItems: 'center',
  },
  manualBtnAccent: { backgroundColor: theme.colors.accent },
  manualBtnGray: { backgroundColor: theme.colors.bgCardStrong },
  manualBtnText: {
    color: theme.colors.textPrimary,
    fontWeight: theme.fontWeight.semibold,
    fontSize: theme.fontSize.sm,
  },
  manualBtnTextDark: {
    color: theme.colors.textOnAccent,
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.sm,
  },
});
