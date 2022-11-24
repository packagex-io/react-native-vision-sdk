import React, { useImperativeHandle, useRef, useState } from 'react';
import {
  View,
  UIManager,
  findNodeHandle,
  Text,
  Platform,
  StyleSheet,
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';

enum ScanMode {
  OCR = 'ocr',
  BARCODE = 'barcode',
  QRCODE = 'qrcode',
}

type Props = {
  children: React.ReactNode;
  refProp: any;
  BarCodeScanHandler: (_e: any) => void;
  OCRScanHandler: (_e: any) => void;
  OnDetectedHandler: (_e: any) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  BarCodeScanHandler = (_e: any) => {},
  OCRScanHandler = (_e: any) => {},
  OnDetectedHandler = (_e: any) => {},
}: Props) => {
  const defaultScanMode = ScanMode.OCR;
  const [mode, setMode] = useState<ScanMode>(defaultScanMode);
  const VisionSDKViewRef = useRef(null);

  useImperativeHandle(refProp, () => ({
    cameraCaptureHandler: () => {
      onPressCapture();
    },
    changeModeHandler: (input: React.SetStateAction<ScanMode>) => {
      onChangeMode(input);
    },
  }));

  const onPressCapture = () => {
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
      mode={mode}
      onDetected={OnDetectedHandler}
      ref={VisionSDKViewRef}
    >
      <View style={styles.childrenContainer}>{children}</View>
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
