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

---

## Migration from v2.x to v3.0

### Breaking Changes in v3.0

**1. `VisionSdkView` Component Removed**

The `VisionSdkView` component has been completely removed in v3.0. Use `VisionCamera` instead:

```tsx
// ❌ v2.x (No longer available)
import VisionSdkView from 'react-native-vision-sdk';
<VisionSdkView mode="barcode" onBarcodeScan={...} />

// ✅ v3.0
import { VisionCamera } from 'react-native-vision-sdk';
<VisionCamera scanMode="barcode" onBarcodeDetected={...} />
```

**2. Deprecated VisionCore Methods Removed**

| Removed Method | Replacement |
|----------------|-------------|
| `loadOnDeviceModels()` | `downloadModel()` + `loadOCRModel()` |
| `predict()` | `predictWithModule()` |
| `unLoadOnDeviceModels()` | `unloadModel()` / `deleteModel()` |

**3. For v2.x Documentation**

If you're using v2.x and need the old documentation, it's available:
- On npm: `npmjs.com/package/react-native-vision-sdk/v/2.x.x`
- Via CLI: `npm show react-native-vision-sdk@2.0.5 readme`
- GitHub releases tagged with v2.x versions

---

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

Here's an example of setting up the **Vision SDK** for barcode scanning in React Native using `VisionCamera`.

```tsx
import React, { useRef, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { VisionCamera, VisionCameraRefProps } from 'react-native-vision-sdk';

const ScannerView = () => {
  const cameraRef = useRef<VisionCameraRefProps>(null);
  const [lastScannedCode, setLastScannedCode] = useState<string>('');

  return (
    <View style={styles.container}>
      <VisionCamera
        ref={cameraRef}
        scanMode="barcode"
        autoCapture={false}
        enableFlash={false}
        zoomLevel={1.0}
        cameraFacing="back"
        onBarcodeDetected={(event) => {
          console.log('Barcodes detected:', event.codes);
          if (event.codes.length > 0) {
            setLastScannedCode(event.codes[0].scannedCode);
          }
        }}
        onCapture={(event) => {
          console.log('Image captured:', event.image);
          console.log('Barcodes in image:', event.barcodes);
        }}
        onError={(error) => {
          console.error('Camera error:', error.message);
        }}
      />
      {lastScannedCode && (
        <View style={styles.overlay}>
          <Text style={styles.codeText}>Scanned: {lastScannedCode}</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1 },
  overlay: {
    position: 'absolute',
    bottom: 50,
    left: 20,
    right: 20,
    backgroundColor: 'rgba(0,0,0,0.7)',
    padding: 15,
    borderRadius: 8,
  },
  codeText: { color: '#fff', fontSize: 16, textAlign: 'center' },
});
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
| `downloadModel()` | `module: OCRModule, apiKey?, token?, progressCallback?` | Download model to disk with progress tracking → Returns `Promise<void>` |
| `loadOCRModel()` | `module: OCRModule, apiKey?, token?, executionProvider?` | Load model into memory for inference |
| `unloadModel()` | `module: OCRModule` | Remove from memory (files stay on disk) → Returns `boolean` |
| `deleteModel()` | `module: OCRModule` | Permanently delete from disk → Returns `boolean` |
| `isModelLoaded()` | `module: OCRModule` | Check if model is loaded → Returns `boolean` |
| `getLoadedModelCount()` | None | Count of loaded models → Returns `number` |
| `findDownloadedModels()` | None | List all downloaded models → Returns `Promise<ModelInfo[]>` |
| `findDownloadedModel()` | `module: OCRModule` | Find specific model → Returns `Promise<ModelInfo \| null>` |
| `findLoadedModels()` | None | List loaded models → Returns `Promise<ModelInfo[]>` |
| `predictWithModule()` | `module: OCRModule, imagePath, barcodes` | Predict with specific model |
| `cancelDownload()` | `module: OCRModule` | Cancel active download for model → Returns `Promise<boolean>` |

**Note:** `OCRModule` = `{ type: 'shipping_label' | 'item_label' | 'bill_of_lading' | 'document_classification', size: 'nano' | 'micro' | 'small' | 'medium' | 'large' | 'xlarge' }`

**See [MODEL_MANAGEMENT_API_REFERENCE.md](./MODEL_MANAGEMENT_API_REFERENCE.md) for complete API documentation.**

#### Benefits Over Old API

- **Separation of Concerns**: Download and load are separate operations
- **Progress Tracking**: Per-download progress callbacks with unique request IDs
- **Model Caching**: Download once, load multiple times
- **Memory Control**: Unload without deleting, or delete permanently
- **Type Safety**: OCRModule type instead of separate strings
- **Explicit Selection**: Specify exact model for predictions

#### Complete Workflow Example

```typescript
const module = { type: 'shipping_label', size: 'large' };

