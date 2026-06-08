import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  type ViewStyle,
  type TextStyle,
} from 'react-native';
import MCIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import { theme } from '../theme';

interface Props {
  onPress: () => void;
  /** Icon name from MaterialCommunityIcons. Takes precedence over `label`. */
  icon?: string;
  /** Fallback text label if no icon provided. */
  label?: string;
  size?: number;
  /** Icon / text rendering size (defaults to size * 0.45) */
  iconSize?: number;
  active?: boolean;
  activeColor?: string;
  style?: ViewStyle;
  labelStyle?: TextStyle;
  disabled?: boolean;
}

/**
 * Translucent gray circular button — matches the iOS demo's 42pt button family.
 * Accepts a MaterialCommunityIcons glyph via `icon`, falling back to text `label`.
 */
export function CircleButton({
  onPress,
  icon,
  label,
  size = 44,
  iconSize,
  active = false,
  activeColor = theme.colors.accent,
  style,
  labelStyle,
  disabled = false,
}: Props) {
  const resolvedIconSize = iconSize ?? Math.round(size * 0.48);
  const color = active ? activeColor : theme.colors.textPrimary;

  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled}
      style={[
        styles.base,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: active
            ? `${activeColor}33`
            : theme.colors.btnCircle,
          borderColor: active ? activeColor : theme.colors.btnCircleBorder,
          opacity: disabled ? 0.35 : 1,
        },
        style,
      ]}
      activeOpacity={0.7}
    >
      {icon ? (
        <MCIcon name={icon} size={resolvedIconSize} color={color} />
      ) : (
        <Text
          style={[styles.label, { color }, labelStyle]}
          numberOfLines={1}
        >
          {label ?? ''}
        </Text>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  base: {
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    textAlign: 'center',
  },
});
