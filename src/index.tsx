import React, { useImperativeHandle, useRef, useState } from 'react';
import {
  View,
  UIManager,
  findNodeHandle,
  Text,
  Platform,
  StyleSheet,
  TouchableOpacity,
  Image,
} from 'react-native';
import { VisionSdkView } from './VisionSdkViewManager';

enum ScanMode {
  OCR = 'ocr',
  BARCODE = 'barcode',
  QRCODE = 'qrcode',
}
// enum AutoMode  ["auto", "manual"]

type Props = {
  children?: React.ReactNode;
  refProp?: any;
  BarCodeScanHandler?: (_e: any) => void;
  OCRScanHandler?: (_e: any) => void;
  OnDetectedHandler?: (_e: any) => void;
  onError?: (e: { nativeEvent: { message: any } }) => void;
};

const Camera: React.FC<Props> = ({
  children,
  refProp,
  BarCodeScanHandler = (_e: any) => {},
  OCRScanHandler = (_e: any) => {},
  onError = (_e: any): void => {},
}: Props) => {
  const defaultScanMode = ScanMode.OCR;
  const [mode, setMode] = useState<ScanMode>(defaultScanMode);
  const [captureMode, setCaptureMode] = useState<String>('auto');
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
      // onDetected={onDetected}
      // OnDetectedHandler={OnDetectedHandler}
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
      // onDetected={onDetected}
      ref={VisionSDKViewRef}
    >
      {/* <View style={[styles.childrenContainer]}>
        <View
          style={[
            styles.row,
            { paddingHorizontal: 20, alignItems: 'flex-start' },
          ]}
        >
          <View>
            <View style={styles.circle} />
          </View>
          <View>
            <View style={styles.circle} />
            <View style={styles.circle} />
            <View style={styles.circle} />
          </View>
        </View>
        <View>
          <View
            style={[styles.row, { marginBottom: 10, paddingHorizontal: 20 }]}
          >
            <View>
              <View style={styles.circle} />
            </View>
            {mode === ScanMode.OCR && (
              <View style={styles.zoomBlock}>
                <View style={[styles.circle]} />
                <View style={styles.circle} />
                <View style={styles.circle} />
              </View>
            )}

            {mode !== ScanMode.OCR && (
              <View style={styles.autoManualBlock}>
                <ActionButton
                  text={'Manual'}
                  textColor={'#000'}
                  style={captureMode === 'manual' && styles.unautoManualButton}
                  // isSeleted={mode === ScanMode.OCR}
                  onPress={() => setCaptureMode('manual')}
                />
                <ActionButton
                  text={'Auto'}
                  textColor={'#000'}
                  style={captureMode === 'auto' && styles.autoManualButton}
                  // isSeleted={mode === ScanMode.OCR}
                  onPress={() => setCaptureMode('auto')}
                />
              </View>
            )}

            <View>
              <View style={styles.circle} />
            </View>
          </View>
          <View
            style={{
              backgroundColor: '#000',
              width: '100%',
              paddingVertical: 20,
            }}
          >
            <View style={[styles.row, { justifyContent: 'space-around' }]}>
              <ActionButton
                text={'BarCode'}
                isSeleted={mode === ScanMode.BARCODE}
                onPress={() => setMode(ScanMode.BARCODE)}
              />
              <ActionButton
                text={'QR Code'}
                isSeleted={mode === ScanMode.QRCODE}
                onPress={() => setMode(ScanMode.QRCODE)}
              />
              <ActionButton
                text={'OCR'}
                isSeleted={mode === ScanMode.OCR}
                onPress={() => setMode(ScanMode.OCR)}
              />
            </View>
            {captureMode === 'manual' ? (
              <View
                style={{
                  alignItems: 'center',
                  justifyContent: 'center',
                  paddingVertical: 20,
                }}
              >
                <ActionButton
                  text={'OCR'}
                  textColor={'#000'}
                  style={{
                    backgroundColor: '#fff',
                    width: 70,
                    height: 70,
                    borderRadius: 35,
                    alignItems: 'center',
                    justifyContent: 'center',
                    paddingVertical: 20,
                  }}
                  onPress={() => onPressCaptures()}
                />
              </View>
            ) : (
              <View />
            )}
          </View>
        </View>
      </View> */}
      {/* </View> */}
      {children}
    </VisionSdkView>
  ) : (
    <View style={styles.flex}>
      <Text>NOT IMPLEMENTED FOR ANDROID YET.</Text>
    </View>
  );
};

export default Camera;

type ActionButtonType = {
  text?: string;
  onPress?: () => void | undefined;
  isSeleted?: boolean | undefined;
  icon?: string | undefined;
  style?: any;
  textColor?: string | undefined | null | any;
};
const ActionButton = ({
  text,
  onPress,
  isSeleted,
  icon,
  style,
  textColor,
}: ActionButtonType) => {
  return (
    <TouchableOpacity style={[style && style]} onPress={onPress}>
      <Text
        style={[
          isSeleted ? styles.buttonText : styles.unbuttonText,
          textColor && { color: textColor },
        ]}
      >
        {text}
      </Text>
      {icon ?? <Image source={icon} />}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  buttonText: {
    fontSize: 15,
    color: 'yellow',
  },
  unbuttonText: {
    fontSize: 15,
    color: 'white',
  },
  zoomBlock: {
    paddingHorizontal: 5,
    borderRadius: 30,
    height: 40,
    flexDirection: 'row',
    backgroundColor: '#00000050',
    justifyContent: 'space-between',
  },
  autoManualBlock: {
    borderRadius: 4,
    // paddingHorizontal: 2,
    height: 40,

    alignItems: 'center',
    flexDirection: 'row',
    backgroundColor: 'grey',
    justifyContent: 'space-between',
  },
  autoManualButton: {
    backgroundColor: '#fff',
    width: 80,
    borderRadius: 4,
    height: 30,
    marginHorizontal: 4,
    paddingHorizontal: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  unautoManualButton: {
    backgroundColor: 'grey',
    width: 80,
    borderRadius: 4,
    height: 30,
    marginHorizontal: 4,
    paddingHorizontal: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  circle: {
    width: 30,
    height: 30,
    borderRadius: 40,
    marginVertical: 5,
    marginHorizontal: 5,
    backgroundColor: '#00000050',
  },
  childrenContainer: {
    paddingTop: 20,
    justifyContent: 'space-between',
    position: 'absolute',
    top: 0,
    left: 0,
    height: '100%',
    width: '100%',
  },
});
