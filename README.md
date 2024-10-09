# React Native Vision SDK

VisionSDK provides a simple and efficient way to detect barcodes and QR codes in both manual and
automatic capturing modes. It also includes AI capabilities to extract information from logistic
documents.

Some key features of the VisionSDK Integration include:

- Barcode and QR code scanning
- Focus on a specific area of camera preview
- Document detection
- Capturing of image
- Information extraction from logistic documents (via both local ML models (offline) and REST API)
  - Shipping Label
  - Bill of Lading
  - Price Tag (under progress)

![Example1](ReadMeContent/Videos/Sample/VisionSDKSample.gif)

## Installation

To install the `react-native-vision-sdk` package, you can use either `npm` or `yarn`:

### Using Npm

```sh
npm install --save react-native-vision-sdk
```

### Using Yarn

```sh
yarn add react-native-vision-sdk
```

### Manual installation

#### IOS

1. Open up `ios/Podfile`

- Set `platform :ios, 15.0   # Vision-sdk requires at least IOS 15.0 or higher`

2. Run `pod install`

IOS Development Requirements

- iOS 15.0+
- Swift: 5.7
- Xcode Version: 13.0

#### ANDROID

Modify your android/build.gradle configuration:
set the minSdkVersion to 29 atleast, Vision-sdk requires at least version 29 of minSdkVersion.

```
buildscript {
  ext {
    buildToolsVersion = "34.0.0"
    minSdkVersion = 29   // Change over here
    compileSdkVersion = 34
    targetSdkVersion = 34
    ndkVersion = "26.1.10909125"
    kotlinVersion = "1.9.24"
  }
```

## Permissions

To use the camera,

1. On Android you must ask for camera permission:
   you have to add the following code to the AndroidManifest.xml:
   ...
   <uses-permission android:name="android.permission.CAMERA" />
   ...

2. On iOS, you must update Info.plist with a usage description for camera
   ...
   <key>NSCameraUsageDescription</key>
   <string>Your own description of the purpose</string>
   ...

### Basic Usage

```js
import React, { useEffect, useRef } from 'react';
import VisionSdkView from 'react-native-vision-sdk';
const ScannerView = () => {
  const visionSdk = useRef(null);
  useEffect(() => {
    visionSdk?.current?.startRunningHandler();
  }, []);
  return (
    <VisionSdkView
      refProp={visionSdk}
      mode="barcode"
      captureMode="auto"
      onBarcodeScan={(value) => console.log('BarCodeScanHandler', value)}
      onOCRScan={(value) => console.log('on OCR Detected', value)}
      onDetected={(value) => console.log('onDetected', value)}
      onError={(value) => console.log('error', value)}
    />
  );
};
```

### Start Camera

This method start camera session and scanning.

```js
visionSdk.current.startRunningHandler();
```

### Restart Camera

This method restart scanning after every scan.

```js
visionSdk.current.restartScanningHandler();
```

### Stop Camera

This method stops camera session and scanning.

```js
visionSdk.current.stopRunningHandler();
```

### Capture Image

You can capture an image when captureMode is manual.

```js
visionSdk.current.cameraCaptureHandler();
```

### Set Focus Settings (Optional)

You can customize camera focus settings.

```js
visionSdk?.current?.setFocusSettings({
  shouldDisplayFocusImage: true,
  shouldScanInFocusImageRect: true,
  showCodeBoundariesInMultipleScan: true,
  validCodeBoundaryBorderColor: '#2abd51',
  validCodeBoundaryBorderWidth: 2,
  validCodeBoundaryFillColor: '#2abd51',
  inValidCodeBoundaryBorderColor: '#cc0829',
  inValidCodeBoundaryBorderWidth: 2,
  inValidCodeBoundaryFillColor: '#cc0829',
  showDocumentBoundaries: true,
  documentBoundaryBorderColor: '#241616',
  documentBoundaryFillColor: '#e3000080',
  focusImageTintColor: '#ffffff',
  focusImageHighlightedColor: '#e30000',
});
```

### Set Object Detection Settings (Optional)

You can customize object detection indications to avoid extra processing.

```js
visionSdk?.current?.setObjectDetectionSettings({
  isTextIndicationOn: true,
  isBarCodeOrQRCodeIndicationOn: true,
  isDocumentIndicationOn: true,
  codeDetectionConfidence: 0.5,
  documentDetectionConfidence: 0.5,
  secondsToWaitBeforeDocumentCapture: 2.0,
});
```

### Set Camera Settings (Optional)

You can customize frames processing by setting N number that will process every Nth frame.

```js
visionSdk?.current?.setCameraSettings({
  nthFrameToProcess: 10,
});
```

### Configure On-Device Model

Configure on-device model by passing model type and model size in configureOnDeviceModel method, starts model configuration.

```js
visionSdk.current.configureOnDeviceModel({
  type: 'shipping_label',
  size: 'large',
});
```

### Props

All the props will be passed.

| **Prop**      | **Type**                                                 | **Description**                                                                                                        |
| ------------- | -------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `refProp`     | `Function`                                               | Catch the reference of the component to manipulate modes or to access callback functions.                              |
| `mode`        | `string: (ocr, barcode, qrcode, barCodeOrQrCode, photo)` | Default mode is ‘barcode’, you can either use other like ocr, qrcode, photo.                                           |
| `captureMode` | `string: (manual, auto)`                                 | Default captureMode is ‘manual’, you can either use ‘auto’.                                                            |
| `apiKey`      | `string`                                                 | In order to use the OCR API/MODEL, You must set your API key or either an Auth token..                                 |
| `token`       | `string`                                                 | In order to use the OCR API/MODEL, You must set your API key or either an Auth token.                                  |
| `environment` | `string: (sandbox, prod)`                                | If you are using OCR mode then you can set your development environment. (Default env is prod)                         |
| `ocrMode`     | `string: (cloud, on-device, on-device-with-translation)` | ocrMode defines whether you want to scan using cloud API, on-Device Model or on-Device Model with response translation |
| `flash`       | `boolean: (true, false)`                                 | You can turn ON/OFF camera flash by using this prop. (Default value is false)                                          |
| `zoomLevel`   | `number:  (1 to 5)`                                      | You can set the Zoom value. Zoom value is device dependent. It will be vary between 1 to 5.                            |
| `locationId`  | `string: (ex# loc_2rpHXFf6ith)`                          | By default your location will get from apiKey or either you can set location id.                                       |
| `options`     | `Object: {x: number, y: string}`                         | Option contains different other optional parameters you can provide along with the image.                              |

### API Key

In order to use the OCR API, you have to set API key. Also, you also need to specify the API
environment that you have the API key for. Please note that these have to be set before using the API call. You can
generate your own API key at [cloud.packagex.io](https://cloud.packagex.io/auth/login). You can find the instruction
guide [here](https://docs.packagex.io/docs/getting-started/welcome).

### Mode Details

barCode - Detects barcode only in this mode.
qrCode - Detects qrcode only in this mode.
barCodeOrQrCode - Detects both qr and bar codes in this mode.
ocr - Use this mode to capture photos for later use in OCR API call.
photo - You can capture simple photos.

### The Response Object

In the callbacks, Success or error will be returned. It returns with the OCR Response from PackageX Platform API Response.

```js
onOCRScan: Return ocr detected data
onDetected: Return the boolean indicators for 'barcode','qrcode','document' and 'text'
onBarcodeScan: Return the detected textual data of barcode or qrcode
onModelDownloadProgress: Return the OCR model downloading status
onError: Returns the object with error code and error message
onImageCaptured: Return image which is capture in photo & OCR mode
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
