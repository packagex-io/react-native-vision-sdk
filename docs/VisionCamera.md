# VisionCamera Component

A minimal, performant camera component for barcode scanning, OCR, and photo capture powered by the Vision SDK.

## Features

- 📷 **Photo capture** with quality optimization
- 📊 **Barcode & QR code scanning** with multiple symbologies
- 📝 **OCR (Optical Character Recognition)** support
- 🎯 **Real-time object detection** (text, barcodes, QR codes, documents)
- ⚡ **Flash control** with toggle support
- 🔍 **Zoom control** with adjustable levels
- 🤖 **Auto & Manual capture modes**
- 📏 **Image sharpness scoring** for quality assurance
- 🎨 **Customizable overlay** - render your own UI on top of the camera

## Installation

The VisionCamera component is included in the `react-native-vision-sdk` package:

```bash
npm install react-native-vision-sdk
# or
yarn add react-native-vision-sdk
```

## Basic Usage

```tsx
import React, { useRef } from 'react';
import { VisionCamera, VisionCameraRefProps } from 'react-native-vision-sdk';

function CameraScreen() {
  const cameraRef = useRef<VisionCameraRefProps>(null);

  const handleCapture = (event) => {
    console.log('Image captured:', event.image);
  };

  const handleBarcodeDetected = (event) => {
    console.log('Barcodes:', event.codes);
  };

  return (
    <VisionCamera
      ref={cameraRef}
      scanMode="barcode"
      onCapture={handleCapture}
      onBarcodeDetected={handleBarcodeDetected}
    />
  );
}
```

## Props

### Camera Configuration

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `scanMode` | `VisionCameraScanMode` | `'photo'` | Camera scan mode: `'photo'`, `'barcode'`, `'qrcode'`, `'barcodeorqrcode'`, `'ocr'`, `'barcodesinglecapture'` |
| `autoCapture` | `boolean` | `false` | Enable automatic capture when document/object is detected |
| `enableFlash` | `boolean` | `false` | Enable/disable camera flash |
| `zoomLevel` | `number` | `1.0` | Camera zoom level (1.0 = no zoom) |

### Event Handlers

| Prop | Type | Description |
|------|------|-------------|
| `onCapture` | `(event: VisionCameraCaptureEvent) => void` | Called when an image is captured |
| `onBarcodeDetected` | `(event: VisionCameraBarcodeDetectedEvent) => void` | Called when barcodes/QR codes are detected |
| `onRecognitionUpdate` | `(event: VisionCameraRecognitionUpdateEvent) => void` | Real-time updates on what's detected in viewfinder |
| `onSharpnessScoreUpdate` | `(event: VisionCameraSharpnessScoreEvent) => void` | Real-time image sharpness score (0-100) |
| `onError` | `(event: VisionCameraErrorResult) => void` | Called when an error occurs |

### Layout

| Prop | Type | Description |
|------|------|-------------|
| `children` | `ReactNode` | Custom overlay components to render on top of camera |

## Scan Modes

### Photo Mode
```tsx
<VisionCamera
  scanMode="photo"
  onCapture={(event) => console.log(event.image)}
/>
```

### Barcode Scanning
```tsx
<VisionCamera
  scanMode="barcode"
  onBarcodeDetected={(event) => {
    event.codes.forEach(code => {
      console.log('Code:', code.scannedCode);
      console.log('Type:', code.symbology);
    });
  }}
/>
```

### QR Code Scanning
```tsx
<VisionCamera
  scanMode="qrcode"
  onBarcodeDetected={(event) => {
    console.log('QR Codes:', event.codes);
  }}
/>
```

### OCR (Text Recognition)
```tsx
<VisionCamera
  scanMode="ocr"
  autoCapture={true}
  onCapture={(event) => console.log('Document captured:', event.image)}
  onRecognitionUpdate={(event) => {
    console.log('Text detected:', event.text);
    console.log('Document detected:', event.document);
  }}
/>
```

## Ref Methods

Access camera controls via ref:

```tsx
const cameraRef = useRef<VisionCameraRefProps>(null);

// Capture photo
cameraRef.current?.capture();

// Start/stop camera
cameraRef.current?.start();
cameraRef.current?.stop();

// Toggle flash
cameraRef.current?.toggleFlash(true);

// Set zoom
cameraRef.current?.setZoom(2.0);
```

### Available Methods

| Method | Parameters | Description |
|--------|------------|-------------|
| `capture()` | - | Capture a photo |
| `start()` | - | Start the camera |
| `stop()` | - | Stop the camera |
| `toggleFlash(enabled)` | `boolean` | Enable/disable flash |
| `setZoom(level)` | `number` | Set zoom level |

## Event Types

### VisionCameraCaptureEvent
```typescript
interface VisionCameraCaptureEvent {
  image: string;          // File path to captured image
  nativeImage?: string;   // Native URI (iOS/Android specific)
}
```

### VisionCameraBarcodeDetectedEvent
```typescript
interface VisionCameraBarcodeDetectedEvent {
  codes: VisionCameraBarcodeResult[];
}

interface VisionCameraBarcodeResult {
  scannedCode: string;              // The barcode value
  symbology: string;                // Barcode type (e.g., "QR", "EAN13")
  boundingBox: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  gs1ExtractedInfo?: Record<string, string>;  // GS1 data if available
}
```

### VisionCameraRecognitionUpdateEvent
```typescript
interface VisionCameraRecognitionUpdateEvent {
  text: boolean;         // Text detected in viewfinder
  barcode: boolean;      // Barcode detected
  qrcode: boolean;       // QR code detected
  document: boolean;     // Document detected
}
```

