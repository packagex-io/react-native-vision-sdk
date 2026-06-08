import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { theme } from '../theme';

interface Segment<T extends string> {
  label: string;
  value: T;
}

interface Props<T extends string> {
  segments: Segment<T>[];
  selected: T;
  onSelect: (value: T) => void;
}

/**
 * Manual | Auto style segmented pill control.
 */
export function SegmentedControl<T extends string>({
  segments,
  selected,
  onSelect,
}: Props<T>) {
  return (
    <View style={styles.container}>
      {segments.map((seg, i) => {
        const active = seg.value === selected;
        return (
          <TouchableOpacity
            key={seg.value}
            style={[
              styles.segment,
              active && styles.segmentActive,
              i === 0 && styles.first,
              i === segments.length - 1 && styles.last,
            ]}
            onPress={() => onSelect(seg.value)}
            activeOpacity={0.75}
          >
            <Text style={[styles.label, active && styles.labelActive]}>
              {seg.label}
            </Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: 'rgba(50,50,50,0.85)',
    borderRadius: 20,
    borderWidth: 1,
    borderColor: 'rgba(80,80,80,0.6)',
    overflow: 'hidden',
  },
  segment: {
    paddingHorizontal: 16,
    paddingVertical: 7,
  },
  segmentActive: {
    backgroundColor: theme.colors.accent,
  },
  first: {
    borderTopLeftRadius: 20,
    borderBottomLeftRadius: 20,
  },
  last: {
    borderTopRightRadius: 20,
    borderBottomRightRadius: 20,
  },
  label: {
    color: theme.colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
  },
  labelActive: {
    color: '#000000',
  },
});
