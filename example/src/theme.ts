/**
 * Design tokens — VisionSDK example app v2.
 * Yellow accent on deep neutral dark surfaces.
 */

export const theme = {
  colors: {
    // Surfaces
    bg: '#0A0A0A',
    bgDeep: '#000000',
    bgCard: 'rgba(255,255,255,0.07)',
    bgCardStrong: 'rgba(255,255,255,0.11)',
    bgModal: '#1C1C1E',
    bgSheet: '#1C1C1E',
    bgOverlay: 'rgba(0,0,0,0.72)',
    bgFrosted: 'rgba(20,20,22,0.82)',
    bgFrostedLight: 'rgba(36,36,40,0.88)',
    // Text
    textPrimary: '#F2F2F7',
    textSecondary: '#AEAEB2',
    textMuted: '#636366',
    textOnAccent: '#000000',
    // Accent — yellow
    accent: '#FFD60A',
    accentDim: 'rgba(255,214,10,0.18)',
    accentBright: '#FFE234',
    accentDark: '#CCA800',
    // Semantic
    success: '#30D158',
    successDim: 'rgba(48,209,88,0.15)',
    error: '#FF453A',
    errorDim: 'rgba(255,69,58,0.15)',
    warning: '#FF9F0A',
    info: '#0A84FF',
    // Indicators
    indicatorOff: '#3A3A3C',
    indicatorOn: '#30D158',
    // Buttons
    btnCircle: 'rgba(28,28,30,0.88)',
    btnCircleBorder: 'rgba(255,255,255,0.12)',
    btnCircleActive: 'rgba(255,214,10,0.2)',
    // Capture ring
    captureRing: 'rgba(255,255,255,0.9)',
    captureRingReady: '#FFD60A',
    captureInner: '#FFFFFF',
    // Dividers
    divider: 'rgba(255,255,255,0.08)',
    dividerStrong: 'rgba(255,255,255,0.14)',
    // Mode switcher
    modeChip: 'rgba(255,255,255,0.08)',
    modeChipActive: 'rgba(255,214,10,0.92)',
    // Copy
    copyBlue: '#0A84FF',
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 20,
    xxl: 24,
    xxxl: 32,
    huge: 48,
  },
  radii: {
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 20,
    xxl: 24,
    circle: 999,
  },
  fontSize: {
    xxs: 10,
    xs: 11,
    sm: 13,
    md: 15,
    lg: 17,
    xl: 20,
    xxl: 24,
    xxxl: 28,
  },
  fontWeight: {
    regular: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
    heavy: '800' as const,
  },
  letterSpacing: {
    tight: -0.3,
    normal: 0,
    wide: 0.5,
    wider: 1,
    widest: 1.5,
  },
};

export type Theme = typeof theme;
