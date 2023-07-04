# React Native Vision SDK

VisionSDK provides a way to detect barcodes and QR codes with both manual and auto capturing modes. It also provides OCR (Optical Character Recognition) for text detection (label scanning with Restful API) modes.
Some key features of the VisionSDK include:
• Support for multiple view types (rectangular, square, full screen) for the scanning window
• Customization options for the scanning window size, shape, and border style
• Capture image and OCR API capabilities

## Installation

yarn add react-native-vision-sdk;
// OR
npm install --save react-native-vision-sdk

## Permissions

To use the camera,

1. On Android you must ask for camera permission:

 <uses-permission android:name="android.permission.CAMERA" />

2. On iOS, you must update Info.plist with a usage description for camera
   ...
   <key>NSCameraUsageDescription</key>
   <string>Your own description of the purpose</string>
   ...

## IOS Development Requirements

• iOS 13.0+
• Swift: 5.4.2
• Xcode Version: 13.0

### Basis Of Usage

```js
import React, { useEffect, useRef } from 'react';
import VisionSdkView from 'react-native-vision-sdk';
const ScannerView = () => {
  const visionSdk = useRef(null);
  return (
    <VisionSdkView
      refProp={visionSdk}
      OCRScanHandler={(value) => console.log('on OCR Detected', value)}
      OnDetectedHandler={(value) => {
        console.log(
          'Detected Barcode =',
          value.nativeEvent ? value.nativeEvent.barCode : value.barcode
        );
        console.log(
          'Detected Text =',
          value.nativeEvent ? value.nativeEvent.text : value.text
        );
      }}
    />
  );
};
```

### Initialization

In order to use the OCR API, you have to set apiKey to your API key. Also, you also need to specify the API environment that you have the API key for. Please note that these have to be set before using the API call. You can generate your own API key at cloud.packagex.io. You can find the instruction guide here.

```js
useEffect (() => {
visionSdk.current.changeModeHandler(
mode, // scanning mode like ocr, barcode, qrcode
apiKey // your api key
locationId // your location id if you have
},[])
```

### Capture Image

You can capture an image when mode is OCR. In OCR mode when capture is called, then in the callback, it will return an image.
```js
visionSdk.current.cameraCaptureHandler()
```

### Close Camera

Stops camera session and scanning.
```js
visionSdk.current.stopRunningHandler()
```

### Props

refProp: Set reference for vision sdk to manipulate modes or to access callback functions
mode: barCode - Detects barcodes only in this mode.
qrCode - Detects qr codes only in this mode.
ocr - Use this mode to capture photos for later user in OCR API call.

captureMode: Default captureMode in ‘auto’, you can either use ‘manual’
locationId: By default your location will get from apiKey or either you can set location id
environment: You can set your development environment like ‘dev’, ‘staging’

### The callback will be called with a response object

### The Response Object
```js
OCRScanHandler: Return ocr detected data
onDetectedHandler: Return the detected data for ‘barcode’ and ‘text’
onBarCodeScanHandler: Return the detected data for barcode
onError: Description of the error, use it for debug purpose only
onImageCaptured: whenever image is capture in OCR mode
ErrorCode: Description camera_unavailable camera not available on device
permission: Permission not satisfied
others: other errors (check errorMessage for description)
```
### In the callbacks, Success or error will be returned. It returns with the OCR Response from PackageX Platform API Response.

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License
MIT
