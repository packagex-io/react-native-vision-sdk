# VisionSDK Sample-App Parity Issues

Logged while rebuilding `example/` to match the native Android (`vision-sdk-android/app`) and iOS (`VisionSDK Demo`) sample apps (2026-06-04). Two categories: **RN wrapper gaps** (native SDK feature not bridged to JS — blocks example-app parity until bridged) and **native demo divergences** (Android demo vs iOS demo differ from each other).

## A. RN wrapper gaps (native SDKs support it, RN wrapper does not expose it)

| # | Gap | Evidence | Impact on example rebuild |
|---|-----|----------|---------------------------|
| 1 | **Price Tag mode not bridged.** `onPriceTagResult` is an empty stub in `android/.../VisionCameraViewManager.kt`; `VisionCameraScanMode` (the type `VisionCamera.tsx` actually uses) has no `'priceTag'` even though legacy `src/types.ts` `ScanMode` does. No event emits `PriceTagDetectionResult`. | Android `DetectionMode.PriceTag`, iOS `.priceTag` both fully demoed natively (price validation list, bounding boxes, inventory). | Price Tag mode **excluded** from rebuilt example. |
| 2 | **Item Retrieval mode not bridged.** `onItemRetrievalResult` stubbed empty on Android bridge; no RN API to pass the items-to-retrieve list or receive match results (iOS returns `[String: Bool]` from the delegate). | Both native demos have full IR mode + inventory management screens. | IR mode + inventory screens **excluded** from rebuilt example. |
| 3 | **Native template-creation UI not bridged** (and not needed for JS parity). The native SDKs ship `startCreateTemplateScreen` (Android) / `GenerateTemplateController` (iOS), but the `template: TemplateData` prop on `VisionCamera` is already bridged on **both** platforms (Android: `applyTemplateJson` / `removeTemplate` on the view; iOS: `detectionSettings.selectedTemplate`). Template creation is fully achievable in JS: scan barcodes in creation mode → collect into `TemplateCode[]` → persist via AsyncStorage → pass as `template` prop. See §D #15 — this was **wrongly logged as a gap**. | Android `TemplateSelectionActivity`, iOS `TemplateSelectionView` demo create/rename/delete natively. | Template creation, management, and application **implemented** in rebuilt example via JS-side flow. |
| 4 | **Model lifecycle events not bridged.** `ModelLifecycleListener` type exists in `src/types.ts` but the Android `VisionSdkModule.kt` listener wiring is commented out; only `onModelDownloadProgress` reaches JS. | Android `ModelManager.initialize { lifecycleListener(...) }`. | Example must poll (`findDownloadedModels`/`isModelLoaded`) after each operation instead of reacting to push events. |
| 5 | **`checkModelUpdates()` / `getActiveDownloadCount()` deliberately disabled** ("NOT AVAILABLE IN iOS SDK — COMMENTED OUT FOR API CONSISTENCY" in `VisionCoreWrapper.ts`), but the Android demo has a "Check Updates" button and an "Active Downloads: N" stat. | Android model-management panel. | "Check Updates" button and active-download counter **excluded** from rebuilt example. |
| 6 | **Camera orientation mode not exposed.** Android `CameraSettings(orientationMode = SENSOR/PORTRAIT/LANDSCAPE/AUTO)` has no RN prop. | `MainActivity.kt` uses `SENSOR`. | Example locked to default orientation behavior. |
| 7 | **Wild Card Scan / connectivity-fallback not a first-class prop.** Natives demo a settings toggle that routes capture → document classification → per-class OCR. RN can *emulate* it in JS (capture → `predictDocumentClassificationCloud`/`predictWithModule(DC)` → route), but there is no native-managed mode. | Android `WildCardScanMode` + iOS `isWildScanOn`. | Rebuilt example implements wild-card **in JS** via chained predict calls (parity in behavior, not in mechanism). |
| 8 | **No sound/vibration feedback bridged.** Both natives play success/error sounds or vibrate (3-state Sound/Vibrate/Silent toggle). RN exposes nothing. | Android `SoundOption`, iOS `selectedSoundMode`. | Rebuilt example uses RN `Vibration` API; "Sound" state is best-effort (no bundled beep without adding a sound dep). |
| 9 | **OBB rotation (`angleDeg`) missing from `onBoundingBoxesUpdate`.** Android emits oriented boxes natively; the bridge JSON drops `angleDeg`. | `CallbackBoxOverlayView` in Android demo handles `ScannedCodeResult.angleDeg`. | JS overlays can only draw axis-aligned boxes. Use `showCodeBoundingBoxes` (native overlay) for parity. |
| 10 | **Latency logger not bridged.** Android demo has share/clear latency-log buttons gated on `LatencyLoggerManager.isEnabled()`. | `MainActivity.kt`. | **Excluded** from rebuilt example. |
| 11 | **Report-an-issue API not bridged.** Android demo's "Report" flow calls `ApiManager.reportAnIssueAsync(...)` with checked error fields + response JSON + resized image; iOS has the equivalent `ErrorReportView` → `reportError(...)`. RN exposes only the `log*DataToPx` confirmation-logging methods, which are a different endpoint/purpose. `ReportErrorType`/`*ErrorFlags` types exist in `src/types.ts` but no method consumes them. | Android `report_text_view.xml` flow; iOS `ErrorReportView`. | Rebuilt example shows the Report UI but wires it to the closest available `log*DataToPx` call, clearly labeled. |
| 12 | **`barcodesinglecapture` unhandled on Android bridge.** TS enum includes it, but `VisionCameraViewManager.kt`'s mode switch has no case — falls through to plain `Barcode`. iOS handling unverified. | `VisionCameraViewManager.kt` line ~228 `else` branch. | Single-vs-multiple capture toggle in the example may behave identically to `barcode` on Android. |
| 13 | **VLM Cloud (receipt/invoice) mode not bridged.** iOS demo has "VLM Cloud" in its 9-item API picker with `ReceiptResponseView`/`InvoiceResponseView`. No RN predict method maps to it. | iOS `APIMode.lvlmCloud`. | **Excluded** from rebuilt example. |

