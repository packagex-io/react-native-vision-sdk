import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { theme } from '../theme';

interface Props {
  presets: number[];
  current: number;
  onSelect: (zoom: number) => void;
}

/**
 * Horizontal row of zoom preset pills.
 * Selected pill text is yellow; others are gray/white.
 */
export function ZoomPills({ presets, current, onSelect }: Props) {
  return (
    <View style={styles.row}>
      {presets.map((z) => {
        const selected = current === z;
        return (
          <TouchableOpacity
            key={z}
            style={[
              styles.pill,
              selected && styles.pillSelected,
            ]}
            onPress={() => onSelect(z)}
            activeOpacity={0.7}
          >
            <Text style={[styles.label, selected && styles.labelSelected]}>
              {z}x
            </Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.45)',
    borderRadius: 20,
    paddingHorizontal: 4,
    paddingVertical: 2,
    gap: 2,
  },
  pill: {
    paddingHorizontal: 12,
    paddingVertical: 5,
    borderRadius: 16,
  },
  pillSelected: {
    backgroundColor: 'rgba(255,214,10,0.2)',
  },
  label: {
    color: theme.colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
  },
  labelSelected: {
    color: theme.colors.accent,
  },
});
