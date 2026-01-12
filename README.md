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

## Requirements

**React Native New Architecture Required:**
- This package requires React Native **0.74.0+** with the **New Architecture (Fabric + TurboModules)** enabled
- The old architecture is no longer supported

**iOS Development Requirements:**
- **iOS**: 16.0+
- **Xcode**: 14.0 or newer
- VisionSDK is automatically linked via CocoaPods (no manual installation needed)

**Android Development Requirements:**
- **minSdkVersion**: 29+
- **Kotlin**: 1.9.0+

## Installation

Install the Vision SDK for React Native using either `npm` or `yarn`:

```sh
npm install --save react-native-vision-sdk
# or
yarn add react-native-vision-sdk
```

### iOS Setup

1. Navigate to the iOS folder and install pods:
   ```sh
   cd ios && pod install && cd ..
   ```

2. Ensure your `ios/Podfile` has the correct platform version:
   ```ruby
   platform :ios, '16.0'  # Vision SDK requires iOS 16.0+
   ```

### Android Setup

Ensure your `android/build.gradle` file has the minimum required versions:

```gradle
buildscript {
  ext {
    buildToolsVersion = "35.0.0"
    minSdkVersion = 29  // Minimum version required by Vision SDK
    compileSdkVersion = 35
    targetSdkVersion = 35
    ndkVersion = "28.0.13004108"
    kotlinVersion = "1.9.0"
  }
}
```

The Vision SDK dependencies are automatically linked via autolinking - no manual dependency configuration needed!

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

Here's a complete example demonstrating how to use headless OCR with the new Model Management API:

```typescript
import React, { useState } from 'react';
import { View, Button, Text, Alert } from 'react-native';
import { VisionCore } from 'react-native-vision-sdk';

const HeadlessOCRExample = () => {
  const [isModelLoaded, setIsModelLoaded] = useState(false);
  const [prediction, setPrediction] = useState('');
  const [downloadProgress, setDownloadProgress] = useState(0);

  const module = {
    type: 'shipping_label',
    size: 'large'
  };

  // Step 1: Download and load model
  const loadModel = async () => {
    try {
      // Set environment first
      VisionCore.setEnvironment('sandbox'); // Use 'production' for production

      // Initialize model manager (REQUIRED on Android, not needed on iOS)
      // iOS: This is a no-op, exists only for API consistency
      VisionCore.initializeModelManager({
        maxConcurrentDownloads: 2,
        enableLogging: true
      });

      // Download model with progress tracking
      await VisionCore.downloadModel(
        module,
        'your-api-key',
        'your-auth-token',
        (progress) => {
          const percent = (progress.progress * 100).toFixed(1);
          setDownloadProgress(progress.progress);
          console.log(`Download: ${percent}%`);
        }
      );

      // Load into memory
      await VisionCore.loadOCRModel(
        module,
        'your-api-key',
        'your-auth-token'
      );

      setIsModelLoaded(true);
      Alert.alert('Success', 'Model loaded and ready!');
    } catch (error) {
      console.error('Failed to load model:', error);
      Alert.alert('Error', `Failed to load model: ${error.message}`);
    }
  };

  // Step 2: Make predictions with specific model
  const runPrediction = async () => {
    if (!isModelLoaded) {
      Alert.alert('Warning', 'Please load model first');
      return;
    }

    try {
      const imagePath = 'path/to/your/image.jpg'; // Can be local file or URI
      const barcodes = ['1234567890']; // Optional barcode data

      // On-device prediction (fast, offline)
      const result = await VisionCore.predictWithModule(
        module,
        imagePath,
        barcodes
      );
      setPrediction(JSON.stringify(result, null, 2));

    } catch (error) {
      console.error('Prediction failed:', error);
      Alert.alert('Error', 'Prediction failed');
    }
  };

  // Step 3: Cloud-only prediction (no model download required)
  const runCloudPrediction = async () => {
    try {
      const imagePath = 'path/to/your/image.jpg';
      const barcodes = ['1234567890'];

      // Cloud prediction with more accuracy
      const cloudResult = await VisionCore.predictShippingLabelCloud(
        imagePath,
        barcodes,
        'your-auth-token',
        'your-api-key',
        'optional-location-id',
        { /* options */ },
        { /* metadata */ },
        { /* recipient */ },
        { /* sender */ },
        true // shouldResizeImage
      );
      setPrediction(JSON.stringify(cloudResult, null, 2));
    } catch (error) {
      console.error('Cloud prediction failed:', error);
      Alert.alert('Error', 'Cloud prediction failed');
    }
  };

  // Step 4: Cleanup
  const unloadModel = async () => {
    try {
      const unloaded = await VisionCore.unloadModel(module);
      if (unloaded) {
        setIsModelLoaded(false);
        Alert.alert('Success', 'Model unloaded from memory');
      }
    } catch (error) {
      Alert.alert('Error', `Failed to unload: ${error.message}`);
    }
  };

  const deleteModel = async () => {
    try {
      const deleted = await VisionCore.deleteModel(module);
      if (deleted) {
        setIsModelLoaded(false);
        Alert.alert('Success', 'Model deleted from disk');
      }
    } catch (error) {
      Alert.alert('Error', `Failed to delete: ${error.message}`);
    }
  };

  return (
    <View style={{ padding: 20 }}>
      <Button
        title="Download & Load Model"
        onPress={loadModel}
        disabled={isModelLoaded}
      />

      {downloadProgress > 0 && downloadProgress < 1 && (
        <Text>Download Progress: {(downloadProgress * 100).toFixed(1)}%</Text>
      )}

      <Button
        title="Run On-Device Prediction"
        onPress={runPrediction}
        disabled={!isModelLoaded}
      />

      <Button
        title="Run Cloud Prediction"
        onPress={runCloudPrediction}
      />

      <Button
        title="Unload Model"
        onPress={unloadModel}
        disabled={!isModelLoaded}
      />

      <Button
        title="Delete Model"
        onPress={deleteModel}
        disabled={!isModelLoaded}
      />

      <Text style={{ marginTop: 20 }}>
        Model Status: {isModelLoaded ? 'Ready' : 'Not Loaded'}
      </Text>

      {prediction ? (
        <Text style={{ marginTop: 10, fontFamily: 'monospace' }}>
          {prediction}
        </Text>
      ) : null}
    </View>
  );
};

export default HeadlessOCRExample;
```

