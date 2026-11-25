package com.bluelabs.bluelauncher.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.TextView

/** Shared drawable builders and focus behavior; palettes are read lazily so theme switches repaint. */
internal object TvUi {
    val interpolator = DecelerateInterpolator(1.7f)

    fun dp(view: View, value: Float): Int = (value * view.resources.displayMetrics.density + 0.5f).toInt()

    /** Gradient card surface; focus tints it toward the accent and brightens the stroke. */
    fun cardBackground(palette: Palette, radiusDp: Float = 16f, focused: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TL_BR
            colors = if (focused) {
                intArrayOf(blend(palette.surfaceRaised, palette.accent, 0.22f), palette.surface)
            } else {
                intArrayOf(palette.surfaceRaised, palette.surface)
            }
            cornerRadius = radiusDp
            setStroke(if (focused) 2 else 1, if (focused) palette.accent else blend(palette.outline, palette.surface, 0.4f))
        }
    }

    /** Focused pills fill with the accent for a strong TV-style spotlight. */
    fun pillBackground(palette: Palette, radiusDp: Float = 24f, selected: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radiusDp
            if (selected) {
                setColor(palette.accent)
                setStroke(2, palette.glow)
            } else {
                setColor(palette.surface)
                setStroke(1, palette.outline)
            }
        }
    }

    fun setSelectableBackground(
        view: View,
        paletteProvider: () -> Palette,
        radiusDp: Float,
        pill: Boolean = false,
        selected: (() -> Boolean)? = null,
    ) {
        fun update(focused: Boolean) {
            val palette = paletteProvider()
            val isSelected = selected?.invoke() == true
            view.background = if (pill) {
                pillBackground(palette, radiusDp, selected = focused || isSelected)
            } else {
                cardBackground(palette, radiusDp, focused)
            }
            if (view is TextView) {
                view.setTextColor(
                    when {
                        pill -> if (focused || isSelected) palette.onAccent else palette.text
                        focused -> palette.accentBright
                        else -> palette.text
                    },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                view.outlineAmbientShadowColor = palette.backgroundDeep
                view.outlineSpotShadowColor = palette.glow
            }
        }
        view.isFocusable = true
        view.isClickable = true
        update(false)
        view.onFocusChangeListener = View.OnFocusChangeListener { focusedView, focused ->
            update(focused)
            focusedView.animate().cancel()
            focusedView.animate()
                .scaleX(if (focused) (if (pill) 1.04f else 1.06f) else 1f)
                .scaleY(if (focused) (if (pill) 1.04f else 1.06f) else 1f)
                .translationZ(if (focused) dp(focusedView, 6f).toFloat() else 0f)
                .setDuration(if (focused) 130L else 100L)
                .setInterpolator(interpolator)
                .start()
        }
    }

    fun roundIcon(view: View, radiusDp: Float = 14f) {
        val radius = dp(view, radiusDp).toFloat()
        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }

    fun params(width: Int, height: Int, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) =
        ViewGroup.MarginLayoutParams(width, height).apply {
            marginStart = left
            marginEnd = right
            topMargin = top
            bottomMargin = bottom
        }

    fun TextView.colors(palette: Palette, enabled: Boolean = true) {
        setTextColor(if (enabled) palette.text else palette.muted)
        val states = arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf())
        val colors = intArrayOf(palette.accentBright, if (enabled) palette.text else palette.muted)
        setTextColor(ColorStateList(states, colors))
    }

    /** Linear blend of two opaque ARGB colors. */
    fun blend(first: Int, second: Int, fraction: Float): Int {
        val inverse = 1f - fraction
        val red = (first shr 16 and 0xFF) * inverse + (second shr 16 and 0xFF) * fraction
        val green = (first shr 8 and 0xFF) * inverse + (second shr 8 and 0xFF) * fraction
        val blue = (first and 0xFF) * inverse + (second and 0xFF) * fraction
        return (0xFF shl 24) or (red.toInt() shl 16) or (green.toInt() shl 8) or blue.toInt()
    }

    /** Brand typeface: bundled Inter (weights 300/400/700) resolved once per process. */
    private var cachedInter: android.graphics.Typeface? = null

    fun inter(context: Context): android.graphics.Typeface {
        cachedInter?.let { return it }
        val loaded = runCatching { android.graphics.Typeface.createFromAsset(context.assets, "fonts/inter_400.ttf") }.getOrNull()
            ?: android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        cachedInter = loaded
        return loaded
    }

    fun interLight(context: Context): android.graphics.Typeface =
        android.graphics.Typeface.create(inter(context), android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL).style)

    /** Material Symbols Rounded icon font (subset bundled as assets/fonts/blue_icons.ttf). */
    private var cachedIcons: android.graphics.Typeface? = null

    fun icons(context: Context): android.graphics.Typeface? {
        if (cachedIcons == null) {
            cachedIcons = runCatching { android.graphics.Typeface.createFromAsset(context.assets, "fonts/blue_icons.ttf") }.getOrNull()
        }
        return cachedIcons
    }
}
