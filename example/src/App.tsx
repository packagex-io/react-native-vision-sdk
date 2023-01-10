import * as React from 'react';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  return (
    <VisionSdkView
      BarCodeScanHandler={(e: { nativeEvent: { code: any } }) =>
        console.log('BarCodeScanHandler', e?.nativeEvent?.code)
      }
      OCRScanHandler={(e: any) => console.log('OCRScanHandler', e)}
      OnDetectedHandler={(e: { nativeEvent: any }) =>
        console.log('OnDetectedHandler', e?.nativeEvent)
      }
    />
  );
}
