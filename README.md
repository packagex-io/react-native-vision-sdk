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

## ![Example1](ReadMeContent/Videos/Sample/VisionSDKSample.gif)

## Installation

Install the Vision SDK for React Native using either `npm` or `yarn`:

```sh
npm install --save react-native-vision-sdk
# or
yarn add react-native-vision-sdk
```

### Manual Installation

#### iOS

1. Open your `ios/Podfile` and add the following line to ensure compatibility:
   ```ruby
   platform :ios, '16.0'  # Vision SDK requires at least iOS 15.0 or higher
   pod 'VisionSDK', "1.5.1"
   ```
2. Run `pod install` to install necessary dependencies.

**iOS Development Requirements:**

- **iOS**: 15.0+
- **Swift**: 5.7
- **Xcode**: 13.0 or newer

#### Android

Edit your `android/build.gradle` file to set the `minSdkVersion` to 29 or higher:

```gradle
buildscript {
  ext {
    buildToolsVersion = "35.0.0"
    minSdkVersion = 29  // Minimum version required by Vision SDK
    compileSdkVersion = 35
    targetSdkVersion = 35
    ndkVersion = "26.1.10909125"
    kotlinVersion = "2.0.20"
  }
}
```

## Android Setup

In the `build.gradle` file of your Android project, add the following dependencies for Android integration:

```gradle
dependencies {
    // Existing dependencies
    implementation 'com.github.packagexlabs:vision-sdk-android:v2.0.31'
    implementation 'com.github.asadullahilyas:HandyUtils:1.1.0'
}
```

After making these changes, sync the project to download the necessary libraries.

---

## Permissions

To use the camera,

### Android

Add the following permission to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

### iOS

Update `Info.plist` with a usage description for the camera:

```xml
<key>NSCameraUsageDescription</key>
<string>Your description of the purpose for camera access</string>
```

---

## Basic Usage Example

Here’s an example of setting up the **Vision SDK** for barcode scanning in React Native.

```tsx
import React, { useEffect, useRef, useState } from 'react';
import VisionSdkView, { VisionSdkRefProps } from 'react-native-vision-sdk';
const ScannerView = () => {
  const visionSdk = useRef<VisionSdkRefProps>(null);

  // Configure Vision SDK settings
  useEffect(() => {
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
    visionSdk?.current?.setObjectDetectionSettings({
      isTextIndicationOn: true,
      isBarCodeOrQRCodeIndicationOn: true,
      isDocumentIndicationOn: true,
      codeDetectionConfidence: 0.5,
      documentDetectionConfidence: 0.5,
      secondsToWaitBeforeDocumentCapture: 2.0,
    });
    visionSdk?.current?.setCameraSettings({
      nthFrameToProcess: 10,
    });
    visionSdk?.current?.startRunningHandler();
  }, []);

  return (
    <VisionSdkView
      ref={visionSdk}
      mode="barcode"
      captureMode="auto"
      flash={false}
      zoomLevel={1.8}
      flash={true}
      onDetected={(event) => {
        console.log('onDetected', event);
        setDetectedData(event);
      }}
      onBarcodeScan={(event) => {
        console.log('onBarcodeScan', event);
        visionSdk.current?.restartScanningHandler();
      }}
      onError={(error) => {
        console.log('onError', error);
      }}
    />
  );
};
```

## SDK Methods

### Camera Controls

1. **Start Camera**: This method start camera session and scanning.
   ```js
   visionSdk.current.startRunningHandler();
   ```
2. **Restart Scanning**: This method restart scanning after every scan.
   ```js
   visionSdk.current.restartScanningHandler();
   ```
3. **Stop Camera**: This method stops camera session and scanning.
   ```js
   visionSdk.current.stopRunningHandler();
   ```
4. **Capture Image** (manual mode only): Capture an image.
   ```js
   visionSdk.current.cameraCaptureHandler();
   ```

---

## Configuration Methods

#### Set Focus Settings (Optional)

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

#### Set Object Detection Settings (Optional)

You can customize object detection indications to avoid extra processing.

