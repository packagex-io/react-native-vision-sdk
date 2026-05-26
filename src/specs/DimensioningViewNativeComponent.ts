import type { HostComponent, ViewProps } from 'react-native';
import type {
  DirectEventHandler,
  Int32,
  WithDefault,
} from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';

// Event type definitions

type DimensioningCaptureEvent = Readonly<{
  // Measurement result serialised as JSON to avoid codegen object-nesting limitations
  measurementJson: string;
}>;

type DimensioningErrorEvent = Readonly<{
  code: Int32;
  message: string;
  reason?: string;
}>;

// Component props
export interface NativeDimensioningProps extends ViewProps {
  mode?: WithDefault<string, 'offline'>; // 'offline' | 'online'
  measurementUnit?: WithDefault<string, 'centimeters'>;
  maximumTrackCount?: Int32;

  onCapture?: DirectEventHandler<DimensioningCaptureEvent>;
  onError?: DirectEventHandler<DimensioningErrorEvent>;
}

export default codegenNativeComponent<NativeDimensioningProps>(
  'DimensioningView'
) as HostComponent<NativeDimensioningProps>;