### VisionCameraSharpnessScoreEvent
```typescript
interface VisionCameraSharpnessScoreEvent {
  sharpnessScore: number;  // 0-100 (higher = sharper)
}
```

## Advanced Examples

### Custom Overlay with Controls
```tsx
import { VisionCamera } from 'react-native-vision-sdk';
import { View, Text, TouchableOpacity } from 'react-native';

function CustomCamera() {
  const cameraRef = useRef(null);
  const [sharpness, setSharpness] = useState(0);

  return (
    <VisionCamera
      ref={cameraRef}
      scanMode="ocr"
      onSharpnessScoreUpdate={(event) => setSharpness(event.sharpnessScore)}
    >
      {/* Custom overlay */}
      <View style={styles.overlay}>
        <Text style={styles.sharpnessText}>
          Sharpness: {sharpness.toFixed(2)}
        </Text>
        <TouchableOpacity
          style={styles.captureButton}
          onPress={() => cameraRef.current?.capture()}
        >
          <Text>Capture</Text>
        </TouchableOpacity>
      </View>
    </VisionCamera>
  );
}
```

### Barcode Scanner with Flash Control
```tsx
function BarcodeScanner() {
  const [flash, setFlash] = useState(false);
  const [codes, setCodes] = useState([]);

  return (
    <View style={{ flex: 1 }}>
      <VisionCamera
        scanMode="barcode"
        enableFlash={flash}
        onBarcodeDetected={(event) => {
          setCodes(event.codes);
        }}
      >
        <View style={styles.controls}>
          <TouchableOpacity onPress={() => setFlash(!flash)}>
            <Text>{flash ? '⚡ Flash ON' : '⚡ Flash OFF'}</Text>
          </TouchableOpacity>
        </View>
      </VisionCamera>

      {codes.length > 0 && (
        <View style={styles.results}>
          {codes.map((code, index) => (
            <Text key={index}>{code.scannedCode}</Text>
          ))}
        </View>
      )}
    </View>
  );
}
```

### Document Scanner with Auto-Capture
```tsx
function DocumentScanner() {
  const [documentDetected, setDocumentDetected] = useState(false);

  return (
    <VisionCamera
      scanMode="ocr"
      autoCapture={true}
      onRecognitionUpdate={(event) => {
        setDocumentDetected(event.document);
      }}
      onCapture={(event) => {
        console.log('Document captured:', event.image);
        // Process the captured document
      }}
    >
      {documentDetected && (
        <View style={styles.detectionIndicator}>
          <Text>📄 Document Detected</Text>
        </View>
      )}
    </VisionCamera>
  );
}
```

### Performance Optimization - Throttled Updates
```tsx
function OptimizedCamera() {
  const [sharpness, setSharpness] = useState(0);
  const lastUpdate = useRef(0);
  const throttleMs = 200; // Update UI at most every 200ms

  const handleSharpnessUpdate = (event) => {
    const now = Date.now();
    if (now - lastUpdate.current >= throttleMs) {
      lastUpdate.current = now;
      setSharpness(event.sharpnessScore);
    }
  };

  return (
    <VisionCamera
      scanMode="ocr"
      onSharpnessScoreUpdate={handleSharpnessUpdate}
    >
      <Text>Sharpness: {sharpness.toFixed(2)}</Text>
    </VisionCamera>
  );
}
```

## Platform-Specific Behavior

### iOS
- Auto-starts camera when view is mounted
- Supports all scan modes
- Flash control available on supported devices
- Zoom range depends on device capabilities

### Android
- Auto-starts camera when view is attached to window
- Supports all scan modes
- Flash control available on supported devices
- Zoom range depends on device capabilities

## Performance Tips

1. **Throttle high-frequency events** like `onSharpnessScoreUpdate` (see example above)
2. **Use `autoCapture={false}`** for manual control to reduce processing
3. **Cleanup on unmount**: The camera automatically stops when the component unmounts
4. **Avoid hot reload during development**: Kill and restart the app instead for best results

## Permissions

### iOS
Add to `Info.plist`:
```xml
<key>NSCameraUsageDescription</key>
<string>We need camera access to scan barcodes and documents</string>
```

### Android
Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Request permissions at runtime:
```tsx
import { PERMISSIONS, request } from 'react-native-permissions';

const requestCameraPermission = async () => {
  const permission = Platform.OS === 'ios'
    ? PERMISSIONS.IOS.CAMERA
    : PERMISSIONS.ANDROID.CAMERA;

  const result = await request(permission);
  return result === 'granted';
};
```

## Troubleshooting

### Camera feed not showing
- Ensure camera permissions are granted
- Check that the component has proper dimensions (flex: 1)
- Verify device has a working camera

### Events not firing after hot reload (Development only)
- This is a known React Native limitation with native views
- **Workaround**: Kill and restart the app instead of hot reloading
- **Note**: This does NOT affect production builds

### Barcodes not detected
- Ensure good lighting conditions
- Check that `scanMode` is set to `'barcode'`, `'qrcode'`, or `'barcodeorqrcode'`
- Verify barcode is within camera view and properly focused

### Poor image quality
- Monitor `onSharpnessScoreUpdate` - values above 70 are generally good
- Ensure adequate lighting
- Keep device steady during capture
- Use auto-focus by tapping on screen (platform-dependent)

## License

Part of the `react-native-vision-sdk` package. See package LICENSE for details.
