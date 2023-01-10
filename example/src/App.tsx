import * as React from 'react';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  return (
    <VisionSdkView
      BarCodeScanHandler={(e: any) => console.log('BarCodeScanHandler', e)}
      OCRScanHandler={(e: any) => console.log('OCRScanHandler', e)}
      OnDetectedHandler={(e: any) => console.log('OnDetectedHandler', e)}
    />
  );
}
