/**
 * SettingsModal — redesigned dark glass sheet.
 *
 * Sections:
 *   1. Toggles: Multiple Scan, Wild Card Scan, Document Auto Capture, Tap-to-Focus, Pinch-Zoom
 *   2. Frame Skip numeric input
 *   3. Model Size selector (Nano / Micro / Large)
 *   4. Models section (7 rows) — download/load/delete/cancel + status + progress
 *   5. Enabled Symbologies
 */
import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Modal,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { VisionCore } from '../../../src';
import type { OCRModule, ModuleSize } from '../../../src/types';
import type { AppSettings } from '../lib/settings';
import { theme } from '../theme';
import { API_KEY } from '../config';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------
type ModelStatus = 'not_downloaded' | 'downloading' | 'downloaded' | 'loaded' | 'error';

interface ModelRowState {
  module: OCRModule;
  status: ModelStatus;
  downloadProgress: number;
  version?: string;
}

const MODEL_ROWS: OCRModule[] = [
  { type: 'shipping_label', size: 'micro' },
  { type: 'shipping_label', size: 'large' },
  { type: 'item_label', size: 'micro' },
  { type: 'item_label', size: 'large' },
  { type: 'document_classification', size: 'micro' },
  { type: 'document_classification', size: 'large' },
  { type: 'bill_of_lading', size: 'large' },
];

const SYMBOLOGIES = [
  'Aztec', 'Codabar', 'Code 128', 'Code 39', 'Code 39 Checksum',
  'Code 39 Full ASCII', 'Code 93', 'Code 93i', 'Data Matrix', 'EAN-8',
  'EAN-13 / UPC-A', 'ITF 2of5', 'ITF14', 'PDF 417', 'QR Code',
  'UPC-E', 'GS1', 'DataBar 14', 'GS1 DataBar Exp.', 'GS1 DataBar Ltd.',
  'Micro QR', 'MicroPDF417',
];

function moduleKey(m: OCRModule): string {
  return `${m.type}:${m.size}`;
}