### Key Benefits of Headless OCR

- **No Camera Dependency**: Process existing images without camera component
- **Fast On-Device Processing**: Local ML models for instant predictions
- **Cloud Enhancement**: Optional cloud processing for higher accuracy
- **Hybrid Workflows**: Combine on-device speed with cloud intelligence
- **Flexible Integration**: Use in any part of your app, not just camera screens

### Model Management API

**NEW in v2.0.2**: The Vision SDK now includes a comprehensive Model Management API for fine-grained control over on-device ML models. This replaces the deprecated `loadModel()` and `unLoadModel()` methods.

#### Quick Start

```typescript
import { VisionCore } from 'react-native-vision-sdk';

const module = { type: 'shipping_label', size: 'large' };

// 1. Initialize (REQUIRED on Android, not needed on iOS - hardcoded no-op)
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });

// 2. Download model with progress tracking
await VisionCore.downloadModel(
  module,
  apiKey,
  token,
  (progress) => console.log(`${(progress.progress * 100).toFixed(1)}%`)
);

// 3. Load into memory
await VisionCore.loadOCRModel(module, apiKey, token);

// 4. Make predictions
const result = await VisionCore.predictWithModule(module, imageUri, barcodes);

// 5. Cleanup
await VisionCore.unloadModel(module);  // From memory
await VisionCore.deleteModel(module);  // From disk (permanent)
```

#### Key Methods

