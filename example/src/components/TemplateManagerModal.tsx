/**
 * TemplateManagerModal — redesigned dark glass sheet.
 */
import React from 'react';
import {
  Alert,
  FlatList,
  Modal,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { theme } from '../theme';
import type { TemplateData } from '../lib/templates';

interface Props {
  visible: boolean;
  templates: TemplateData[];
  activeTemplateId: string | null;
  onApply: (template: TemplateData) => void;
  onRemove: () => void;
  onDelete: (id: string) => void;
  onDeleteAll: () => void;
  onStartCreate: () => void;
  onClose: () => void;
}

export function TemplateManagerModal({
  visible,
  templates,
  activeTemplateId,
  onApply,
  onRemove,
  onDelete,
  onDeleteAll,
  onStartCreate,
  onClose,
}: Props) {
  const handleDeleteAll = () => {
    if (templates.length === 0) return;
    Alert.alert(
      'Delete All Templates?',
      `Permanently delete ${templates.length} template(s)?`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Delete All', style: 'destructive', onPress: onDeleteAll },
      ]
    );
  };

  const handleDelete = (id: string) => {
    Alert.alert('Delete Template?', 'This cannot be undone.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => onDelete(id) },
    ]);
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.sheet}>
          {/* Grab handle */}
          <View style={styles.grabber} />

          {/* Header */}
          <View style={styles.header}>
            <Text style={styles.title}>Templates</Text>
            <TouchableOpacity
              onPress={onClose}
              style={styles.closeBtn}
              hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
            >
              <MCIcon name="close" size={18} color={theme.colors.textSecondary} />
            </TouchableOpacity>
          </View>

          {/* Create button */}
          <TouchableOpacity style={styles.createBtn} onPress={onStartCreate} activeOpacity={0.8}>
            <MCIcon name="plus" size={16} color={theme.colors.textOnAccent} style={{ marginRight: 7 }} />
            <Text style={styles.createBtnText}>Create New Template</Text>
          </TouchableOpacity>

          {/* List */}
          {templates.length === 0 ? (
            <View style={styles.empty}>
              <MCIcon name="layers-outline" size={36} color={theme.colors.indicatorOff} style={{ marginBottom: 12 }} />
              <Text style={styles.emptyText}>No saved templates</Text>
              <Text style={styles.emptyHint}>
                Tap "Create New Template", then aim at barcodes to build one.
              </Text>
            </View>
          ) : (
            <FlatList
              data={templates}
              keyExtractor={(item) => item.id}
              style={styles.list}
              showsVerticalScrollIndicator={false}
              renderItem={({ item }) => {
                const isActive = item.id === activeTemplateId;
                return (
                  <View style={[styles.row, isActive && styles.rowActive]}>
                    <View style={styles.rowIcon}>
                      <MCIcon
                        name={isActive ? 'check-decagram' : 'layers-outline'}
                        size={16}
                        color={isActive ? theme.colors.accent : theme.colors.textMuted}
                      />
                    </View>
                    <View style={styles.rowInfo}>
                      <Text style={[styles.rowId, isActive && styles.rowIdActive]} numberOfLines={1}>
                        {item.id}
                      </Text>
                      <Text style={styles.rowCodes} numberOfLines={2}>
                        {item.templateCodes.length} code{item.templateCodes.length !== 1 ? 's' : ''}: {item.templateCodes.map((c) => c.codeString).join(' · ')}
                      </Text>
                    </View>
                    <View style={styles.rowActions}>
                      <TouchableOpacity
                        style={[styles.applyBtn, isActive && styles.applyBtnActive]}
                        onPress={() => { if (isActive) onRemove(); else onApply(item); }}
                        activeOpacity={0.75}
                      >
                        <Text style={[styles.applyBtnText, isActive && styles.applyBtnTextActive]}>
                          {isActive ? 'Remove' : 'Apply'}
                        </Text>
                      </TouchableOpacity>
                      <TouchableOpacity
                        style={styles.deleteBtn}
                        onPress={() => handleDelete(item.id)}
                        hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
                      >
                        <MCIcon name="trash-can-outline" size={15} color={theme.colors.error} />
                      </TouchableOpacity>
                    </View>
                  </View>
                );
              }}
            />
          )}

          {/* Delete all */}
          {templates.length > 0 ? (
            <TouchableOpacity style={styles.deleteAllBtn} onPress={handleDeleteAll} activeOpacity={0.75}>
              <MCIcon name="trash-can-outline" size={14} color={theme.colors.error} style={{ marginRight: 7 }} />
              <Text style={styles.deleteAllBtnText}>Delete All</Text>
            </TouchableOpacity>
          ) : null}
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.65)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: theme.colors.bgModal,
    borderTopLeftRadius: theme.radii.xxl,
    borderTopRightRadius: theme.radii.xxl,
    paddingHorizontal: theme.spacing.xxl,
    paddingBottom: 28,
    maxHeight: '75%',
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
    marginBottom: 2,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: theme.spacing.md,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
    marginBottom: theme.spacing.lg,
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
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: theme.colors.bgCard,
    borderWidth: 1,
    borderColor: theme.colors.btnCircleBorder,
  },
  createBtn: {
    backgroundColor: theme.colors.accent,
    borderRadius: theme.radii.lg,
    paddingVertical: 13,
    alignItems: 'center',
    marginBottom: theme.spacing.lg,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  createBtnText: {
    color: theme.colors.textOnAccent,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.bold,
  },
  empty: {
    paddingVertical: theme.spacing.xxxl,
    alignItems: 'center',
  },
  emptyText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.md,
    fontWeight: theme.fontWeight.semibold,
    marginBottom: 6,
  },
  emptyHint: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xs,
    textAlign: 'center',
    paddingHorizontal: theme.spacing.lg,
    lineHeight: 18,
  },
  list: {
    marginBottom: theme.spacing.md,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 13,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.divider,
  },
  rowActive: {
    backgroundColor: theme.colors.accentDim,
    borderRadius: theme.radii.md,
    paddingHorizontal: 8,
    borderBottomColor: 'transparent',
    marginBottom: 2,
  },
  rowIcon: {
    width: 28,
    alignItems: 'center',
    marginRight: 10,
  },
  rowInfo: {
    flex: 1,
    marginRight: 8,
  },
  rowId: {
    color: theme.colors.textPrimary,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
  },
  rowIdActive: {
    color: theme.colors.accent,
  },
  rowCodes: {
    color: theme.colors.textMuted,
    fontSize: theme.fontSize.xxs,
    marginTop: 2,
    lineHeight: 14,
  },
  rowActions: {
    flexDirection: 'row',
    gap: 7,
    alignItems: 'center',
  },
  applyBtn: {
    backgroundColor: theme.colors.bgCardStrong,
    borderRadius: theme.radii.sm,
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderWidth: 1,
    borderColor: theme.colors.dividerStrong,
  },
  applyBtnActive: {
    backgroundColor: theme.colors.accent,
    borderColor: theme.colors.accent,
  },
  applyBtnText: {
    color: theme.colors.textSecondary,
    fontSize: theme.fontSize.xxs,
    fontWeight: theme.fontWeight.bold,
  },
  applyBtnTextActive: {
    color: theme.colors.textOnAccent,
  },
  deleteBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: theme.colors.errorDim,
    alignItems: 'center',
    justifyContent: 'center',
  },
  deleteAllBtn: {
    borderWidth: 1,
    borderColor: theme.colors.errorDim,
    borderRadius: theme.radii.lg,
    paddingVertical: 11,
    alignItems: 'center',
    marginTop: 4,
    flexDirection: 'row',
    justifyContent: 'center',
    backgroundColor: theme.colors.errorDim,
  },
  deleteAllBtnText: {
    color: theme.colors.error,
    fontSize: theme.fontSize.sm,
    fontWeight: theme.fontWeight.semibold,
  },
});