```js
visionSdk?.current?.setObjectDetectionSettings({
  isTextIndicationOn: true,
  isBarCodeOrQRCodeIndicationOn: true,
  isDocumentIndicationOn: true,
  codeDetectionConfidence: 0.5,
  documentDetectionConfidence: 0.5,
  secondsToWaitBeforeDocumentCapture: 2,
});
```

#### Set Camera Settings (Optional)

You can customize frames processing by setting N number that will process every Nth frame.

```js
visionSdk?.current?.setCameraSettings({
  nthFrameToProcess: 10,
});
```

#### Configure On-Device Model

Configure on-device model by passing model type and model size in configureOnDeviceModel method, starts model configuration.

```js
visionSdk.current.configureOnDeviceModel({
  type: 'shipping_label',
  size: 'large',
});
```

## Prediction Methods

The SDK offers several prediction methods categorized based on the type of processing:

---

### `on-device`

The methods in this category use on-device processing, allowing for fast, offline analysis of images and barcodes, suitable for situations without internet access.

#### `getPrediction`

This method uses an on-device model to perform predictions on the provided image and barcode data, ensuring fast, private processing.

```js
/**
 * This method uses an on-device model to get a prediction based on the provided image and barcode data.
 * @param image The image to be analyzed.
 * @param barcode An array of barcode values to analyze alongside the image.
 */
visionSdk.current.getPrediction(image, barcode);
```

---

### `on-device-with-translation`

Methods in this category use on-device models combined with synchronized cloud transformations, which enhance the prediction accuracy and add more context to the results.

#### `getPredictionWithCloudTransformations`

This method uses an on-device model with synchronized cloud transformations for more comprehensive and detailed analysis.

```js
/**
 * This method uses an on-device model with synchronized cloud transformations to get a prediction.
 * @param image The image to be analyzed.
 * @param barcode An array of barcode values to analyze alongside the image.
 */
visionSdk.current.getPredictionWithCloudTransformations(image, barcode);
```

---

### `cloud`

The methods in this category use cloud processing, which is ideal for complex analyses requiring more computational power or enhanced data resources.

#### `getPredictionShippingLabelCloud`

This method uses cloud processing to analyze a shipping label image and any associated barcodes.

```js
/**
 * This method uses cloud processing to get a prediction for a shipping label.
 * @param image The image of the shipping label.
 * @param barcode Array of barcode strings associated with the shipping label.
 */
visionSdk.current.getPredictionShippingLabelCloud(image, barcode);
```

---

### `bill-of-lading`

The methods in this section are optimized specifically for Bill of Lading documents and use cloud processing tailored to the document’s requirements.

#### `getPredictionBillOfLadingCloud`

This method applies cloud processing to analyze Bill of Lading images and associated barcodes, providing data relevant to logistics and shipping.

```js
/**
 * This method uses cloud processing to get a prediction for a Bill of Lading.
 * @param image The image of the Bill of Lading.
 * @param barcode Array of barcode strings.
 * @param withImageResizing (Optional) Whether to resize the image (default: true).
 */
visionSdk.current.getPredictionBillOfLadingCloud(
  image,
  barcode,
  withImageResizing
);
```

### `item_label`

The methods in this section are optimized specifically for item label documents and use cloud processing tailored to the document’s requirements.

#### `getPredictionItemLabelCloud`

This method analyzes item labels using cloud processing, focusing on logistics-specific details.

```js
/**
 * This method uses cloud processing to get a prediction for an item label.
 * @param image The image of the item label.
 * @param withImageResizing (Optional) Whether to resize the image (default: true).
 */

visionSdk.current.getPredictionItemLabelCloud(image, withImageResizing);
```

### `document_classification`

The methods in this section are optimized specifically for document classification documents and use cloud processing tailored to the document’s requirements.

#### `getPredictionDocumentClassificationCloud`

This method is tailored for analyzing document classification images and their associated barcodes using cloud processing.

```js
/**
 * This method uses cloud processing to get a prediction for document classification images.
 * @param image The image of the Bill of Lading.
 */

visionSdk.current.getPredictionDocumentClassificationCloud(image);
```

## Error Reporting

### `reportError`

handle errors on the device. It supports capturing relevant UI information and logs for debugging.

