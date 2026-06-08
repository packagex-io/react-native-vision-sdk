import React from 'react';
import { View, StyleSheet } from 'react-native';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { theme } from '../theme';

interface Props {
  /** One of: 'text' | 'barcode' | 'qrcode' | 'document' */
  type: 'text' | 'barcode' | 'qrcode' | 'document';
  active: boolean;
}

const ICON_MAP: Record<Props['type'], string> = {
  text: 'format-text',
  barcode: 'barcode',
  qrcode: 'qrcode',
  document: 'file-document-outline',
};

/**
 * One icon in the top-right indicator column.
 * Inactive = gray; active = green (#68CE68), matching both native demos.
 * 42×42 translucent circle per the iOS spec.
 */
export function IndicatorDot({ type, active }: Props) {
  const color = active ? theme.colors.indicatorOn : theme.colors.indicatorOff;
  const bgColor = active ? 'rgba(104,206,104,0.18)' : theme.colors.btnCircle;
  const borderColor = active ? theme.colors.indicatorOn : theme.colors.btnCircleBorder;

  return (
    <View
      style={[
        styles.circle,
        { backgroundColor: bgColor, borderColor },
      ]}
    >
      <MCIcon name={ICON_MAP[type]} size={20} color={color} />
    </View>
  );
}

const styles = StyleSheet.create({
  circle: {
    width: 42,
    height: 42,
    borderRadius: 21,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
});
