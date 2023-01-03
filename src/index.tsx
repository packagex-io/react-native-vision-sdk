import React, { useImperativeHandle, useRef, useState } from 'react';
import {
  View,
  UIManager,
  findNodeHandle,
  Text,
  Platform,
  StyleSheet,
  TouchableOpacity,
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';

enum ScanMode {
  OCR = 'ocr',
  BARCODE = 'barcode',
  QRCODE = 'qrcode',
}
// enum AutoMode  ["auto", "manual"]

type Props = {
  children: React.ReactNode;
  refProp: any;
  BarCodeScanHandler: (_e: any) => void;
  OCRScanHandler: (_e: any) => void;
  OnDetectedHandler: (_e: any) => void;
  onError: (e: { nativeEvent: { code: any } }) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  BarCodeScanHandler = (_e: any) => {},
  OCRScanHandler = (_e: any) => {},
  OnDetectedHandler = (_e: any) => {},
  onError = (_e: any) => {},
}: Props) => {
  const defaultScanMode = ScanMode.OCR;
  const [mode, setMode] = useState<ScanMode>(defaultScanMode);
  // const [captureMode, setCaptureMode] = useState<String>('auto');
  // const [apiKey, setAPIKey] = useState < String > ('key_stag_7da7b5e917tq2eCckhc5QnTr1SfpvFGjwbTfpu1SQYy242xPjBz2mk3hbtzN6eB85MftxVw1zj5K5XBF')

  const VisionSDKViewRef = useRef(null);

  useImperativeHandle(refProp, () => ({
    cameraCaptureHandler: () => {
      onPressCaptures();
    },
    changeModeHandler: (input: React.SetStateAction<ScanMode>) => {
      onChangeMode(input);
    },
  }));

  const onPressCaptures = () => {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(VisionSDKViewRef.current),
      (UIManager.hasViewManagerConfig('VisionSDKView') &&
        UIManager.getViewManagerConfig('VisionSDKView').Commands
          .captureImage) ||
        0,
      []
    );
  };

  const onChangeMode = (input: React.SetStateAction<ScanMode>) => {
    setMode(input);
  };

  return Platform.OS === 'ios' ? (
    <VisionSdkView
      style={styles.flex}
      onBarcodeScanSuccess={BarCodeScanHandler}
      onOCRDataReceived={OCRScanHandler}
      OnDetectedHandler={OnDetectedHandler}
      // onOCRDataReceived={({ nativeEvent }) =>
      //   console.log('onOCRDataReceived', nativeEvent)
      // }
      // onBarcodeScanSuccess={({ nativeEvent }) =>
      //   console.log('onBarcodeScanSuccess', nativeEvent)
      // }
      mode={mode}
      apiKey={
        'key_stag_7da7b5e917tq2eCckhc5QnTr1SfpvFGjwbTfpu1SQYy242xPjBz2mk3hbtzN6eB85MftxVw1zj5K5XBF'
      }
      captureMode={'auto'}
      onError={onError}
      onDetected={onDetected}
      ref={VisionSDKViewRef}
    >
      <View style={styles.childrenContainer}>
        <TouchableOpacity
          // onPress={() => setCaptureMode('manual')}
          onPress={() => setMode(ScanMode.OCR)}
          // style={{ position: 'absolute', bottom: 50, alignSelf: 'center' }}
        >
          <Text>'change Mode'</Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => onPressCaptures()}
          // style={{ position: 'absolute', bottom: 100, alignSelf: 'center' }}
        >
          <Text>'Clink'</Text>
        </TouchableOpacity>
      </View>
      {children}
    </VisionSdkView>
  ) : (
    <View style={styles.flex}>
      <Text>NOT IMPLEMENTED FOR ANDROID YET.</Text>
    </View>
  );
};

export default Camera;

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  childrenContainer: {
    position: 'absolute',
    top: 0,
    left: 0,
    height: '100%',
    width: '100%',
  },
});
