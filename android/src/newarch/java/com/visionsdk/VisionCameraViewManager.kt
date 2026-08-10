package com.visionsdk

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter
import io.packagex.visionsdk.core.DetectionMode
import io.packagex.visionsdk.core.ScanningMode
import io.packagex.visionsdk.dto.ScannedCodeResult
import io.packagex.visionsdk.exceptions.VisionSDKException
import io.packagex.visionsdk.interfaces.CameraLifecycleCallback
import io.packagex.visionsdk.interfaces.ScannerCallback
import io.packagex.visionsdk.ui.views.VisionCameraView
import com.visionsdk.utils.toDp

/**
 * Fabric-compatible ViewManager for VisionCameraView
 * This wraps the actual VisionCameraView from the VisionSDK
 *
 * Note: On Android, both VisionCameraView and VisionSdkView use the same
 * underlying VisionCameraView from the SDK.
 */
@ReactModule(name = VisionCameraViewManager.REACT_CLASS)
class VisionCameraViewManager(private val appContext: ReactApplicationContext) :
    ViewGroupManager<VisionCameraView>(),
    com.facebook.react.viewmanagers.VisionCameraViewManagerInterface<VisionCameraView> {

    companion object {
        const val REACT_CLASS = "VisionCameraView"
        const val TAG = "VisionCameraView Fabric"
    }

    // Fabric delegate — without this, Fabric can't route props to the @ReactProp
    // setters below and every prop (scanMode, zoomLevel, detectionConfigJson, …)
    // is silently dropped.
    private val viewManagerDelegate: com.facebook.react.uimanager.ViewManagerDelegate<VisionCameraView> =
        com.facebook.react.viewmanagers.VisionCameraViewManagerDelegate<VisionCameraView, VisionCameraViewManager>(this)

    override fun getDelegate(): com.facebook.react.uimanager.ViewManagerDelegate<VisionCameraView> =
        viewManagerDelegate

    private var visionCameraView: VisionCameraView? = null
    private var hasStarted = false
    private var currentCallback: ViewCallback? = null
    private var isCameraReady = false
    private var pendingScanArea: com.facebook.react.bridge.ReadableMap? = null
    private var hasScanAreaBeenSet = false // Track if scanArea prop was explicitly set
    // Per-view native-overlay state. A single ViewManager is shared across all
    // VisionCameraView instances, so storing these as ViewManager fields would
    // leak the most-recently-configured view's style + pending-enabled flag
    // into every other camera (and across remounts). WeakHashMap entries clear
    // when the view is GC'd, so we don't pin destroyed views in memory.
    private data class OverlayState(
        // Pending value for `showCodeBoundingBoxes` — applied on
        // `onCameraStarted` because `getFocusRegionManager()` throws during
        // Fabric preallocateView.
        var pendingShowCodeBoundingBoxes: Boolean? = null,
        // Defaults match Skia purple from the prior JS path.
        var borderColor: Int = android.graphics.Color.rgb(139, 92, 246),
        var borderWidthDp: Float = 3f,
        var fillColor: Int = android.graphics.Color.argb(51, 139, 92, 246),
    )

    private val overlayStates = java.util.WeakHashMap<VisionCameraView, OverlayState>()

    private fun overlayStateFor(view: VisionCameraView): OverlayState =
        overlayStates.getOrPut(view) { OverlayState() }
    private var currentDetectionMode: DetectionMode = DetectionMode.Photo // Track current detection mode
    private val density = appContext.resources.displayMetrics.density

    // Camera Controls API (Phase 3) — per-view CameraSettings, tracked the same way as
    // overlayStates above (a single ViewManager instance is shared across all camera views).
    private val cameraSettingsByView = java.util.WeakHashMap<VisionCameraView, io.packagex.visionsdk.config.CameraSettings>()
    private fun cameraSettingsFor(view: VisionCameraView): io.packagex.visionsdk.config.CameraSettings =
        cameraSettingsByView.getOrPut(view) { io.packagex.visionsdk.config.CameraSettings() }

    // Camera Controls API (Phase 3) — last JS-declared value for the "persistent" control
    // props (torch/zoomRatio/focusMode/pinnedLensId). The underlying native camera core
    // resets these on a genuine facing/lens change (input-device swap), so we re-apply them
    // ourselves on every transition into CameraStatus.RUNNING (§8). Legacy aliases
    // (enableFlash/zoomLevel) and the setTorchEnabled/setZoom commands feed the same
    // tracked state via applyTorch/applyZoomRatio so reassertion works regardless of
    // which prop/command name a consumer used. setFocusPoint is a one-shot action and is
    // deliberately NOT tracked/reasserted here.
    private data class ControlPropsState(
        var torch: Boolean = false,
        var zoomRatio: Double = 1.0,
        var focusMode: String = "continuous",
        var pinnedLensId: String? = null,
        // Review fix (Group D #4) — tracks the lens selection actually applied via
        // applyLensSelection, so reassertControlProps can skip re-applying an unchanged
        // selection. setLensSelection -> cameraSession.update -> performReconcile does an
        // UNCONDITIONAL unbind/rebind, so reasserting on every RUNNING transition (incl.
        // ordinary start with no pin) was flapping isPreviewActive / double-binding at
        // startup. The lens already persists across rebinds via cameraConfigurationFromSettings
        // (Group A), so we only need to re-apply when the resolved selection actually changes.
        var lastAppliedLensId: String? = null,
        var lensApplied: Boolean = false,
        // Review fix (parity #1) — last CameraLensFace delivered via the `cameraFacing`
        // prop (setCameraFacing), tracked independently of
        // cameraSettingsByView[view].cameraLensFace because applyLensSelection's
        // cross-facing pin path overwrites that settings object's facing to match the
        // PINNED lens. Without a separate record of the prop's own value, unpinning
        // (Auto) had nothing to restore to and Android was left stuck on whichever
        // facing the last pin happened to use — iOS's cameraFacing is a plain stored
        // prop and correctly falls back to it on unpin (see RNVisionCameraView.swift
        // syncFacing/currentFacing). See applyLensSelection's Auto/unknown-lens branches.
        var propCameraFacing: io.packagex.visionsdk.core.CameraLensFace = io.packagex.visionsdk.core.CameraLensFace.Back,
    )
    private val controlPropsByView = java.util.WeakHashMap<VisionCameraView, ControlPropsState>()
    private fun controlPropsFor(view: VisionCameraView): ControlPropsState =
        controlPropsByView.getOrPut(view) { ControlPropsState() }

    private fun applyTorch(view: VisionCameraView, enabled: Boolean) {
        controlPropsFor(view).torch = enabled
        view.setFlashTurnedOn(enabled)
    }

    private fun applyZoomRatio(view: VisionCameraView, ratio: Float) {
        controlPropsFor(view).zoomRatio = ratio.toDouble()
        view.setZoomRatio(ratio)
    }

    // Duration-based ramp — parity with iOS's rampZoomRatio (spec §8 follow-up). Tracks
    // the FINAL target in controlPropsFor (same as applyZoomRatio) so a facing/lens
    // switch mid-ramp reasserts the target, not whatever the ticker had reached — the
    // ramp itself is driven by CameraController/SessionReconciler (see
    // VisionCameraView.rampZoomRatio's doc), not by this Fabric-facing wrapper.
    private fun applyRampZoomRatio(view: VisionCameraView, ratio: Float, durationMs: Long) {
        controlPropsFor(view).zoomRatio = ratio.toDouble()
        view.rampZoomRatio(ratio, durationMs)
    }

    private fun focusModeFromString(mode: String?): io.packagex.visionsdk.camera.core.FocusMode =
        when (mode?.lowercase()) {
            "single" -> io.packagex.visionsdk.camera.core.FocusMode.SINGLE
            "locked" -> io.packagex.visionsdk.camera.core.FocusMode.LOCKED
            else -> io.packagex.visionsdk.camera.core.FocusMode.CONTINUOUS
        }

    private fun applyFocusMode(view: VisionCameraView, mode: String?) {
        controlPropsFor(view).focusMode = mode ?: "continuous"
        view.setFocusMode(focusModeFromString(mode))
    }

    // Review fix (parity #1, Android/iOS unpin restore) — bring the view's camera
    // facing back in line with the last `cameraFacing` prop value. Mirrors iOS's
    // `syncFacing(to: currentFacing, ...)` calls in its Auto/unknown-lens branches:
    // a cross-facing pin overwrites cameraSettingsByView's facing to match the
    // PINNED lens (see the bottom of this function), and nothing else ever resets
    // it back, so unpinning left Android on the wrong camera while iOS correctly
    // returned to the `cameraFacing` prop. No-op when facing already matches (same
    // guard style as the cross-facing pin path below).
    private fun restoreFacingFromProp(view: VisionCameraView) {
        val propFacing = controlPropsFor(view).propCameraFacing
        if (cameraSettingsFor(view).cameraLensFace != propFacing) {
            val updated = cameraSettingsFor(view).copy(cameraLensFace = propFacing)
            cameraSettingsByView[view] = updated
            view.setCameraSettings(updated)
        }
    }

    private fun applyLensSelection(view: VisionCameraView, lensId: String?) {
        // Review fix (Group D #5) — a cleared Fabric optional-string prop can arrive as
        // "" rather than null; treat it the same as unpin/Auto (matches iOS).
        if (lensId.isNullOrEmpty()) {
            controlPropsFor(view).apply {
                lastAppliedLensId = null
                lensApplied = true
            }
            restoreFacingFromProp(view)
            view.setLensSelection(io.packagex.visionsdk.camera.core.LensSelection.Auto)
            return
        }
        controlPropsFor(view).apply {
            lastAppliedLensId = lensId
            lensApplied = true
        }
        // Bug fix (QA Group A #2) — a pinnable lens id only ever appears under its OWN
        // facing's list (a front lens is never returned by lenses(BACK) and vice versa), so
        // gating this lookup to whatever facing happens to be currently applied meant
        // pinning a lens from the OTHER facing silently fell back to Auto with a warning —
        // reproduced on-device: pin id=5 (front) while cameraFacing is still "back" logs
        // "pinnedLensId '5' unknown or unpinnable for facing=BACK". Search both facings so
        // the pin resolves regardless of prop-set order, then bring the camera's facing in
        // line with whichever facing the resolved lens actually belongs to.
        val snapshot = io.packagex.visionsdk.camera.core.CameraCapabilities.snapshot(appContext)
        val lens = (
            snapshot.lenses(io.packagex.visionsdk.camera.core.LensFacing.BACK) +
                snapshot.lenses(io.packagex.visionsdk.camera.core.LensFacing.FRONT)
            ).firstOrNull { it.id == lensId && it.isPinnable }
        if (lens == null) {
            Log.w(TAG, "pinnedLensId '$lensId' unknown or unpinnable — falling back to Auto")
            restoreFacingFromProp(view)
            view.setLensSelection(io.packagex.visionsdk.camera.core.LensSelection.Auto)
            markPendingLensUnavailableWarning(view)
            return
        }
        // setLensSelection() builds its CameraConfiguration from cameraSettingsFacing()
        // (see VisionCameraView.setLensSelection), so facing and the pinned lens must agree
        // before we hand it off — otherwise the SDK is asked to pin a lens under a facing
        // it doesn't belong to.
        val neededFacing = if (lens.facing == io.packagex.visionsdk.camera.core.LensFacing.FRONT)
            io.packagex.visionsdk.core.CameraLensFace.Front
        else io.packagex.visionsdk.core.CameraLensFace.Back
        if (cameraSettingsFor(view).cameraLensFace != neededFacing) {
            val updated = cameraSettingsFor(view).copy(cameraLensFace = neededFacing)
            cameraSettingsByView[view] = updated
            view.setCameraSettings(updated)
        }
        view.setLensSelection(io.packagex.visionsdk.camera.core.LensSelection.Pin(lens))
    }

    // Re-apply the last JS-declared control props onto a fresh camera session. Called on
    // every transition into CameraStatus.RUNNING (initial start, and post facing/lens-switch
    // stop+start cycles) since the native core resets torch/zoom/focusMode on those (§5.4).
    // Torch/zoom/focusMode are cheap runtime settings and always reassert. The lens selection
    // is different — setLensSelection triggers a full unbind/rebind (Group D #4) — so it's
    // only re-applied when the resolved selection actually differs from what's currently
    // applied; otherwise an ordinary start-with-no-pin would unbind/rebind for nothing.
    private fun reassertControlProps(view: VisionCameraView) {
        val props = controlPropsByView[view] ?: return
        view.setFlashTurnedOn(props.torch)
        view.setZoomRatio(props.zoomRatio.toFloat())
        view.setFocusMode(focusModeFromString(props.focusMode))
        if (!props.lensApplied || props.lastAppliedLensId != props.pinnedLensId) {
            applyLensSelection(view, props.pinnedLensId)
        }
    }

    // Camera Controls API (Phase 3) — one-shot warning stashed when a requested
    // pinnedLensId can't be resolved; merged into the very next emitted CameraStateEvent
    // (bypassing the throttle) rather than fired as its own event.
    private data class PendingLensWarning(var flagged: Boolean = false)
    private val pendingLensWarnings = java.util.WeakHashMap<VisionCameraView, PendingLensWarning>()
    private fun markPendingLensUnavailableWarning(view: VisionCameraView) {
        pendingLensWarnings.getOrPut(view) { PendingLensWarning() }.flagged = true
    }

    // Camera Controls API (Phase 3) — throttled onCameraStateChanged emission bookkeeping.
    private val CAMERA_STATE_THROTTLE_MS = 100L // 10 Hz, matches RECOGNITION_UPDATE_THROTTLE_MS convention
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private data class CameraStateEmitState(
        var lastEmitTime: Long = 0L,
        var lastStatus: String? = null,
        // Review fix (Group D #1, trailing-edge throttle) — the runnable scheduled to
        // deliver the latest dropped state at the window boundary, so a burst ending
        // mid-window doesn't leave JS stuck on a stale value. Replaced (not queued) on
        // every subsequent drop so only the LATEST state is ever pending.
        var pendingTrailingRunnable: Runnable? = null,
    )
    private val cameraStateEmitStates = java.util.WeakHashMap<VisionCameraView, CameraStateEmitState>()
    private val cameraStateListenersByView = java.util.WeakHashMap<VisionCameraView, io.packagex.visionsdk.camera.core.CameraStateListener>()

    private fun mapCameraErrorCode(error: io.packagex.visionsdk.camera.core.CameraError): String =
        when (error) {
            is io.packagex.visionsdk.camera.core.CameraError.PermissionDenied -> "permission-denied"
            is io.packagex.visionsdk.camera.core.CameraError.LensUnavailable -> "lens-unavailable"
            is io.packagex.visionsdk.camera.core.CameraError.ConfigurationFailed -> "configuration-failed"
        }

    private fun cameraStateToMap(
        view: VisionCameraView,
        state: io.packagex.visionsdk.camera.core.CameraState
    ): com.facebook.react.bridge.WritableMap {
        val map = Arguments.createMap()
        map.putString("status", state.status.name.lowercase())
        state.error?.let {
            map.putString("errorCode", mapCameraErrorCode(it))
            map.putString("errorMessage", it.toString())
        }
        val pendingWarning = pendingLensWarnings[view]
        val warning = state.warning
        if (warning != null) {
            map.putString("warningCode", mapCameraErrorCode(warning))
            map.putString("warningMessage", warning.toString())
        } else if (pendingWarning?.flagged == true) {
            map.putString("warningCode", "lens-unavailable")
            map.putString("warningMessage", "pinnedLensId unavailable — falling back to Auto")
            pendingWarning.flagged = false // one-shot; consumed
        }
        map.putString("facing", if (state.facing == io.packagex.visionsdk.camera.core.LensFacing.FRONT) "front" else "back")
        state.activeLens?.let { map.putString("activeLensId", it.id) }
        map.putDouble("zoomRatio", state.zoomRatio.toDouble())
        map.putDouble("minZoomRatio", state.minZoomRatio.toDouble())
        map.putDouble("maxZoomRatio", state.maxZoomRatio.toDouble())
        map.putBoolean("torchEnabled", state.isTorchEnabled)
        map.putString("focusMode", state.focusMode.name.lowercase())
        map.putBoolean("isPreviewActive", state.isPreviewActive)
        return map
    }

    private fun emitCameraState(view: VisionCameraView, state: io.packagex.visionsdk.camera.core.CameraState, bypassThrottle: Boolean) {
        val emitState = cameraStateEmitStates.getOrPut(view) { CameraStateEmitState() }
        val newStatus = state.status.name
        val statusChanged = emitState.lastStatus != newStatus
        if (statusChanged && newStatus.equals("RUNNING", ignoreCase = true)) {
            reassertControlProps(view)
        }
        // Review fix (Group D #2) — a pending lens-unavailable warning must bypass the
        // throttle immediately (it's a one-shot notice, not a value that survives being
        // superseded), not just ride along on whatever unrelated emission happens next.
        val hasPendingLensWarning = pendingLensWarnings[view]?.flagged == true
        val now = System.currentTimeMillis()
        val bypass = bypassThrottle || statusChanged || state.error != null || state.warning != null || hasPendingLensWarning
        val shouldEmit = bypass || shouldEmitThrottledEvent(emitState.lastEmitTime, CAMERA_STATE_THROTTLE_MS)

        // Any newer state (emitted or dropped) supersedes a previously scheduled trailing
        // emit — cancel it so we never deliver a stale value after a fresher one.
        emitState.pendingTrailingRunnable?.let { mainHandler.removeCallbacks(it) }
        emitState.pendingTrailingRunnable = null

        if (!shouldEmit) {
            // Review fix (Group D #1) — trailing-edge throttle. This is a drop-throttle:
            // without this, a burst ending inside the throttle window leaves JS with a
            // stale value forever. Schedule the LATEST dropped state to fire at the
            // window boundary; a later emit (immediate or another scheduled trailing
            // one) will cancel/replace this via the removeCallbacks above.
            val delay = (CAMERA_STATE_THROTTLE_MS - (now - emitState.lastEmitTime)).coerceIn(0L, CAMERA_STATE_THROTTLE_MS)
            val runnable = Runnable { doEmitCameraState(view, state, emitState) }
            emitState.pendingTrailingRunnable = runnable
            mainHandler.postDelayed(runnable, delay)
            return
        }
        doEmitCameraState(view, state, emitState)
    }

    private fun doEmitCameraState(view: VisionCameraView, state: io.packagex.visionsdk.camera.core.CameraState, emitState: CameraStateEmitState) {
        emitState.lastEmitTime = System.currentTimeMillis()
        emitState.lastStatus = state.status.name
        emitState.pendingTrailingRunnable = null
        // Review fix (C1) — view.context is the FragmentActivity passed into
        // VisionCameraView(activity, null) at construction (createViewInstance), never a
        // ThemedReactContext, so `view.context as? ThemedReactContext` always failed and
        // silently dropped every onCameraStateChanged emission. Resolve the JS module from
        // the captured ReactApplicationContext instead — the same path ViewCallback.sendEvent
        // already uses successfully for the other camera events.
        try {
            appContext.getJSModule(RCTEventEmitter::class.java)
                .receiveEvent(view.id, "onCameraStateChanged", cameraStateToMap(view, state))
        } catch (e: Exception) {
            // The state callback can arrive after the view/context has been torn down
            // (e.g. mid onDropViewInstance) — never crash the camera pipeline over a
            // best-effort JS event.
            Log.w(TAG, "Failed to emit onCameraStateChanged (view/context likely torn down): ${e.message}")
        }
    }

    // Event throttling - timestamps for last emitted events
    private var lastSharpnessScoreUpdateTime = 0L

    // Throttle intervals in milliseconds
    private val SHARPNESS_SCORE_UPDATE_THROTTLE_MS = 200L // 5 FPS

    override fun getName(): String = REACT_CLASS

    // Let the native VisionCameraView layout its own children (camera preview surface)
    // Without this, Yoga overrides child layout and the preview gets cropped/zoomed
    override fun needsCustomLayoutForChildren(): Boolean = true

    override fun createViewInstance(context: ThemedReactContext): VisionCameraView {
        Log.d(TAG, "createViewInstance")

        val activity = appContext.currentActivity as? androidx.fragment.app.FragmentActivity
            ?: throw IllegalStateException("Activity must be a FragmentActivity")

        val newView = VisionCameraView(activity, null)

        // Set layout parameters to ensure view is visible
        newView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Initialize with default settings
        newView.configure(
            isMultipleScanEnabled = false,
            detectionMode = DetectionMode.Photo,
            scanningMode = ScanningMode.Manual
        )

        // Create a new callback instance for this view
        val callback = ViewCallback(newView, appContext)
        newView.setCameraLifecycleCallback(callback)
        newView.setScannerCallback(callback)

        // Camera Controls API (Phase 3) — subscribe to full CameraState changes so
        // `useCameraControls().state` tracks the camera going forward. The one-shot
        // replay-on-attach emit (§8) is deliberately NOT done here — see
        // onAfterUpdateTransaction (Group D review fix #3): at this point Fabric hasn't
        // assigned the view's react tag yet, so receiveEvent(NO_ID, ...) is silently
        // discarded and JS never mounted its handlers anyway.
        val cameraStateListener = io.packagex.visionsdk.camera.core.CameraStateListener { state ->
            emitCameraState(newView, state, bypassThrottle = false)
        }
        newView.addCameraStateListener(cameraStateListener)
        cameraStateListenersByView[newView] = cameraStateListener

        // Update the current view reference and callback
        visionCameraView = newView
        currentCallback = callback

        Log.d(TAG, "VisionCameraView created and configured (id: ${newView.id})")

        return newView
    }

    // Review fix (Group D #3) — one-shot guard for the replay-on-attach emit below.
    // WeakHashMap so a dropped/GC'd view doesn't pin this entry.
    private val hasReplayedInitialCameraState = java.util.WeakHashMap<VisionCameraView, Boolean>()

    // Bug fix (post QA Group A #1 regression) — the mount-time "start camera once" gate
    // below used to share `hasStarted` with the stop()/start() command guards. This QA
    // screen streams CameraState into props at ~10Hz (CAMERA_STATE_THROTTLE_MS), so Fabric
    // calls onAfterUpdateTransaction continuously. Once stop() started resetting
    // `hasStarted = false` (so a later start() command actually restarts), the very next
    // incidental prop-commit hit THIS block instead and silently auto-restarted the camera
    // behind Stop's back — on a view whose previewView was never detached, which recursively
    // re-triggers its own attach listener inside VisionCameraView.addViewFun() (SDK-side
    // reentrancy bug) and freezes the main thread. Reproduced on-device: Stop → camera
    // silently resumes within ~100ms; a later Stop during heavier re-render traffic instead
    // hit the SDK recursion and hung the UI. Track "have we auto-started this view on
    // mount" separately, per-view, so stop()'s hasStarted reset can never re-arm it.
    private val hasAutoStartedOnMount = java.util.WeakHashMap<VisionCameraView, Boolean>()

    override fun onAfterUpdateTransaction(view: VisionCameraView) {
        super.onAfterUpdateTransaction(view)

        // Request layout to ensure proper sizing
        view.requestLayout()

        // Camera Controls API (Phase 3) — one-shot replay of the current CameraState so
        // `useCameraControls().state` is never stale-undefined on an already-running
        // camera (§8 replay-on-attach). Relocated here from createViewInstance (Group D
        // review fix #3): by the time onAfterUpdateTransaction runs, Fabric has assigned
        // the view's real react tag and committed initial props, so this is the earliest
        // point the emit is guaranteed to reach a mounted JS handler.
        if (hasReplayedInitialCameraState[view] != true) {
            hasReplayedInitialCameraState[view] = true
            emitCameraState(view, view.currentCameraState(), bypassThrottle = true)
        }

        // Only auto-start the camera once per view (on initial mount) — gated on its OWN
        // flag, deliberately NOT `hasStarted`, so an explicit stop() can never re-arm this
        // block on the next incidental prop-commit (see hasAutoStartedOnMount comment).
        if (visionCameraView == view && hasAutoStartedOnMount[view] != true) {
            hasAutoStartedOnMount[view] = true
            hasStarted = true
            Log.d(TAG, "Starting camera for view id: ${view.id}")

            // Wait for view to be attached to window
            if (view.isAttachedToWindow) {
                view.post {
                    Log.d(TAG, "Starting camera with view dimensions: ${view.width}x${view.height}")
                    view.startCamera()
                }
            } else {
                view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        Log.d(TAG, "View attached to window, starting camera")
                        view.removeOnAttachStateChangeListener(this)
                        view.post {
                            Log.d(TAG, "Starting camera with view dimensions: ${view.width}x${view.height}")
                            view.startCamera()
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        // No-op
                    }
                })
            }
        }
    }

    override fun onDropViewInstance(view: VisionCameraView) {
        super.onDropViewInstance(view)
        Log.d(TAG, "Dropping view instance with id: ${view.id}")

        // Only reset if this is the currently active view
        if (visionCameraView == view) {
            Log.d(TAG, "Resetting state for currently active view")
            hasStarted = false
            visionCameraView = null
            currentCallback = null
        }

        // Cancel any pending trailing-edge emit (Group D #1) — no point delivering a
        // stale camera state to a view that's already been torn down.
        cameraStateEmitStates[view]?.pendingTrailingRunnable?.let { mainHandler.removeCallbacks(it) }

        // Camera Controls API (Phase 3) — unregister the state listener. The listener's
        // callback can still be invoked concurrently/after this point; emitCameraState's
        // own try/catch keeps that path safe.
        cameraStateListenersByView[view]?.let {
            try {
                view.removeCameraStateListener(it)
            } catch (e: Exception) {
                Log.w(TAG, "removeCameraStateListener failed: ${e.message}")
            }
        }
        cameraStateListenersByView.remove(view)

        // Stop the camera
        try {
            view.stopCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera: ${e.message}")
        }
    }

    /**
     * Check if enough time has passed since the last event emission to throttle high-frequency events
     * @param lastTime The timestamp of the last emission
     * @param throttleMs The throttle interval in milliseconds
     * @return true if the event should be emitted, false if it should be skipped
     */
    private fun shouldEmitThrottledEvent(lastTime: Long, throttleMs: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastTime) >= throttleMs
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        return mutableMapOf(
            "onCapture" to mapOf("registrationName" to "onCapture"),
            "onError" to mapOf("registrationName" to "onError"),
            "onRecognitionUpdate" to mapOf("registrationName" to "onRecognitionUpdate"),
            "onSharpnessScoreUpdate" to mapOf("registrationName" to "onSharpnessScoreUpdate"),
            "onBarcodeDetected" to mapOf("registrationName" to "onBarcodeDetected"),
            "onBoundingBoxesUpdate" to mapOf("registrationName" to "onBoundingBoxesUpdate"),
            "onCameraStateChanged" to mapOf("registrationName" to "onCameraStateChanged"),
            "onCameraStopped" to mapOf("registrationName" to "onCameraStopped")
        )
    }

    // MARK: - Props

    @ReactProp(name = "enableFlash")
    override fun setEnableFlash(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setEnableFlash: $enabled")
        applyTorch(view, enabled)
    }

    @ReactProp(name = "zoomLevel")
    override fun setZoomLevel(view: VisionCameraView, level: Double) {
        Log.d(TAG, "setZoomLevel: $level")
        applyZoomRatio(view, level.toFloat())
    }

    // Camera Controls API (Phase 3) — canonical props; enableFlash/zoomLevel above are
    // deprecated aliases feeding the exact same applyTorch/applyZoomRatio path so
    // reassertion-after-facing/lens-change (see reassertControlProps) works regardless
    // of which prop name a consumer uses. Prop-collision precedence ("new wins") is a
    // JS-layer concern (Group G / Task 12) — this layer only ever sees one value at a time.
    @ReactProp(name = "torch", defaultBoolean = false)
    override fun setTorch(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setTorch: $enabled")
        applyTorch(view, enabled)
    }

    @ReactProp(name = "zoomRatio", defaultDouble = 1.0)
    override fun setZoomRatio(view: VisionCameraView, ratio: Double) {
        Log.d(TAG, "setZoomRatio: $ratio")
        applyZoomRatio(view, ratio.toFloat())
    }

    @ReactProp(name = "focusMode")
    override fun setFocusMode(view: VisionCameraView, mode: String?) {
        Log.d(TAG, "setFocusMode: $mode")
        applyFocusMode(view, mode)
    }

    @ReactProp(name = "pinnedLensId")
    override fun setPinnedLensId(view: VisionCameraView, lensId: String?) {
        Log.d(TAG, "setPinnedLensId: $lensId")
        // Review fix (Group D #5) — normalize "" to null (unpin/Auto) at the source so
        // downstream tracking (reassertControlProps' lastAppliedLensId comparison) never
        // sees an empty-string/null mismatch loop.
        val normalized = lensId?.takeIf { it.isNotEmpty() }
        controlPropsFor(view).pinnedLensId = normalized
        applyLensSelection(view, normalized)
    }

    @ReactProp(name = "scanMode")
    override fun setScanMode(view: VisionCameraView, mode: String?) {
        Log.d(TAG, "setScanMode: $mode")
        val detectionMode = when (mode?.lowercase()) {
            "ocr" -> DetectionMode.OCR
            "barcode" -> DetectionMode.Barcode
            "qrcode" -> DetectionMode.QRCode
            "photo" -> DetectionMode.Photo
            "barcodeorqrcode" -> DetectionMode.BarcodeOrQRCode
            else -> DetectionMode.Barcode
        }
        currentDetectionMode = detectionMode // Track current mode
        view.setDetectionMode(detectionMode)
    }

    @ReactProp(name = "autoCapture")
    override fun setAutoCapture(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setAutoCapture: $enabled")
        val scanningMode = if (enabled) ScanningMode.Auto else ScanningMode.Manual
        view.setScanningMode(scanningMode)
    }

    @ReactProp(name = "showCodeBoundingBoxes")
    override fun setShowCodeBoundingBoxes(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setShowCodeBoundingBoxes: $enabled (cameraReady=$isCameraReady)")
        // Defer until camera ready — getFocusRegionManager() throws
        // FocusRegionManagerNotAvailable during Fabric preallocateView.
        overlayStateFor(view).pendingShowCodeBoundingBoxes = enabled
        if (isCameraReady) {
            applyShowCodeBoundingBoxes(view, enabled)
        }
    }

    @ReactProp(name = "barcodeBoundingBoxBorderColor")
    override fun setBarcodeBoundingBoxBorderColor(view: VisionCameraView, color: String?) {
        parseColorOrNull(color)?.let {
            overlayStateFor(view).borderColor = it
            reapplyOverlayStyleIfActive(view)
        }
    }

    @ReactProp(name = "barcodeBoundingBoxBorderWidth", defaultDouble = 3.0)
    override fun setBarcodeBoundingBoxBorderWidth(view: VisionCameraView, widthDp: Double) {
        overlayStateFor(view).borderWidthDp = widthDp.toFloat()
        reapplyOverlayStyleIfActive(view)
    }

    @ReactProp(name = "barcodeBoundingBoxFillColor")
    override fun setBarcodeBoundingBoxFillColor(view: VisionCameraView, color: String?) {
        parseColorOrNull(color)?.let {
            overlayStateFor(view).fillColor = it
            reapplyOverlayStyleIfActive(view)
        }
    }

    private fun applyShowCodeBoundingBoxes(view: VisionCameraView, enabled: Boolean) {
        try {
            // Native overlay rendering needs both flags: multiple-scan mode lets
            // the overlay's Choreographer/spring loop run, and the focus-settings
            // flag toggles BarcodeOverlayView.drawingEnabled.
            if (enabled) {
                view.setMultipleScanEnabled(true)
            }
            val s = overlayStateFor(view)
            // BarcodeOverlayView sets strokePaint.strokeWidth = baseStrokeWidth.toFloat()
            // directly (raw pixels), so multiply the dp-based prop by density.
            val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                context = appContext,
                showCodeBoundariesInMultipleScan = enabled,
                validCodeBoundaryBorderColor = s.borderColor,
                validCodeBoundaryBorderWidth = (s.borderWidthDp * density).toInt(),
                validCodeBoundaryFillColor = s.fillColor,
            )
            view.getFocusRegionManager()?.setFocusSettings(focusSettings)
            Log.d(TAG, "showCodeBoundingBoxes applied: $enabled border=#${"%08X".format(s.borderColor)} width=${s.borderWidthDp}dp fill=#${"%08X".format(s.fillColor)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply showCodeBoundingBoxes: ${e.message}")
        }
    }

    private fun reapplyOverlayStyleIfActive(view: VisionCameraView) {
        if (isCameraReady && overlayStateFor(view).pendingShowCodeBoundingBoxes == true) {
            applyShowCodeBoundingBoxes(view, true)
        }
    }

    private fun parseColorOrNull(hex: String?): Int? {
        if (hex.isNullOrBlank()) return null
        return try { android.graphics.Color.parseColor(hex) } catch (e: Exception) {
            Log.w(TAG, "Bad color string: $hex"); null
        }
    }

    @ReactProp(name = "cameraFacing")
    override fun setCameraFacing(view: VisionCameraView, facing: String?) {
        Log.d(TAG, "setCameraFacing: $facing")
        val lensFace = when (facing?.lowercase()) {
            "front" -> io.packagex.visionsdk.core.CameraLensFace.Front
            else -> io.packagex.visionsdk.core.CameraLensFace.Back
        }
        // Review fix (parity #1) — track the raw prop value separately from
        // cameraSettingsByView, which applyLensSelection's cross-facing pin path can
        // overwrite; see restoreFacingFromProp / ControlPropsState.propCameraFacing.
        controlPropsFor(view).propCameraFacing = lensFace
        val updated = cameraSettingsFor(view).copy(cameraLensFace = lensFace)
        cameraSettingsByView[view] = updated
        view.setCameraSettings(updated)
    }

    @ReactProp(name = "frameSkip")
    override fun setFrameSkip(view: VisionCameraView, skip: Int) {
        Log.d(TAG, "setFrameSkip: $skip")
        // Frame skip would be configured via CameraSettings
    }

    @ReactProp(name = "scanAreaJson")
    override fun setScanAreaJson(view: VisionCameraView, scanAreaJson: String?) {
        Log.d(TAG, "setScanArea: $scanAreaJson")

        // Parse JSON string to ReadableMap
        val scanArea = if (!scanAreaJson.isNullOrEmpty()) {
            try {
                val jsonObject = org.json.JSONObject(scanAreaJson)
                com.facebook.react.bridge.Arguments.makeNativeMap(
                    mapOf(
                        "x" to jsonObject.getDouble("x"),
                        "y" to jsonObject.getDouble("y"),
                        "width" to jsonObject.getDouble("width"),
                        "height" to jsonObject.getDouble("height")
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse scanArea JSON: ${e.message}")
                null
            }
        } else {
            null
        }

        // Store the scanArea for later application
        pendingScanArea = scanArea
        hasScanAreaBeenSet = true // Mark that scanArea has been explicitly set

        // Only apply if camera is ready
        if (!isCameraReady) {
            Log.d(TAG, "Camera not ready, storing scanArea for later application")
            return
        }

        applyScanArea(view, scanArea)
    }

    private fun applyScanArea(view: VisionCameraView, scanArea: com.facebook.react.bridge.ReadableMap?) {
        try {
            if (scanArea != null) {
                // When scan area is defined, disable multiple scan mode
                view.setMultipleScanEnabled(false)

                val x = scanArea.getDouble("x")
                val y = scanArea.getDouble("y")
                val width = scanArea.getDouble("width")
                val height = scanArea.getDouble("height")

                val xPx = (x * density).toFloat()
                val yPx = (y * density).toFloat()
                val widthPx = (width * density).toFloat()
                val heightPx = (height * density).toFloat()

                val focusRect = android.graphics.RectF(xPx, yPx, xPx + widthPx, yPx + heightPx)
                val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                    context = appContext,
                    shouldScanInFocusImageRect = true,
                    focusImageRect = focusRect,
                    showCodeBoundariesInMultipleScan = false,
                )
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "Scan area applied - single scan mode enabled with focus rect: $focusRect")
            } else {
                // If no scan area, enable multiple scan mode
                view.setMultipleScanEnabled(true)

                val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                    context = appContext,
                    showCodeBoundariesInMultipleScan = false,
                )
                view.getFocusRegionManager()?.setFocusSettings(focusSettings)
                Log.d(TAG, "No scan area - multiple scan mode enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply scanArea: ${e.message}", e)
        }
    }

    @ReactProp(name = "detectionConfigJson")
    override fun setDetectionConfigJson(view: VisionCameraView, configJson: String?) {
        Log.d(TAG, "setDetectionConfig: $configJson")
        // Empty/null → keep SDK defaults (all detectors on) for backwards compat.
        if (configJson.isNullOrEmpty()) return
        try {
            val obj = org.json.JSONObject(configJson)
            // When consumer passes a config, opt-in semantics: anything not
            // explicitly true is off. This is critical on Photo mode — without
            // it the SDK runs MLKit text recognition on every frame, queuing
            // 1.5×W×H byte[] InputImages internally and OOM'ing within seconds.
            view.setObjectDetectionConfiguration(
                io.packagex.visionsdk.config.ObjectDetectionConfiguration(
                    isTextIndicationOn = obj.optBoolean("text", false),
                    isBarcodeOrQRCodeIndicationOn =
                        obj.optBoolean("barcode", false) ||
                            obj.optBoolean("qrcode", false) ||
                            obj.optBoolean("qrCode", false),
                    isDocumentIndicationOn = obj.optBoolean("document", false),
                    isImageSharpnessIndicationOn = obj.optBoolean("sharpness", false),
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse detectionConfig JSON", e)
        }
    }

    @ReactProp(name = "templateJson")
    override fun setTemplateJson(view: VisionCameraView, templateJson: String?) {
        if (templateJson.isNullOrEmpty()) {
            view.removeTemplate()
            return
        }

        try {
            val success = view.applyTemplateJson(templateJson)
            if (!success) {
                Log.e(TAG, "Failed to apply template - applyTemplateJson returned false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying template: ${e.message}", e)
        }
    }

    // MARK: - Commands

    override fun receiveCommand(
        root: VisionCameraView,
        commandId: String,
        args: ReadableArray?
    ) {
        Log.d(TAG, "receiveCommand called with commandId: '$commandId', view id: ${root.id}")

        // Handle string command names (Fabric codegen)
        when (commandId) {
            "capture" -> capture(root)
            "stop" -> stop(root)
            "start" -> start(root)
            "rescan" -> rescan(root)
            "toggleFlash" -> {
                val enabled = args?.getBoolean(0) ?: false
                toggleFlash(root, enabled)
            }
            "setZoom" -> {
                val level = args?.getDouble(0) ?: 1.0
                setZoom(root, level.toFloat())
            }
            "rampZoomRatio" -> {
                val ratio = (args?.getDouble(0) ?: 1.0).toFloat()
                val durationMs = (args?.getDouble(1) ?: 0.0).toLong()
                rampZoomRatio(root, ratio, durationMs.toInt())
            }
            "setFocusSettings" -> {
                val settingsJson = args?.getString(0) ?: "{}"
                setFocusSettings(root, settingsJson)
            }
            "pauseDetection" -> pauseDetection(root)
            "resumeDetection" -> resumeDetection(root)
            "setTorchEnabled" -> {
                val enabled = args?.getBoolean(0) ?: false
                setTorchEnabled(root, enabled)
            }
            "setFocusPoint" -> {
                val x = (args?.getDouble(0) ?: 0.0).toFloat()
                val y = (args?.getDouble(1) ?: 0.0).toFloat()
                setFocusPoint(root, x, y)
            }
            else -> Log.w(TAG, "Unknown command: $commandId")
        }
    }

    override fun capture(view: VisionCameraView) {
        Log.d(TAG, "capture called")
        view.capture()
    }

    override fun stop(view: VisionCameraView) {
        Log.d(TAG, "stop called")
        // Bug fix (QA Group A #1) — hasStarted was only ever set (never cleared), so after
        // an explicit stop() every subsequent start() command hit the guard below and
        // silently no-opped forever. Reproduced on-device: Stop then Start logs "start
        // called - camera already started or scheduled, ignoring" and the preview stays
        // black. Reset it here so a stop+start cycle actually restarts the camera.
        hasStarted = false
        view.stopCamera()
    }

    override fun start(view: VisionCameraView) {
        // Guard against duplicate start. Fabric's `onAfterUpdateTransaction` schedules
        // the initial `startCamera()` via `view.post`, so on a fast mount+start sequence
        // a JS-triggered `Commands.start()` can land BEFORE the posted runnable executes.
        // At that point `isCameraStarted()` still returns false but `hasStarted` was
        // already set synchronously by `onAfterUpdateTransaction`, so check both.
        // Without this, `startCamera()` runs twice — second pass tears down PreviewView's
        // surface, orphans `imageCaptureUseCase`, next takePicture() fails with
        // "Not bound to a valid Camera".
        if (hasStarted || view.isCameraStarted()) {
            Log.d(TAG, "start called - camera already started or scheduled, ignoring")
            return
        }
        Log.d(TAG, "start called - starting")
        // Mark scheduled synchronously so a subsequent `onAfterUpdateTransaction` won't
        // queue a second startCamera either.
        hasStarted = true
        view.startCamera()
    }

    override fun rescan(view: VisionCameraView) {
        Log.d(TAG, "rescan called")
        // Bug fix (QA — torch/stop/rescan lockup): rescan() resumes scanning on a
        // BOUND camera (its normal post-capture use). After an explicit stop() the
        // camera is unbound, so view.rescan() can't rebuild it AND leaves the SDK's
        // isCameraStarted() stuck true — which makes the next start() command hit its
        // `hasStarted || isCameraStarted()` guard and silently no-op, wedging the
        // camera black permanently. Reproduced on-device: torch on → Stop → Rescan →
        // black, then Start logs "already started or scheduled, ignoring". When we're
        // not started, treat Rescan as a fresh start instead (start() reasserts torch/
        // zoom/focus on the RUNNING transition, so torch survives the cycle).
        if (!hasStarted && !view.isCameraStarted()) {
            Log.d(TAG, "rescan on a stopped camera — starting instead")
            start(view)
            return
        }
        view.rescan()
    }

    override fun toggleFlash(view: VisionCameraView, enabled: Boolean) {
        // Legacy alias -> torch (§8 "Command collision policy": toggleFlash -> torch).
        Log.d(TAG, "toggleFlash called with enabled: $enabled")
        applyTorch(view, enabled)
    }

    override fun setZoom(view: VisionCameraView, level: Float) {
        Log.d(TAG, "setZoom called with level: $level")
        applyZoomRatio(view, level)
    }

    override fun rampZoomRatio(view: VisionCameraView, ratio: Float, durationMs: Int) {
        Log.d(TAG, "rampZoomRatio called with ratio: $ratio durationMs: $durationMs")
        applyRampZoomRatio(view, ratio, durationMs.toLong())
    }

    override fun pauseDetection(view: VisionCameraView) {
        Log.d(TAG, "pauseDetection called")
        view.pauseDetection()
    }

    override fun resumeDetection(view: VisionCameraView) {
        Log.d(TAG, "resumeDetection called")
        view.resumeDetection()
    }

    // Camera Controls API (Phase 3) — setTorchEnabled is intentionally NOT named setTorch:
    // the `torch` prop already generates setTorch(view, boolean) on
    // VisionCameraViewManagerInterface; a same-named/same-erased-signature command method
    // fails javac ("method already defined"). See src/specs/VisionCameraViewNativeComponent.ts.
    override fun setTorchEnabled(view: VisionCameraView, enabled: Boolean) {
        Log.d(TAG, "setTorchEnabled called with enabled: $enabled")
        applyTorch(view, enabled)
    }

    override fun setFocusPoint(view: VisionCameraView, x: Float, y: Float) {
        Log.d(TAG, "setFocusPoint called with x=$x y=$y")
        view.setFocusPoint(x, y)
    }

    private fun parseColor(hex: String?, default: Int): Int {
        if (hex.isNullOrEmpty()) return default
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (e: Exception) {
            default
        }
    }

    override fun setFocusSettings(view: VisionCameraView, settingsJson: String) {
        Log.d(TAG, "setFocusSettings called with: $settingsJson")
        try {
            val json = org.json.JSONObject(settingsJson)

            val shouldScanInFocusImageRect = json.optBoolean("shouldScanInFocusImageRect", false)
            val showCodeBoundariesInMultipleScan = json.optBoolean("showCodeBoundariesInMultipleScan", true)
            val showDocumentBoundaries = json.optBoolean("showDocumentBoundaries", false)

            val focusSettings = io.packagex.visionsdk.config.FocusSettings(
                context = appContext,
                shouldScanInFocusImageRect = shouldScanInFocusImageRect,
                showCodeBoundariesInMultipleScan = showCodeBoundariesInMultipleScan,
                showDocumentBoundaries = showDocumentBoundaries,
                validCodeBoundaryBorderColor = parseColor(
                    json.optString("validCodeBoundaryBorderColor", null),
                    android.graphics.Color.GREEN
                ),
                validCodeBoundaryBorderWidth = json.optInt("validCodeBoundaryBorderWidth", 2),
                validCodeBoundaryFillColor = parseColor(
                    json.optString("validCodeBoundaryFillColor", null),
                    android.graphics.Color.argb(76, 0, 255, 0)
                ),
                invalidCodeBoundaryBorderColor = parseColor(
                    json.optString("inValidCodeBoundaryBorderColor", null),
                    android.graphics.Color.RED
                ),
                invalidCodeBoundaryBorderWidth = json.optInt("inValidCodeBoundaryBorderWidth", 2),
                invalidCodeBoundaryFillColor = parseColor(
                    json.optString("inValidCodeBoundaryFillColor", null),
                    android.graphics.Color.argb(76, 255, 0, 0)
                ),
                documentBoundaryBorderColor = parseColor(
                    json.optString("documentBoundaryBorderColor", null),
                    android.graphics.Color.YELLOW
                ),
                documentBoundaryFillColor = parseColor(
                    json.optString("documentBoundaryFillColor", null),
                    android.graphics.Color.argb(76, 255, 255, 0)
                ),
            )

            view.getFocusRegionManager()?.setFocusSettings(focusSettings)
            Log.d(TAG, "Focus settings applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply focus settings: ${e.message}", e)
        }
    }

    // MARK: - ViewCallback inner class
    inner class ViewCallback(
        private val view: VisionCameraView,
        private val context: ReactApplicationContext
    ) : ScannerCallback, CameraLifecycleCallback {

        // Content-hash dedup state. Set by onIndicationsBoundingBoxes — emits
        // are skipped when the (sorted scannedCode + 3-dp quantized rect) hash
        // matches the previous frame. Subsumes the prior empty-only dedup.
        private var lastContentHash = 0

        private fun sendEvent(eventName: String, params: com.facebook.react.bridge.WritableMap) {
            if (view.isAttachedToWindow) {
                try {
                    context.getJSModule(RCTEventEmitter::class.java)
                        .receiveEvent(view.id, eventName, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending event $eventName: ${e.message}")
                }
            }
        }

        // ScannerCallback implementation
        override fun onScanResult(barcodeList: List<ScannedCodeResult>) {
            // Build codes array as JSON for Fabric
            val codesArray = org.json.JSONArray()
            for (code in barcodeList) {
                val codeObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    code.boundingBox?.let { box ->
                        put("boundingBox", org.json.JSONObject().apply {
                            put("x", box.left.toDouble())
                            put("y", box.top.toDouble())
                            put("width", box.width().toDouble())
                            put("height", box.height().toDouble())
                        })
                    }

                    // 0–1 normalized rect in image coordinates, top-left origin.
                    // Use this when overlaying on the captured image — it survives
                    // aspect-ratio differences between preview and saved photo.
                    put("normalizedBoundingBox", org.json.JSONObject().apply {
                        put("x", code.normalizedBoundingBox.left.toDouble())
                        put("y", code.normalizedBoundingBox.top.toDouble())
                        put("width", code.normalizedBoundingBox.width().toDouble())
                        put("height", code.normalizedBoundingBox.height().toDouble())
                    })

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Obj = org.json.JSONObject()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Obj.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Obj)
                    }
                }
                codesArray.put(codeObj)
            }

            val event = Arguments.createMap()
            event.putString("codesJson", codesArray.toString())
            sendEvent("onBarcodeDetected", event)

            // DIAGNOSTIC: auto-rescan disabled. rescan() tears down the camera,
            // analyzer, and overlay view, then rebuilds everything — matches the
            // "feels like restart" flicker. Consumer can call rescan imperatively.
            // visionCameraView?.rescan()
        }

        override fun onFailure(exception: VisionSDKException) {
            val event = Arguments.createMap()
            event.putString("message", exception.message ?: "Unknown error")
            event.putInt("code", exception.errorCode ?: -1)
            sendEvent("onError", event)

            // DIAGNOSTIC: auto-rescan-on-failure disabled for the same reason.
            // view.postDelayed({ visionCameraView?.rescan() }, 100)
        }

        override fun onIndications(
            barcodeDetected: Boolean,
            qrCodeDetected: Boolean,
            textDetected: Boolean,
            documentDetected: Boolean
        ) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onIndications: barcode=$barcodeDetected qr=$qrCodeDetected text=$textDetected doc=$documentDetected")
            }

            // Recognition updates emitted per native frame (throttle removed) so the
            // JS-side FPS chip reflects the true processing rate. onBarcodeDetected was
            // already unthrottled; the JS consumer must keep its per-event work cheap.
            val event = Arguments.createMap()
            event.putBoolean("text", textDetected)
            event.putBoolean("barcode", barcodeDetected)
            event.putBoolean("qrcode", qrCodeDetected)
            event.putBoolean("document", documentDetected)
            sendEvent("onRecognitionUpdate", event)
        }

        override fun onIndicationsBoundingBoxes(
            barcodeBoundingBoxes: List<ScannedCodeResult>,
            qrCodeBoundingBoxes: List<ScannedCodeResult>,
            documentBoundingBox: android.graphics.Rect?
        ) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onIndicationsBoundingBoxes called - barcodes: ${barcodeBoundingBoxes.size}, qr: ${qrCodeBoundingBoxes.size}, doc: ${documentBoundingBox != null}")
            }

            // Content-hash dedup. Native overlay emits at ~20fps with spring-smoothed
            // sub-pixel jitter; ~60-80% of frames are visually identical. Quantize the
            // normalized rect to 3 decimal places (~1 px on a 1080-wide preview) and
            // skip the bridge work if the payload matches the previous emit. Empties
            // hash to the same constant — replaces the prior empty-only dedup.
            val contentHash = run {
                val sb = StringBuilder()
                (barcodeBoundingBoxes + qrCodeBoundingBoxes)
                    .sortedBy { it.scannedCode }
                    .forEach { c ->
                        val r = c.normalizedBoundingBox
                        sb.append(c.scannedCode).append('|')
                        sb.append("%.3f,%.3f,%.3f,%.3f".format(r.left, r.top, r.width(), r.height()))
                        sb.append(';')
                    }
                documentBoundingBox?.let {
                    sb.append('D').append(it.left).append(',').append(it.top).append(',').append(it.right).append(',').append(it.bottom)
                }
                sb.toString().hashCode()
            }
            if (contentHash == lastContentHash) return
            lastContentHash = contentHash
            // Build barcode bounding boxes JSON array
            val barcodeRectsJsonArray = org.json.JSONArray()
            barcodeBoundingBoxes.forEach { code ->
                val boxObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Obj = org.json.JSONObject()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Obj.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Obj)
                    }

                    code.boundingBox?.let { box ->
                        put("boundingBox", org.json.JSONObject().apply {
                            put("x", box.left.toDp(density))
                            put("y", box.top.toDp(density))
                            put("width", box.width().toDp(density))
                            put("height", box.height().toDp(density))
                        })
                    }

                    // 0–1 normalized rect in image coordinates, top-left origin.
                    put("normalizedBoundingBox", org.json.JSONObject().apply {
                        put("x", code.normalizedBoundingBox.left.toDouble())
                        put("y", code.normalizedBoundingBox.top.toDouble())
                        put("width", code.normalizedBoundingBox.width().toDouble())
                        put("height", code.normalizedBoundingBox.height().toDouble())
                    })
                }
                barcodeRectsJsonArray.put(boxObj)
            }

            // Build QR code bounding boxes JSON array
            val qrCodeRectsJsonArray = org.json.JSONArray()
            qrCodeBoundingBoxes.forEach { code ->
                val boxObj = org.json.JSONObject().apply {
                    put("scannedCode", code.scannedCode)
                    put("symbology", code.symbology.toString())

                    if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                        val gs1Obj = org.json.JSONObject()
                        code.gs1ExtractedInfo?.forEach { (key, value) ->
                            gs1Obj.put(key, value)
                        }
                        put("gs1ExtractedInfo", gs1Obj)
                    }

                    code.boundingBox?.let { box ->
                        put("boundingBox", org.json.JSONObject().apply {
                            put("x", box.left.toDp(density))
                            put("y", box.top.toDp(density))
                            put("width", box.width().toDp(density))
                            put("height", box.height().toDp(density))
                        })
                    }

                    // 0–1 normalized rect in image coordinates, top-left origin.
                    put("normalizedBoundingBox", org.json.JSONObject().apply {
                        put("x", code.normalizedBoundingBox.left.toDouble())
                        put("y", code.normalizedBoundingBox.top.toDouble())
                        put("width", code.normalizedBoundingBox.width().toDouble())
                        put("height", code.normalizedBoundingBox.height().toDouble())
                    })
                }
                qrCodeRectsJsonArray.put(boxObj)
            }

            val event = Arguments.createMap()
            event.putString("barcodeBoundingBoxesJson", barcodeRectsJsonArray.toString())
            event.putString("qrCodeBoundingBoxesJson", qrCodeRectsJsonArray.toString())

            // Convert document bounding box (this stays as object)
            documentBoundingBox?.let { box ->
                val boxMap = Arguments.createMap()
                boxMap.putInt("x", box.left.toDp(density))
                boxMap.putInt("y", box.top.toDp(density))
                boxMap.putInt("width", box.width().toDp(density))
                boxMap.putInt("height", box.height().toDp(density))
                event.putMap("documentBoundingBox", boxMap)
            }

            sendEvent("onBoundingBoxesUpdate", event)
        }

        override fun onItemRetrievalResult(scannedCodeResults: ScannedCodeResult) {
            // Not used in minimal implementation
        }

        override fun onPriceTagResult(priceTagData: io.packagex.visionsdk.core.pricetag.PriceTagData) {
            // Not used in minimal implementation
        }

        override fun onImageSharpnessScore(imageSharpnessScore: Double) {
            // Throttle sharpness score updates
            if (!shouldEmitThrottledEvent(lastSharpnessScoreUpdateTime, SHARPNESS_SCORE_UPDATE_THROTTLE_MS)) {
                return
            }
            lastSharpnessScoreUpdateTime = System.currentTimeMillis()

            val event = Arguments.createMap()
            event.putDouble("sharpnessScore", imageSharpnessScore)
            sendEvent("onSharpnessScoreUpdate", event)
        }

        override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>, imageSharpnessScore: Float) {
            Log.d(TAG, "onImageCaptured called with ${scannedCodeResults.size} barcodes, sharpnessScore: $imageSharpnessScore")
            try {
                val tempDir = appContext.cacheDir
                val fileName = "camera_${System.currentTimeMillis()}.jpg"
                val file = java.io.File(tempDir, fileName)

                java.io.FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }

                // Build barcodes array as JSON for Fabric
                val barcodesArray = org.json.JSONArray()
                scannedCodeResults.forEach { code ->
                    val codeObj = org.json.JSONObject().apply {
                        put("scannedCode", code.scannedCode)
                        put("symbology", code.symbology.toString())

                        code.boundingBox?.let { box ->
                            put("boundingBox", org.json.JSONObject().apply {
                                put("x", box.left.toDouble())
                                put("y", box.top.toDouble())
                                put("width", box.width().toDouble())
                                put("height", box.height().toDouble())
                            })
                        }

                        // 0–1 normalized rect in image coordinates, top-left origin.
                        put("normalizedBoundingBox", org.json.JSONObject().apply {
                            put("x", code.normalizedBoundingBox.left.toDouble())
                            put("y", code.normalizedBoundingBox.top.toDouble())
                            put("width", code.normalizedBoundingBox.width().toDouble())
                            put("height", code.normalizedBoundingBox.height().toDouble())
                        })

                        if (!code.gs1ExtractedInfo.isNullOrEmpty()) {
                            val gs1Obj = org.json.JSONObject()
                            code.gs1ExtractedInfo?.forEach { (key, value) ->
                                gs1Obj.put(key, value)
                            }
                            put("gs1ExtractedInfo", gs1Obj)
                        }
                    }
                    barcodesArray.put(codeObj)
                }

                Log.d(TAG, "barcodesJson: ${barcodesArray.toString()}")

                val event = Arguments.createMap()
                event.putString("image", file.absolutePath)
                event.putString("nativeImage", file.toURI().toString())
                event.putDouble("sharpnessScore", imageSharpnessScore.toDouble())
                event.putString("barcodesJson", barcodesArray.toString())

                Log.d(TAG, "Sending onCapture event with barcodesJson length: ${barcodesArray.toString().length}")
                sendEvent("onCapture", event)

                // DIAGNOSTIC: auto-rescan-after-capture disabled.
                // visionCameraView?.rescan()
            } catch (e: Exception) {
                val event = Arguments.createMap()
                event.putString("message", "Failed to save image: ${e.message}")
                sendEvent("onError", event)

                // DIAGNOSTIC: auto-rescan-on-save-error disabled.
                // visionCameraView?.rescan()
            }
        }

        override fun onImageCaptured(bitmap: Bitmap, scannedCodeResults: List<ScannedCodeResult>) {
            // Empty stub - only the version with sharpness score is used
        }

        // CameraLifecycleCallback implementation
        override fun onCameraStarted() {
            Log.d(TAG, "✅ Camera started successfully")
            isCameraReady = true

            // Only apply scan area if it was explicitly set via props
            if (hasScanAreaBeenSet) {
                Log.d(TAG, "Applying pending scan area settings")
                applyScanArea(view, pendingScanArea)
            } else {
                Log.d(TAG, "No scan area set, skipping focus settings application")
            }

            // Apply deferred showCodeBoundingBoxes (set during preallocateView
            // before getFocusRegionManager was available).
            overlayStateFor(view).pendingShowCodeBoundingBoxes?.let {
                Log.d(TAG, "Applying pending showCodeBoundingBoxes: $it")
                applyShowCodeBoundingBoxes(view, it)
            }
        }

        override fun onCameraStopped() {
            Log.d(TAG, "⏹️ Camera stopped")
            isCameraReady = false

            // Teardown-complete signal (consumer-requested, cross-platform with iOS's
            // stopRunning(completion:) — see VisionCameraTypes.ts `VisionCameraStoppedEvent`
            // for the timing note). This callback is already driven by the camera state
            // listener's transition to IDLE (genuine CameraX unbind completion), so no
            // extra completion plumbing is needed on this side.
            sendEvent("onCameraStopped", Arguments.createMap())
        }
    }
}
