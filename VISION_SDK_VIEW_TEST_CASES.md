# VisionSdkView Component - Test Cases Documentation

**Version:** 1.6.0 (React Native 0.76 - Fabric Architecture)
**Last Updated:** 2025-01-17
**Architecture:** Fabric-only (New Architecture)

---

## Test Environment Setup

### Prerequisites
- React Native 0.76+
- New Architecture enabled
- iOS 15.0+ / Android API 21+
- Valid API key and token from PackageX Vision SDK

### Test Configurations
- **iOS Simulator:** iPhone 16 Pro (iOS 18+)
- **Android Emulator:** Pixel 8 (API 34+)
- **Physical Devices:** Recommended for camera-related tests

---

## Component Overview

### Summary
- **Total Props:** 15
- **Total Commands:** 22
- **Total Events:** 13
- **Architecture:** Fabric with Codegen

---

## 1. Props Testing

### 1.1 String Props

#### Test Case 1.1.1: Mode Prop
**Priority:** High
**Props:** `mode`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.1.1a | Set mode to 'barcode' | `mode="barcode"` | Camera initializes in barcode scanning mode | ⬜ |
| P1.1.1b | Set mode to 'ocr' | `mode="ocr"` | Camera initializes in OCR mode | ⬜ |
| P1.1.1c | Set mode to 'photo' | `mode="photo"` | Camera initializes in photo mode | ⬜ |
| P1.1.1d | Change mode dynamically | Toggle between modes | Mode switches without crash | ⬜ |

#### Test Case 1.1.2: Capture Mode Prop
**Priority:** High
**Props:** `captureMode`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.1.2a | Set captureMode to 'manual' | `captureMode="manual"` | Requires manual capture trigger | ⬜ |
| P1.1.2b | Set captureMode to 'auto' | `captureMode="auto"` | Auto-captures on detection | ⬜ |
| P1.1.2c | Change captureMode at runtime | Toggle modes | Behavior updates correctly | ⬜ |

#### Test Case 1.1.3: API Credentials
**Priority:** Critical
**Props:** `apiKey`, `token`, `locationId`, `environment`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.1.3a | Valid API key and token | Valid credentials | SDK initializes successfully | ⬜ |
| P1.1.3b | Invalid API key | Invalid key | Error event emitted | ⬜ |
| P1.1.3c | Missing token | Empty/null token | Falls back to default or errors | ⬜ |
| P1.1.3d | Environment: 'production' | `environment="production"` | Uses production endpoints | ⬜ |
| P1.1.3e | Environment: 'development' | `environment="development"` | Uses dev endpoints | ⬜ |

#### Test Case 1.1.4: OCR Configuration
**Priority:** High
**Props:** `ocrMode`, `ocrType`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.1.4a | OCR mode: 'cloud' | `ocrMode="cloud"` | Uses cloud OCR | ⬜ |
| P1.1.4b | OCR mode: 'on_device' | `ocrMode="on_device"` | Uses on-device OCR | ⬜ |
| P1.1.4c | OCR type: 'shipping_label' | `ocrType="shipping_label"` | Detects shipping labels | ⬜ |
| P1.1.4d | OCR type: 'item_label' | `ocrType="item_label"` | Detects item labels | ⬜ |
| P1.1.4e | OCR type: 'bill_of_lading' | `ocrType="bill_of_lading"` | Detects BOL documents | ⬜ |

#### Test Case 1.1.5: Model Execution Provider (Android)
**Priority:** Medium
**Props:** `modelExecutionProviderAndroid`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.1.5a | Provider: 'NNAPI' | `modelExecutionProviderAndroid="NNAPI"` | Uses NNAPI on Android | ⬜ |
| P1.1.5b | Provider: 'GPU' | `modelExecutionProviderAndroid="GPU"` | Uses GPU acceleration | ⬜ |

### 1.2 Boolean Props

#### Test Case 1.2.1: Image Resize Flag
**Priority:** Medium
**Props:** `shouldResizeImage`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.2.1a | Resize enabled | `shouldResizeImage={true}` | Images resized before upload | ⬜ |
| P1.2.1b | Resize disabled | `shouldResizeImage={false}` | Original size maintained | ⬜ |

#### Test Case 1.2.2: Flash Control
**Priority:** High
**Props:** `flash`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.2.2a | Flash enabled | `flash={true}` | Camera flash turns on | ⬜ |
| P1.2.2b | Flash disabled | `flash={false}` | Camera flash turns off | ⬜ |
| P1.2.2c | Toggle flash at runtime | Switch value | Flash toggles immediately | ⬜ |

