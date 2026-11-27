package com.bluelabs.bluelauncher.ui.components

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.ui.TvUi

internal class HeroView(
    private val context: Context,
    private var palette: Palette,
    private val onResumeClick: (LauncherApp?) -> Unit,
) {
    val view: FrameLayout = FrameLayout(context).apply {
        clipChildren = false
        clipToPadding = false
    }

    private val kicker = TextView(context)
    private val title = TextView(context)
    private val hint = TextView(context)
    private val count = TextView(context)
    private val badge = TextView(context)
    private lateinit var orbLarge: View
    private lateinit var orbSmall: View

    private var resumeApp: LauncherApp? = null
    private var pulseActive = false
    private var pulsesLeft = 0

    init {
        buildView()
    }

    private fun dp(v: Float): Int = TvUi.dp(context, v)

    private fun buildView() {
        view.background = heroBackground()
        view.elevation = dp(2f).toFloat()

        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(28f), dp(16f), dp(12f), dp(16f))
        }

        info.addView(kicker.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_kicker)
            textSize = 10.5f
            letterSpacing = 0.18f
            setTextColor(palette.accentBright)
        }, TvUi.params(-1, -2, bottom = dp(6f)))

        info.addView(title.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_title)
            textSize = 27f
            letterSpacing = -0.02f
            setTextColor(palette.text)
            setLineSpacing(0f, 1.05f)
        }, TvUi.params(-1, -2, bottom = dp(5f)))

        hint.apply { textSize = 12.5f; setTextColor(palette.muted) }
        info.addView(hint, TvUi.params(-1, -2))

        val infoWidth = (context.resources.displayMetrics.widthPixels * 0.62f).toInt()
        view.addView(info, FrameLayout.LayoutParams(infoWidth, -1))

        val orbWidth = (context.resources.displayMetrics.widthPixels * 0.30f).toInt().coerceAtMost(dp(310f))
        orbLarge = View(context).apply {
            background = orbBackground(palette.glow.alpha(58), palette.glow.alpha(16))
        }
        view.addView(
            orbLarge,
            FrameLayout.LayoutParams(orbWidth, orbWidth, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = -orbWidth / 3
            },
        )

        orbSmall = View(context).apply {
            background = orbBackground(palette.accentBright.alpha(30), 0x00000000)
        }
        view.addView(
            orbSmall,
            FrameLayout.LayoutParams(orbWidth / 2, orbWidth / 2, Gravity.END or Gravity.TOP).apply {
                marginEnd = dp(24f)
                topMargin = dp(8f)
            },
        )

        badge.apply {
            typeface = TvUi.icons(context)
            text = GLYPH_PLAY.toString()
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(palette.onAccent)
            isFocusable = true
            isClickable = true
        }
        paintHeroBadge(badge.isFocused)
        badge.onFocusChangeListener = View.OnFocusChangeListener { v, focused ->
            paintHeroBadge(focused)
            v.animate().cancel()
            v.animate()
                .scaleX(if (focused) 1.12f else 1f)
                .scaleY(if (focused) 1.12f else 1f)
                .translationZ(if (focused) dp(8f).toFloat() else 0f)
                .setDuration(if (focused) 160L else 120L)
                .setInterpolator(if (focused) TvUi.focusInterpolator else TvUi.interpolator)
                .start()
        }
        badge.setOnClickListener { onResumeClick(resumeApp) }
        view.addView(
            badge,
            FrameLayout.LayoutParams(dp(62f), dp(62f), Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = dp(30f)
            },
        )

        count.apply {
            textSize = 11f
            setTextColor(palette.muted)
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        view.addView(
            count,
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
                marginEnd = dp(20f)
                bottomMargin = dp(12f)
            },
        )
    }

    fun setResumeApp(app: LauncherApp?) {
        val hadResume = resumeApp != null
        resumeApp = app
        badge.contentDescription = app?.let { context.getString(R.string.hero_resume, it.label) }
        if (app != null && !hadResume && badge.isAttachedToWindow) startPulse()
        if (app == null) stopPulse()
    }

    fun updatePalette(newPalette: Palette) {
        this.palette = newPalette
        view.background = heroBackground()
        kicker.setTextColor(palette.accentBright)
        title.setTextColor(palette.text)
        hint.setTextColor(palette.muted)
        count.setTextColor(palette.muted)
        badge.setTextColor(palette.onAccent)
        paintHeroBadge(badge.isFocused)
        orbLarge.background = orbBackground(palette.glow.alpha(58), palette.glow.alpha(16))
        orbSmall.background = orbBackground(palette.accentBright.alpha(30), 0x00000000)
    }

    fun setContent(appsCount: Int, normalizedQuery: String) {
        count.text = if (appsCount == 0) {
            context.getString(R.string.hero_empty)
        } else {
            context.resources.getQuantityString(R.plurals.hero_ready, appsCount, appsCount)
        }
        hint.text = when {
            normalizedQuery.isNotEmpty() -> context.getString(R.string.hero_hint_search)
            appsCount == 0 -> context.getString(R.string.empty_apps_hint)
            else -> context.getString(R.string.hero_hint_idle)
        }
    }

    fun startPulse() {
        pulseActive = true
        pulsesLeft = HERO_PULSES
        pulseStep(out = true)
    }

    fun stopPulse() {
        pulseActive = false
        pulsesLeft = 0
        badge.animate().cancel()
        if (!badge.isFocused) {
            badge.scaleX = 1f
            badge.scaleY = 1f
        }
    }

    private fun pulseStep(out: Boolean) {
        if (pulsesLeft <= 0 || !pulseActive) {
            badge.animate().cancel()
            badge.scaleX = 1f
            badge.scaleY = 1f
            return
        }
        pulsesLeft--
        badge.animate()
            .scaleX(if (out) 1.06f else 1f).scaleY(if (out) 1.06f else 1f)
            .setDuration(1100L)
            .setInterpolator(TvUi.interpolator)
            .withEndAction {
                if (!pulseActive) return@withEndAction
                if (badge.isFocused || !badge.isAttachedToWindow || resumeApp == null) {
                    pulseActive = false
                    badge.scaleX = 1f
                    badge.scaleY = 1f
                } else {
                    pulseStep(!out)
                }
            }
            .start()
    }

    private fun paintHeroBadge(focused: Boolean) {
        badge.background = GradientDrawable().apply {
            setColor(if (focused) palette.accentBright else palette.accent)
            shape = GradientDrawable.OVAL
            setStroke(if (focused) 3 else 2, if (focused) palette.glow else palette.glow.alpha(120))
        }
    }

    private fun heroBackground(): LayerDrawable {
        val card = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(palette.surfaceRaised, palette.surface))
            .apply { cornerRadius = dp(26f).toFloat(); setStroke(1, palette.outline) }
        val glow = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(palette.glow.alpha(52), palette.glow.alpha(10), 0x00000000))
            .apply { cornerRadius = dp(26f).toFloat() }
        return LayerDrawable(arrayOf(glow, card))
    }

    private fun orbBackground(inner: Int, outer: Int) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(inner, outer, 0x00000000),
    ).apply { shape = GradientDrawable.OVAL }

    private fun Int.alpha(alpha: Int): Int = (this and 0x00ffffff) or (alpha.coerceIn(0, 255) shl 24)

    companion object {
        private const val HERO_PULSES = 4
        private const val GLYPH_PLAY = ''
    }
}
