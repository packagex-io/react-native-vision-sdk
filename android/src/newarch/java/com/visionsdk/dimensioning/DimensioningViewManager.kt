package com.visionsdk.dimensioning

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

/**
 * Android ViewManager for `DimensioningView`.
 *
 * Since Android has no LiDAR, this manager renders a static [TextView]
 * explaining that the feature is iOS-only. No events are emitted.
 */
@ReactModule(name = DimensioningViewManager.REACT_CLASS)
class DimensioningViewManager(
    private val appContext: ReactApplicationContext,
) : SimpleViewManager<TextView>() {

    companion object {
        const val REACT_CLASS = "DimensioningView"
    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): TextView {
        val tv = TextView(context)
        tv.text = "Dimensioning is not supported on Android. LiDAR is required (iOS only)."
        tv.setTextColor(Color.parseColor("#666666"))
        tv.setBackgroundColor(Color.parseColor("#F0F0F0"))
        tv.gravity = Gravity.CENTER
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        tv.setPadding(16.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
        return tv
    }

    // Accept but ignore all props — the view is a static placeholder.
    @ReactProp(name = "mode")
    fun setMode(view: TextView, mode: String?) { /* no-op */ }

    @ReactProp(name = "measurementUnit")
    fun setMeasurementUnit(view: TextView, unit: String?) { /* no-op */ }

    @ReactProp(name = "maximumTrackCount", defaultInt = 5)
    fun setMaximumTrackCount(view: TextView, count: Int) { /* no-op */ }

    @ReactProp(name = "overlayMode")
    fun setOverlayMode(view: TextView, mode: String?) { /* no-op */ }

    @ReactProp(name = "cloudUrl")
    fun setCloudUrl(view: TextView, url: String?) { /* no-op */ }

    @ReactProp(name = "cloudApiKey")
    fun setCloudApiKey(view: TextView, key: String?) { /* no-op */ }

    @ReactProp(name = "cloudSdkId")
    fun setCloudSdkId(view: TextView, id: String?) { /* no-op */ }

    @ReactProp(name = "enableTelemetry", defaultBoolean = false)
    fun setEnableTelemetry(view: TextView, enabled: Boolean) { /* no-op */ }

    // Commands are accepted and ignored — there is no AR session to stop or start.
    // Declared so a JS call doesn't throw on Android.
    override fun receiveCommand(view: TextView, commandId: String?, args: com.facebook.react.bridge.ReadableArray?) {
        when (commandId) {
            "stop", "start" -> { /* no-op */ }
            else -> super.receiveCommand(view, commandId, args)
        }
    }

    private fun Int.dp(ctx: Context): Int =
        (this * ctx.resources.displayMetrics.density).toInt()
}
