import * as React from 'react';
import VisionSdkView from 'react-native-vision-sdk';

export default function App() {
  return (
    <VisionSdkView
      children={undefined}
      refProp={undefined}
      BarCodeScanHandler={function (_e: any): void {
        throw new Error('Function not implemented.');
      }}
      OCRScanHandler={function (_e: any): void {
        throw new Error('Function not implemented.');
      }}
      OnDetectedHandler={function (_e: any): void {
        throw new Error('Function not implemented.');
      }}
    />
  );
}