| Method | Arguments | Description |
|--------|-----------|-------------|
| `initializeModelManager()` | `config: { maxConcurrentDownloads?, enableLogging? }` | Initialize model manager (**Android only** - iOS is hardcoded no-op) |
| `isModelManagerInitialized()` | None | Check initialization status (**Android only** - iOS always returns `true`) |
| `downloadModel()` | `module: OCRModule, apiKey, token, progressCallback?` | Download model to disk with progress tracking → Returns `Promise<void>` |
| `loadOCRModel()` | `module: OCRModule, apiKey, token, platform?, executionProvider?` | Load model into memory for inference |
| `unloadModel()` | `module: OCRModule` | Remove from memory (files stay on disk) → Returns `boolean` |
| `deleteModel()` | `module: OCRModule` | Permanently delete from disk → Returns `boolean` |
| `isModelLoaded()` | `module: OCRModule` | Check if model is loaded → Returns `boolean` |
| `getLoadedModelCount()` | None | Count of loaded models → Returns `number` |
| `findDownloadedModels()` | None | List all downloaded models → Returns `Promise<ModelInfo[]>` |
| `findDownloadedModel()` | `module: OCRModule` | Find specific model → Returns `Promise<ModelInfo \| null>` |
| `findLoadedModels()` | None | List loaded models → Returns `Promise<ModelInfo[]>` |
| `predictWithModule()` | `module: OCRModule, imagePath, barcodes` | Predict with specific model |
| `cancelDownload()` | `module: OCRModule` | Cancel active download for model → Returns `Promise<boolean>` |
| `onModelLifecycle()` | `listener: (event) => void` | Listen to lifecycle events → Returns `EmitterSubscription` |

**Note:** `OCRModule` = `{ type: 'shipping_label' | 'item_label' | 'bill_of_lading' | 'document_classification', size: 'nano' | 'micro' | 'small' | 'medium' | 'large' | 'xlarge' }`

**See [MODEL_MANAGEMENT_API_REFERENCE.md](./MODEL_MANAGEMENT_API_REFERENCE.md) for complete API documentation.**

#### Benefits Over Old API

- **Separation of Concerns**: Download and load are separate operations
- **Progress Tracking**: Per-download progress callbacks with unique request IDs
- **Model Caching**: Download once, load multiple times
- **Memory Control**: Unload without deleting, or delete permanently
- **Type Safety**: OCRModule type instead of separate strings
- **Explicit Selection**: Specify exact model for predictions

#### Migration from Deprecated API

**DEPRECATED** (will be removed in v3.0.0):
```typescript
// OLD - Don't use
await VisionCore.loadModel(token, apiKey, 'shipping_label', 'large');
await VisionCore.unLoadModel('shipping_label', true);
const result = await VisionCore.predict(imageUri, barcodes);
```

**New API** (recommended):
```typescript
// NEW - Use this
const module = { type: 'shipping_label', size: 'large' };
await VisionCore.downloadModel(module, apiKey, token);
await VisionCore.loadOCRModel(module, apiKey, token);
const result = await VisionCore.predictWithModule(module, imageUri, barcodes);
await VisionCore.unloadModel(module);
await VisionCore.deleteModel(module);
```

### Advanced Model Management

#### Download Multiple Models Concurrently

```typescript
const models = [
  { type: 'shipping_label', size: 'large' },
  { type: 'item_label', size: 'medium' },
  { type: 'bill_of_lading', size: 'large' }
];

// Configure concurrent downloads
VisionCore.initializeModelManager({ maxConcurrentDownloads: 3 });

// Download all models in parallel with individual progress tracking
const downloads = models.map(module =>
  VisionCore.downloadModel(module, apiKey, token, (progress) => {
    console.log(`${progress.module.type}: ${(progress.progress * 100).toFixed(1)}%`);
  })
);

// Wait for all to complete
await Promise.all(downloads);
console.log('All downloads complete');
```

#### Switch Between Models

```typescript
const model1 = { type: 'shipping_label', size: 'large' };
const model2 = { type: 'item_label', size: 'medium' };

// Ensure both are downloaded (only need to download once)
await VisionCore.downloadModel(model1, apiKey, token);
await VisionCore.downloadModel(model2, apiKey, token);

// Use first model
await VisionCore.loadOCRModel(model1, apiKey, token);
const result1 = await VisionCore.predictWithModule(model1, image1, barcodes1);

// Switch to second model (unload first to free memory)
await VisionCore.unloadModel(model1);
await VisionCore.loadOCRModel(model2, apiKey, token);
const result2 = await VisionCore.predictWithModule(model2, image2, barcodes2);
```

