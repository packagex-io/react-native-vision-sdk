import * as React from 'react';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  return (
    <VisionSdkView
      BarCodeScanHandler={(e) =>
        console.log('BarCodeScanHandler', e?.nativeEvent?.code)
      }
      OCRScanHandler={(e) => console.log('OCRScanHandler', e)}
      OnDetectedHandler={(e) =>
        console.log('OnDetectedHandler', e?.nativeEvent)
      }
    />
  );
}
