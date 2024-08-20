package io.packagex.visionsdk.config

import android.graphics.Color
import android.graphics.RectF
import android.provider.CalendarContract.Colors
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.visionsdk.R

data class FocusSettings(
    @DrawableRes val focusImage: Int = R.drawable.default_focus_frame,
    val focusImageRect: RectF = RectF(0.0F, 0.0F, 0.0F, 0.0F),
    val shouldDisplayFocusImage: Boolean = true,
    val shouldScanInFocusImageRect: Boolean = true,

    @ColorInt val focusImageTintColor: Int = Color.WHITE,
    @ColorInt val focusImageHighlightedColor: Int = Color.WHITE,

    val showCodeBoundariesInMultipleScan: Boolean = true,
    val validCodeBoundaryBorderColor: Int = Color.GREEN,
    val validCodeBoundaryBorderWidth: Int = 1,
    val validCodeBoundaryFillColor: Int = Color.argb(76, 0, 255, 0), // Green color with 30% alpha value
    val invalidCodeBoundaryBorderColor: Int = Color.RED,
    val invalidCodeBoundaryBorderWidth: Int = 1,
    val invalidCodeBoundaryFillColor: Int = Color.argb(76, 255, 0, 0), // Red color with 30% alpha value

    val showDocumentBoundaries: Boolean = true,
    @ColorInt val documentBoundaryBorderColor: Int = Color.YELLOW,
    @ColorInt val documentBoundaryFillColor: Int = Color.argb(76, 255, 255, 0),
) {
    fun isFrameAddedAndApplicable() = focusImageRect.isEmpty.not() && shouldScanInFocusImageRect
}