```js
/**
 * reportError for handling errors on the device.
 *
 * @param payload - An object containing the following properties:
 *   @param reportText - A custom error message.
 *   @param size - Size of the device module where the error occurred.
 *   @param type - Type of the device module where the error occurred.
 *   @param image - (Optional) Captured UI image related to the error.
 *   @param response - (Optional) Device module response message.
 */
visionSdk.current.reportError(data);
```

## Template Management

### `createTemplate`

This method is used to create a new template for use in cloud predictions.

```typescript
/**
 * Creates a new template.
 */
visionSdk.current.createTemplate();
```

### `getAllTemplates`

This method is used to get all saved templates.

```typescript
/**
 * Gets all saved templates.
 */
visionSdk.current.getAllTemplates();
```

### `deleteTemplateWithId`

This method is used to delete a specific template by its ID.

```typescript
/**
 * Deletes a specific template by its ID.
 * @param id - The unique identifier of the template to be deleted.
 */
visionSdk.current.deleteTemplateWithId(id);
```

### `deleteAllTemplates`

This method is used to delete all templates from storage.

```typescript
/**
 * Deletes all templates from storage.
 */
visionSdk.current.deleteAllTemplates();
```

---

## Configuration

Use the `VisionSdkView` component to configure and manage Vision SDK’s features.

### Props

