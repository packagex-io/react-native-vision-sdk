import React from 'react';
import { create, act } from 'react-test-renderer';

// This repo has no @testing-library/react-hooks dependency (it's not
// installed and doesn't support this repo's React 19 pin), so this test
// drives the hook the same way src/__tests__/index.test.tsx drives
// components: mount a harness with react-test-renderer, capture the hook's
// return value on each render, and assert on it inside `act`.

// getCameraCapabilities() is intentionally never resolved here — these tests
// only assert on the synchronous reset-to-`undefined` behavior on view-instance
// change, not on the fetched value, and leaving it pending avoids a stray
// "state update not wrapped in act" warning from a resolution firing after
// the test body returns.
jest.mock('../VisionCoreWrapper', () => ({
  VisionCore: {
    getCameraCapabilities: jest.fn(() => new Promise(() => { })),
  },
}));

import { useCameraControls } from '../camera-controls/useCameraControls';
import type { VisionCameraStateEvent } from '../VisionCameraTypes';

type Controls = ReturnType<typeof useCameraControls>;

function Harness({ onRender }: { onRender: (controls: Controls) => void }) {
  const controls = useCameraControls();
  onRender(controls);
  return null;
}

function renderCameraControls(): { current: Controls } {
  const holder: { current: Controls | null } = { current: null };
  act(() => {
    create(
      <Harness
        onRender={(controls) => {
          holder.current = controls;
        }}
      />
    );
  });
  return holder as { current: Controls };
}

function attach(hook: { current: Controls }, instance: any) {
  act(() => {
    (hook.current.ref as any)(instance);
  });
}

const runningState: VisionCameraStateEvent = {
  status: 'running',
  facing: 'back',
  zoomRatio: 1,
  minZoomRatio: 1,
  maxZoomRatio: 4,
  torchEnabled: false,
  focusMode: 'continuous',
  isPreviewActive: true,
};

describe('useCameraControls', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('starts with state and capabilities undefined', () => {
    const hook = renderCameraControls();
    expect(hook.current.state).toBeUndefined();
    expect(hook.current.capabilities).toBeUndefined();
  });

  it('resets state and capabilities when the underlying view instance changes, then repopulates from the new instance\'s events', () => {
    const hook = renderCameraControls();

    const instanceA = {
      setZoom: jest.fn(),
      setTorch: jest.fn(),
      setFocusPoint: jest.fn(),
    } as any;
    attach(hook, instanceA);
    act(() => {
      hook.current.onCameraStateChanged(runningState);
    });
    expect(hook.current.state?.status).toBe('running');
    expect(hook.current.state?.zoomRatio).toBe(1);

    const instanceB = {
      setZoom: jest.fn(),
      setTorch: jest.fn(),
      setFocusPoint: jest.fn(),
    } as any;
    attach(hook, instanceB);
    expect(hook.current.state).toBeUndefined();
    expect(hook.current.capabilities).toBeUndefined();

    // Repopulation: the new instance's own state event lands and is reported
    // verbatim — never a stale carryover from instanceA (whose zoomRatio was 1).
    act(() => {
      hook.current.onCameraStateChanged({ ...runningState, zoomRatio: 2 });
    });
    expect(hook.current.state).toBeDefined();
    expect(hook.current.state?.zoomRatio).toBe(2);
  });

  it('reports the state event\'s zoomRatio verbatim even when it differs from the requested value (state truthfulness)', () => {
    const hook = renderCameraControls();
    const instance = {
      setZoom: jest.fn(),
      setTorch: jest.fn(),
      setFocusPoint: jest.fn(),
    } as any;
    attach(hook, instance);

    act(() => {
      hook.current.setZoom(5.0);
    });
    expect(instance.setZoom).toHaveBeenCalledWith(5.0);

    // Native clamps the requested 5.0 down to 4.2 and reports that in the
    // next state event — the hook is a pure passthrough of whatever the
    // native layer reports, it never second-guesses or echoes the request.
    act(() => {
      hook.current.onCameraStateChanged({ ...runningState, zoomRatio: 4.2 });
    });

    expect(hook.current.state?.zoomRatio).toBe(4.2);
  });

  it('populates state immediately when a replay event fires synchronously on ref attach (never observably stale-undefined)', () => {
    const renderedStates: Array<VisionCameraStateEvent | undefined> = [];
    const holder: { current: Controls | null } = { current: null };
    act(() => {
      create(
        <Harness
          onRender={(controls) => {
            holder.current = controls;
            renderedStates.push(controls.state);
          }}
        />
      );
    });
    expect(renderedStates).toEqual([undefined]); // initial mount, no camera attached yet

    const instance = {
      setZoom: jest.fn(),
      setTorch: jest.fn(),
      setFocusPoint: jest.fn(),
    } as any;

    // Simulates the real native replay-on-attach behavior (Tasks 13/17): the
    // view emits its current state synchronously as soon as the listener is
    // wired up, in the same tick as ref attach.
    act(() => {
      (holder.current!.ref as any)(instance);
      holder.current!.onCameraStateChanged(runningState);
    });

    // React 18+ batches both synchronous updates from the same act() call
    // into a single commit — a consumer of this hook never observes an
    // intermediate "reset to undefined, then repopulated" render.
    expect(renderedStates).toEqual([undefined, runningState]);
    expect(holder.current!.state).toEqual(runningState);
  });

  it('setZoom/setTorch/setFocusPoint call through to the attached ref instance', () => {
    const hook = renderCameraControls();
    const instance = {
      setZoom: jest.fn(),
      setTorch: jest.fn(),
      setFocusPoint: jest.fn(),
    } as any;
    attach(hook, instance);

    act(() => {
      hook.current.setZoom(2.5);
      hook.current.setTorch(true);
      hook.current.setFocusPoint(0.4, 0.6);
    });

    expect(instance.setZoom).toHaveBeenCalledWith(2.5);
    expect(instance.setTorch).toHaveBeenCalledWith(true);
    expect(instance.setFocusPoint).toHaveBeenCalledWith(0.4, 0.6);
  });
});