// Initialize (required on Android)
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });

// Download and load model
await VisionCore.downloadModel(module, apiKey, token);
await VisionCore.loadOCRModel(module, apiKey, token);

// Make predictions
const result = await VisionCore.predictWithModule(module, imageUri, barcodes);

// Cleanup when done
await VisionCore.unloadModel(module);  // From memory
await VisionCore.deleteModel(module);  // From disk (permanent)
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

Switch cameras by updating the `cameraFacing` prop:

```tsx
import React, { useState, useRef } from 'react';
import { Button } from 'react-native';
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

### VisionCamera + VisionCore Architecture

`VisionCamera` is the primary camera component for all scanning needs. For OCR capabilities, combine it with `VisionCore`:

| **Component** | **Purpose** |
|---------------|-------------|
| **VisionCamera** | Camera UI for barcode/QR scanning, image capture |
| **VisionCore** | Headless OCR, model management, cloud predictions |

**Typical Workflow:**
1. Use `VisionCamera` to scan barcodes and capture images
2. Use `VisionCore.downloadModel()` + `loadOCRModel()` to prepare on-device OCR
3. Use `VisionCore.predictWithModule()` for on-device predictions
4. Or use `VisionCore.predictShippingLabelCloud()` etc. for cloud predictions

See the [Headless OCR Example](#headless-ocr-example) and [Model Management API](#model-management-api) sections for complete workflows.

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

The Vision SDK supports headless OCR operations that work independently of the camera component. These methods allow you to perform predictions on existing images without needing the camera view.

```typescript
import { VisionCore } from 'react-native-vision-sdk';

// Set your environment
VisionCore.setEnvironment('sandbox'); // or 'prod'

// Initialize model manager (required on Android)
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });

// Download and load a model
const module = { type: 'shipping_label', size: 'large' };
await VisionCore.downloadModel(module, 'your-api-key');
await VisionCore.loadOCRModel(module, 'your-api-key');

// Make predictions on any image
const result = await VisionCore.predictWithModule(
  module,
  '/path/to/image.jpg',
  ['barcode1', 'barcode2']
);
```

#### Available Headless Methods:

**On-Device Prediction:**
- **`VisionCore.predictWithModule(module, imagePath, barcodes)`** - On-device prediction with explicit model selection

**Cloud Predictions (no model download required):**
- **`VisionCore.predictShippingLabelCloud(imagePath, barcodes, ...options)`** - Cloud shipping label prediction
- **`VisionCore.predictItemLabelCloud(imagePath, ...options)`** - Cloud item label prediction
- **`VisionCore.predictBillOfLadingCloud(imagePath, barcodes, ...options)`** - Cloud bill of lading prediction
- **`VisionCore.predictDocumentClassificationCloud(imagePath, ...options)`** - Cloud document classification

**Hybrid:**
- **`VisionCore.predictWithCloudTransformations(imagePath, barcodes, options)`** - On-device prediction + cloud transformations

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

**NEW in v2.0.6+**: Templates are now **stateless** - the SDK no longer manages template storage. You are responsible for storing and managing templates in your app (e.g., using AsyncStorage).

### What are Templates?

Templates define barcode matching patterns for scanning. Once created, a template contains reference barcodes that can be used to match against scanned codes during OCR operations, improving accuracy and filtering.

### Template Workflow

#### 1. Create Template

Use `createTemplate()` to open the template creation UI. The SDK will return the template data via the `onCreateTemplate` event.

```typescript
// Trigger template creation UI
visionSdk.current.createTemplate();
```

#### 2. Handle Barcode Detection

Listen for barcode detections via the `onBarcodeDetected` callback. In template creation mode, allow users to select barcodes to add to the template.

```typescript
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { TemplateData, TemplateCode } from 'react-native-vision-sdk';

// State for template creation
const [isTemplateMode, setIsTemplateMode] = useState(false);
const [templateCodes, setTemplateCodes] = useState<TemplateCode[]>([]);

// Handle barcode detection - add to template when in template mode
const handleBarcodeDetected = (event: { codes: Array<{ scannedCode: string; symbology: string; boundingBox: any }> }) => {
  if (!isTemplateMode) return;

  // Allow user to tap detected barcodes to add to template
  // See complete example below for UI implementation
};

// Add a barcode to the template
const addBarcodeToTemplate = (code: { scannedCode: string; symbology: string; boundingBox: any }) => {
  setTemplateCodes(prev => {
    const alreadyExists = prev.some(
      c => c.codeString === code.scannedCode && c.codeSymbology === code.symbology
    );
    if (alreadyExists) return prev;
    return [...prev, {
      codeString: code.scannedCode,
      codeSymbology: code.symbology,
      boundingBox: code.boundingBox
    }];
  });
};

