# Release Notes

### v2.0.4 — 2026-01-12

  #### Bug Fixes

  - **iOS Frozen Frame Issue**
    Fixed frozen frame display when stopping and restarting the VisionCamera component or navigating away and back. The camera now properly clears the preview layer by recreating the view instance, preventing stale frames from previous sessions.

  #### Improvements

  - **iOS Camera Performance**
    Significantly improved camera startup performance by removing unnecessary delays:
    - Removed 600ms minimum operation interval between start/stop calls
    - Removed 400ms sleep after startRunning()
    - Removed 200ms sleep after stopRunning()
    - Removed 100ms delay on initial camera start

    Camera now starts ~700ms faster with no loss of stability. Rapid start/stop operations work reliably without causing AVFoundation corruption.

  - **iOS View Lifecycle Management**
    Improved view lifecycle handling in VisionCamera component:
    - Implemented `recreateCameraView()` method to properly handle navigation events
    - Enhanced `didMoveToWindow()` to detect when view is added/removed from window hierarchy
    - Camera view now recreates on navigation back to clear frozen frames
    - Better memory management with proper cleanup of old camera instances

---

### v2.0.3 — 2025-12-24

  #### Bug Fixes

  - **iOS Camera Lifecycle Stability**
    Fixed camera session corruption that occurred during rapid start/stop cycles on iOS. The previous implementation allowed overlapping AVFoundation operations, causing 'Fig assert: hasFigCaptureSession' errors and breaking camera callbacks (sharpness scores, recognition updates, capture).

  #### Improvements

  - **Enhanced Camera State Management (iOS)**
    - Implemented three-state tracking system (actual state, target state, transition status) to properly handle camera lifecycle
    - Added serial operation queue to prevent overlapping AVFoundation calls
    - Enforced 600ms minimum interval between operations for hardware stability
    - Smart operation coalescing: skips intermediate operations when users rapidly toggle camera (e.g., start→stop→start becomes just start)
    - Operations run on background queue to prevent main thread blocking and UI jank
    - Improved camera responsiveness and reliability during rapid user interactions

---

### v2.0.2 — 2025-12-15

  #### New Features

  - **Model Management API**
    Complete overhaul of on-device model management with 14 new APIs for fine-grained control:
    - `initializeModelManager()` - Initialize with configuration (Android required, iOS no-op)
    - `isModelManagerInitialized()` - Check initialization status
    - `downloadModel()` - Download models with progress tracking and cancellation support
    - `cancelDownload()` - Cancel active downloads for specific models
    - `loadOCRModel()` - Load models into memory with execution provider selection (Android: CPU/NNAPI/XNNPACK)
    - `unloadModel()` - Unload from memory (files remain on disk)
    - `isModelLoaded()` - Check if model is loaded
    - `getLoadedModelCount()` - Get count of loaded models
    - `findDownloadedModels()` - List all downloaded models with metadata
    - `findDownloadedModel()` - Find specific model info
    - `findLoadedModels()` - List currently loaded models
    - `deleteModel()` - Permanently delete from disk
    - `predictWithModule()` - Make predictions with specific model
    - `onModelLifecycle()` - Subscribe to lifecycle events (download/load/unload/delete)

  #### Improvements

  - **Granular Model Control**
    - Separate download and load operations for better resource management
    - Download once, load multiple times without re-downloading
    - Per-download progress callbacks with model-specific tracking
    - Concurrent model downloads with configurable limits
    - Model version tracking and metadata queries

  - **Platform-Specific Optimizations**
    - Android: Singleton pattern with explicit initialization requirement
    - Android: Execution provider selection (CPU, NNAPI, XNNPACK)
    - iOS: Automatic initialization (no-op methods for API consistency)
    - Both platforms: Granular model unloading support

  #### Deprecations

  - **Deprecated Methods** (will be removed in v3.0.0)
    - `loadModel()` → Use `downloadModel()` + `loadOCRModel()`
    - `unLoadModel()` → Use `unloadModel()` and/or `deleteModel()`
    - `predict()` → Use `predictWithModule()`

  #### Breaking Changes

  - **OCRModule Type Introduction**
    New type-safe parameter format: `{ type: string, size: string }` replaces separate string parameters

