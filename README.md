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
   pod 'VisionSDK', "1.9.8"
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
    kotlinVersion = "1.9.0"
  }
}
```

## Android Setup

In the `build.gradle` file of your Android project, add the following dependencies for Android integration:

```gradle
dependencies {
    // Existing dependencies
    implementation 'com.github.packagexlabs:vision-sdk-android:v2.4.22'
    implementation 'com.github.asadullahilyas:HandyUtils:1.1.6'
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

## Headless OCR Example

Here's a complete example demonstrating how to use the new headless OCR functionality:

```typescript
import React, { useState } from 'react';
import { View, Button, Text, Alert } from 'react-native';
import { VisionCore } from 'react-native-vision-sdk';

const HeadlessOCRExample = () => {
  const [isModelLoaded, setIsModelLoaded] = useState(false);
  const [prediction, setPrediction] = useState('');

  // Step 1: Initialize and load model
  const loadModel = async () => {
    try {
      // Set environment first
      VisionCore.setEnvironment('sandbox'); // Use 'prod' for production

      // Load on-device model with progress tracking
      VisionCore.addListener('onModelDownloadProgress', (progress) => {
        console.log('Download progress:', progress);
        if (progress.isReady) {
          setIsModelLoaded(true);
          Alert.alert('Success', 'Model loaded and ready!');
        }
      });

      await VisionCore.loadModel({
        token: 'your-auth-token',
        apiKey: 'your-api-key',
        modelType: 'shipping_label', // shipping_label, item_label, bill_of_lading, document_classification
        modelSize: 'large' // nano, micro, small, medium, large, xlarge
      });
    } catch (error) {
      console.error('Failed to load model:', error);
      Alert.alert('Error', 'Failed to load model');
    }
  };

  // Step 2: Make predictions
  const runPrediction = async () => {
    if (!isModelLoaded) {
      Alert.alert('Warning', 'Please load model first');
      return;
    }

    try {
      const imagePath = 'path/to/your/image.jpg'; // Can be local file or URI
      const barcodes = ['1234567890']; // Optional barcode data

      // On-device prediction (fast, offline)
      const result = await VisionCore.predict(imagePath, barcodes);
      setPrediction(result);

      // Alternative: Cloud prediction with more accuracy
      // const cloudResult = await VisionCore.predictShippingLabelCloud(
      //   imagePath,
      //   barcodes,
      //   {
      //     token: 'your-token',
      //     apiKey: 'your-api-key',
      //     locationId: 'optional-location-id',
      //     shouldResizeImage: true,
      //     options: { customParam: 'value' },
      //     metadata: { source: 'mobile-app' }
      //   }
      // );

    } catch (error) {
      console.error('Prediction failed:', error);
      Alert.alert('Error', 'Prediction failed');
    }
  };

  // Step 3: Hybrid prediction (on-device + cloud enhancement)
  const runHybridPrediction = async () => {
    try {
      const imagePath = 'path/to/your/image.jpg';
      const barcodes = ['1234567890'];

      // Gets on-device prediction then enhances it with cloud processing
      const enhancedResult = await VisionCore.predictWithCloudTransformations(
        imagePath,
        barcodes,
        {
          token: 'your-token',
          apiKey: 'your-api-key',
          locationId: 'optional-location-id',
          shouldResizeImage: true
        }
      );
      setPrediction(enhancedResult);
    } catch (error) {
      console.error('Hybrid prediction failed:', error);
    }
  };

  return (
    <View style={{ padding: 20 }}>
      <Button
        title="Load Model"
        onPress={loadModel}
        disabled={isModelLoaded}
      />

      <Button
        title="Run On-Device Prediction"
        onPress={runPrediction}
        disabled={!isModelLoaded}
      />

      <Button
        title="Run Hybrid Prediction"
        onPress={runHybridPrediction}
        disabled={!isModelLoaded}
      />

      <Text style={{ marginTop: 20 }}>
        Model Status: {isModelLoaded ? 'Ready' : 'Not Loaded'}
      </Text>

      {prediction ? (
        <Text style={{ marginTop: 10 }}>
          Prediction Result: {prediction}
        </Text>
      ) : null}
    </View>
  );
};

export default HeadlessOCRExample;
```

### Key Benefits of Headless OCR

- **🚀 No Camera Dependency**: Process existing images without camera component
- **⚡ Fast On-Device Processing**: Local ML models for instant predictions
- **🌐 Cloud Enhancement**: Optional cloud processing for higher accuracy
- **🔄 Hybrid Workflows**: Combine on-device speed with cloud intelligence
- **📱 Flexible Integration**: Use in any part of your app, not just camera screens

### Model Management

**NEW**: The Vision SDK now supports unloading on-device models to free up memory and disk space when they're no longer needed.

#### Unloading Models

```typescript
import { VisionCore } from 'react-native-vision-sdk';

// Unload a specific model type
const unloadSpecificModel = async () => {
  try {
    const result = await VisionCore.unLoadModel(
      'shipping_label',  // Model type to unload
      true               // shouldDeleteFromDisk - removes model files from disk
    );
    console.log(result); // "Model unloaded successfully"
  } catch (error) {
    console.error('Failed to unload model:', error);
  }
};

// Unload all models
const unloadAllModels = async () => {
  try {
    const result = await VisionCore.unLoadModel(
      null,  // Pass null to unload all models
      true   // shouldDeleteFromDisk
    );
    console.log(result); // "All models unloaded successfully"
  } catch (error) {
    console.error('Failed to unload models:', error);
  }
};
```

#### VisionCore.unLoadModel Parameters

| **Parameter** | **Type** | **Required** | **Description** |
|---------------|----------|--------------|-----------------|
| `modelType` | `string \| null` | Yes | The type of model to unload (e.g., 'shipping_label', 'bill_of_lading'). Pass `null` to unload all models. |
| `shouldDeleteFromDisk` | `boolean` | No (default: `false`) | If `true`, deletes model files from disk. If `false`, keeps files for faster reloading. |

**Use Cases:**
- Free up memory when switching between different model types
- Clean up disk space after processing
- Prepare for app updates or model version changes
- Optimize app performance by removing unused models

## VisionCamera - Minimal Camera Component

**NEW**: `VisionCamera` is a lightweight, minimal camera component designed for barcode scanning and OCR. Unlike the full `VisionSDK` component, it provides a streamlined API without requiring API keys or cloud configuration for basic scanning functionality.

### Basic VisionCamera Example

```tsx
import React, { useRef } from 'react';
import { VisionCamera, VisionCameraRefProps } from 'react-native-vision-sdk';

const SimpleScannerView = () => {
  const cameraRef = useRef<VisionCameraRefProps>(null);

  return (
    <VisionCamera
      ref={cameraRef}
      scanMode="barcode"
      autoCapture={false}
      enableFlash={false}
      zoomLevel={1.0}
      onBarcodeDetected={(event) => {
        console.log('Barcodes detected:', event.codes);
        // event.codes is an array of detected barcodes with enhanced metadata:
        event.codes.forEach(code => {
          console.log('Value:', code.scannedCode);        // "1234567890"
          console.log('Type:', code.symbology);           // "CODE_128"
          console.log('Position:', code.boundingBox);     // { x, y, width, height }
          console.log('GS1 Data:', code.gs1ExtractedInfo); // { "01": "12345", ... }
        });
      }}
      onCapture={(event) => {
        console.log('Image captured:', event.image);
        console.log('Sharpness score:', event.sharpnessScore); // 0.0 - 1.0
        console.log('Detected barcodes:', event.barcodes);     // Array of barcodes in image
      }}
      onError={(error) => {
        console.error('Error:', error.message);
        console.error('Error code:', error.code); // Numeric error code
      }}
    />
  );
};
```

### VisionCamera Props

| **Prop** | **Type** | **Default** | **Description** |
|----------|----------|-------------|-----------------|
| `scanMode` | `'photo' \| 'barcode' \| 'qrcode' \| 'barcodeOrQrCode' \| 'ocr'` | `'photo'` | Detection mode for the camera |
| `autoCapture` | `boolean` | `false` | Automatically capture when detection is successful |
| `enableFlash` | `boolean` | `false` | Enable/disable camera flash |
| `zoomLevel` | `number` | `1.0` | Camera zoom level (device dependent, typically 1.0-5.0) |
| `scanArea` | `{ x: number, y: number, width: number, height: number }` | `undefined` | Restrict scanning to a specific region (coordinates in dp) |
| `detectionConfig` | `object` | See below | Configure object detection settings |
| `frameSkip` | `number` | `undefined` | Process every Nth frame for performance optimization |

### Detection Config Object

```tsx
detectionConfig={{
  text: true,              // Enable text detection (iOS only)
  barcode: true,           // Enable barcode detection
  document: true,          // Enable document detection (iOS only)
  barcodeConfidence: 0.5,  // Barcode detection confidence (0-1, iOS only)
  documentConfidence: 0.5, // Document confidence (0-1, iOS only)
  documentCaptureDelay: 2.0 // Delay before auto-capture (seconds, iOS only)
}}
```

### VisionCamera Events

| **Event** | **Description** | **Payload** |
|-----------|-----------------|-------------|
| `onBarcodeDetected` | Fired when barcode(s) are detected | `{ codes: Array<BarcodeResult> }` - See details below |
| `onCapture` | Fired when image is captured | `{ image: string, nativeImage: string, sharpnessScore?: number, barcodes?: Array<BarcodeResult> }` |
| `onRecognitionUpdate` | Continuous updates of detected objects | `{ text: boolean, barcode: boolean, qrcode: boolean, document: boolean }` |
| `onSharpnessScoreUpdate` | Image sharpness score updates | `{ sharpnessScore: number }` |
| `onBoundingBoxesUpdate` | Bounding boxes for detected objects | `{ barcodeBoundingBoxes: Array<DetectedCodeBoundingBox>, qrCodeBoundingBoxes: Array<DetectedCodeBoundingBox>, documentBoundingBox: BoundingBox }` |
| `onError` | Error events | `{ message: string, code?: number }` |

#### Enhanced Event Payloads

##### BarcodeResult Interface
```typescript
interface BarcodeResult {
  scannedCode: string;           // The barcode value
  symbology: string;             // Barcode type (e.g., "CODE_128", "QR_CODE", "EAN_13")
  boundingBox: {                 // Position of the barcode in the frame
    x: number;
    y: number;
    width: number;
    height: number;
  };
  gs1ExtractedInfo?: {           // GS1 data if applicable
    [key: string]: string;
  };
}
```

##### DetectedCodeBoundingBox Interface (for onBoundingBoxesUpdate)
```typescript
interface DetectedCodeBoundingBox {
  scannedCode: string;           // iOS: actual barcode value | Android: empty string ""
  symbology: string;             // iOS: barcode type | Android: empty string ""
  gs1ExtractedInfo: {            // iOS: actual GS1 data | Android: empty object {}
    [key: string]: string;
  };
  boundingBox: {                 // Available on both platforms
    x: number;
    y: number;
    width: number;
    height: number;
  };
}
```

**Platform Note:** On Android, `onBoundingBoxesUpdate` only provides bounding box coordinates. The `scannedCode`, `symbology`, and `gs1ExtractedInfo` fields will be empty due to Android VisionSDK limitations. For full barcode metadata on Android, use the `onBarcodeDetected` event instead.

##### Capture Event Enhancements
The `onCapture` event now includes:
- **`sharpnessScore`** (number, 0-1): Image quality score - higher values indicate sharper images
- **`barcodes`** (Array<BarcodeResult>): Any barcodes detected in the captured image, available in both OCR and barcode modes

##### Error Event Enhancements
The `onError` event now includes:
- **`code`** (number, optional): Numeric error code for programmatic handling
- **iOS Note:** Error codes 13, 14, 15, and 16 are automatically filtered and won't trigger the `onError` callback

### VisionCamera Methods (via ref)

The camera **starts automatically** when mounted - you don't need to call `start()` manually in most cases.

```tsx
const cameraRef = useRef<VisionCameraRefProps>(null);

// Capture image manually (when autoCapture is false)
cameraRef.current?.capture();

// Start camera (only needed if you previously stopped it)
// Useful when camera screen stays in navigation stack but goes to background
cameraRef.current?.start();

// Stop camera (e.g., when screen goes to background)
// Useful to pause camera when not actively scanning
cameraRef.current?.stop();
```

**Note**: Flash and zoom are controlled via props (`enableFlash`, `zoomLevel`), not ref methods. Update the prop values to change these settings dynamically.

### Advanced VisionCamera Example with Scan Area

```tsx
import React, { useRef, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import { VisionCamera, VisionCameraRefProps } from 'react-native-vision-sdk';

const AdvancedScannerView = () => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [flashEnabled, setFlashEnabled] = useState(false);

  return (
    <View style={styles.container}>
      <VisionCamera
        ref={cameraRef}
        scanMode="barcode"
        autoCapture={true}
        enableFlash={flashEnabled}
        zoomLevel={1.5}
        // Restrict scanning to center region (200x100 dp area)
        scanArea={{
          x: 100,
          y: 300,
          width: 200,
          height: 100
        }}
        detectionConfig={{
          barcode: true,
          barcodeConfidence: 0.7
        }}
        frameSkip={10} // Process every 10th frame for better performance
        onBarcodeDetected={(event) => {
          console.log(`Detected ${event.codes.length} barcode(s)`);
          event.codes.forEach(code => {
            console.log(`Type: ${code.symbology}, Value: ${code.scannedCode}`);
            console.log(`Position:`, code.boundingBox);
          });
        }}
        onBoundingBoxesUpdate={(event) => {
          // Real-time bounding box updates for visual overlays
          // Note: On Android, scannedCode/symbology will be empty strings
          event.barcodeBoundingBoxes.forEach(box => {
            console.log('Barcode position:', box.boundingBox);
            // iOS: box.scannedCode and box.symbology available
            // Android: Use onBarcodeDetected for metadata
          });
        }}
        onError={(error) => {
          console.error('Error:', error.message, 'Code:', error.code);
        }}
      />

      {/* Overlay UI for scan area visualization */}
      <View style={[styles.scanAreaOverlay, {
        left: 100,
        top: 300,
        width: 200,
        height: 100
      }]} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  scanAreaOverlay: {
    position: 'absolute',
    borderWidth: 2,
    borderColor: '#00FF00',
    backgroundColor: 'transparent'
  }
});
```

### Key Differences: VisionCamera vs VisionSDK

| **Feature** | **VisionCamera** | **VisionSDK** |
|-------------|------------------|---------------|
| **Setup Complexity** | Minimal - no API keys needed | Requires API key/token for cloud features |
| **Use Case** | Simple barcode/QR scanning | Full OCR + cloud predictions |
| **Bundle Size** | Lightweight | Full-featured |
| **Configuration** | Props-based | Imperative methods + props |
| **Cloud Integration** | No | Yes (shipping labels, BOL, etc.) |
| **Offline Capability** | Full (barcode/QR only) | Partial (requires model download for OCR) |

### When to Use VisionCamera

- ✅ Simple barcode or QR code scanning
- ✅ On-device OCR (when coupled with `VisionCore` for predictions)
- ✅ No cloud OCR needed
- ✅ Want minimal setup
- ✅ Building a lightweight scanner
- ✅ Custom UI overlays for scanning region

### When to Use VisionSDK (Full Component)

- ✅ Need OCR for shipping labels, bills of lading
- ✅ Cloud prediction API integration
- ✅ On-device ML model inference
- ✅ Complex document processing workflows
- ✅ Template management

---

## Platform-Specific Limitations & Differences

While we strive to maintain feature parity across iOS and Android, certain limitations exist due to differences in the underlying native VisionSDK implementations.

### Android Limitations

#### 1. Bounding Box Metadata (Android VisionSDK 2.4.22)

**Affected Events:** `onBoundingBoxesUpdate`, `onIndicationsBoundingBoxes`

The Android VisionSDK returns only bounding box coordinates (`List<Rect>`) without barcode metadata. As a result:

```typescript
// iOS - Full metadata available
{
  barcodeBoundingBoxes: [
    {
      scannedCode: "1234567890",      // ✅ Available
      symbology: "CODE_128",          // ✅ Available
      gs1ExtractedInfo: { /* ... */ }, // ✅ Available
      boundingBox: { x: 10, y: 20, width: 100, height: 50 }
    }
  ]
}

// Android - Only coordinates available
{
  barcodeBoundingBoxes: [
    {
      scannedCode: "",                // ❌ Empty string
      symbology: "",                  // ❌ Empty string
      gs1ExtractedInfo: {},           // ❌ Empty object
      boundingBox: { x: 10, y: 20, width: 100, height: 50 } // ✅ Available
    }
  ]
}
```

**Workaround:** Use the `onBarcodeDetected` event on Android, which provides full barcode metadata including `scannedCode`, `symbology`, `boundingBox`, and `gs1ExtractedInfo`.

```typescript
// Recommended approach for Android
onBarcodeDetected={(event) => {
  event.codes.forEach(code => {
    console.log('Barcode:', code.scannedCode);
    console.log('Type:', code.symbology);
    console.log('Position:', code.boundingBox);
    console.log('GS1:', code.gs1ExtractedInfo);
  });
}}
```

#### 2. Detection Configuration

Some detection config options are iOS-only:
- `detectionConfig.text` - iOS only
- `detectionConfig.document` - iOS only
- `detectionConfig.barcodeConfidence` - iOS only
- `detectionConfig.documentConfidence` - iOS only
- `detectionConfig.documentCaptureDelay` - iOS only

These options are accepted on Android but have no effect.

### iOS Limitations

#### 1. Error Code Filtering

**Affected Events:** `onError`

iOS automatically filters error codes 13, 14, 15, and 16 to prevent excessive error callbacks during normal operation. These errors will not trigger the `onError` callback.

```typescript
// Error codes 13, 14, 15, 16 are silently filtered on iOS
onError={(error) => {
  console.log('Error code:', error.code); // Will never be 13, 14, 15, or 16
  console.log('Error message:', error.message);
}}
```

### Model Management Differences

| Feature | iOS | Android |
|---------|-----|---------|
| Implementation | `OnDeviceOCRManager` | `OnDeviceOCRManagerSingleton` |
| Unload specific model | ✅ Supported | ⚠️ Destroys all models |
| Unload all models | ✅ Supported | ✅ Supported |
| Delete from disk | ✅ Supported | ⚠️ Limited |

**Android Note:** Due to the singleton pattern, calling `VisionCore.unLoadModel()` with a specific model type will still destroy the entire singleton instance, effectively unloading all models.

### Feature Parity Table

| Feature | iOS | Android |
|---------|-----|---------|
| Barcode Detection | ✅ Full support | ✅ Full support |
| Bounding Boxes (coordinates) | ✅ Full support | ✅ Full support |
| Bounding Boxes (metadata) | ✅ Full metadata | ⚠️ Coordinates only |
| Error codes | ✅ With filtering | ✅ Full support |
| Sharpness score | ✅ Supported | ✅ Supported |
| GS1 extraction | ✅ Supported | ✅ Supported |
| Model management | ✅ Granular | ⚠️ All-or-nothing |
| Detection config | ✅ Full support | ⚠️ Partial support |

**Legend:**
- ✅ Fully supported
- ⚠️ Limited or different behavior
- ❌ Not available

---

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

### Headless OCR Workflows

**NEW**: The Vision SDK now supports headless OCR operations that work independently of the camera component. These methods allow you to perform predictions on existing images without needing the camera view.

```typescript
import { VisionCore } from 'react-native-vision-sdk';

// First, set your environment and load a model
VisionCore.setEnvironment('sandbox'); // or 'prod'
await VisionCore.loadModel({
  token: 'your-token',
  apiKey: 'your-api-key',
  modelType: 'shipping_label',
  modelSize: 'large'
});

// Now you can make predictions on any image
const result = await VisionCore.predict('/path/to/image.jpg', ['barcode1', 'barcode2']);
```

#### Available Headless Methods:

- **`VisionCore.predict(imagePath, barcodes)`** - On-device prediction
- **`VisionCore.predictShippingLabelCloud(imagePath, barcodes, options)`** - Cloud shipping label prediction
- **`VisionCore.predictItemLabelCloud(imagePath, options)`** - Cloud item label prediction
- **`VisionCore.predictBillOfLadingCloud(imagePath, barcodes, options)`** - Cloud bill of lading prediction
- **`VisionCore.predictDocumentClassificationCloud(imagePath, options)`** - Cloud document classification
- **`VisionCore.predictWithCloudTransformations(imagePath, barcodes, options)`** - Hybrid on-device + cloud prediction

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
