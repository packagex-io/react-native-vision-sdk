/**
 * ResultScreen — full-screen OCR result display, redesigned.
 *
 * Layout:
 *   - Captured image as background at 35% opacity
 *   - Deep dark overlay for legibility
 *   - Animated progress bar (yellow, 7s)
 *   - Scrollable sectioned field cards
 *   - Document Classification: large centered label with accent ring
 *   - Collapsible Raw JSON
 *   - Report button (header right)
 */
import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Animated,
  Alert,
  Image,
  Keyboard,
  LayoutChangeEvent,
  Modal,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { VisionCore } from '../../../src';
import type { VisionCameraBarcodeResult } from '../../../src/VisionCameraTypes';
import {
  detectResponseType,
  extractBillOfLading,
  extractDocumentClass,
  extractItemLabel,
  extractShippingLabel,
  type Field,
  type Section,
} from '../lib/responseExtractor';
import { theme } from '../theme';
import { API_KEY } from '../config';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------
type OCRModuleType = 'shipping_label' | 'bill_of_lading' | 'item_label' | 'document_classification';

interface ReportFields {
  SL: string[];
  BOL: string[];
  IL: string[];
  DC: string[];
}

const REPORT_FIELDS: ReportFields = {
  SL: ['Tracking No.', 'Weight', 'Receiver Name', 'Sender Name', 'Courier Name', 'Dimensions', 'Receiver Address', 'Sender Address'],
  BOL: ['Reference #', 'Load #', 'PO #', 'Invoice #', 'Customer PO #', 'Order #', 'Bill of Lading', 'Master BOL', 'Line BOL', 'House BOL', 'Shipping ID', 'Shipping Date'],
  IL: ['Supplier Name', 'Supplier Address', 'Item Name', 'Item SKU', 'Dimensions', 'Weight', 'Quantity', 'Production Date'],
  DC: ['Document Class'],
};

export interface ResultScreenProps {
  visible: boolean;
  imagePath: string;
  response: Record<string, unknown> | null;
  barcodes: VisionCameraBarcodeResult[];
  moduleType: OCRModuleType;
  isProcessing: boolean;
  onClose: () => void;
}

// ---------------------------------------------------------------------------
// Processing bar
// ---------------------------------------------------------------------------
function ProcessingBar({ active }: { active: boolean }) {
  const width = useRef(new Animated.Value(0)).current;
  const anim = useRef<Animated.CompositeAnimation | null>(null);

  useEffect(() => {
    if (active) {
      width.setValue(0);
      anim.current = Animated.timing(width, {
        toValue: 1,
        duration: 7000,
        useNativeDriver: false,
      });
      anim.current.start();
    } else {
      anim.current?.stop();
      Animated.timing(width, { toValue: 1, duration: 150, useNativeDriver: false }).start();
    }
    return () => { anim.current?.stop(); };
  }, [active, width]);

  return (
    <View style={barStyles.track}>
      <Animated.View
        style={[
          barStyles.fill,
          { width: width.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] }) },
        ]}
      />
      <Animated.View
        style={[
          barStyles.glow,
          { left: width.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] }) },
        ]}
      />
    </View>
  );
}

const barStyles = StyleSheet.create({
  track: {
    height: 3,
    backgroundColor: 'rgba(255,214,10,0.18)',
    overflow: 'hidden',
    position: 'relative',
  },
  fill: {
    height: '100%',
    backgroundColor: theme.colors.accent,
  },
  glow: {
    position: 'absolute',
    top: -3,
    width: 20,
    height: 9,
    backgroundColor: theme.colors.accentBright,
    borderRadius: 4,
    opacity: 0.7,
  },
});

// ---------------------------------------------------------------------------
// Field row
// ---------------------------------------------------------------------------
function FieldRow({ label, value }: Field) {
  if (!value || value === '—') {
    return (
      <View style={fStyles.row}>
        <Text style={fStyles.label}>{label}</Text>
        <Text style={fStyles.valueMuted}>—</Text>
      </View>
    );
  }
  return (
    <View style={fStyles.row}>
      <Text style={fStyles.label}>{label}</Text>
      <Text style={fStyles.value} selectable>{value}</Text>
    </View>
  );
}

