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

- iOS 15.0+
- Swift: 5.7
- Xcode Version: 13.0

### Basis Of Usage

```js
import React, { useEffect, useRef } from 'react';
import VisionSdkView from 'react-native-vision-sdk';
const ScannerView = () => {
  const visionSdk = useRef(null);
  useEffect(() => {
    visionSdk?.current?.setHeight(1);
    visionSdk?.current?.startRunningHandler();
  }, []);
  return (
    <VisionSdkView
      refProp={visionSdk}
      mode="barcode"
      onBarcodeScan={(value) => console.log('BarCodeScanHandler', value)}
      onOCRScan={(value) => console.log('on OCR Detected', value)}
      onDetected={(value) => {
        console.log(
          'Detected Barcode =',
          value.nativeEvent ? value.nativeEvent.barCode : value.barcode
        );
        console.log(
          'Detected Text =',
          value.nativeEvent ? value.nativeEvent.text : value.text
        );
      }}
      onError={(e: any) => {
        console.log('error', e);
      }}
    />
  );
};
```

### Camera View Height

Set Camera View Height

```js
// This is only required for IOS
visionSdk.current.setHeight(1); // value should be in between 0 to 1.
```

### Start Camera

Start camera session and scanning.

```js
visionSdk.current.startRunningHandler();
```

### Close Camera

Stops camera session and scanning.

```js
visionSdk.current.stopRunningHandler();
```

### Capture Image

You can capture an image when captureMode is manual.

```js
visionSdk.current.cameraCaptureHandler();
```

### Set Zoom Value

You can set the Zoom value. Zoom value is device dependent. It will be vary between 1 to 5.

```js
visionSdk.current.setToDefaultZoom(1.8); 
```

### Toggle Torch

Toggle the torch ON/OFF.

```js
visionSdk.current.onPressToggleTorchHandler(true); //false for OFF
```

### Configure On-Device Model

Configure on-device model by passing model type and model size. Before calling configureOnDeviceModel you have to set isOnDeviceOCR prop with true.

```js
visionSdk.current.configureOnDeviceModel({ type: 'shipping_label', size: 'large' });
```


### Props

All the props will be passed.

| **Prop**                    | **Type**                                | **Description**                                                                                                   |
| --------------------------- | --------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `refProp`                   | `Function`                              | Catch the reference of the component to manipulate modes or to access callback functions.                         |
| `isOnDeviceOCR`             | `boolean: (true, false)`                | This prop will work if the mode is ocr for document detection in OnDevice/Cloud based on value.                   |
| `apiKey`                    | `string`                                | In order to use the OCR API/MODEL, you have to set your API key / Auth token.                                     |
| `token`                     | `string`                                | In order to use the OCR API/MODEL, you have to set your Auth token / API key.                                     |
| `mode`                      | `string: (ocr, barcode, qrcode, photo)` | Default mode is ‘barcode’, you can either use other like ocr, qrcode, photo.                                      |
| `captureMode`               | `string: (manual, auto)`                | Default captureMode is ‘manual’, you can either use ‘auto’.                                                       |
| `showDocumentBoundaries`    | `boolean: (true, false)`                | To draw boundaries around detected document in camera stream. (Default value is false)                            |
| `delayTime`                 | `number: (milliseconds)`                | Time threshold to wait before capturing a document automatically in OCR mode. (Default value is 100 milliseconds) |
| `locationId`                | `string: (ex# loc_2rpHXFf6ith)`         | By default your location will get from apiKey or either you can set location id.                                  |
| `options`                   | `Object: {x: number, y: string}`        | Option contains different other optional parameters you can provide along with the image (optional)               |
| `environment`               | `string: (sandbox, prod)`               | If you are using OCR mode then you can set your development environment. (Default env is prod)                    |
| `showScanFrame`             | `boolean: (true, false)`                | You can use rectangle frame by setting it true, (Default value is false)                                          |
| `captureWithScanFrame`      | `boolean: (true, false)`                | You can choose the capture area to be full screen or rectangular frame.                                           |

### API Key

In order to use the OCR API, you have to set API key. Also, you also need to specify the API
environment that you have the API key for. Please note that these have to be set before using the API call. You can
generate your own API key at [cloud.packagex.io](https://cloud.packagex.io/auth/login). You can find the instruction
guide [here](https://docs.packagex.io/docs/getting-started/welcome).

### Mode Details

barCode - Detects barcodes only in this mode.
qrCode - Detects qr codes only in this mode.
ocr - Use this mode to capture photos for later user in OCR API call.
photo - You can capture simple photos.

### The Response Object

In the callbacks, Success or error will be returned. It returns with the OCR Response from PackageX Platform API Response.

```js
onOCRScan: Return ocr detected data
onDetected: Return the detected data for ‘barcode’,'qrcode' and ‘text’
onBarcodeScan: Return the detected data for barcode in barcode mode
onModelDownloadProgress: : Return the OCR model dowmloading status
onError: Description of the error, use it for debug purpose only
onImageCaptured: whenever image is capture in photo mode
ErrorCode: Description camera_unavailable camera not available on device
permission: Permission not satisfied
others: other errors (check errorMessage for description)
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
