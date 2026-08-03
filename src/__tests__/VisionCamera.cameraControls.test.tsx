import React from 'react';
import { create, act, ReactTestRenderer } from 'react-test-renderer';

// Mock the native component before importing anything that uses it — matches
// the harness style in src/__tests__/index.test.tsx.
jest.mock('../specs/VisionCameraViewNativeComponent', () => {
  const { forwardRef } = require('react');
  const MockNativeComponent = forwardRef((props: any, ref: any) => {
    const { View } = require('react-native');
    return <View ref={ref} {...props} />;
  });
  MockNativeComponent.displayName = 'MockVisionCameraViewNative';
  return {
    __esModule: true,
    default: MockNativeComponent,
    Commands: {
      capture: jest.fn(),
      stop: jest.fn(),
      start: jest.fn(),
      rescan: jest.fn(),
      toggleFlash: jest.fn(),
      setZoom: jest.fn(),
      setFocusSettings: jest.fn(),
      pauseDetection: jest.fn(),
      resumeDetection: jest.fn(),
      setTorchEnabled: jest.fn(),
      setFocusPoint: jest.fn(),
    },
  };
});

import { VisionCamera } from '../VisionCamera';
import { VisionCameraView } from '../VisionCameraViewManager';

function renderInAct(element: React.ReactElement): ReactTestRenderer {
  let tree: ReactTestRenderer;
  act(() => {
    tree = create(element);
  });
  return tree!;
}

describe('VisionCamera camera-controls prop collisions (spec §8)', () => {
  it('warns exactly once when both zoomLevel (deprecated) and zoomRatio are set, and forwards the canonical zoomRatio to the native view', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });

    const tree1 = renderInAct(<VisionCamera zoomLevel={2.0} zoomRatio={3.0} />);
    // A second, independent VisionCamera instance with the same collision —
    // the warning is a module-level "warn once" (not per-instance), so it
    // must still only have fired a single time overall.
    renderInAct(<VisionCamera zoomLevel={2.0} zoomRatio={3.0} />);

    const viewProps = tree1.root.findByType(VisionCameraView as any).props;
    expect(viewProps.zoomRatio).toBe(3.0);
    expect(viewProps.zoomLevel).toBe(3.0);

    expect(warnSpy).toHaveBeenCalledTimes(1);
    expect(warnSpy.mock.calls[0]![0]).toContain('zoomRatio` wins');

    warnSpy.mockRestore();
  });

  it('warns exactly once when both enableFlash (deprecated) and torch are set, and forwards the canonical torch value to the native view', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });

    const tree = renderInAct(<VisionCamera enableFlash={true} torch={false} />);
    // Re-render with the same collision — still only warns once.
    act(() => {
      tree.update(<VisionCamera enableFlash={true} torch={false} />);
    });

    const viewProps = tree.root.findByType(VisionCameraView as any).props;
    expect(viewProps.torch).toBe(false);
    expect(viewProps.enableFlash).toBe(false);

    expect(warnSpy).toHaveBeenCalledTimes(1);
    expect(warnSpy.mock.calls[0]![0]).toContain('torch` wins');

    warnSpy.mockRestore();
  });
});

describe('VisionCamera pinnedLensId fallback (spec §5.3/§8)', () => {
  it('never throws for an unpinnable/unknown lens id — resolution happens natively, the JS layer just renders', () => {
    expect(() => {
      renderInAct(<VisionCamera pinnedLensId="not-a-real-lens" />);
    }).not.toThrow();
  });

  it('surfaces the native lens-unavailable fallback warning through onCameraStateChanged (Android shape: warningMessage omitted)', () => {
    const onCameraStateChanged = jest.fn();
    const tree = renderInAct(
      <VisionCamera pinnedLensId="not-a-real-lens" onCameraStateChanged={onCameraStateChanged} />
    );
    const viewProps = tree.root.findByType(VisionCameraView as any).props;

    act(() => {
      viewProps.onCameraStateChanged({
        nativeEvent: {
          status: 'running',
          facing: 'back',
          zoomRatio: 1,
          minZoomRatio: 1,
          maxZoomRatio: 4,
          torchEnabled: false,
          focusMode: 'continuous',
          isPreviewActive: true,
          warningCode: 'lens-unavailable',
          // Android omits warningMessage entirely rather than sending ''.
        },
      });
    });

    expect(onCameraStateChanged).toHaveBeenCalledTimes(1);
    const event = onCameraStateChanged.mock.calls[0][0];
    expect(event.warningCode).toBe('lens-unavailable');
    expect(event.warningMessage).toBeUndefined();
    expect(event.status).toBe('running'); // never a thrown exception, never status: 'error'
  });

  it('surfaces the native lens-unavailable fallback warning through onCameraStateChanged (iOS shape: absent warningMessage sent as \'\', normalized to undefined)', () => {
    const onCameraStateChanged = jest.fn();
    const tree = renderInAct(
      <VisionCamera pinnedLensId="not-a-real-lens" onCameraStateChanged={onCameraStateChanged} />
    );
    const viewProps = tree.root.findByType(VisionCameraView as any).props;

    act(() => {
      viewProps.onCameraStateChanged({
        nativeEvent: {
          status: 'running',
          facing: 'back',
          zoomRatio: 1,
          minZoomRatio: 1,
          maxZoomRatio: 4,
          torchEnabled: false,
          focusMode: 'continuous',
          isPreviewActive: true,
          warningCode: 'lens-unavailable',
          // iOS's Fabric typed emitter sends absent optional strings as '""'
          // rather than omitting the field — must normalize to undefined
          // identically to the Android (field-omitted) shape above.
          warningMessage: '',
        },
      });
    });

    expect(onCameraStateChanged).toHaveBeenCalledTimes(1);
    const event = onCameraStateChanged.mock.calls[0][0];
    expect(event.warningCode).toBe('lens-unavailable');
    expect(event.warningMessage).toBeUndefined();
  });
});