---

### v2.0.1 — 2025-12-05

  #### Bug Fixes

  - **Android Item Label Response Structure**
    Fixed Android `extended_response` structure for item label predictions to match iOS format, ensuring consistent cross-platform behavior.

---

### v2.0.0 — 2025-12-02

  #### Breaking Changes

  - **Old Architecture Removed**
    Completely removed support for React Native's old architecture. This version requires React Native 0.74+ with New Architecture (Fabric + TurboModules) enabled.
  - **Minimum React Native Version**
    Updated minimum React Native requirement to 0.74.0 for New Architecture compatibility.

  #### New Features

  - **New Architecture Migration Complete**
    Full migration to React Native's New Architecture:
    - TurboModules integration complete on both iOS and Android
    - Fabric renderer support finalized
    - Improved performance and reduced bridge overhead
  - **Babel Preset Update**
    Updated Babel preset configuration for React Native 0.82+ compatibility, ensuring future-proof builds.

  #### Platform-Specific Improvements

  - **Android NDK 28.0.13004108**
    Upgraded Android NDK to version 28.0.13004108 with 16KB page size support for better memory management on modern Android devices.
  - **Template System Fixes**
    Fixed barcode template selection issues on Android.
  - **Podspec Configuration**
    Improved iOS Podspec configuration for better CocoaPods compatibility.

---

### v1.5.30 — 2025-11-14

  #### Improvements

  - **Native SDK Updates**
    - Updated Android VisionSDK to latest version for improved performance and stability
    - Upgraded Android NDK to 27.1 with 16KB page size support

---

### v1.5.29 — 2025-11-12

  #### Bug Fixes

  - **iOS Camera Lifecycle**
    Resolved camera lifecycle management issues on iOS, improving camera start/stop reliability.
  - **Android Remote Image Support**
    Added support for processing remote images on Android platform.

---

### v1.5.28 — 2025-11-06

  #### Improvements

  - **Native SDK Updates**
    Updated iOS VisionSDK to latest version for improved performance and bug fixes.

---

### v1.5.27 — 2025-10-27

  #### New Features

  - **Camera Switching Support (iOS)**
    Added comprehensive front/back camera switching functionality for iOS:
    - New `cameraFacing` prop for VisionCamera component ('back' | 'front')
    - Automatic prop-based camera switching
    - Added `cameraPosition` parameter to `setCameraSettings` method for VisionSdkView
    - Support for imperative camera switching via ref (1 = back, 2 = front)

---

### v1.5.26 — 2025-10-20

  #### New Features

  - Full Bounding Box Metadata Support (Android) - Achieved complete feature parity with iOS
  - Upgraded Android VisionSDK from v2.4.22 to v2.4.23
  - Extract full barcode metadata including scannedCode, symbology, and gs1ExtractedInfo from bounding boxes
  - Updated onIndicationsBoundingBoxes to use List[ScannedCodeResult] for comprehensive metadata

### v1.5.25 — 2025-10-14

  #### New Features

  - **Model Management - Added VisionCore.unLoadModel() method**
    - Cleanup on-device models from memory and disk
    - Helps manage storage and memory resources
  - **Enhanced Barcode Detection - Full metadata support across platforms**
    - Added symbology, boundingBox, and gs1ExtractedInfo to barcode events
    - Enhanced onCapture event with sharpnessScore and barcodes array
    - Improved onBarcodeDetected with comprehensive metadata
  - **Better Error Handling - Added error codes to onError callback**
    - iOS: Filters error codes 13, 14, 15, 16 for cleaner error handling
    - More granular error information for debugging