| **Prop**                           | **Type**                                                                                      | **Description**                                                                                                                                                                                                             |
| ---------------------------------- | --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ref`                              | `Function`                                                                                    | Catch the reference of the component to manipulate modes or to access callback functions.                                                                                                                                   |
| `mode`                             | `string: (ocr, barcode, qrcode, barCodeOrQrCode, photo)`                                      | Default mode is ‘barcode’, you can either use other like ocr, qrcode, photo.                                                                                                                                                |
| `captureMode`                      | `string: (manual, auto)`                                                                      | Default captureMode is ‘manual’, you can either use ‘auto’.                                                                                                                                                                 |
| `apiKey`                           | `string`                                                                                      | In order to use the OCR API/MODEL, You must set your API key or either an Auth token..                                                                                                                                      |
| `token`                            | `string`                                                                                      | In order to use the OCR API/MODEL, You must set your API key or either an Auth token.                                                                                                                                       |
| `environment`                      | `string: (sandbox, prod)`                                                                     | If you are using OCR mode then you can set your development environment. (Default env is prod)                                                                                                                              |
| `ocrMode`                          | `string: (cloud, on-device, on-device-with-translation, item_label, document_classification)` | ocrMode defines whether you want to scan using cloud API, on-Device Model or on-Device Model with response translation                                                                                                      |
| `isMultipleScanEnabled`            | `boolean: (true, false)`                                                                      | You can enable or disable multiple scan mode by using this prop. (Default value is false)                                                                                                                                   |
| `isEnableAutoOcrResponseWithImage` | `boolean: (true, false)`                                                                      | You can enable or disable automatic OCR responses that include the image with the OCR result using the isEnableAutoOcrResponseWithImage property. It accepts a boolean value (true or false), with a default value of true. |
| `flash`                            | `boolean: (true, false)`                                                                      | You can turn ON/OFF camera flash by using this prop. (Default value is false)                                                                                                                                               |
| `zoomLevel`                        | `number:  (1 to 5)`                                                                           | You can set the Zoom value. Zoom value is device dependent. It will be vary between 1 to 5.                                                                                                                                 |
| `locationId`                       | `string: (ex# loc_2rpHXFf6ith)`                                                               | By default your location will get from apiKey or either you can set location id.                                                                                                                                            |
| `options`                          | `Object: {x: number, y: string}`                                                              | Option contains different other optional parameters you can provide along with the image.                                                                                                                                   |
| `onDetected`                       | `function`                                                                                    | Callback for detection events.                                                                                                                                                                                              |
| `onBarcodeScan`                    | `function`                                                                                    | Callback when a barcode is scanned.                                                                                                                                                                                         |
| `onOCRScan`                        | `function`                                                                                    | Callback for OCR events.                                                                                                                                                                                                    |
| `onImageCaptured`                  | `function`                                                                                    | Callback for image capture events.                                                                                                                                                                                          |
| `onModelDownloadProgress`          | `function`                                                                                    | Event to monitor model download progress.                                                                                                                                                                                   |
| `onError`                          | `function`                                                                                    | Callback for handling errors.                                                                                                                                                                                               |
| `onCreateTemplate`                 | `function`                                                                                    | Callback event handler that triggers when a template is successfully created.                                                                                                                                               |
| `onGetTemplates`                   | `function`                                                                                    | Callback event handler that triggers when templates are successfully retrieved.                                                                                                                                             |
| `onDeleteTemplateById`             | `function`                                                                                    | Callback event handler that triggers when a template is successfully deleted using its ID.                                                                                                                                  |
| `onDeleteTemplates`                | `function`                                                                                    | Callback event handler that triggers when multiple templates are successfully deleted.                                                                                                                                      |

---

## Event Handlers

The Vision SDK provides several event handlers to handle different types of responses from the SDK. Here is an explanation of each:

### 1. `onDetected`

Triggered when any target (barcode, document, QR code, etc.) is detected.

- **Response**:
  ```json
  {
    "barcode": false,
    "document": false,
    "qrcode": false,
    "text": false
  }
  ```
  - `barcode`, `document`, `qrcode`, `text`: Indicates the detection of each type.

### 2. `onBarcodeScan`

Called when a barcode is successfully scanned.

- **Response**:
  ```json
  {
    "code": []
  }
  ```
  - `code`: The scanned barcode data.

### 3. `onOCRScan`

Triggered when OCR detects and returns text.

- **Response**:
  ```json
  {
    "data": {}
  }
  ```
  - `data`: Text recognized through OCR.

### 4. `onImageCaptured`

Fires when an image is captured.

- **Response**:
  ```json
  {
    "barcodes": [],
    "image": "/path/to/image"
  }
  ```
  - `barcodes`: An array of detected barcodes in the image.
  - `image`: File path to the captured image.

### 5. `onModelDownloadProgress`

Tracks the download progress of the model.

- **Response**:
  ```json
  { "downloadStatus": true, "progress": 1 }
  ```
  - `downloadStatus`: Indicates if downloading is ongoing.
  - `progress`: A number (0 to 1) indicating download progress.

### 6. `onError`

Called when an error occurs.

- **Response**:
  ```json
  {
    "code": 1,
    "message": ""
  }
  ```
  - `code`: Error code.
  - `message`: Description of the error.

## Example Usage

Here's how to set up the Vision SDK in your React Native component:

```tsx
import React, { useEffect, useRef, useState } from 'react';
import VisionSdkView, { VisionSdkRefProps } from 'react-native-vision-sdk';
const ScannerView = () => {
  const visionSdk = useRef<VisionSdkRefProps>(null);

  // Configure Vision SDK settings
  useEffect(() => {
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
    visionSdk?.current?.setObjectDetectionSettings({
      isTextIndicationOn: true,
      isBarCodeOrQRCodeIndicationOn: true,
      isDocumentIndicationOn: true,
      codeDetectionConfidence: 0.5,
      documentDetectionConfidence: 0.5,
      secondsToWaitBeforeDocumentCapture: 2.0,
    });
    visionSdk?.current?.setCameraSettings({
      nthFrameToProcess: 10,
    });
    visionSdk?.current?.startRunningHandler();
  }, []);

  return (
    <VisionSdkView
      ref={visionSdk}
      mode="ocr"
      captureMode="manual"
      ocrMode="cloud"
      environment="your-environment"
      locationId="your-location-id"
      apiKey="your-api-key"
      flash={false}
      zoomLevel={1.8}
      onDetected={(event) => {
        console.log('onDetected', event);
        setDetectedData(event);
      }}
      onOCRScan={(event) => {
        console.log('onOCRScan', event);
        visionSdk.current?.restartScanningHandler();
      }}
      onImageCaptured={(event) => {
        console.log('onImageCaptured', event);
        visionSdk.current?.restartScanningHandler();
      }}
      onModelDownloadProgress={(event) => {
        console.log('onModelDownloadProgress', event);
        if (event.downloadStatus) {
          visionSdk.current?.startRunningHandler();
        }
      }}
      onError={(error) => {
        console.log('onError', error);
      }}
    />
  );
};
```

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

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
