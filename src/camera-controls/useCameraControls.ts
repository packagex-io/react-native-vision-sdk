import { useCallback, useRef, useState } from 'react';
import type { RefCallback, RefObject } from 'react';
import { VisionCore } from '../VisionCoreWrapper';
import type { VisionCameraRefProps, VisionCameraStateEvent } from '../VisionCameraTypes';
import type { CameraCapabilities } from '../types';

/**
 * Primary documented entry point for the Camera Controls API (spec §8).
 * Owns the wiring between `<VisionCamera>` and application state:
 *
 * ```tsx
 * const camera = useCameraControls();
 * <VisionCamera ref={camera.ref} onCameraStateChanged={camera.onCameraStateChanged} />
 * ```
 *
 * Resets `state`/`capabilities` to `undefined` whenever the underlying view
 * instance changes (e.g. a keyed remount), so a fresh view never briefly
 * renders its predecessor's state — the replay event (native side, on
 * listener attach) repopulates `state` immediately after.
 */
export function useCameraControls(): {
  ref: RefCallback<VisionCameraRefProps>;
  /**
   * Review fix (API shape) — `ref` above is a callback ref: correct to hand to
   * `<VisionCamera ref={camera.ref} />`, but consumers can't read `.current` off
   * a callback ref, which the old (dishonest) `ref: RefObject<...>` typing implied
   * they could — `camera.ref.current` was always undefined. `cameraRef` is a real
   * `RefObject`, kept in sync inside the same callback below, for consumers who
   * need imperative access beyond setZoom/setTorch/setFocusPoint (e.g.
   * capture/start/stop/rescan).
   */
  cameraRef: RefObject<VisionCameraRefProps | null>;
  onCameraStateChanged: (event: VisionCameraStateEvent) => void;
  state: VisionCameraStateEvent | undefined;
  capabilities: CameraCapabilities | undefined;
  setZoom: (ratio: number) => void;
  setTorch: (on: boolean) => void;
  setFocusPoint: (x: number, y: number) => void;
} {
  const [state, setState] = useState<VisionCameraStateEvent | undefined>(undefined);
  const [capabilities, setCapabilities] = useState<CameraCapabilities | undefined>(undefined);
  const instanceRef = useRef<VisionCameraRefProps | null>(null);

  // A plain RefObject can't observe attach/detach transitions — only a
  // callback ref can, and resetting state/capabilities on view-instance
  // change requires observing that transition. Callback refs are assignable
  // wherever a `ref` prop is expected on a forwardRef component, so this is
  // safe to hand to <VisionCamera ref={camera.ref} />. `instanceRef` (exposed
  // below as `cameraRef`) is kept in sync here so consumers who need a real
  // `.current` (not just this hook's setZoom/setTorch/setFocusPoint) have one.
  const ref = useCallback((instance: VisionCameraRefProps | null) => {
    if (instance !== instanceRef.current) {
      setState(undefined);
      setCapabilities(undefined);
      if (instance) {
        // Capture the instance this fetch belongs to. Without it, a slow fetch for
        // a detached view can resolve after a newer view attached and overwrite the
        // newer capabilities — permanently, if the two responses land out of order.
        const fetchedFor = instance;
        VisionCore.getCameraCapabilities()
          .then((caps) => {
            if (instanceRef.current === fetchedFor) {
              setCapabilities(caps);
            }
          })
          .catch((err) => {
            // Capabilities are best-effort; the state stream remains authoritative.
            console.warn('[useCameraControls] getCameraCapabilities failed:', err);
          });
      }
    }
    instanceRef.current = instance;
  }, []);

  const onCameraStateChanged = useCallback((event: VisionCameraStateEvent) => {
    setState(event);
  }, []);

  const setZoom = useCallback((ratio: number) => {
    instanceRef.current?.setZoom(ratio);
  }, []);

  const setTorch = useCallback((on: boolean) => {
    instanceRef.current?.setTorch(on);
  }, []);

  const setFocusPoint = useCallback((x: number, y: number) => {
    instanceRef.current?.setFocusPoint(x, y);
  }, []);

  return {
    ref,
    cameraRef: instanceRef,
    onCameraStateChanged,
    state,
    capabilities,
    setZoom,
    setTorch,
    setFocusPoint,
  };
}
