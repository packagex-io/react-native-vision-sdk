# Model Management API Reference

Complete API reference for the Vision SDK Model Management system.

## Table of Contents

1. [Overview](#overview)
2. [Initialization](#initialization)
3. [Download Operations](#download-operations)
4. [Load/Unload Operations](#loadunload-operations)
5. [Query Operations](#query-operations)
6. [Delete Operations](#delete-operations)
7. [Prediction](#prediction-with-specific-models)
8. [Event Listeners](#event-listeners)
9. [Types & Interfaces](#types--interfaces)
10. [Error Handling](#error-handling)
11. [Platform Differences](#platform-differences)
12. [Migration Guide](#migration-guide)

---

## Overview

The Model Management API provides fine-grained control over the lifecycle of on-device ML models. This includes downloading, loading into memory, querying status, making predictions, and cleanup operations.

### Key Concepts

- **Download**: Fetch model files from server to disk
- **Load**: Load model from disk into memory for inference
- **Unload**: Remove model from memory (files remain on disk)
- **Delete**: Permanently remove model files from disk
- **OCRModule**: Type representing a specific model (type + size combination)

### Basic Workflow

```typescript
import { VisionCore } from 'react-native-vision-sdk';

// 1. Initialize (REQUIRED on Android, optional on iOS)
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });

// 2. Download model
await VisionCore.downloadModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token
);

// 3. Load into memory
await VisionCore.loadOCRModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token
);

// 4. Make predictions
const result = await VisionCore.predictWithModule(
  { type: 'shipping_label', size: 'large' },
  imageUri,
  barcodes
);

// 5. Cleanup
await VisionCore.unloadModel({ type: 'shipping_label', size: 'large' });
await VisionCore.deleteModel({ type: 'shipping_label', size: 'large' });
```

---

## Initialization

### `initializeModelManager(config)`

Initialize the Model Manager singleton with configuration options.

**Signature:**
```typescript
initializeModelManager(config: ModelManagerConfig): void
```

**Parameters:**
- `config` (ModelManagerConfig):
  - `maxConcurrentDownloads?: number` - Maximum concurrent downloads (default: 1)
  - `enableLogging?: boolean` - Enable debug logging (default: false)

**Example:**
```typescript
VisionCore.initializeModelManager({
  maxConcurrentDownloads: 3,
  enableLogging: true
});
```

**Platform Requirements:**
- **Android**: **REQUIRED** - Must be called before any model operations
- **iOS**: **Not needed** - This method exists only for API consistency. iOS has a hardcoded implementation that does nothing. You can safely skip calling this on iOS.

**Notes:**
- **Android**: Should be called once, typically at app startup
- **Android**: Calling multiple times will reinitialize with new config
- **Android**: Model operations will fail without initialization
- **iOS**: This method is a no-op (does nothing)

---

### `isModelManagerInitialized()`

Check if the Model Manager has been initialized.

**Signature:**
```typescript
isModelManagerInitialized(): boolean
```

**Returns:**
- `boolean` - `true` if initialized, `false` otherwise

**Example:**
```typescript
// Android: Check before any operations
if (!VisionCore.isModelManagerInitialized()) {
  VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });
}

// Now safe to proceed with downloads/loads
await VisionCore.downloadModel(module, apiKey, token);
```

**Platform Notes:**
- **Android**: Returns `false` until `initializeModelManager()` is called
- **iOS**: **Always returns `true`** - This is a hardcoded implementation for API consistency. iOS doesn't require initialization, so this method always returns `true` regardless of whether `initializeModelManager()` was called.

---

## Download Operations

### `downloadModel(module, apiKey, token, progressCallback?)`

Download a model from the server to device storage.

**Signature:**
```typescript
downloadModel(
  module: OCRModule,
  apiKey?: string | null,
  token?: string | null,
  progressCallback?: (progress: DownloadProgress) => void
): Promise<void>
```

**Parameters:**
- `module` (OCRModule): Model to download
  - `type`: 'shipping_label' | 'item_label' | 'bill_of_lading' | 'document_classification'
  - `size`: 'nano' | 'micro' | 'small' | 'medium' | 'large' | 'xlarge'
- `apiKey` (string | null, optional): API key for authentication
- `token` (string | null, optional): Auth token for authentication
- `progressCallback` (function, optional): Progress updates

**Returns:**
- `Promise<void>` - Resolves when download completes

**Example:**
```typescript
await VisionCore.downloadModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token,
  (progress) => {
    console.log(`Download progress: ${(progress.progress * 100).toFixed(1)}%`);
  }
);
```

**Progress Callback:**
The callback receives a `DownloadProgress` object:
```typescript
{
  module: OCRModule;          // The module being downloaded
  progress: number;           // 0.0 to 1.0 (download progress percentage)
}
```

**Error Handling:**
```typescript
try {
  await VisionCore.downloadModel(module, apiKey, token, (progress) => {
    console.log(`Progress: ${(progress.progress * 100).toFixed(1)}%`);
  });
  console.log('Download complete');
} catch (error) {
  console.error('Download failed:', error.message);
}
```

---

### `cancelDownload(module)`

Cancel an active download operation for a specific model.

**Signature:**
```typescript
cancelDownload(module: OCRModule): Promise<boolean>
```

**Parameters:**
- `module` (OCRModule): The model whose download you want to cancel

**Returns:**
- `Promise<boolean>` - `true` if cancelled, `false` if no active download for this model

**Example:**
```typescript
const module = { type: 'shipping_label', size: 'large' };

// Start download
VisionCore.downloadModel(module, apiKey, token, (progress) => {
  console.log(`Progress: ${progress.progress * 100}%`);
});

// Later... cancel the download for this specific model
const cancelled = await VisionCore.cancelDownload(module);
if (cancelled) {
  console.log('Download cancelled successfully');
}
```

**Note:** Cancels the download for the specified model. If multiple downloads are in progress, only the download for this specific model will be cancelled.

---

## Load/Unload Operations

### `loadOCRModel(module, apiKey, token, executionProvider?)`

Load a model from disk into memory for inference.

**Signature:**
```typescript
loadOCRModel(
  module: OCRModule,
  apiKey?: string | null,
  token?: string | null,
  executionProvider?: ExecutionProvider
): Promise<void>
```

**Parameters:**
- `module` (OCRModule): Model to load
- `apiKey` (string | null, optional): API key
- `token` (string | null, optional): Auth token
- `executionProvider` (ExecutionProvider, optional): Android only
  - `'CPU'` (default) - Best compatibility
  - `'NNAPI'` - Android Neural Networks API
  - `'XNNPACK'` - Optimized for ARM

**Returns:**
- `Promise<void>` - Resolves when loaded

**Example:**
```typescript
// Basic usage (CPU execution)
await VisionCore.loadOCRModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token
);

// Android: Use NNAPI for potentially faster inference
await VisionCore.loadOCRModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token,
  'NNAPI'
);
```

**Notes:**
- If model not downloaded, will automatically download first
- Loading the same model again has no effect
- iOS ignores `executionProvider` parameter

---

### `unloadModel(module)`

Unload a model from memory. Files remain on disk for faster reloading.

**Signature:**
```typescript
unloadModel(module: OCRModule): Promise<boolean>
```

**Parameters:**
- `module` (OCRModule): Model to unload

**Returns:**
- `Promise<boolean>` - `true` if unloaded, `false` if wasn't loaded

**Example:**
```typescript
const unloaded = await VisionCore.unloadModel({
  type: 'shipping_label',
  size: 'large'
});

if (unloaded) {
  console.log('Model unloaded from memory');
} else {
  console.log('Model was not loaded');
}
```

**Platform Note:**
- **iOS**: Granular unloading of specific models
- **Android**: Due to singleton pattern, unloads ALL models

---

### `isModelLoaded(module)`

Check if a specific model is currently loaded in memory.

**Signature:**
```typescript
isModelLoaded(module: OCRModule): boolean
```

**Parameters:**
- `module` (OCRModule): Model to check

**Returns:**
- `boolean` - `true` if loaded, `false` otherwise

**Example:**
```typescript
const loaded = VisionCore.isModelLoaded({
  type: 'shipping_label',
  size: 'large'
});

if (!loaded) {
  await VisionCore.loadOCRModel(module, apiKey, token);
}
```

---

### `getLoadedModelCount()`

Get the number of models currently loaded in memory.

**Signature:**
```typescript
getLoadedModelCount(): number
```

**Returns:**
- `number` - Count of loaded models

**Example:**
```typescript
const count = VisionCore.getLoadedModelCount();
console.log(`${count} model(s) currently loaded`);

if (count > 2) {
  // Unload some models to free memory
}
```

---

## Query Operations

### `findDownloadedModels()`

List all models downloaded to device storage.

**Signature:**
```typescript
findDownloadedModels(): Promise<ModelInfo[]>
```

**Returns:**
- `Promise<ModelInfo[]>` - Array of downloaded models

**Example:**
```typescript
const downloaded = await VisionCore.findDownloadedModels();

downloaded.forEach(model => {
  console.log(`Model: ${model.module.type} (${model.module.size})`);
  console.log(`  Size: ${(model.sizeInBytes / 1024 / 1024).toFixed(2)} MB`);
  console.log(`  Path: ${model.filePath}`);
  console.log(`  Downloaded: ${model.downloadedAt}`);
});
```

**ModelInfo Structure:**
```typescript
{
  module: OCRModule;
  sizeInBytes: number;
  filePath: string;
  downloadedAt: string;       // ISO 8601 timestamp
  isLoaded: boolean;
}
```

---

### `findDownloadedModel(module)`

Find information about a specific downloaded model.

**Signature:**
```typescript
findDownloadedModel(module: OCRModule): Promise<ModelInfo | null>
```

**Parameters:**
- `module` (OCRModule): Model to find

**Returns:**
- `Promise<ModelInfo | null>` - Model info if found, `null` otherwise

**Example:**
```typescript
const modelInfo = await VisionCore.findDownloadedModel({
  type: 'shipping_label',
  size: 'large'
});

if (modelInfo) {
  console.log('Model found:', modelInfo.filePath);
} else {
  console.log('Model not downloaded');
  // Download it
  await VisionCore.downloadModel(module, apiKey, token);
}
```

---

### `findLoadedModels()`

List all models currently loaded in memory.

**Signature:**
```typescript
findLoadedModels(): Promise<ModelInfo[]>
```

**Returns:**
- `Promise<ModelInfo[]>` - Array of loaded models

**Example:**
```typescript
const loaded = await VisionCore.findLoadedModels();

console.log(`${loaded.length} model(s) loaded:`);
loaded.forEach(model => {
  console.log(`- ${model.module.type} (${model.module.size})`);
});
```

---

## Delete Operations

### `deleteModel(module)`

Permanently delete a model from disk.

**Signature:**
```typescript
deleteModel(module: OCRModule): Promise<boolean>
```

**Parameters:**
- `module` (OCRModule): Model to delete

**Returns:**
- `Promise<boolean>` - `true` if deleted, `false` if not found

**Example:**
```typescript
const deleted = await VisionCore.deleteModel({
  type: 'shipping_label',
  size: 'large'
});

if (deleted) {
  console.log('Model deleted from disk');
} else {
  console.log('Model was not found on disk');
}
```

**Notes:**
- Model will be unloaded from memory if currently loaded
- Deletion is permanent - model must be re-downloaded to use again
- Frees up disk space

**Complete Cleanup Pattern:**
```typescript
// Unload from memory first (optional - delete will do this)
await VisionCore.unloadModel(module);

// Delete from disk
await VisionCore.deleteModel(module);
```

---

## Prediction with Specific Models

### `predictWithModule(module, imagePath, barcodes)`

Perform OCR prediction using a specific model.

**Signature:**
```typescript
predictWithModule(
  module: OCRModule,
  imagePath: string,
  barcodes: string[]
): Promise<any>
```

**Parameters:**
- `module` (OCRModule): The model to use for prediction
- `imagePath` (string): Path to image file or URI
- `barcodes` (string[]): Array of barcode strings detected in image

**Returns:**
- `Promise<any>` - Prediction result (structure varies by model type)

**Example:**
```typescript
const result = await VisionCore.predictWithModule(
  { type: 'shipping_label', size: 'large' },
  'file:///path/to/image.jpg',
  ['1234567890', '9876543210']
);

console.log('Prediction:', result);
```

**Model Types & Results:**

**Shipping Label:**
```typescript
{
  sender: {
    name?: string;
    address?: string;
    // ...
  };
  recipient: {
    name?: string;
    address?: string;
    // ...
  };
  trackingNumber?: string;
  // ...
}
```

**Item Label:**
```typescript
{
  productName?: string;
  sku?: string;
  barcode?: string;
  // ...
}
```

**Requires:**
- Model must be loaded into memory first
- Image must be accessible from the provided path

---

## Types & Interfaces

### OCRModule

Represents a specific model configuration.

```typescript
type OCRModule = {
  type: 'shipping_label' | 'item_label' | 'bill_of_lading' | 'document_classification';
  size: 'nano' | 'micro' | 'small' | 'medium' | 'large' | 'xlarge';
};
```

**Example:**
```typescript
const module: OCRModule = {
  type: 'shipping_label',
  size: 'large'
};
```

---

### ModelManagerConfig

Configuration for initializing the Model Manager.

```typescript
type ModelManagerConfig = {
  maxConcurrentDownloads?: number;
  enableLogging?: boolean;
};
```

---

### DownloadProgress

Progress information for model downloads.

```typescript
interface DownloadProgress {
  module: OCRModule;       // The module being downloaded
  progress: number;        // 0.0 to 1.0 (download percentage)
}
```

---

### ModelInfo

Information about a model on disk.

```typescript
type ModelInfo = {
  module: OCRModule;
  sizeInBytes: number;
  filePath: string;
  downloadedAt: string;    // ISO 8601 timestamp
  isLoaded: boolean;
};
```

---

### ExecutionProvider

Android-only: ML execution provider.

```typescript
type ExecutionProvider = 'CPU' | 'NNAPI' | 'XNNPACK';
```

**Recommendations:**
- `CPU`: Best compatibility, works on all devices
- `NNAPI`: Potentially faster on supported devices (Android 8.1+)
- `XNNPACK`: Optimized for ARM processors

---

## Error Handling

### Common Error Codes

```typescript
try {
  await VisionCore.downloadModel(module, apiKey, token);
} catch (error) {
  switch (error.code) {
    case 'MODEL_NOT_FOUND':
      // Invalid model type or size
      break;
    case 'NETWORK_ERROR':
      // No internet connection
      break;
    case 'AUTHENTICATION_FAILED':
      // Invalid API key or token
      break;
    case 'STORAGE_FULL':
      // Insufficient disk space
      break;
    case 'MODEL_ALREADY_DOWNLOADING':
      // Download in progress for this model
      break;
    default:
      console.error('Unexpected error:', error.message);
  }
}
```

### Best Practices

1. **Always handle errors:**
```typescript
try {
  await VisionCore.loadOCRModel(module, apiKey, token);
} catch (error) {
  Alert.alert('Error', `Failed to load model: ${error.message}`);
}
```

2. **Check status before operations:**
```typescript
// Check if downloaded before loading
const modelInfo = await VisionCore.findDownloadedModel(module);
if (!modelInfo) {
  await VisionCore.downloadModel(module, apiKey, token);
}

// Check if loaded before prediction
if (!VisionCore.isModelLoaded(module)) {
  await VisionCore.loadOCRModel(module, apiKey, token);
}
```

3. **Handle progress failures:**
```typescript
await VisionCore.downloadModel(module, apiKey, token, (progress) => {
  if (progress.status === 'failed') {
    Alert.alert('Download Failed', progress.error);
  }
});
```

---

## Platform Differences

### iOS vs Android

| Feature | iOS | Android |
|---------|-----|---------|
| **Initialization** | Not needed (hardcoded no-op) | **Required** (must call before operations) |
| **isModelManagerInitialized()** | Always returns `true` | Returns actual status |
| Model unloading | Granular (specific model) | Granular (specific model) |
| Execution provider | Not exposed | CPU, NNAPI, XNNPACK |
| Concurrent downloads | Supported | Supported |
| Model switching | Supported | Supported |
| Lifecycle events | Full support | Full support |

### Android-Specific Behavior

**Initialization Requirement:**
Android requires explicit initialization of the Model Manager before any model operations. Attempting to download, load, or query models without initialization will result in an error.

```typescript
// Android: This will fail if not initialized
await VisionCore.downloadModel(module, apiKey, token); // Error

// Android: Correct approach
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });
await VisionCore.downloadModel(module, apiKey, token); // Works

// iOS: Works without initialization (initialization methods are no-ops)
await VisionCore.downloadModel(module, apiKey, token); // Works
```

### iOS-Specific Behavior

**Initialization Not Required:**
iOS does not require initialization. The `initializeModelManager()` and `isModelManagerInitialized()` methods were added only for API consistency with Android:

- `initializeModelManager()` - Does nothing (hardcoded no-op)
- `isModelManagerInitialized()` - Always returns `true` (hardcoded)

You can safely call these methods for cross-platform code consistency, but they have no effect on iOS.

**Model Unloading:**
Both iOS and Android support granular model unloading. You can unload specific models without affecting other loaded models.

```typescript
// Both iOS and Android: Unloads only the specified model
await VisionCore.unloadModel({ type: 'shipping_label', size: 'large' });

// Other loaded models remain in memory
const stillLoaded = VisionCore.isModelLoaded({ type: 'item_label', size: 'medium' });
// Returns true if item_label was loaded
```

---

## Migration Guide

### From Deprecated API to New API

#### Loading Models

**Old (Deprecated):**
```typescript
// Will be removed in v3.0.0
await VisionCore.loadModel(
  token,
  apiKey,
  'shipping_label',
  'large'
);
```

**New (Recommended):**
```typescript
// Initialize once
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });

// Download (with progress tracking)
await VisionCore.downloadModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token,
  (progress) => console.log(`Progress: ${progress.progress * 100}%`)
);

// Load into memory
await VisionCore.loadOCRModel(
  { type: 'shipping_label', size: 'large' },
  apiKey,
  token
);
```

**Benefits:**
- Separate download and load phases
- Progress tracking per download
- Model caching (download once, load multiple times)

---

#### Unloading Models

**Old (Deprecated):**
```typescript
// Unload specific model
await VisionCore.unLoadModel('shipping_label', true);

// Unload all models
await VisionCore.unLoadModel(null, true);
```

**New (Recommended):**
```typescript
// Unload from memory (keeps file on disk)
await VisionCore.unloadModel({
  type: 'shipping_label',
  size: 'large'
});

// Delete from disk permanently
await VisionCore.deleteModel({
  type: 'shipping_label',
  size: 'large'
});
```

**Benefits:**
- Clear separation: unload vs delete
- Boolean return values for status checking
- Type-safe OCRModule parameter

---

#### Making Predictions

**Old (Deprecated):**
```typescript
// Uses currently configured model
const result = await VisionCore.predict(imagePath, barcodes);
```

**New (Recommended):**
```typescript
// Specify exact model to use
const result = await VisionCore.predictWithModule(
  { type: 'shipping_label', size: 'large' },
  imagePath,
  barcodes
);
```

**Benefits:**
- Explicit model selection
- No global state dependency
- Switch models without reconfiguration

---

### Complete Migration Example

**Before:**
```typescript
// Old API
import { VisionCore } from 'react-native-vision-sdk';

// Load model
VisionCore.addListener('onModelDownloadProgress', (progress) => {
  console.log('Progress:', progress.progress);
});

await VisionCore.loadModel(token, apiKey, 'shipping_label', 'large');

// Predict
const result = await VisionCore.predict(imagePath, barcodes);

// Cleanup
await VisionCore.unLoadModel('shipping_label', true);
```

**After:**
```typescript
// New API
import { VisionCore } from 'react-native-vision-sdk';

const module = { type: 'shipping_label', size: 'large' };

// Initialize
VisionCore.initializeModelManager({ maxConcurrentDownloads: 2 });

// Download with progress
await VisionCore.downloadModel(
  module,
  apiKey,
  token,
  (progress) => console.log('Progress:', progress.progress)
);

// Load
await VisionCore.loadOCRModel(module, apiKey, token);

// Predict
const result = await VisionCore.predictWithModule(module, imagePath, barcodes);

// Cleanup
await VisionCore.unloadModel(module);  // From memory
await VisionCore.deleteModel(module);  // From disk
```

---

## Complete Workflow Examples

### Download Multiple Models Concurrently

```typescript
const models = [
  { type: 'shipping_label', size: 'large' },
  { type: 'item_label', size: 'medium' },
  { type: 'bill_of_lading', size: 'large' }
];

// Initialize with concurrent downloads
VisionCore.initializeModelManager({ maxConcurrentDownloads: 3 });

// Download all models in parallel
const downloads = models.map(module =>
  VisionCore.downloadModel(module, apiKey, token, (progress) => {
    console.log(`${progress.module.type}: ${(progress.progress * 100).toFixed(1)}%`);
  })
);

// Wait for all downloads
await Promise.all(downloads);
console.log('All downloads complete');
```

---

### Switch Between Models

```typescript
const model1 = { type: 'shipping_label', size: 'large' };
const model2 = { type: 'item_label', size: 'medium' };

// Ensure both are downloaded
await VisionCore.downloadModel(model1, apiKey, token);
await VisionCore.downloadModel(model2, apiKey, token);

// Use first model
await VisionCore.loadOCRModel(model1, apiKey, token);
const result1 = await VisionCore.predictWithModule(model1, image1, barcodes1);

// Switch to second model
await VisionCore.unloadModel(model1);  // Free memory
await VisionCore.loadOCRModel(model2, apiKey, token);
const result2 = await VisionCore.predictWithModule(model2, image2, barcodes2);
```

---

### Check Before Operations

```typescript
const module = { type: 'shipping_label', size: 'large' };

// Check if downloaded
const info = await VisionCore.findDownloadedModel(module);
if (!info) {
  console.log('Model not downloaded, downloading now...');
  await VisionCore.downloadModel(module, apiKey, token);
}

// Check if loaded
if (!VisionCore.isModelLoaded(module)) {
  console.log('Model not loaded, loading now...');
  await VisionCore.loadOCRModel(module, apiKey, token);
}

// Now safe to predict
const result = await VisionCore.predictWithModule(module, imagePath, barcodes);
```

---

## See Also

- [MODEL_MANAGEMENT_IMPLEMENTATION.md](./MODEL_MANAGEMENT_IMPLEMENTATION.md) - Implementation status and technical details
- [README.md](./README.md) - Main SDK documentation
- [Example App](./example/src/ModelManagementScreen.tsx) - Complete working example

---

**Last Updated**: 2025-12-15
**Version**: 2.1.0