function moduleName(m: OCRModule): string {
  const typeMap: Record<string, string> = {
    shipping_label: 'SL',
    item_label: 'IL',
    document_classification: 'DC',
    bill_of_lading: 'BOL',
  };
  return `${typeMap[m.type] ?? m.type} · ${m.size.charAt(0).toUpperCase() + m.size.slice(1)}`;
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------
interface Props {
  visible: boolean;
  settings: AppSettings;
  onClose: () => void;
  onSave: (updated: AppSettings) => void;
  noApiKey?: boolean;
}

export function SettingsModal({ visible, settings, onClose, onSave, noApiKey }: Props) {
  const [local, setLocal] = useState<AppSettings>(settings);
  const [frameSkipText, setFrameSkipText] = useState(String(settings.frameSkip));
  const [modelRows, setModelRows] = useState<ModelRowState[]>(
    MODEL_ROWS.map((m) => ({ module: m, status: 'not_downloaded', downloadProgress: 0 }))
  );
  const [refreshing, setRefreshing] = useState(false);
  const [downloadingKey, setDownloadingKey] = useState<string | null>(null);

  const update = useCallback(<K extends keyof AppSettings>(key: K, value: AppSettings[K]) => {
    setLocal((prev) => ({ ...prev, [key]: value }));
  }, []);

  const refreshModels = useCallback(async () => {
    setRefreshing(true);
    try {
      const downloaded = await VisionCore.findDownloadedModels();
      setModelRows((prev) =>
        prev.map((row) => {
          const info = downloaded.find(
            (d) => d.module.type === row.module.type && d.module.size === row.module.size
          );
          if (!info) {
            return { ...row, status: 'not_downloaded', downloadProgress: 0, version: undefined };
          }
          const loaded = VisionCore.isModelLoaded(row.module);
          return {
            ...row,
            status: loaded ? 'loaded' : 'downloaded',
            version: info.version,
            downloadProgress: 0,
          };
        })
      );
    } catch {
      // Non-fatal
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    if (visible) {
      setLocal(settings);
      setFrameSkipText(String(settings.frameSkip));
      refreshModels();
    }
  }, [visible, settings, refreshModels]);

  const handleClose = useCallback(() => {
    const parsed = parseInt(frameSkipText, 10);
    const fs = Number.isNaN(parsed) || parsed < 1 ? 10 : parsed;
    onSave({ ...local, frameSkip: fs });
    onClose();
  }, [local, frameSkipText, onSave, onClose]);

  // ---------------------------------------------------------------------------
  // Per-model actions
  // ---------------------------------------------------------------------------
  const handleDownload = useCallback(async (module: OCRModule) => {
    const key = moduleKey(module);
    setDownloadingKey(key);
    setModelRows((prev) =>
      prev.map((r) =>
        moduleKey(r.module) === key
          ? { ...r, status: 'downloading', downloadProgress: 0 }
          : r
      )
    );
    try {
      await VisionCore.downloadModel(module, API_KEY, null, (p) => {
        setModelRows((prev) =>
          prev.map((r) =>
            moduleKey(r.module) === key ? { ...r, downloadProgress: p.progress } : r
          )
        );
      });
      setModelRows((prev) =>
        prev.map((r) =>
          moduleKey(r.module) === key
            ? { ...r, status: 'downloaded', downloadProgress: 1 }
            : r
        )
      );
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      setModelRows((prev) =>
        prev.map((r) =>
          moduleKey(r.module) === key ? { ...r, status: 'error', downloadProgress: 0 } : r
        )
      );
      Alert.alert('Download Failed', msg);
    } finally {
      setDownloadingKey(null);
    }
  }, []);

  const handleCancelDownload = useCallback(async (module: OCRModule) => {
    try {
      await VisionCore.cancelDownload(module);
      setModelRows((prev) =>
        prev.map((r) =>
          moduleKey(r.module) === moduleKey(module)
            ? { ...r, status: 'not_downloaded', downloadProgress: 0 }
            : r
        )
      );
      setDownloadingKey(null);
    } catch {
      // ignore
    }
  }, []);

  const handleLoad = useCallback(async (module: OCRModule) => {
    try {
      await VisionCore.loadOCRModel(module, API_KEY);
      setModelRows((prev) =>
        prev.map((r) =>
          moduleKey(r.module) === moduleKey(module) ? { ...r, status: 'loaded' } : r
        )
      );
    } catch (e: unknown) {
      Alert.alert('Load Failed', e instanceof Error ? e.message : String(e));
    }
  }, []);

  const handleUnload = useCallback((module: OCRModule) => {
    VisionCore.unloadModel(module);
    setModelRows((prev) =>
      prev.map((r) =>
        moduleKey(r.module) === moduleKey(module) ? { ...r, status: 'downloaded' } : r
      )
    );
  }, []);

  const handleDelete = useCallback(async (module: OCRModule) => {
    Alert.alert('Delete Model', `Delete ${moduleName(module)}?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          try {
            await VisionCore.deleteModel(module);
            setModelRows((prev) =>
              prev.map((r) =>
                moduleKey(r.module) === moduleKey(module)
                  ? { ...r, status: 'not_downloaded', downloadProgress: 0, version: undefined }
                  : r
              )
            );
          } catch (e: unknown) {
            Alert.alert('Delete Failed', e instanceof Error ? e.message : String(e));
          }
        },
      },
    ]);
  }, []);

  // ---------------------------------------------------------------------------
  // Render helpers
  // ---------------------------------------------------------------------------
  const renderToggle = (
    label: string,
    value: boolean,
    onChange: (v: boolean) => void,
    note?: string
  ) => (
    <View style={styles.settingRow} key={label}>
      <View style={styles.settingLabelCol}>
        <Text style={styles.settingLabel}>{label}</Text>
        {note ? <Text style={styles.settingNote}>{note}</Text> : null}
      </View>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: theme.colors.indicatorOff, true: theme.colors.accent }}
        thumbColor={value ? theme.colors.textOnAccent : '#5A5A5E'}
      />
    </View>
  );

  const renderModelRow = (row: ModelRowState) => {
    const key = moduleKey(row.module);
    const isThisDownloading = downloadingKey === key;

    const statusLabel = (): string => {
      switch (row.status) {
        case 'not_downloaded': return 'Not downloaded';
        case 'downloading': return `${Math.round(row.downloadProgress * 100)}%`;
        case 'downloaded': return row.version ? `v${row.version}` : 'Ready';
        case 'loaded': return row.version ? `Loaded · v${row.version}` : 'Loaded';
        case 'error': return 'Error';
      }
    };

    const statusColor = (): string => {
      switch (row.status) {
        case 'not_downloaded': return theme.colors.textMuted;
        case 'downloading': return theme.colors.warning;
        case 'downloaded': return theme.colors.info;
        case 'loaded': return theme.colors.success;
        case 'error': return theme.colors.error;
      }
    };

    const statusDotStyle = { backgroundColor: statusColor() };

    return (
      <View key={key} style={styles.modelRow}>
        <View style={styles.modelInfo}>
          <View style={styles.modelNameRow}>
            <View style={[styles.statusDot, statusDotStyle]} />
            <Text style={styles.modelName}>{moduleName(row.module)}</Text>
          </View>
          <Text style={[styles.modelStatus, { color: statusColor() }]}>{statusLabel()}</Text>
          {row.status === 'downloading' ? (
            <View style={styles.progressBar}>
              <View
                style={[
                  styles.progressFill,
                  { width: `${Math.round(row.downloadProgress * 100)}%` as `${number}%` },
                ]}
              />
            </View>
          ) : null}
        </View>
        <View style={styles.modelActions}>
          {row.status === 'not_downloaded' && (
            <TouchableOpacity
              style={[styles.modelBtn, styles.modelBtnAccent]}
              onPress={() => handleDownload(row.module)}
            >
              <MCIcon name="download" size={13} color={theme.colors.textOnAccent} style={{ marginRight: 4 }} />
              <Text style={styles.modelBtnTextDark}>Get</Text>
            </TouchableOpacity>
          )}
          {row.status === 'downloading' && (
            <TouchableOpacity
              style={[styles.modelBtn, styles.modelBtnRed]}
              onPress={() => handleCancelDownload(row.module)}
            >
              <Text style={styles.modelBtnText}>Stop</Text>
            </TouchableOpacity>
          )}
          {row.status === 'downloaded' && (
            <>
              <TouchableOpacity
                style={[styles.modelBtn, styles.modelBtnGreen]}
                onPress={() => handleLoad(row.module)}
              >
                <Text style={styles.modelBtnText}>Load</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modelIconBtn}
                onPress={() => handleDelete(row.module)}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
              >
                <MCIcon name="trash-can-outline" size={15} color={theme.colors.error} />
              </TouchableOpacity>
            </>
          )}
          {row.status === 'loaded' && (
            <>
              <TouchableOpacity
                style={[styles.modelBtn, styles.modelBtnGray]}
                onPress={() => handleUnload(row.module)}
              >
                <Text style={styles.modelBtnText}>Unload</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modelIconBtn}
                onPress={() => handleDelete(row.module)}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
              >
                <MCIcon name="trash-can-outline" size={15} color={theme.colors.error} />
              </TouchableOpacity>
            </>
          )}
          {row.status === 'error' && (
            <TouchableOpacity
              style={[styles.modelBtn, styles.modelBtnAccent]}
              onPress={() => handleDownload(row.module)}
            >
              <Text style={styles.modelBtnTextDark}>Retry</Text>
            </TouchableOpacity>
          )}
          {isThisDownloading && (
            <ActivityIndicator
              size="small"
              color={theme.colors.accent}
              style={{ marginLeft: 4 }}
            />
          )}
        </View>
      </View>
    );
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      onRequestClose={handleClose}
    >
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          {/* Grab handle */}
          <View style={styles.grabber} />

          {/* Header */}
          <View style={styles.header}>
            <Text style={styles.title}>Settings</Text>
            <TouchableOpacity
              onPress={handleClose}
              style={styles.closeBtn}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            >
              <MCIcon name="close" size={20} color={theme.colors.textSecondary} />
            </TouchableOpacity>
          </View>

          <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false}>

            {/* API key warning */}
            {noApiKey ? (
              <View style={styles.apiBanner}>
                <MCIcon name="alert-circle" size={16} color="#fff" style={{ marginRight: 8 }} />
                <Text style={styles.apiBannerText}>
                  No API key. Add LOCAL_API_KEY in example/src/config.local.ts.
                </Text>
              </View>
            ) : null}

            {/* Scanning toggles */}
            <Text style={styles.sectionHeader}>Scanning</Text>
            <View style={styles.card}>
              {renderToggle('Multiple Scan', local.multipleScan, (v) => update('multipleScan', v),
                'Uses barcodesinglecapture mode on Android')}
              {renderToggle('Wild Card Scan', local.wildCardScan, (v) => update('wildCardScan', v),
                'Auto-detect document type then route to model')}
              {renderToggle('Document Auto Capture', local.documentAutoCapture, (v) => update('documentAutoCapture', v))}
              {renderToggle('Tap to Focus', local.focusTapEnabled, (v) => update('focusTapEnabled', v))}
              {renderToggle('Pinch / Pan to Zoom', local.pinchZoomEnabled, (v) => update('pinchZoomEnabled', v))}
            </View>

            {/* Frame Skip */}
            <Text style={styles.sectionHeader}>Frame Skip</Text>
            <View style={[styles.card, styles.settingRow]}>
              <View style={styles.settingLabelCol}>
                <Text style={styles.settingLabel}>Process every Nth frame</Text>
                <Text style={styles.settingNote}>Lower = more CPU; Higher = lower latency</Text>
              </View>
              <TextInput
                style={styles.numberInput}
                value={frameSkipText}
                onChangeText={setFrameSkipText}
                keyboardType="number-pad"
                maxLength={3}
                selectTextOnFocus
              />
            </View>

            {/* Model Size */}
            <Text style={styles.sectionHeader}>Default Model Size</Text>
            <View style={[styles.card, styles.segmentRow]}>
              {(['nano', 'micro', 'large'] as ModuleSize[]).map((sz) => (
                <TouchableOpacity
                  key={sz}
                  style={[styles.segBtn, local.modelSize === sz && styles.segBtnActive]}
                  onPress={() => update('modelSize', sz)}
                >
                  <Text style={[styles.segBtnLabel, local.modelSize === sz && styles.segBtnLabelActive]}>
                    {sz.charAt(0).toUpperCase() + sz.slice(1)}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            {/* Models */}
            <View style={styles.modelsSectionHeader}>
              <Text style={styles.sectionHeader}>On-Device Models</Text>
              <TouchableOpacity
                onPress={refreshModels}
                style={styles.refreshBtn}
                hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
              >
                {refreshing ? (
                  <ActivityIndicator size="small" color={theme.colors.accent} />
                ) : (
                  <MCIcon name="refresh" size={18} color={theme.colors.accent} />
                )}
              </TouchableOpacity>
            </View>
            <View style={styles.card}>
              {modelRows.map(renderModelRow)}
            </View>

            {/* Enabled Symbologies */}
            <Text style={styles.sectionHeader}>Enabled Symbologies</Text>
            <View style={[styles.card, styles.symbologiesGrid]}>
              {SYMBOLOGIES.map((s) => (
                <View key={s} style={styles.symbologyChip}>
                  <Text style={styles.symbologyText}>{s}</Text>
                </View>
              ))}
            </View>

            <View style={styles.bottomPad} />
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------
const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: theme.colors.bgSheet,
    borderTopLeftRadius: theme.radii.xxl,
    borderTopRightRadius: theme.radii.xxl,
    maxHeight: '92%',
    borderTopWidth: 1,
    borderTopColor: theme.colors.dividerStrong,
  },
  grabber: {
    width: 36,
    height: 4,
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderRadius: 2,
    alignSelf: 'center',
    marginTop: 10,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: theme.spacing.xxl,
    paddingVertical: theme.spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
  },
  title: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.xl,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.tight,
  },
  closeBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.colors.bgCard,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  scroll: {
    paddingHorizontal: theme.spacing.xxl,
  },
  card: {
    backgroundColor: theme.colors.bgCard,
    borderRadius: theme.radii.lg,
    marginBottom: 4,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: theme.colors.divider,
  },
  sectionHeader: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
    letterSpacing: theme.letterSpacing.widest,
    textTransform: 'uppercase',
    marginTop: 20,
    marginBottom: 8,
  },
  settingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 11,
    paddingHorizontal: theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
  },
  settingLabelCol: {
    flex: 1,
    marginRight: 12,
  },
  settingLabel: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.medium,
  },
  settingNote: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xxs,
    marginTop: 2,
    lineHeight: 14,
  },
  numberInput: {
    backgroundColor: theme.colors.bgCardStrong,
    color: theme.colors.textPrimary,
    borderRadius: theme.radii.sm,
    paddingHorizontal: 12,
    paddingVertical: 7,
    width: 72,
    textAlign: 'center',
    fontSize: theme.fontSize.sm,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
    fontWeight: theme.fontWeight.semibold,
  },
  segmentRow: {
    flexDirection: 'row',
    gap: 1,
    padding: 1,
  },
  segBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: theme.radii.md,
    alignItems: 'center',
    backgroundColor: 'transparent',
  },
  segBtnActive: {
    backgroundColor: theme.colors.accent,
    margin: 2,
    borderRadius: theme.radii.md,
  },
  segBtnLabel: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
  },
  segBtnLabelActive: {
    color: theme.colors.textOnAccent,
    fontWeight: theme.fontWeight.bold,
  },
  modelsSectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  refreshBtn: {
    paddingVertical: 4,
    paddingHorizontal: 8,
    marginTop: 12,
  },
  modelRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 11,
    paddingHorizontal: theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
  },
  modelInfo: {
    flex: 1,
    marginRight: 8,
  },
  modelNameRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 7,
  },
  modelName: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
  },
  modelStatus: {
    fontSize: theme.fontSize.xxs,
    marginTop: 2,
    marginLeft: 13,
  },
  progressBar: {
    height: 2,
    backgroundColor: theme.colors.bgCardStrong,
    borderRadius: 1,
    marginTop: 5,
    marginLeft: 13,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: theme.colors.accent,
  },
  modelActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  modelBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: theme.radii.sm,
    flexDirection: 'row',
    alignItems: 'center',
  },
  modelIconBtn: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.colors.errorDim,
  },
  modelBtnAccent: { backgroundColor: theme.colors.accent },
  modelBtnGreen: { backgroundColor: 'rgba(48,209,88,0.85)' },
  modelBtnRed: { backgroundColor: theme.colors.error },
  modelBtnGray: {
    backgroundColor: theme.colors.bgCardStrong,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
  },
  modelBtnText: {
    color: '#fff',
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
  },
  modelBtnTextDark: {
    color: theme.colors.textOnAccent,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
  },
  symbologiesGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    padding: theme.spacing.md,
  },
  symbologyChip: {
    backgroundColor: theme.colors.bgCardStrong,
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: theme.radii.sm,
    borderWidth: 1,
    borderColor: theme.colors.divider,
  },
  symbologyText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.medium,
  },
  bottomPad: {
    height: 40,
  },
  apiBanner: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: theme.colors.errorDim,
    borderWidth: 1,
    borderColor: theme.colors.error,
    borderRadius: theme.radii.md,
    padding: 12,
    marginTop: 12,
    marginBottom: 4,
  },
  apiBannerText: {
    flex: 1,
    color: theme.colors.error,
    fontSize: theme.fontSize.xs,
    lineHeight: 16,
  },
});