#### Query Model Status

```typescript
// Check if specific model is downloaded
const modelInfo = await VisionCore.findDownloadedModel({
  type: 'shipping_label',
  size: 'large'
});

if (modelInfo) {
  console.log('Model found on disk');
  console.log('Size:', (modelInfo.sizeInBytes / 1024 / 1024).toFixed(2), 'MB');
  console.log('Downloaded:', modelInfo.downloadedAt);
} else {
  console.log('Model not downloaded');
}

// List all downloaded models
const downloaded = await VisionCore.findDownloadedModels();
console.log(`${downloaded.length} model(s) on disk`);

// List loaded models
const loaded = await VisionCore.findLoadedModels();
console.log(`${loaded.length} model(s) in memory`);

// Get count quickly
const count = VisionCore.getLoadedModelCount();
```

#### Listen to Model Lifecycle Events

```typescript
const subscription = VisionCore.onModelLifecycle((event) => {
  console.log('Event:', event.type);
  console.log('Module:', `${event.module.type} (${event.module.size})`);

  switch (event.type) {
    case 'onDownloadStarted':
      console.log('Download started');
      break;
    case 'onDownloadCompleted':
      console.log('Download completed');
      break;
    case 'onModelLoaded':
      console.log('Model loaded into memory');
      break;
    case 'onModelUnloaded':
      console.log('Model unloaded from memory');
      break;
    case 'onModelDeleted':
      console.log('Model deleted from disk');
      break;
  }
});

// Later: unsubscribe
subscription.remove();
```

#### Cancel Downloads

```typescript
const module = { type: 'shipping_label', size: 'large' };

// Start download
VisionCore.downloadModel(
  module,
  apiKey,
  token,
  (progress) => {
    console.log(`Progress: ${progress.progress * 100}%`);

    // Cancel if progress is too slow
    if (progress.progress < 0.1) {
      VisionCore.cancelDownload(module);
    }
  }
);

// Or cancel later by module
const cancelled = await VisionCore.cancelDownload(module);
if (cancelled) {
  console.log('Download cancelled for this model');
}
```

#### Best Practices

**1. Initialize Once (Required on Android Only)**
```typescript
// At app startup (MUST call on Android before any model operations)
// iOS: Not needed - hardcoded no-op for API consistency
VisionCore.initializeModelManager({
  maxConcurrentDownloads: 2,
  enableLogging: __DEV__  // Only in development
});

// Android: Check initialization status
// iOS: Always returns true (hardcoded)
if (!VisionCore.isModelManagerInitialized()) {
  VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });
}
```

**2. Check Before Operations**
```typescript
// Avoid unnecessary downloads
const info = await VisionCore.findDownloadedModel(module);
if (!info) {
  await VisionCore.downloadModel(module, apiKey, token);
}

// Ensure model is loaded before prediction
if (!VisionCore.isModelLoaded(module)) {
  await VisionCore.loadOCRModel(module, apiKey, token);
}
```

**3. Clean Up Unused Models**
```typescript
// When done with a model
await VisionCore.unloadModel(module);  // Frees memory

// When permanently done
await VisionCore.deleteModel(module);  // Frees disk space
```

**4. Handle Errors Gracefully**
```typescript
try {
  await VisionCore.downloadModel(module, apiKey, token);
} catch (error) {
  if (error.code === 'NETWORK_ERROR') {
    Alert.alert('No Connection', 'Please check your internet connection');
  } else if (error.code === 'STORAGE_FULL') {
    Alert.alert('Storage Full', 'Please free up some space');
  } else {
    Alert.alert('Error', error.message);
  }
}
```

## VisionCamera - Minimal Camera Component

**NEW**: `VisionCamera` is a lightweight, minimal camera component designed for barcode scanning and OCR. Unlike the full `VisionSDK` component, it provides a streamlined API without requiring API keys or cloud configuration for basic scanning functionality.

