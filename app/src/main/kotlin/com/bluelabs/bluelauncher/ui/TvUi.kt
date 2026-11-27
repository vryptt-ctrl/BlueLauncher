package com.bluelabs.bluelauncher.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.HorizontalScrollView
import android.widget.TextView

/** Shared drawable builders and focus behavior; palettes are read lazily so theme switches repaint. */
internal object TvUi {
    val interpolator = DecelerateInterpolator(1.7f)

    fun dp(view: View, value: Float): Int = (value * view.resources.displayMetrics.density + 0.5f).toInt()
    fun dp(context: Context, value: Float): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

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
        onFocus: ((Boolean) -> Unit)? = null,
    ): () -> Unit {
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
                        // Card options keep their accent "checked" colour when unfocused;
                        // without this a selected settings row reverted to body text as
                        // soon as the cursor moved away.
                        focused || isSelected -> palette.accentBright
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
                // Overshoot on press-in for a springy TV feel; decelerate on release.
                .scaleX(if (focused) (if (pill) 1.05f else 1.07f) else 1f)
                .scaleY(if (focused) (if (pill) 1.05f else 1.07f) else 1f)
                .translationZ(if (focused) dp(focusedView, 6f).toFloat() else 0f)
                .setDuration(if (focused) 160L else 120L)
                .setInterpolator(if (focused) focusInterpolator else interpolator)
                .start()
            onFocus?.invoke(focused)
        }
        // The returned refresher lets a caller repaint a row whose checked state
        // changed while it stayed on screen (settings toggles).
        return { update(view.hasFocus()) }
    }

    /** Slight overshoot used when a view takes focus. */
    val focusInterpolator = OvershootInterpolator(1.55f)

    /** Springy entrance for grid/strip children: fade in from the leading edge, staggered by index. */
    fun staggerIn(view: View, index: Int) {
        val delay = (index * 22L).coerceAtMost(220L)
        view.alpha = 0f
        view.translationX = dp(view, 26f).toFloat()
        view.animate().cancel()
        view.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(delay)
            .setDuration(230L)
            .setInterpolator(interpolator)
            .start()
    }

    /** Center the focused card inside its horizontal strip with a smooth scroll. */
    fun centerCardInStrip(card: View) {
        val strip = card.parent as? ViewGroup ?: return
        val scroll = strip.parent as? HorizontalScrollView ?: return
        val target = card.left + card.width / 2 - scroll.width / 2
        val clamped = target.coerceIn(0, (strip.width - scroll.width).coerceAtLeast(0))
        if (scroll.scrollX != clamped) scroll.smoothScrollTo(clamped, 0)
        ensureVerticalVisible(card)
    }

    /**
     * Keep the focused card fully on screen vertically. The focus scale (1.07x)
     * makes the card slightly larger than its layout box, and ScrollView's own
     * scroll-to-focus aligns the card flush with the viewport edge (running
     * after this listener), which clips the scaled stroke. The scroll is posted
     * so it runs last and keeps a margin around the card.
     */
    fun ensureVerticalVisible(card: View) {
        var parent: ViewGroup? = card.parent as? ViewGroup
        while (parent != null && parent !is android.widget.ScrollView) parent = parent.parent as? ViewGroup
        val scrollView = parent as? android.widget.ScrollView ?: return
        scrollView.post {
            val content = scrollView.getChildAt(0) ?: return@post
            // Card position in content coordinates.
            var top = 0
            var node: View = card
            while (node !== content) {
                top += node.top
                node = node.parent as? View ?: return@post
            }
            val margin = dp(card, 10f)
            val viewport = scrollView.height - scrollView.paddingTop - scrollView.paddingBottom
            val cardTop = top - margin
            val cardBottom = top + card.height + margin
            val visibleTop = scrollView.scrollY
            val visibleBottom = scrollView.scrollY + viewport
            val target = when {
                cardTop < visibleTop -> cardTop
                cardBottom > visibleBottom -> cardBottom - viewport
                else -> return@post
            }
            val maxScroll = (content.height - viewport).coerceAtLeast(0)
            scrollView.smoothScrollTo(0, target.coerceIn(0, maxScroll))
        }
    }

    fun params(width: Int, height: Int, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) =
        ViewGroup.MarginLayoutParams(width, height).apply {
            marginStart = left
            marginEnd = right
            topMargin = top
            bottomMargin = bottom
        }

    /** Linear blend of two opaque ARGB colors. */
    fun blend(first: Int, second: Int, fraction: Float): Int {
        val inverse = 1f - fraction
        val red = (first shr 16 and 0xFF) * inverse + (second shr 16 and 0xFF) * fraction
        val green = (first shr 8 and 0xFF) * inverse + (second shr 8 and 0xFF) * fraction
        val blue = (first and 0xFF) * inverse + (second and 0xFF) * fraction
        return (0xFF shl 24) or (red.toInt() shl 16) or (green.toInt() shl 8) or blue.toInt()
    }

    /**
     * Lets one TextView mix the bundled symbol font with the brand font: only the
     * private-use codepoints are switched to `blue_icons.ttf`, everything else keeps the
     * view's own typeface. That matters for headings, which were previously set entirely
     * in the icon font (Latin fell back to Roboto or tofu) and for bold labels, which must
     * stay bold. Returns the input unchanged when there is no glyph to swap.
     */
    fun iconLabel(context: Context, text: CharSequence): CharSequence {
        val glyphTypeface = icons(context) ?: return text
        val plain = text.toString()
        if (plain.isEmpty()) return text
        val runs = glyphRuns(plain)
        if (runs.isEmpty()) return text
        val span = SpannableString(plain)
        runs.forEach { (start, end) ->
            span.setSpan(TypefacesSpan(glyphTypeface), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
        return span
    }

    /**
     * Half-open ranges of private-use glyphs in [text], one entry per contiguous run.
     * Pure so it can be unit tested without a Context/Typeface.
     */
    fun glyphRuns(text: CharSequence): List<Pair<Int, Int>> {
        val runs = mutableListOf<Pair<Int, Int>>()
        var runStart = -1
        for (index in text.indices) {
            val isIcon = isIconCodepoint(text[index])
            if (isIcon && runStart < 0) runStart = index
            if (!isIcon && runStart >= 0) {
                runs += runStart to index
                runStart = -1
            }
        }
        if (runStart >= 0) runs += runStart to text.length
        return runs
    }

    /** `TypefaceSpan(Typeface)` needs API 28; minSdk is 23, so this paint override replaces it. */
    private class TypefacesSpan(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateDrawState(textPaint: TextPaint) {
            textPaint.typeface = typeface
        }

        override fun updateMeasureState(textPaint: TextPaint) {
            textPaint.typeface = typeface
        }
    }

    /** Private-use codepoints are exactly the subset bundled in `blue_icons.ttf`. */
    fun isIconCodepoint(value: Char): Boolean = value.code in 0xE000..0xF8FF

    /** Brand typefaces: bundled Inter weights, resolved once per process. */
    private var cachedInter: android.graphics.Typeface? = null
    private var cachedInterBold: android.graphics.Typeface? = null
    private var cachedInterLight: android.graphics.Typeface? = null

    private fun loadInter(context: Context, asset: String, fallbackStyle: Int): android.graphics.Typeface =
        runCatching { android.graphics.Typeface.createFromAsset(context.assets, asset) }.getOrNull()
            ?: android.graphics.Typeface.create("sans-serif", fallbackStyle)

    fun inter(context: Context): android.graphics.Typeface =
        cachedInter ?: loadInter(context, "fonts/inter_400.ttf", android.graphics.Typeface.NORMAL).also { cachedInter = it }

    /**
     * Real Inter Bold instead of a synthetic bold. The bundled asset is a separate
     * weight, so calling [Typeface.create] on the regular file (the previous approach)
     * only emboldened the glyphs algorithmically.
     */
    fun interBold(context: Context): android.graphics.Typeface =
        cachedInterBold
            ?: loadInter(context, "fonts/inter_700.ttf", android.graphics.Typeface.BOLD).also { cachedInterBold = it }

    /** Inter Light (weight 300) for the clock; previously this resolved to regular 400. */
    fun interLight(context: Context): android.graphics.Typeface =
        cachedInterLight
            ?: loadInter(context, "fonts/inter_300.ttf", android.graphics.Typeface.NORMAL).also { cachedInterLight = it }

    /** Material Symbols Rounded icon font (subset bundled as assets/fonts/blue_icons.ttf). */
    private var cachedIcons: android.graphics.Typeface? = null

    fun icons(context: Context): android.graphics.Typeface? {
        if (cachedIcons == null) {
            cachedIcons = runCatching { android.graphics.Typeface.createFromAsset(context.assets, "fonts/blue_icons.ttf") }.getOrNull()
        }
        return cachedIcons
    }
}