### v1.5.24 — 2025-10-07

  New Component: VisionCamera.Added a new lightweight VisionCamera component as an alternative to the full-featured VisionSdkView, providing core camera functionality with a

  #### New Features

  - **Multi-Mode Scanning Support**
    - Photo mode
    - Barcode scanning
    - QR code scanning
    - OCR (text recognition)
    - Combined modes
  - **Real-time Detection Events**
    - Text detection
    - Barcode detection
    - QR code detection
    - Document detection
  - **Camera Controls**
    - Flash control via props and ref methods
    - Zoom control via props and ref methods
    - Auto and manual capture modes
  - **Developer Experience**
    - Custom overlay support via children prop
    - Full TypeScript support with comprehensive type definitions
    - Image sharpness scoring for quality assurance
    - Auto-start on mount with proper lifecycle management

  ### Platform Support

  - Android: Native implementation using VisionCameraView from Vision SDK
  - iOS: Native implementation using CodeScannerView from Vision SDK
  - Both platforms support identical APIs and features

### v1.5.18 — 2025-09-29

  Major Enhancement: Cross-Platform Event Consistency. This release brings complete parity between iOS and Android implementations with enhanced barcode metadata and sharpness scoring.

  #### New Features

  - **Comprehensive Sharpness Score Support**
    - Added onSharpnessScore event callback across both platforms
    - Included sharpnessScore parameter in onImageCaptured events
    - New SharpnessScoreEvent TypeScript interface
  - **Enhanced Barcode Metadata - Both platforms now provide identical event structure:**
    - scannedCode: Barcode value
    - symbology: Barcode type/format
    - boundingBox: Coordinate and dimension data
    - gs1ExtractedInfo: Extracted GS1 data (optional)

### v1.5.16 — 2025-09-13

####  Added
- **Headless OCR Prediction Methods**
  New camera-independent prediction methods for processing existing images:
  - `predict()`: On-device OCR prediction for existing images
  - `predictShippingLabelCloud()`: Cloud shipping label prediction
  - `predictItemLabelCloud()`: Cloud item label prediction
  - `predictBillOfLadingCloud()`: Cloud bill of lading prediction
  - `predictDocumentClassificationCloud()`: Cloud document classification
  - `predictWithCloudTransformations()`: Hybrid on-device + cloud prediction

####  Changed
- **Updated Native SDKs**
  - iOS VisionSDK updated to latest version
  - Android VisionSDK updated to v2.4.16

---

### v1.5.12 — 2025-08-27

####  Added
- **Metadata Support for Logging APIs**
  Enhanced `logItemLabelDataToPx` and `logShippingLabelDataToPx` methods with optional metadata parameter for improved data context and tracking.

####  Changed
- **Updated Native SDKs**
  - Android VisionSDK updated to v2.4.12
  - iOS VisionSDK updated to v1.9.1

---

### v1.5.8 — 2025-08-09

####  Changed
- **Model Download Status Improvements**
  Fixed model download status reporting and improved readiness handling on Android. Enhanced `isReady` flag behavior for better loading state management.

####  Fixed
- **Android Model Download Progress**
  Improved accuracy of model download status reporting using `isModelAlreadyDownloaded()` method.

---

### v1.5.4 — 2025-07-28

####  Changed
- **Native SDK Updates**
  - Upgraded Android Vision SDK to v2.4.6
  - Updated on-headers version for security improvements

---

### v1.5.0 — 2025-07-25

####  Added
- **Android Model Execution Provider Configuration**  **DEPRECATED - Will be removed in future versions**
  New `modelExecutionProviderAndroid` prop allows configuration of OCR model execution on Android:
  - `CPU`: Default CPU execution (most compatible)
  - `NNAPI`: Android Neural Networks API for hardware acceleration
  - `XNNPACK`: Optimized CPU execution backend

####  Changed
- **Enhanced OCR Response Consistency**
  Improved key consistency across iOS and Android platforms with backward-compatible duplicate keys (e.g., `barcode_values` and `barcodeValues`).

####  Fixed
- **OCR Response Inconsistency**
  Addressed inconsistent response keys between iOS and Android platforms while maintaining backward compatibility.

---

### v1.4.8 — 2025-05-12

####  Added
- **Template-Based Barcode Detection**
  New "Templates" mode lets users freeze the camera view and tap on specific barcode types to include or exclude them, targeting only the formats you need.