### Basic VisionCamera Example

```tsx
import React, { useRef, useState } from 'react';
import { Button } from 'react-native';
import { VisionCamera, VisionCameraRefProps, CameraFacing } from 'react-native-vision-sdk';

const SimpleScannerView = () => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [cameraFacing, setCameraFacing] = useState<CameraFacing>('back');

  return (
    <>
      <VisionCamera
        ref={cameraRef}
        scanMode="barcode"
        autoCapture={false}
        enableFlash={false}
        zoomLevel={1.0}
        cameraFacing={cameraFacing}
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

      {/* Camera switch button */}
      <Button
        title={`Switch to ${cameraFacing === 'back' ? 'Front' : 'Back'} Camera`}
        onPress={() => setCameraFacing(prev => prev === 'back' ? 'front' : 'back')}
      />
    </>
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
| `cameraFacing` | `'back' \| 'front'` | `'back'` | Camera facing direction - 'back' for rear camera or 'front' for front-facing camera. **iOS**: Fully supported \| **Android**: Placeholder (not yet functional) |
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

**Platform Note:** As of Android VisionSDK v2.4.23, `onBoundingBoxesUpdate` now provides **full barcode metadata** including `scannedCode`, `symbology`, and `gs1ExtractedInfo`, achieving **full feature parity with iOS**!

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

**Note**: Flash, zoom, and camera facing are controlled via props (`enableFlash`, `zoomLevel`, `cameraFacing`), not ref methods. Update the prop values to change these settings dynamically.

### Camera Switching (Front/Back)

Both `VisionCamera` and `VisionSdkView` components support switching between front and back cameras.

#### VisionCamera (Prop-based)

Switch cameras by updating the `cameraFacing` prop:

```tsx
import React, { useState, useRef } from 'react';
import { VisionCamera, VisionCameraRefProps, CameraFacing } from 'react-native-vision-sdk';

const CameraSwitchExample = () => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [cameraFacing, setCameraFacing] = useState<CameraFacing>('back');

  const toggleCamera = () => {
    setCameraFacing(prev => prev === 'back' ? 'front' : 'back');
  };

  return (
    <>
      <VisionCamera
        ref={cameraRef}
        scanMode="barcode"
        cameraFacing={cameraFacing}
        onBarcodeDetected={(event) => {
          console.log('Barcode detected:', event.codes);
        }}
      />
      <Button title="Switch Camera" onPress={toggleCamera} />
    </>
  );
};
```

#### VisionSdkView (Ref-based)

Use the `setCameraSettings` method to switch cameras:

```tsx
import React, { useRef } from 'react';
import VisionSdkView, { VisionSdkRefProps } from 'react-native-vision-sdk';