// Save the template
const saveTemplate = async () => {
  if (templateCodes.length === 0) return;

  const template: TemplateData = {
    id: `template_${Date.now()}`,
    templateCodes,
  };

  await AsyncStorage.setItem(`template_${template.id}`, JSON.stringify(template));
  setTemplateCodes([]);
  setIsTemplateMode(false);
};
```

#### 3. Apply Template to Scanner

To use a template during scanning, pass it via the `template` prop:

```typescript
const [activeTemplate, setActiveTemplate] = useState<TemplateData | null>(null);

// Load and apply a template
const loadAndApplyTemplate = async (templateId: string) => {
  try {
    const templateJson = await AsyncStorage.getItem(`template_${templateId}`);
    if (templateJson) {
      const template = JSON.parse(templateJson);
      setActiveTemplate(template);
      console.log('Template applied successfully');
    }
  } catch (error) {
    console.error('Failed to load template:', error);
  }
};

// In your component:
<VisionCamera
  ref={cameraRef}
  scanMode="barcode"
  template={activeTemplate}
  onBarcodeDetected={handleBarcodeDetected}
  onCapture={handleCapture}
  // ... other props
/>
```

#### 4. Remove Active Template

To remove the currently active template, set it to `null`:

```typescript
// Remove template from scanner
setActiveTemplate(null);
```

### Complete Example with State Management

```typescript
import React, { useRef, useState, useEffect, useCallback } from 'react';
import { View, Button, FlatList, Text, Alert, TouchableOpacity, Modal } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { VisionCamera, VisionCameraRefProps } from 'react-native-vision-sdk';
import type { TemplateData, TemplateCode } from 'react-native-vision-sdk';

const TEMPLATES_STORAGE_KEY = '@vision_sdk_templates';