#### Test Case 1.2.3: Auto OCR with Image
**Priority:** Medium
**Props:** `isEnableAutoOcrResponseWithImage`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.2.3a | Auto OCR enabled | `isEnableAutoOcrResponseWithImage={true}` | OCR includes image in response | ⬜ |
| P1.2.3b | Auto OCR disabled | `isEnableAutoOcrResponseWithImage={false}` | OCR without image | ⬜ |

#### Test Case 1.2.4: Multiple Scan Mode
**Priority:** High
**Props:** `isMultipleScanEnabled`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.2.4a | Multiple scan enabled | `isMultipleScanEnabled={true}` | Can scan multiple items | ⬜ |
| P1.2.4b | Multiple scan disabled | `isMultipleScanEnabled={false}` | Single scan only | ⬜ |

### 1.3 Number Props

#### Test Case 1.3.1: Zoom Level
**Priority:** High
**Props:** `zoomLevel`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.3.1a | Zoom level: 1.0 (default) | `zoomLevel={1.0}` | No zoom applied | ⬜ |
| P1.3.1b | Zoom level: 2.0 | `zoomLevel={2.0}` | 2x zoom applied | ⬜ |
| P1.3.1c | Zoom level: 0.5 | `zoomLevel={0.5}` | Handles gracefully or errors | ⬜ |
| P1.3.1d | Zoom level: 5.0 (max) | `zoomLevel={5.0}` | Maximum zoom or clamped | ⬜ |
| P1.3.1e | Dynamic zoom change | Increment/decrement | Zoom updates smoothly | ⬜ |

### 1.4 Object Props (JSON)

#### Test Case 1.4.1: Options JSON
**Priority:** Medium
**Props:** `optionsJson`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| P1.4.1a | Valid JSON options | `optionsJson={JSON.stringify({...})}` | Options parsed and applied | ⬜ |
| P1.4.1b | Invalid JSON | `optionsJson="invalid"` | Handles gracefully, logs error | ⬜ |
| P1.4.1c | Empty JSON | `optionsJson="{}"` | Uses defaults | ⬜ |

---

## 2. Commands Testing

### 2.1 Basic Camera Commands

#### Test Case 2.1.1: Capture Image
**Priority:** Critical
**Command:** `captureImage()`

| Test ID | Test Case | Steps | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.1.1a | Capture in photo mode | 1. Set mode='photo'<br>2. Call captureImage() | onImageCaptured event fires with URI | ⬜ |
| C2.1.1b | Capture in barcode mode | 1. Set mode='barcode'<br>2. Call captureImage() | Image captured with barcode data | ⬜ |
| C2.1.1c | Capture in OCR mode | 1. Set mode='ocr'<br>2. Call captureImage() | Image captured with OCR data | ⬜ |
| C2.1.1d | Rapid successive captures | Call captureImage() 5 times quickly | All captures complete or queued | ⬜ |
| C2.1.1e | Capture without camera permission | Deny permission, call command | Error event emitted | ⬜ |

#### Test Case 2.1.2: Start/Stop Running
**Priority:** High
**Commands:** `startRunning()`, `stopRunning()`

| Test ID | Test Case | Steps | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.1.2a | Start camera | Call startRunning() | Camera preview starts | ⬜ |
| C2.1.2b | Stop camera | Call stopRunning() | Camera preview stops | ⬜ |
| C2.1.2c | Toggle start/stop | Alternate calls | Camera responds correctly | ⬜ |
| C2.1.2d | Multiple start calls | Call startRunning() 3 times | Handles idempotently | ⬜ |
| C2.1.2e | Stop when already stopped | Call stopRunning() twice | No crash or error | ⬜ |

#### Test Case 2.1.3: Restart Scanning
**Priority:** High
**Command:** `restartScanning()`

| Test ID | Test Case | Steps | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.1.3a | Restart in barcode mode | 1. Scan barcode<br>2. Call restartScanning() | Clears previous scan, ready for new | ⬜ |
| C2.1.3b | Restart in OCR mode | 1. Perform OCR<br>2. Call restartScanning() | Clears OCR results | ⬜ |
| C2.1.3c | Restart before any scan | Call on fresh mount | No error, ready to scan | ⬜ |

### 2.2 Configuration Commands