## B. Native demo divergences (Android demo ≠ iOS demo)

| # | Divergence | Android demo | iOS demo |
|---|------------|--------------|----------|
| 1 | **Dimensioning** | Absent | Present (ARKit/LiDAR sheet, offline, cm, maxTrackCount 5). RN exposes `DimensioningView` (iOS 17+ only) — kept in rebuilt example, iOS-only. |
| 2 | **Text Templates sub-app** | Absent | Present (entire self-contained sub-app: server/email setup, template scanner, debug/demo result views). Not part of VisionSDK's RN surface — excluded from rebuild. |
| 3 | **OCR mode selection UX** | Two controls: Online/On-Device radio + 4-item module spinner | Single 9-item picker (module × cloud/on-device + VLM) |
| 4 | **Model management UX** | Dedicated overlay panel with class+size spinners, status, stats, 7 buttons | "Models" section inside Settings sheet listing 7 fixed model rows with per-row actions |
| 5 | **Model sizes offered** | Nano / Micro / Large radio in settings | Micro / Large only (per-model fixed list) |
| 6 | **Zoom presets** | min/1x/2x buttons + continuous slider behind an arrow toggle | Fixed 1x/1.5x/2x (OCR) or 1x/2x/3x (barcode/QR); no slider. Default zoom: iOS OCR 1.5x, barcode/QR 2x; Android defaults 1x. |
| 7 | **Inactivity handling** | 60s no-detection → camera stops, "Start Camera" overlay | None |
| 8 | **Gallery import for OCR** | Absent | Present (photo library → OCR pipeline) |
| 9 | **Emulator behavior** | Capture on emulator returns bundled sample images | None |
| 10 | **Default environment/key** | Hardcoded staging key in `MainActivity.onCreate` | 4-env enum in `ContentView.swift`, current = sandbox (Secrets.xcconfig exists but unused) |
| 11 | **Manual/Auto default** | Manual | Document Auto Capture default ON (OCR) |
| 12 | **Sharpness gate threshold** | `Sharpness.NORMALIZED_THRESHOLD` | hardcoded `0.5` |

## C. Example-app-side issues found during audit

1. `example/` reads `process.env.PACKAGEX_API_KEY` but no dotenv/babel transform is wired — the value is always `undefined` at runtime. Rebuild uses an explicit `example/src/config.ts` (gitignored key) instead.
2. Home screen showed hardcoded "v3.0.0" while package is at 3.0.10+.
3. `syncWithPx()` util (`example/src/utils/index.ts`) was unreachable from any screen.

## D. Gaps discovered during 2026-06-04 rebuild

| # | Gap | Notes |
|---|-----|-------|
| 14 | **Tap-to-Focus / Pinch-Zoom not independently togglable via RN API.** `setFocusSettings` accepts `shouldDisplayFocusImage` which shows the focus UI, and `shouldScanInFocusImageRect` (true/false), but there is no dedicated prop for enabling/disabling touch-to-focus or pinch-to-zoom as separate concerns. The native iOS `FocusSettings` and Android `FocusSettings` control different subsets of behavior. Rebuilt example exposes toggles in Settings that call `setFocusSettings` as the closest available lever, but the mapping is approximate. |
| 15 | **Template row — RESOLVED (wrongly logged as a gap).** The `template: TemplateData` prop on `VisionCamera` is bridged on both platforms. Template *creation* does not require `startCreateTemplateScreen` / `GenerateTemplateController` — a JS-side scan-and-collect flow (detect barcodes → accumulate `TemplateCode[]` → persist via AsyncStorage → pass as `template` prop) delivers equivalent UX. The rebuilt example now includes: a "Templates" pill in barcode/QR scan modes (shows active state), a `TemplateManagerModal` (list/apply/remove/delete/create), and a creation mode where detected barcodes auto-accumulate with a live bottom panel for review. See §A #3 for updated wrapper-gap assessment. |
| 16 | **Sound playback not implemented.** RN has no built-in beep/success-sound mechanism without a third-party dep. Settings expose Sound/Vibrate/Silent toggle; Vibrate and Silent work via `react-native`'s `Vibration` API. "Sound" mode falls back to vibration in the example (no beep played). Adding a sound dependency was ruled out per constraints. |
| 17 | **Mode picker uses bottom sheet, not native dropdown popup.** The Android demo uses a native Android Spinner widget whose dropdown appears as a popup overlay anchored below the spinner. RN does not have a native equivalent, so the rebuilt example uses a custom bottom sheet (`SheetPicker`). Visual difference: the picker slides in from the bottom instead of appearing inline. |
| 18 | **Zoom slider uses amber/yellow color, not blue.** The Android demo's zoom slider is styled with the Android system SeekBar (blue/purple thumb and track). Our RN implementation uses the app's amber accent color (#FFC107) for theme consistency. |
