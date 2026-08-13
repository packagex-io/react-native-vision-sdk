import type { HostComponent, ViewProps } from 'react-native';
import type {
  DirectEventHandler,
  Int32,
  WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';

// Event type definitions
//
// Every payload is a JSON string rather than a structured object. Codegen does
// not handle nested objects or arrays inside event payloads, and these carry
// arrays of points and tracks — so they are serialised natively and parsed in
// DimensioningView.tsx. Same technique as the existing measurementJson.

type DimensioningCaptureEvent = Readonly<{
  // Measurement result serialised as JSON to avoid codegen object-nesting limitations
  measurementJson: string;
}>;

type DimensioningErrorEvent = Readonly<{
  code: Int32;
  message: string;
  reason?: string;
}>;

type DimensioningMeasurementUpdateEvent = Readonly<{
  // { trackingState, primaryTrackId, tracks: [...] }
  updateJson: string;
}>;

type DimensioningOverlayEvent = Readonly<{
  // { boxes: [...], planes: [...], hud: {...} } — only fires when overlayMode === 'callback'
  overlayJson: string;
}>;

type DimensioningTelemetryEvent = Readonly<{
  // { type: 'measurementCaptured' | 'measurementAborted', ...payload }
  // Only fires when enableTelemetry is true. 'measurementAborted' is the only
  // signal for a capture that failed mid-session — the SDK's view exposes no
  // error callback of its own.
  telemetryJson: string;
}>;

// Component props
export interface NativeDimensioningProps extends ViewProps {
  mode?: WithDefault<string, 'offline'>; // 'offline' | 'online'
  measurementUnit?: WithDefault<string, 'centimeters'>;
  maximumTrackCount?: Int32;

  // 'builtIn' (default) | 'none' | 'callback'. 'callback' suppresses the SDK's
  // own graphics and streams geometry through onOverlayUpdate instead.
  overlayMode?: WithDefault<string, 'builtIn'>;

  // Cloud segmentation credentials, used when mode === 'online'. When left
  // empty the native side falls back to the host app's VSDKConstants, which is
  // how online mode behaved before VisionSDK 2.7.0.
  cloudUrl?: string;
  cloudApiKey?: string;
  cloudSdkId?: string;

  // Gates delivery of onTelemetry. Off by default.
  enableTelemetry?: WithDefault<boolean, false>;

  onCapture?: DirectEventHandler<DimensioningCaptureEvent>;
  onError?: DirectEventHandler<DimensioningErrorEvent>;
  onMeasurementUpdate?: DirectEventHandler<DimensioningMeasurementUpdateEvent>;
  onOverlayUpdate?: DirectEventHandler<DimensioningOverlayEvent>;
  onTelemetry?: DirectEventHandler<DimensioningTelemetryEvent>;
}

interface NativeCommands {
  // Tears the AR view down and releases the rear camera. ARKit and
  // AVCaptureSession cannot share it, so call this before showing <VisionCamera>.
  stop: (viewRef: React.ElementRef<HostComponent<NativeDimensioningProps>>) => void;
  // Re-creates the AR view after stop().
  start: (viewRef: React.ElementRef<HostComponent<NativeDimensioningProps>>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  // Append-only. Index position is a wire format — the iOS numeric-command-id
  // fallback in DimensioningViewComponentView.mm maps ids to names by this
  // order, so inserting mid-array renumbers every command after it. New
  // commands go last.
  supportedCommands: ['stop', 'start'],
});

export default codegenNativeComponent<NativeDimensioningProps>(
  'DimensioningView'
) as HostComponent<NativeDimensioningProps>;