const fStyles = StyleSheet.create({
  row: {
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.05)',
  },
  label: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.wider,
    textTransform: 'uppercase',
    marginBottom: 3,
  },
  value: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    lineHeight: 18,
  },
  valueMuted: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.sm,
  },
});

// ---------------------------------------------------------------------------
// Section block
// ---------------------------------------------------------------------------
function SectionBlock({ sec }: { sec: Section }) {
  if (sec.fields.length === 0) return null;
  return (
    <View style={secStyles.block}>
      <Text style={secStyles.title}>{sec.title}</Text>
      {sec.fields.map((f, i) => <FieldRow key={i} {...f} />)}
    </View>
  );
}

const secStyles = StyleSheet.create({
  block: {
    backgroundColor: theme.colors.bgCard,
    borderRadius: theme.radii.lg,
    padding: theme.spacing.lg,
    marginBottom: theme.spacing.md,
    borderWidth: 1,
    borderColor: theme.colors.divider,
  },
  title: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.widest,
    textTransform: 'uppercase',
    marginBottom: theme.spacing.sm,
  },
});

// ---------------------------------------------------------------------------
// Raw JSON collapsible
// ---------------------------------------------------------------------------
function RawJson({ response }: { response: Record<string, unknown> }) {
  const [open, setOpen] = useState(false);
  return (
    <View style={rawStyles.container}>
      <TouchableOpacity
        onPress={() => setOpen(!open)}
        style={rawStyles.toggle}
      >
        <MCIcon
          name={open ? 'chevron-down' : 'chevron-right'}
          size={16}
          color={theme.colors.textSecondary}
          style={{ marginRight: 6 }}
        />
        <Text style={rawStyles.toggleText}>Raw JSON</Text>
      </TouchableOpacity>
      {open ? (
        <ScrollView
          horizontal
          style={rawStyles.jsonScroll}
          showsHorizontalScrollIndicator={false}
        >
          <Text style={rawStyles.json} selectable>
            {JSON.stringify(response, null, 2)}
          </Text>
        </ScrollView>
      ) : null}
    </View>
  );
}

const rawStyles = StyleSheet.create({
  container: {
    backgroundColor: theme.colors.bgCard,
    borderRadius: theme.radii.lg,
    marginBottom: theme.spacing.md,
    borderWidth: 1,
    borderColor: theme.colors.divider,
    overflow: 'hidden',
  },
  toggle: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: theme.spacing.md,
  },
  toggleText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.semibold,
    letterSpacing: theme.letterSpacing.wide,
  },
  jsonScroll: {
    paddingHorizontal: theme.spacing.md,
    paddingBottom: theme.spacing.md,
  },
  json: {
    color: theme.colors.textSecondary,
    fontSize: 10,
    fontFamily: 'monospace',
    lineHeight: 15,
  },
});

// ---------------------------------------------------------------------------
// Report modal
// ---------------------------------------------------------------------------
interface ReportModalProps {
  visible: boolean;
  moduleType: OCRModuleType;
  imagePath: string;
  response: Record<string, unknown> | null;
  barcodes: VisionCameraBarcodeResult[];
  onClose: () => void;
}