#### Test Case 2.2.1: Set Metadata
**Priority:** Medium
**Command:** `setMetaData(metaDataJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.1a | Valid metadata JSON | `{userId: "123", session: "abc"}` | Metadata stored and sent with requests | ⬜ |
| C2.2.1b | Invalid JSON | `"not json"` | Handles gracefully, logs error | ⬜ |
| C2.2.1c | Empty metadata | `{}` | Clears existing metadata | ⬜ |
| C2.2.1d | Update metadata | Call twice with different data | Latest metadata used | ⬜ |

#### Test Case 2.2.2: Set Recipient
**Priority:** Medium
**Command:** `setRecipient(recipientJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.2a | Valid recipient | `{name: "John", address: "..."}` | Recipient data stored | ⬜ |
| C2.2.2b | Partial recipient | `{name: "John"}` | Accepts partial data | ⬜ |
| C2.2.2c | Empty recipient | `{}` | Clears recipient | ⬜ |

#### Test Case 2.2.3: Set Sender
**Priority:** Medium
**Command:** `setSender(senderJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.3a | Valid sender | `{name: "Alice", address: "..."}` | Sender data stored | ⬜ |
| C2.2.3b | Partial sender | `{name: "Alice"}` | Accepts partial data | ⬜ |
| C2.2.3c | Empty sender | `{}` | Clears sender | ⬜ |

#### Test Case 2.2.4: Configure On-Device Model
**Priority:** High
**Command:** `configureOnDeviceModel(configsJson, token, apiKey)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.4a | Valid model config | Valid JSON, credentials | Model downloads, onModelDownloadProgress fires | ⬜ |
| C2.2.4b | Invalid credentials | Invalid token/apiKey | Error event emitted | ⬜ |
| C2.2.4c | Network unavailable | Offline mode | Download fails gracefully | ⬜ |
| C2.2.4d | Model already downloaded | Reconfigure with same | Skips download or updates | ⬜ |

#### Test Case 2.2.5: Set Focus Settings
**Priority:** Medium
**Command:** `setFocusSettings(focusSettingsJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.5a | Auto focus enabled | `{autoFocus: true}` | Camera uses auto-focus | ⬜ |
| C2.2.5b | Manual focus | `{autoFocus: false, focusDepth: 0.5}` | Fixed focus applied | ⬜ |
| C2.2.5c | Invalid settings | `{unknown: true}` | Ignores or uses defaults | ⬜ |

#### Test Case 2.2.6: Set Object Detection Settings
**Priority:** High
**Command:** `setObjectDetectionSettings(settingsJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.6a | Enable barcode detection | `{enableBarcode: true}` | Detects barcodes, fires onBoundingBoxesDetected | ⬜ |
| C2.2.6b | Enable document detection | `{enableDocument: true}` | Detects documents | ⬜ |
| C2.2.6c | Disable all detection | `{enableBarcode: false, enableDocument: false}` | No detection events | ⬜ |
| C2.2.6d | Invalid settings | Invalid JSON | Handles gracefully | ⬜ |

#### Test Case 2.2.7: Set Camera Settings
**Priority:** Medium
**Command:** `setCameraSettings(cameraSettingsJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.2.7a | Set exposure | `{exposure: 0.5}` | Camera exposure adjusts | ⬜ |
| C2.2.7b | Set ISO | `{iso: 400}` | Camera ISO changes | ⬜ |
| C2.2.7c | Set white balance | `{whiteBalance: "auto"}` | White balance mode applied | ⬜ |
| C2.2.7d | Multiple settings | Multiple properties | All settings applied | ⬜ |

### 2.3 Cloud Prediction Commands

#### Test Case 2.3.1: Get Prediction (Basic)
**Priority:** High
**Command:** `getPrediction(image, barcodeJson)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.1a | Valid image path | file:// URI + barcode array | onOCRScan fires with results | ⬜ |
| C2.3.1b | Invalid image path | "invalid/path" | onError event fires | ⬜ |
| C2.3.1c | Empty barcode array | `[]` | Prediction runs without barcode | ⬜ |
| C2.3.1d | Network timeout | Slow network | Timeout error or retry | ⬜ |

#### Test Case 2.3.2: Get Prediction with Cloud Transformations
**Priority:** High
**Command:** `getPredictionWithCloudTransformations(...)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.2a | Full parameters | All params provided | Cloud processing with transformations | ⬜ |
| C2.3.2b | With metadata | Include metadata JSON | Metadata sent to cloud | ⬜ |
| C2.3.2c | With recipient/sender | Include recipient + sender | Address data processed | ⬜ |
| C2.3.2d | shouldResizeImage=true | Set flag to true | Image resized before upload | ⬜ |
| C2.3.2e | shouldResizeImage=false | Set flag to false | Original image sent | ⬜ |

#### Test Case 2.3.3: Get Prediction Shipping Label Cloud
**Priority:** Critical
**Command:** `getPredictionShippingLabelCloud(...)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.3a | Valid shipping label image | Clear label image | OCR extracts tracking, addresses | ⬜ |
| C2.3.3b | Blurry shipping label | Low quality image | onSharpnessScore warns or fails | ⬜ |
| C2.3.3c | Non-label image | Random photo | onError or empty results | ⬜ |
| C2.3.3d | With barcode overlay | Image + barcode data | Combines barcode + OCR | ⬜ |

#### Test Case 2.3.4: Get Prediction Bill of Lading Cloud
**Priority:** High
**Command:** `getPredictionBillOfLadingCloud(...)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.4a | Valid BOL document | Clear BOL image | Extracts reference numbers, dates | ⬜ |
| C2.3.4b | Multi-page BOL | Single page of multi-page | Processes single page | ⬜ |
| C2.3.4c | Partial BOL | Cropped document | Extracts available fields | ⬜ |

#### Test Case 2.3.5: Get Prediction Item Label Cloud
**Priority:** High
**Command:** `getPredictionItemLabelCloud(...)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.5a | Valid item label | Clear product label | Extracts SKU, name, supplier | ⬜ |
| C2.3.5b | Multiple items in frame | 2+ labels visible | Focuses on primary or errors | ⬜ |
| C2.3.5c | Barcode + label | Product with barcode | Combines both data sources | ⬜ |

#### Test Case 2.3.6: Get Prediction Document Classification Cloud
**Priority:** Medium
**Command:** `getPredictionDocumentClassificationCloud(...)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.6a | Shipping label | Label image | Classifies as "shipping_label" | ⬜ |
| C2.3.6b | Bill of lading | BOL image | Classifies as "bill_of_lading" | ⬜ |
| C2.3.6c | Item label | Product label | Classifies as "item_label" | ⬜ |
| C2.3.6d | Unknown document | Random paper | Returns "unknown" or best guess | ⬜ |

#### Test Case 2.3.7: Report Error
**Priority:** Critical
**Command:** `reportError(dataJson, token, apiKey)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.3.7a | Valid error report | Shipping label error data | Report sent successfully | ⬜ |
| C2.3.7b | Missing token | Empty token | Uses fallback from props | ⬜ |
| C2.3.7c | Missing apiKey | Empty apiKey | Uses fallback from constants | ⬜ |
| C2.3.7d | Invalid JSON | Malformed dataJson | onError event fires | ⬜ |
| C2.3.7e | All error types | shipping_label, item_label, BOL, DC | All types accepted | ⬜ |

### 2.4 Template Commands

#### Test Case 2.4.1: Create Template
**Priority:** High
**Command:** `createTemplate()`

| Test ID | Test Case | Steps | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.4.1a | Create new template | 1. Call createTemplate()<br>2. Capture template image | onCreateTemplate fires with success | ⬜ |
| C2.4.1b | Create without capture | Call before camera ready | onError event fires | ⬜ |
| C2.4.1c | Create duplicate | Create same template twice | Second overwrites or errors | ⬜ |

#### Test Case 2.4.2: Get All Templates
**Priority:** High
**Command:** `getAllTemplates()`

| Test ID | Test Case | Steps | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.4.2a | Get existing templates | 1. Create 3 templates<br>2. Call getAllTemplates() | onGetTemplates fires with array | ⬜ |
| C2.4.2b | Get when empty | Call with no templates | onGetTemplates fires with empty array | ⬜ |
| C2.4.2c | Parse template data | Receive templates | dataJson parses correctly | ⬜ |

#### Test Case 2.4.3: Delete Template by ID
**Priority:** High
**Command:** `deleteTemplateWithId(templateId)`

| Test ID | Test Case | Input | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.4.3a | Delete existing template | Valid template ID | onDeleteTemplateById fires success=true | ⬜ |
| C2.4.3b | Delete non-existent template | Invalid ID | onDeleteTemplateById fires success=false | ⬜ |
| C2.4.3c | Delete then verify | Delete + getAllTemplates | Template removed from list | ⬜ |

#### Test Case 2.4.4: Delete All Templates
**Priority:** Medium
**Command:** `deleteAllTemplates()`

| Test ID | Test Case | Steps | Expected Output | Status |
|---------|-----------|-------|-----------------|---------|
| C2.4.4a | Delete all existing | 1. Create 5 templates<br>2. Call deleteAllTemplates() | onDeleteTemplates fires success=true | ⬜ |
| C2.4.4b | Delete when empty | Call with no templates | onDeleteTemplates fires success=true | ⬜ |
| C2.4.4c | Verify deletion | Delete + getAllTemplates | Empty array returned | ⬜ |

---

## 3. Events Testing

### 3.1 Scanning Events

#### Test Case 3.1.1: Barcode Scan Event
**Priority:** Critical
**Event:** `onBarcodeScan`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.1.1a | Scan single QR code | Point at QR code | `{codes: [{scannedCode, symbology, boundingBox}]}` | ⬜ |
| E3.1.1b | Scan single barcode | Point at barcode | codes array with barcode data | ⬜ |
| E3.1.1c | Scan multiple codes | Multiple codes in frame | codes array with all detected codes | ⬜ |
| E3.1.1d | Event data structure | Any scan | NO codesJson field, only codes array | ⬜ |
| E3.1.1e | Rapid scans | Wave over multiple codes | Multiple events fire correctly | ⬜ |
| E3.1.1f | Invalid barcode | Damaged/unreadable code | No event or empty codes | ⬜ |

#### Test Case 3.1.2: OCR Scan Event
**Priority:** Critical
**Event:** `onOCRScan`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.1.2a | Shipping label OCR | Scan shipping label | Parsed address, tracking, courier | ⬜ |
| E3.1.2b | Item label OCR | Scan product label | SKU, name, supplier, dimensions | ⬜ |
| E3.1.2c | BOL OCR | Scan bill of lading | Reference numbers, dates, addresses | ⬜ |
| E3.1.2d | Cloud OCR response | Cloud mode | dataJson parses to object correctly | ⬜ |
| E3.1.2e | On-device OCR response | On-device mode | dataJson parses correctly | ⬜ |
| E3.1.2f | OCR with image | isEnableAutoOcrResponseWithImage=true | Response includes base64 image | ⬜ |

#### Test Case 3.1.3: Bounding Boxes Detected Event
**Priority:** High
**Event:** `onBoundingBoxesDetected`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.1.3a | Barcode bounding boxes | Barcode in frame | barcodeBoundingBoxesJson parses to array | ⬜ |
| E3.1.3b | QR code bounding boxes | QR code in frame | qrCodeBoundingBoxesJson parses to array | ⬜ |
| E3.1.3c | Document bounding box | Document in frame | documentBoundingBox with x, y, width, height | ⬜ |
| E3.1.3d | Multiple objects | Barcode + document | Both bounding boxes present | ⬜ |
| E3.1.3e | Moving object | Pan across code | Bounding box updates in real-time | ⬜ |

#### Test Case 3.1.4: Price Tag Detected Event
**Priority:** Medium
**Event:** `onPriceTagDetected`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.1.4a | Detect price tag | Point at price label | price, sku, boundingBox populated | ⬜ |
| E3.1.4b | Price only | Tag without SKU | price present, sku empty/null | ⬜ |
| E3.1.4c | SKU only | Tag without price | sku present, price empty/null | ⬜ |

#### Test Case 3.1.5: Generic Detected Event
**Priority:** Medium
**Event:** `onDetected`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.1.5a | Object detection | Detectable object | type and dataJson populated | ⬜ |
| E3.1.5b | Parse dataJson | Any detection | dataJson parses to valid object | ⬜ |

### 3.2 Camera Events

#### Test Case 3.2.1: Image Captured Event
**Priority:** Critical
**Event:** `onImageCaptured`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.2.1a | Capture photo | Call captureImage() | uri, width, height populated | ⬜ |
| E3.2.1b | Image URI format | Capture | URI is valid file:// path | ⬜ |
| E3.2.1c | Image dimensions | Capture | width and height are numbers > 0 | ⬜ |
| E3.2.1d | Access captured image | Use URI | Image file exists and readable | ⬜ |

#### Test Case 3.2.2: Sharpness Score Event
**Priority:** Medium
**Event:** `onSharpnessScore`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.2.2a | Sharp image | Hold steady on target | score close to 1.0 (high) | ⬜ |
| E3.2.2b | Blurry image | Move camera rapidly | score close to 0.0 (low) | ⬜ |
| E3.2.2c | Score range | Various conditions | score is Float between 0.0-1.0 | ⬜ |
| E3.2.2d | Real-time updates | Continuous monitoring | Events fire frequently | ⬜ |

#### Test Case 3.2.3: Model Download Progress Event
**Priority:** High
**Event:** `onModelDownloadProgress`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.2.3a | Download start | configureOnDeviceModel() | progress=0, downloadStatus=true, isReady=false | ⬜ |
| E3.2.3b | Download in progress | During download | progress incrementing (0.0-1.0) | ⬜ |
| E3.2.3c | Download complete | Download finishes | progress=1.0, isReady=true | ⬜ |
| E3.2.3d | Download failed | Network error | downloadStatus=false | ⬜ |
| E3.2.3e | Already downloaded | Reconfigure same model | isReady=true immediately | ⬜ |

### 3.3 Template Events

#### Test Case 3.3.1: Create Template Event
**Priority:** High
**Event:** `onCreateTemplate`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.3.1a | Successful creation | createTemplate() succeeds | success=true, dataJson with template | ⬜ |
| E3.3.1b | Failed creation | createTemplate() fails | success=false, dataJson with error | ⬜ |
| E3.3.1c | Template data | Parse dataJson | Contains template ID, name, metadata | ⬜ |

#### Test Case 3.3.2: Get Templates Event
**Priority:** High
**Event:** `onGetTemplates`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.3.2a | Get existing templates | getAllTemplates() | success=true, dataJson with array | ⬜ |
| E3.3.2b | Get empty templates | getAllTemplates() no templates | success=true, dataJson="[]" | ⬜ |
| E3.3.2c | Parse templates | Parse dataJson | Array of template objects | ⬜ |
| E3.3.2d | Template structure | Each template | Has id, name, timestamp, etc. | ⬜ |

#### Test Case 3.3.3: Delete Template by ID Event
**Priority:** High
**Event:** `onDeleteTemplateById`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.3.3a | Successful deletion | deleteTemplateWithId(valid) | success=true | ⬜ |
| E3.3.3b | Failed deletion | deleteTemplateWithId(invalid) | success=false | ⬜ |

#### Test Case 3.3.4: Delete Templates Event
**Priority:** Medium
**Event:** `onDeleteTemplates`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.3.4a | Successful deletion | deleteAllTemplates() | success=true | ⬜ |
| E3.3.4b | Delete when empty | deleteAllTemplates() no templates | success=true | ⬜ |

### 3.4 Error Events

#### Test Case 3.4.1: Error Event
**Priority:** Critical
**Event:** `onError`

| Test ID | Test Case | Trigger | Expected Event Data | Status |
|---------|-----------|---------|---------------------|---------|
| E3.4.1a | Camera permission denied | Deny camera access | message contains permission error | ⬜ |
| E3.4.1b | Invalid API credentials | Wrong apiKey/token | message contains auth error | ⬜ |
| E3.4.1c | Network error | Disconnect internet | message contains network error | ⬜ |
| E3.4.1d | Invalid command parameter | Pass bad JSON | message contains parse error | ⬜ |
| E3.4.1e | SDK initialization error | SDK fails to init | message with initialization error | ⬜ |

---

## 4. Integration Testing

### 4.1 Fabric Architecture

#### Test Case 4.1.1: Codegen Verification
**Priority:** Critical

| Test ID | Test Case | Verification | Status |
|---------|-----------|--------------|---------|
| I4.1.1a | Codegen files generated | Check build output for generated files | ⬜ |
| I4.1.1b | iOS Fabric component | VisionSdkViewComponentView.mm exists and compiles | ⬜ |
| I4.1.1c | Android Fabric component | Generated Java/Kotlin files present | ⬜ |
| I4.1.1d | Command mapping | All 22 commands mapped correctly | ⬜ |

#### Test Case 4.1.2: Legacy vs Fabric Compatibility
**Priority:** High

| Test ID | Test Case | Test Approach | Status |
|---------|-----------|---------------|---------|
| I4.1.2a | Event structure consistency | Compare event data between architectures | ⬜ |
| I4.1.2b | Command behavior | Same commands produce same results | ⬜ |
| I4.1.2c | Props application | Props work identically in both | ⬜ |

### 4.2 Cross-Platform Testing

#### Test Case 4.2.1: iOS vs Android Parity
**Priority:** High

| Test ID | Test Case | Platforms | Expected Result | Status |
|---------|-----------|-----------|-----------------|---------|
| I4.2.1a | All commands work | iOS + Android | Identical behavior | ⬜ |
| I4.2.1b | All events fire | iOS + Android | Same event data | ⬜ |
| I4.2.1c | All props apply | iOS + Android | Same visual/functional result | ⬜ |
| I4.2.1d | Model download | iOS + Android | Both download and run models | ⬜ |

#### Test Case 4.2.2: Platform-Specific Features
**Priority:** Medium

| Test ID | Test Case | Platform | Feature | Status |
|---------|-----------|----------|---------|---------|
| I4.2.2a | NNAPI provider | Android | modelExecutionProviderAndroid="NNAPI" | ⬜ |
| I4.2.2b | GPU provider | Android | modelExecutionProviderAndroid="GPU" | ⬜ |
| I4.2.2c | Metal acceleration | iOS | Default metal usage | ⬜ |

### 4.3 Performance Testing

#### Test Case 4.3.1: Memory Management
**Priority:** Critical

| Test ID | Test Case | Metrics | Pass Criteria | Status |
|---------|-----------|---------|---------------|---------|
| I4.3.1a | Memory leak check | Monitor over 10 minutes | No continuous growth | ⬜ |
| I4.3.1b | Mount/unmount cycles | Mount/unmount 100 times | Memory returns to baseline | ⬜ |
| I4.3.1c | Image capture memory | Capture 50 images | Memory cleaned after capture | ⬜ |
| I4.3.1d | Model download memory | Download large model | Memory released after download | ⬜ |

#### Test Case 4.3.2: Performance Benchmarks
**Priority:** High

| Test ID | Test Case | Metric | Target | Status |
|---------|-----------|--------|--------|---------|
| I4.3.2a | Camera startup time | Mount to preview | < 1 second | ⬜ |
| I4.3.2b | Barcode detection latency | Detection to event | < 100ms | ⬜ |
| I4.3.2c | OCR processing time | Capture to result | < 2 seconds (cloud), < 500ms (on-device) | ⬜ |
| I4.3.2d | Image capture time | Trigger to onImageCaptured | < 500ms | ⬜ |
| I4.3.2e | Command execution | Any command call | < 50ms to start | ⬜ |

#### Test Case 4.3.3: Battery Impact
**Priority:** Medium

| Test ID | Test Case | Duration | Acceptable Drain | Status |
|---------|-----------|----------|------------------|---------|
| I4.3.3a | Continuous scanning | 10 minutes | < 15% battery | ⬜ |
| I4.3.3b | Idle camera | 10 minutes preview | < 10% battery | ⬜ |
| I4.3.3c | Background/foreground | Multiple cycles | Minimal drain when paused | ⬜ |

### 4.4 Edge Cases & Error Handling

#### Test Case 4.4.1: Resource Constraints
**Priority:** High

| Test ID | Test Case | Condition | Expected Behavior | Status |
|---------|-----------|-----------|-------------------|---------|
| I4.4.1a | Low memory | Simulate low memory | Graceful degradation, no crash | ⬜ |
| I4.4.1b | Low storage | Full disk | Error message, prevents download | ⬜ |
| I4.4.1c | Slow network | 2G simulation | Timeout handling, retry logic | ⬜ |
| I4.4.1d | No network | Offline | Error event, clear message | ⬜ |

#### Test Case 4.4.2: Lifecycle Management
**Priority:** Critical

| Test ID | Test Case | Scenario | Expected Behavior | Status |
|---------|-----------|----------|-------------------|---------|
| I4.4.2a | App backgrounded | Home button | Camera pauses, releases resources | ⬜ |
| I4.4.2b | App foregrounded | Return from background | Camera resumes automatically | ⬜ |
| I4.4.2c | Incoming call | Phone call interrupts | Camera stops, restarts after call | ⬜ |
| I4.4.2d | Component unmount | Navigate away | Resources cleaned, no memory leak | ⬜ |
| I4.4.2e | Rapid mount/unmount | Fast navigation | No crash, handles gracefully | ⬜ |

#### Test Case 4.4.3: Concurrent Operations
**Priority:** High

| Test ID | Test Case | Operations | Expected Behavior | Status |
|---------|-----------|------------|-------------------|---------|
| I4.4.3a | Multiple captures | Call captureImage() 5x fast | All queued or latest wins | ⬜ |
| I4.4.3b | Scan while downloading | Barcode scan + model download | Both proceed independently | ⬜ |
| I4.4.3c | Multiple cloud requests | 3 getPrediction() calls | All complete or queue properly | ⬜ |
| I4.4.3d | Prop changes during scan | Change mode mid-scan | Handles gracefully, no crash | ⬜ |

---

## 5. Regression Testing Checklist

### 5.1 Pre-Release Validation

Before pushing to production, ensure ALL critical and high-priority tests pass:

- [ ] **All Critical Priority Tests (E.g., C2.1.1, C2.3.7, E3.1.1, E3.1.2, I4.1.1):** 100% pass rate
- [ ] **All High Priority Tests:** ≥95% pass rate
- [ ] **No P1/P0 bugs open:** All critical bugs resolved
- [ ] **Performance benchmarks met:** See Test Case 4.3.2
- [ ] **Memory leak check passed:** See Test Case 4.3.1
- [ ] **Cross-platform parity verified:** iOS + Android identical behavior
- [ ] **Backward compatibility:** Legacy architecture still works (if supported)

### 5.2 Platform-Specific Checklist

**iOS:**
- [ ] Tested on iOS 15, 16, 17, 18
- [ ] Physical device testing (iPhone 12+)
- [ ] Camera permission handling
- [ ] Background/foreground lifecycle

**Android:**
- [ ] Tested on API 21, 29, 33, 34
- [ ] Physical device testing (Pixel, Samsung)
- [ ] Camera permission handling
- [ ] NNAPI/GPU providers tested

### 5.3 Documentation & Examples

- [ ] Test cases document updated
- [ ] Example app demonstrates all features
- [ ] README includes migration guide
- [ ] API documentation current
- [ ] Changelog includes breaking changes

---

## 6. Test Automation Recommendations

### 6.1 Unit Tests
- Commands: Mock native module, test JS bridge
- Event handlers: Verify parsing and callbacks
- Props: Test serialization (especially JSON props)

### 6.2 Integration Tests
- Detox/Appium: Test critical user flows
- Barcode scanning workflow
- Template creation/deletion
- Cloud prediction pipeline

### 6.3 E2E Tests
- Full scan-to-result workflows
- Multi-step operations (e.g., scan → capture → predict)
- Error recovery flows

### 6.4 Performance Tests
- Memory profiler (Instruments/Android Profiler)
- Network throttling tests
- Battery usage monitoring

---

## 7. Bug Severity & Priority Guide

### Severity Levels

**P0 - Critical:**
- App crashes
- Data loss
- Security vulnerabilities
- Complete feature failure

**P1 - High:**
- Major functionality broken
- Incorrect data returned
- Memory leaks
- Significant performance degradation

**P2 - Medium:**
- Minor functionality issues
- UI glitches
- Suboptimal performance
- Missing non-critical features

**P3 - Low:**
- Cosmetic issues
- Edge case bugs
- Documentation errors

---

## 8. Test Results Template

### Test Execution Summary

**Test Run Date:** YYYY-MM-DD
**Tester:** [Name]
**Build Version:** [Version]
**Platform:** iOS/Android
**Device:** [Device Model]

#### Results Overview

| Priority | Total | Passed | Failed | Blocked | Skip | Pass Rate |
|----------|-------|--------|--------|---------|------|-----------|
| Critical |       |        |        |         |      |           |
| High     |       |        |        |         |      |           |
| Medium   |       |        |        |         |      |           |
| Low      |       |        |        |         |      |           |
| **Total**|       |        |        |         |      |           |

#### Failed Test Cases

| Test ID | Test Case | Failure Reason | Bug ID | Assignee |
|---------|-----------|----------------|--------|----------|
|         |           |                |        |          |

#### Notes & Observations

[Additional comments, blockers, or concerns]

---

## 9. Contact & Support

**Questions or Issues:**
- GitHub Issues: [Repository URL]
- Internal Slack: #vision-sdk-support
- Documentation: [Docs URL]

**Test Case Maintenance:**
- Owner: [Team/Person]
- Last Review: [Date]
- Next Review: [Date]

---

**End of Test Cases Document**