const LegacyCameraSwitchExample = () => {
  const visionSdkRef = useRef<VisionSdkRefProps>(null);

  const switchToFrontCamera = () => {
    visionSdkRef.current?.setCameraSettings({
      cameraPosition: 2  // 2 = front camera
    });
  };

  const switchToBackCamera = () => {
    visionSdkRef.current?.setCameraSettings({
      cameraPosition: 1  // 1 = back camera
    });
  };

  return (
    <>
      <VisionSdkView
        ref={visionSdkRef}
        mode="barcode"
        onBarcodeScan={(event) => {
          console.log('Barcode scanned:', event);
        }}
      />
      <Button title="Front Camera" onPress={switchToFrontCamera} />
      <Button title="Back Camera" onPress={switchToBackCamera} />
    </>
  );
};
```

**Platform Support:**
- **iOS**: Fully functional - Switches between front and back cameras seamlessly
- **Android**: Placeholder implementation - Prop/method is accepted but camera switching is not yet functional (awaiting VisionSDK Android support)

**Type Export:**
```typescript
import { CameraFacing } from 'react-native-vision-sdk';
// CameraFacing = 'back' | 'front'
```

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

- Simple barcode or QR code scanning
- On-device OCR (when coupled with `VisionCore` for predictions)
- No cloud OCR needed
- Want minimal setup
- Building a lightweight scanner
- Custom UI overlays for scanning region

### When to Use VisionSDK (Full Component)

- Need OCR for shipping labels, bills of lading
- Cloud prediction API integration
- On-device ML model inference
- Complex document processing workflows
- Template management

---

## Platform-Specific Limitations & Differences

While we strive to maintain feature parity across iOS and Android, certain limitations exist due to differences in the underlying native VisionSDK implementations.

### Android Improvements

#### 1. Bounding Box Metadata - **FULL PARITY ACHIEVED** (Android VisionSDK v2.4.23+)

**Affected Events:** `onBoundingBoxesUpdate`, `onIndicationsBoundingBoxes`

As of Android VisionSDK v2.4.23, the Android platform now provides **full barcode metadata** in bounding box events, achieving complete feature parity with iOS!

```typescript
// Both iOS and Android - Full metadata now available on both platforms!
{
  barcodeBoundingBoxes: [
    {
      scannedCode: "1234567890",      // Available on both platforms
      symbology: "CODE_128",          // Available on both platforms
      gs1ExtractedInfo: { /* ... */ }, // Available on both platforms
      boundingBox: { x: 10, y: 20, width: 100, height: 50 }
    }
  ]
}
```

**What Changed:**
- Previous versions (v2.4.22 and earlier) only provided `List<Rect>` coordinates
- Version v2.4.23+ now uses `List<ScannedCodeResult>` with full metadata
- No workarounds needed - both `onBarcodeDetected` and `onBoundingBoxesUpdate` provide complete data

#### 2. Detection Configuration

Some detection config options are iOS-only:
- `detectionConfig.text` - iOS only
- `detectionConfig.document` - iOS only
- `detectionConfig.barcodeConfidence` - iOS only
- `detectionConfig.documentConfidence` - iOS only
- `detectionConfig.documentCaptureDelay` - iOS only

These options are accepted on Android but have no effect.

### Model Management Platform Notes

Both iOS and Android support:
- Granular model unloading (unload specific models)
- Concurrent model downloads
- Model switching without re-download
- Full lifecycle event tracking

Platform-specific differences:
- **Android**: Requires `initializeModelManager()` before operations
- **Android**: Supports execution provider selection (CPU, NNAPI, XNNPACK)
- **iOS**: Initialization methods are optional (no-ops)
- **iOS**: Execution provider not exposed

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
| Unload specific model | Supported | Supported |
| Unload all models | Supported | Supported |
| Delete from disk | Supported | Supported |
| Initialization required | Optional (no-op) | Required |
| Execution provider | Not exposed | CPU, NNAPI, XNNPACK |

### Feature Parity Table

| Feature | iOS | Android (v2.4.23+) |
|---------|-----|---------|
| Barcode Detection | Full support | Full support |
| Bounding Boxes (coordinates) | Full support | Full support |
| Bounding Boxes (metadata) | Full metadata | **Full metadata** |
| Camera Switching (Front/Back) | Full support | Placeholder |
| Error codes | With filtering | Full support |
| Sharpness score | Supported | Supported |
| GS1 extraction | Supported | Supported |
| Model management | Full support | Full support |
| Detection config | Full support | Partial support |

**Legend:**
- Full support/Supported - Feature is fully functional
- Partial support - Feature has limitations
- Placeholder - Feature not yet functional

**Major Improvement:** As of Android VisionSDK v2.4.23, bounding box metadata is now fully supported on both platforms!

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
await VisionCore.loadModel(
  'your-token',
  'your-api-key',
  'shipping_label',
  'large'
);

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

You can customize frames processing and camera position.

```js
visionSdk?.current?.setCameraSettings({
  nthFrameToProcess: 10,  // Process every Nth frame (default: 10)
  cameraPosition: 1,      // 1 = back camera, 2 = front camera
});
```

**Parameters:**
- `nthFrameToProcess` (number): Process every Nth frame for performance optimization (default: 10)
- `cameraPosition` (number): Camera position - `1` for back camera, `2` for front camera. **iOS**: Fully supported | **Android**: Placeholder (not yet functional)

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