function ReportModal({ visible, moduleType, imagePath, response, barcodes, onClose }: ReportModalProps) {
  const fieldList = REPORT_FIELDS[moduleType.toUpperCase() as keyof ReportFields] ?? REPORT_FIELDS.SL;
  const [selectedFields, setSelectedFields] = useState<Set<string>>(new Set());
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const toggleField = (f: string) => {
    setSelectedFields((prev) => {
      const next = new Set(prev);
      next.has(f) ? next.delete(f) : next.add(f);
      return next;
    });
  };

  const handleSubmit = useCallback(async () => {
    if (submitting) return;
    setSubmitting(true);
    try {
      const barcodesArr = barcodes.map((b) => b.scannedCode);
      if (moduleType === 'shipping_label') {
        await VisionCore.logShippingLabelDataToPx(
          imagePath, barcodesArr, response, null, API_KEY,
          null, { error_fields: Array.from(selectedFields), message }, null, null, null, true
        );
      } else if (moduleType === 'bill_of_lading') {
        await VisionCore.logBillOfLadingDataToPx(
          imagePath, barcodesArr, response, null, API_KEY,
          null, { error_fields: Array.from(selectedFields), message }, true
        );
      } else if (moduleType === 'item_label') {
        await VisionCore.logItemLabelDataToPx(
          imagePath, barcodesArr, response, null, API_KEY,
          true, { error_fields: Array.from(selectedFields), message }
        );
      } else {
        await VisionCore.logDocumentClassificationDataToPx(imagePath, response, null, API_KEY, true);
      }
      Alert.alert('Report Submitted', 'Thank you for your feedback.');
      onClose();
    } catch (e: unknown) {
      Alert.alert('Submit Failed', e instanceof Error ? e.message : String(e));
    } finally {
      setSubmitting(false);
    }
  }, [submitting, moduleType, imagePath, response, barcodes, selectedFields, message, onClose]);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <TouchableOpacity
        style={repStyles.overlay}
        activeOpacity={1}
        onPress={() => Keyboard.dismiss()}
      >
        <View style={repStyles.card}>
          {/* Header */}
          <View style={repStyles.header}>
            <Text style={repStyles.title}>Report Issue</Text>
            <TouchableOpacity onPress={onClose} style={repStyles.closeBtn}>
              <MCIcon name="close" size={18} color={theme.colors.textSecondary} />
            </TouchableOpacity>
          </View>
          <Text style={repStyles.subtitle}>Select incorrect fields</Text>

          <View style={repStyles.chips}>
            {fieldList.map((f) => (
              <TouchableOpacity
                key={f}
                style={[repStyles.chip, selectedFields.has(f) && repStyles.chipActive]}
                onPress={() => toggleField(f)}
              >
                <Text style={[repStyles.chipLabel, selectedFields.has(f) && repStyles.chipLabelActive]}>
                  {f}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <TextInput
            style={repStyles.input}
            placeholder="Describe the issue…"
            placeholderTextColor={theme.colors.textMuted}
            value={message}
            onChangeText={setMessage}
            multiline
            maxLength={1000}
          />

          <TouchableOpacity
            style={repStyles.submitBtn}
            onPress={handleSubmit}
          >
            <Text style={repStyles.submitBtnText}>
              {submitting ? 'Submitting…' : 'Submit Report'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity style={repStyles.cancelBtn} onPress={onClose}>
            <Text style={repStyles.cancelBtnText}>Cancel</Text>
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    </Modal>
  );
}

const repStyles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'flex-end',
  },
  card: {
    backgroundColor: theme.colors.bgModal,
    borderTopLeftRadius: theme.radii.xxl,
    borderTopRightRadius: theme.radii.xxl,
    padding: theme.spacing.xxl,
    paddingBottom: 32,
    borderTopWidth: 1,
    borderTopColor: theme.colors.dividerStrong,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  title: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.lg,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.tight,
  },
  closeBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: theme.colors.bgCard,
    alignItems: 'center',
    justifyContent: 'center',
  },
  subtitle: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xs,
    marginBottom: theme.spacing.lg,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 7,
    marginBottom: theme.spacing.lg,
  },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: theme.radii.circle,
    backgroundColor: theme.colors.bgCard,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
  },
  chipActive: {
    backgroundColor: theme.colors.accentDim,
    borderColor: theme.colors.accent,
  },
  chipLabel: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.medium,
  },
  chipLabelActive: {
    color: theme.colors.accent,
    fontWeight: theme.fontWeight.semibold,
  },
  input: {
    backgroundColor: theme.colors.bgCard,
    borderRadius: theme.radii.md,
    padding: 13,
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
    height: 90,
    textAlignVertical: 'top',
    marginBottom: theme.spacing.lg,
  },
  submitBtn: {
    backgroundColor: theme.colors.accent,
    paddingVertical: 13,
    borderRadius: theme.radii.lg,
    alignItems: 'center',
    marginBottom: theme.spacing.sm,
  },
  submitBtnText: {
    color: theme.colors.textOnAccent,
    fontWeight: theme.fontWeight.bold,
    fontSize: theme.fontSize.sm,
  },
  cancelBtn: {
    paddingVertical: 13,
    borderRadius: theme.radii.lg,
    alignItems: 'center',
    backgroundColor: theme.colors.bgCardStrong,
  },
  cancelBtnText: {
    color: theme.colors.textPrimary,
    fontWeight: theme.fontWeight.semibold,
    fontSize: theme.fontSize.sm,
  },
});