const TemplateManagementExample = () => {
  const cameraRef = useRef<VisionCameraRefProps>(null);

  // Template state
  const [savedTemplates, setSavedTemplates] = useState<TemplateData[]>([]);
  const [activeTemplate, setActiveTemplate] = useState<TemplateData | null>(null);

  // Template creation state
  const [isTemplateMode, setIsTemplateMode] = useState(false);
  const [templateCodes, setTemplateCodes] = useState<TemplateCode[]>([]);
  const [showTemplateManager, setShowTemplateManager] = useState(false);

  // Load all templates from storage
  const loadTemplates = useCallback(async () => {
    try {
      const json = await AsyncStorage.getItem(TEMPLATES_STORAGE_KEY);
      if (json) {
        setSavedTemplates(JSON.parse(json));
      }
    } catch (error) {
      console.error('Failed to load templates:', error);
    }
  }, []);

  // Persist templates to storage
  const persistTemplates = useCallback(async (templates: TemplateData[]) => {
    try {
      await AsyncStorage.setItem(TEMPLATES_STORAGE_KEY, JSON.stringify(templates));
      setSavedTemplates(templates);
    } catch (error) {
      console.error('Failed to save templates:', error);
    }
  }, []);

  // Add barcode to template being created
  const handleAddBarcodeToTemplate = useCallback((code: {
    scannedCode: string;
    symbology: string;
    boundingBox: { x: number; y: number; width: number; height: number };
  }) => {
    setTemplateCodes(prev => {
      const alreadyExists = prev.some(
        c => c.codeString === code.scannedCode && c.codeSymbology === code.symbology
      );
      if (alreadyExists) return prev;
      return [...prev, {
        codeString: code.scannedCode,
        codeSymbology: code.symbology,
        boundingBox: code.boundingBox
      }];
    });
  }, []);

  // Save the template
  const handleSaveTemplate = useCallback(async () => {
    if (templateCodes.length === 0) return;

    const newTemplate: TemplateData = {
      id: `template_${Date.now()}`,
      templateCodes,
    };

    const updated = [...savedTemplates, newTemplate];
    await persistTemplates(updated);
    setTemplateCodes([]);
    setIsTemplateMode(false);
    Alert.alert('Template Saved', `Template saved with ${newTemplate.templateCodes.length} code(s).`);
  }, [templateCodes, savedTemplates, persistTemplates]);

  // Apply/remove template
  const handleApplyTemplate = useCallback((template: TemplateData) => {
    if (activeTemplate?.id === template.id) {
      setActiveTemplate(null);
    } else {
      setActiveTemplate(template);
    }
    setShowTemplateManager(false);
  }, [activeTemplate]);

  // Delete a template
  const handleDeleteTemplate = useCallback(async (id: string) => {
    if (activeTemplate?.id === id) {
      setActiveTemplate(null);
    }
    const updated = savedTemplates.filter(t => t.id !== id);
    await persistTemplates(updated);
  }, [savedTemplates, persistTemplates, activeTemplate]);

  // Load templates on mount
  useEffect(() => {
    loadTemplates();
  }, [loadTemplates]);

  return (
    <View style={{ flex: 1 }}>
      <VisionCamera
        ref={cameraRef}
        scanMode="barcode"
        template={activeTemplate}
        onBarcodeDetected={(event) => {
          // In template mode, display barcodes for user to tap and add
          if (isTemplateMode) {
            // Your UI should show bounding boxes that users can tap
            // to call handleAddBarcodeToTemplate(code)
          }
        }}
        onCapture={(event) => console.log('Captured:', event)}
      />

      {/* Template Manager Button */}
      <TouchableOpacity
        style={{ position: 'absolute', top: 50, right: 20 }}
        onPress={() => setShowTemplateManager(true)}
      >
        <Text>Templates</Text>
      </TouchableOpacity>

      {/* Template Creation Panel (when in template mode) */}
      {isTemplateMode && (
        <View style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: 20 }}>
          <Text>Template ({templateCodes.length} codes)</Text>
          <Button title="Save Template" onPress={handleSaveTemplate} disabled={templateCodes.length === 0} />
          <Button title="Cancel" onPress={() => { setTemplateCodes([]); setIsTemplateMode(false); }} />
        </View>
      )}

      {/* Template Manager Modal */}
      <Modal visible={showTemplateManager} animationType="slide" transparent>
        <View style={{ flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.5)' }}>
          <View style={{ backgroundColor: '#1a1a2e', padding: 20, borderTopLeftRadius: 20, borderTopRightRadius: 20 }}>
            <Text style={{ color: '#fff', fontSize: 18 }}>Templates</Text>

            <Button title="+ Create New Template" onPress={() => {
              setShowTemplateManager(false);
              setTemplateCodes([]);
              setIsTemplateMode(true);
            }} />

            <FlatList
              data={savedTemplates}
              keyExtractor={(item) => item.id}
              renderItem={({ item }) => (
                <View style={{ flexDirection: 'row', padding: 10, alignItems: 'center' }}>
                  <Text style={{ flex: 1, color: '#fff' }}>
                    {item.id} ({item.templateCodes.length} codes)
                    {activeTemplate?.id === item.id ? ' (Active)' : ''}
                  </Text>
                  <Button
                    title={activeTemplate?.id === item.id ? 'Remove' : 'Apply'}
                    onPress={() => handleApplyTemplate(item)}
                  />
                  <Button title="Delete" onPress={() => handleDeleteTemplate(item.id)} />
                </View>
              )}
            />

            <Button title="Close" onPress={() => setShowTemplateManager(false)} />
          </View>
        </View>
      </Modal>
    </View>
  );
};
```

### Migration from Old API

**DEPRECATED** (removed in v3.0):
```typescript
// OLD - VisionSdkView methods no longer exist
visionSdk.current.createTemplate();            // REMOVED
visionSdk.current.getAllTemplates();           // REMOVED
visionSdk.current.deleteTemplateWithId(id);    // REMOVED
visionSdk.current.deleteAllTemplates();        // REMOVED
visionSdk.current.setObjectDetectionSettings({ selectedTemplate: json });  // REMOVED

// OLD - VisionSdkView events no longer fire
onCreateTemplate={(event) => {}}               // REMOVED
onGetTemplates={(event) => {}}                 // REMOVED
onDeleteTemplateById={(event) => {}}           // REMOVED
onDeleteTemplates={(event) => {}}              // REMOVED
```

**NEW** (v3.0+):
```typescript
// NEW - Use VisionCamera with template prop
import { VisionCamera } from 'react-native-vision-sdk';
import type { TemplateData } from 'react-native-vision-sdk';

// Template creation is done in React Native (see example above)
const [templateCodes, setTemplateCodes] = useState<TemplateCode[]>([]);

// Apply template via prop
const [activeTemplate, setActiveTemplate] = useState<TemplateData | null>(null);

<VisionCamera
  template={activeTemplate}  // Pass TemplateData object or null
  // ... other props
/>

// Delete template from your storage
await AsyncStorage.removeItem(`template_${id}`);
```

### Key Changes

| Old API (VisionSdkView) | New API (VisionCamera) | Notes |
|-------------------------|------------------------|-------|
| `createTemplate()` method | Build in React Native | Tap barcodes to add to template |
| `getAllTemplates()` | ❌ Removed | Manage storage with AsyncStorage/Redux |
| `deleteTemplateWithId(id)` | ❌ Removed | Delete from your storage manually |
| `deleteAllTemplates()` | ❌ Removed | Clear your storage manually |
| `onCreateTemplate` event | Build template in state | Full control over UI/UX |
| `setObjectDetectionSettings({ selectedTemplate })` | `<VisionCamera template={...} />` | Pass via prop |

---

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