- **`priceTag` Scanning Mode & `onPriceTagDetected` Event**
  Dedicated `priceTag` mode optimized for retail price-tag scanning. Fires an `onPriceTagDetected` callback with:
  - `price` (string)
  - `sku` (string)
  - `boundingBox` (`x`, `y`, `width`, `height`)

- **`onBoundingBoxesDetected` Event**
  Emits whenever any barcode or QR code is detected on screen, returning an array of bounding boxes for real-time custom overlay rendering.

####  Changed
- **Bump VisionSDK Dependency**
  Podspec and Gradle configurations now pin the underlying VisionSDK to **v1.8.0**.

- **Example App Updates**
  - Added `TemplateSelectionView.tsx` for barcode template selection.
  - Updated `ModeSelectionView.tsx` and `CameraScreen.tsx` to support the new modes and events.

####  Fixed
- **TypeScript Types**
  Added missing definitions for the `onPriceTagDetected` handler in `src/types.ts`.


### v1.4.7 — 2025-04-15

####  Bug Fixes
- Correct density conversion for `focusImageRect` on Android to ensure the focus rectangle scales properly across device densities.

---

### v1.4.6 — 2025-04-09

####  Improvements
- Bumped core and example dependencies (e.g. `@babel/helpers`, `image-size`) to address vulnerabilities and keep packages up to date.

####  Bug Fixes
- iOS `setFocusSettings` now correctly updates `shouldDisplayFocusImage` for both `true` and `false`.

---

### v1.4.5 — 2025-03-27

####  Improvements
- Updated peer dependencies to support React Native ≥ 0.73.0 and React ≥ 18.0.0.

---

### v1.4.4 — 2025-03-26

####  Improvements
- iOS Podspec now pins the VisionSDK pod version automatically, removing the need for manual Podfile overrides.
- Upgraded Android VisionSDK library for compatibility with Android SDK 23.

---

### v1.4.3 — 2025-03-17

####  Improvements
- Upgraded native Android VisionSDK to the latest release and added explicit typecasts in the Podspec to satisfy Android Gradle Plugin requirements.

---

### v1.4.2 — 2025-03-06

####  Improvements
- Security updates: bumped `cross-spawn` to 7.0.6, `@octokit/request-error` to 5.1.1, and `@octokit/request` to 8.4.1.

---

### v1.4.1 — 2025-03-06

####  Improvements
- Maintenance release with no user‑facing changes.

---

### v1.3.2 — 2025-03-06

####  Bug Fixes
- Downgraded Kotlin to 1.9.0 to restore compatibility with Android Expo builds.

---

### v1.3.1 — 2025-03-04

####  Improvements
- Patch release with no significant changes.

---

### v1.3.0 — 2025-02-24

####  Improvements
1. Refactored imperative handlers (prediction, error reporting, model configuration) to accept additional parameters for greater flexibility.
2. Optimized event listener registration and teardown to improve performance and prevent memory leaks.

---

### v1.2.2 — 2025-02-13

####  Improvements
- Reverted an earlier event‑listener optimization to prevent unintended breaking changes.

---

### v1.2.1 — 2025-02-11

####  Improvements
- Updated the VisionSDK Pod version in iOS Podspec; enforced Yarn by removing stray `package-lock.json`.

---

### v1.2.0 — 2025-02-11

####  Improvements
- New `shouldResizeImage` prop to control whether images are resized before being sent to the SDK.

---

### v1.0.10 — 2025-02-06

####  Improvements
- Introduced an `isReady` flag to signal when the model has fully initialized after download, on both iOS and Android.

---

### v1.0.9 — 2025-01-30

####  Improvements
1. GS1 barcode support with `gs1ExtractedInfo` for richer barcode metadata.
2. New `ocrType` prop to decouple model type selection (e.g. `shipping_label`, `item_label`) from `ocrMode`.
3. Refactored `reportError` handler to handle multiple model classes and improved example app performance.
4. Fixed missing permissions setup for `react-native-permissions-lib`.