// ---------------------------------------------------------------------------
// Main ResultScreen
// ---------------------------------------------------------------------------
export function ResultScreen({
  visible,
  imagePath,
  response,
  barcodes,
  moduleType,
  isProcessing,
  onClose,
}: ResultScreenProps) {
  const [showReport, setShowReport] = useState(false);
  // TEST SCAFFOLD — bbox overlay dimensions
  const [previewSize, setPreviewSize] = useState({ width: 0, height: 0 });
  const handlePreviewLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    setPreviewSize({ width, height });
  };
  // Drive the container aspect from the actual captured-image dims so the image
  // fills it exactly (no letterbox) and the normalized-coord overlay math holds.
  const [imgAspect, setImgAspect] = useState<number | null>(null);
  React.useEffect(() => {
    if (!imagePath) {
      setImgAspect(null);
      return;
    }
    const uri = imagePath.startsWith('file://') ? imagePath : `file://${imagePath}`;
    Image.getSize(
      uri,
      (w, h) => setImgAspect(h > 0 ? w / h : null),
      () => setImgAspect(null)
    );
  }, [imagePath]);

  const sections: Section[] = React.useMemo(() => {
    if (!response) return [];
    const rt = detectResponseType(response);
    if (rt === 'shipping_label') return extractShippingLabel(response);
    if (rt === 'bill_of_lading') return extractBillOfLading(response);
    if (rt === 'item_label') return extractItemLabel(response);
    return [];
  }, [response]);

  const docClass: string | null = React.useMemo(() => {
    if (!response) return null;
    if (detectResponseType(response) === 'document_classification') {
      return extractDocumentClass(response);
    }
    return null;
  }, [response]);

  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose}>
      <SafeAreaView style={styles.container}>
        {/* Background image */}
        {imagePath ? (
          <Image
            source={{ uri: imagePath.startsWith('file://') ? imagePath : `file://${imagePath}` }}
            style={styles.bgImage}
            resizeMode="cover"
          />
        ) : null}
        <View style={styles.bgOverlay} />

        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={onClose} style={styles.backBtn} activeOpacity={0.7}>
            <MCIcon name="chevron-left" size={26} color={theme.colors.accent} />
            <Text style={styles.backBtnText}>Back</Text>
          </TouchableOpacity>
          <Text style={styles.headerTitle} numberOfLines={1}>
            {isProcessing ? 'Processing…' : 'Result'}
          </Text>
          <TouchableOpacity
            onPress={() => setShowReport(true)}
            style={[styles.reportHeaderBtn, (isProcessing || !response) && { opacity: 0.3 }]}
            disabled={isProcessing || !response}
            activeOpacity={0.7}
          >
            <MCIcon name="flag-outline" size={16} color={theme.colors.accent} style={{ marginRight: 4 }} />
            <Text style={styles.reportHeaderBtnText}>Report</Text>
          </TouchableOpacity>
        </View>

        {/* Progress bar */}
        <ProcessingBar active={isProcessing} />

        {isProcessing ? (
          <View style={styles.processingCenter}>
            <View style={styles.processingPulse}>
              <MCIcon name="image-search" size={32} color={theme.colors.accent} />
            </View>
            <Text style={styles.processingText}>Analyzing…</Text>
            <Text style={styles.processingSubtext}>This may take a few seconds</Text>
          </View>
        ) : (
          <ScrollView
            style={styles.scroll}
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
          >
            {/* TEST SCAFFOLD — captured-image bbox overlay.
                Uses resizeMode="contain" so the image fills the container
                exactly, making normalized-coord multiply straightforward.
                Remove or gate behind a flag before shipping. */}
            {imagePath && imgAspect && barcodes.some((b) => b.normalizedBoundingBox) ? (
              <View style={overlayStyles.card}>
                <View
                  style={[overlayStyles.imageContainer, { aspectRatio: imgAspect }]}
                  onLayout={handlePreviewLayout}
                >
                  <Image
                    source={{ uri: imagePath.startsWith('file://') ? imagePath : `file://${imagePath}` }}
                    style={overlayStyles.previewImage}
                    resizeMode="cover"
                  />
                  {previewSize.width > 0 && barcodes.map((b, i) => {
                    const nb = b.normalizedBoundingBox;
                    if (!nb) return null;
                    return (
                      <View
                        key={i}
                        style={{
                          position: 'absolute',
                          left: nb.x * previewSize.width,
                          top: nb.y * previewSize.height,
                          width: nb.width * previewSize.width,
                          height: nb.height * previewSize.height,
                          borderWidth: 2,
                          borderColor: '#39FF14',
                        }}
                      />
                    );
                  })}
                </View>
                <Text style={overlayStyles.label}>
                  normalizedBoundingBox overlay ({barcodes.filter((b) => b.normalizedBoundingBox).length} bbox)
                </Text>
              </View>
            ) : null}

            {/* Document Classification */}
            {docClass ? (
              <View style={styles.dcContainer}>
                <View style={styles.dcRing}>
                  <MCIcon name="file-check-outline" size={28} color={theme.colors.accent} />
                </View>
                <Text style={styles.dcLabel}>{docClass}</Text>
                <Text style={styles.dcSub}>Document Classification</Text>
              </View>
            ) : null}

            {/* Sections */}
            {sections.map((sec, i) => <SectionBlock key={i} sec={sec} />)}

            {/* Raw JSON (always shown as collapsible) */}
            {response ? <RawJson response={response} /> : null}

            {!response && !docClass ? (
              <Text style={styles.emptyText}>No result data.</Text>
            ) : null}
          </ScrollView>
        )}
      </SafeAreaView>

      <ReportModal
        visible={showReport}
        moduleType={moduleType}
        imagePath={imagePath}
        response={response}
        barcodes={barcodes}
        onClose={() => setShowReport(false)}
      />
    </Modal>
  );
}

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  bgImage: {
    ...StyleSheet.absoluteFillObject,
    opacity: 0.3,
  },
  bgOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.72)',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: theme.spacing.lg,
    paddingVertical: theme.spacing.md,
  },
  backBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    minWidth: 70,
    paddingVertical: 6,
  },
  backBtnText: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
    marginLeft: 2,
  },
  headerTitle: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.lg,
    fontWeight: theme.fontWeight.bold,
    flex: 1,
    textAlign: 'center',
    letterSpacing: theme.letterSpacing.tight,
  },
  reportHeaderBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    minWidth: 70,
    justifyContent: 'flex-end',
    paddingVertical: 6,
  },
  reportHeaderBtnText: {
    color: theme.colors.accent,
    fontSize: theme.fontSize.xs,
    fontWeight: theme.fontWeight.semibold,
  },
  processingCenter: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  processingPulse: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: theme.colors.accentDim,
    borderWidth: 1,
    borderColor: 'rgba(255,214,10,0.4)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  processingText: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.lg,
    fontWeight: theme.fontWeight.semibold,
  },
  processingSubtext: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.sm,
  },
  scroll: { flex: 1 },
  scrollContent: {
    paddingHorizontal: theme.spacing.lg,
    paddingVertical: theme.spacing.lg,
    paddingBottom: 48,
  },
  dcContainer: {
    alignItems: 'center',
    paddingVertical: theme.spacing.xxxl,
    marginBottom: theme.spacing.lg,
  },
  dcRing: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: theme.colors.accentDim,
    borderWidth: 1,
    borderColor: 'rgba(255,214,10,0.35)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: theme.spacing.lg,
  },
  dcLabel: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.xxl,
    fontWeight: theme.fontWeight.bold,
    textAlign: 'center',
    letterSpacing: theme.letterSpacing.tight,
  },
  dcSub: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xs,
    letterSpacing: theme.letterSpacing.wider,
    textTransform: 'uppercase',
    marginTop: 6,
  },
  emptyText: {
    color: theme.colors.textMuted,
    textAlign: 'center',
    marginTop: 48,
    fontSize: theme.fontSize.sm,
  },
});

// TEST SCAFFOLD styles — bbox overlay card
const overlayStyles = StyleSheet.create({
  card: {
    marginBottom: 16,
    borderRadius: 8,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(57,255,20,0.35)',
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  imageContainer: {
    width: '100%',
    position: 'relative',
  },
  previewImage: {
    width: '100%',
    height: '100%',
  },
  label: {
    color: '#39FF14',
    fontSize: 11,
    paddingHorizontal: 8,
    paddingVertical: 4,
    opacity: 0.85,
  },
});
