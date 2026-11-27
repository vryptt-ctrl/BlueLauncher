package com.bluelabs.bluelauncher.ui.components

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
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

    private val kickerBar = View(context)
    private val kicker = TextView(context)
    private val title = TextView(context)
    private val hint = TextView(context)
    private val count = TextView(context)
    private val resumeAction = LinearLayout(context)
    private val resumeIconTile = FrameLayout(context)
    private val resumeIcon = ImageView(context)
    private val resumeLabels = LinearLayout(context)
    private val resumeKicker = TextView(context)
    private val resumeAppName = TextView(context)
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
        view.elevation = dp(4f).toFloat()

        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(28f), dp(16f), dp(16f), dp(16f))
        }

        val kickerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        kickerBar.background = GradientDrawable().apply {
            setColor(palette.accentBright)
            cornerRadius = dp(2f).toFloat()
        }
        kickerRow.addView(kickerBar, TvUi.params(dp(3f), dp(12f), right = dp(8f)))
        kickerRow.addView(kicker.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_kicker)
            textSize = 10.5f
            letterSpacing = 0.18f
            setTextColor(palette.accentBright)
        }, TvUi.params(-2, -2))
        info.addView(kickerRow, TvUi.params(-1, -2, bottom = dp(7f)))

        info.addView(title.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_title)
            textSize = 26f
            letterSpacing = -0.02f
            setTextColor(palette.text)
            setLineSpacing(0f, 1.06f)
        }, TvUi.params(-1, -2, bottom = dp(6f)))

        hint.apply {
            typeface = TvUi.inter(context)
            textSize = 12.5f
            setTextColor(palette.muted)
        }
        info.addView(hint, TvUi.params(-1, -2))

        val infoWidth = (context.resources.displayMetrics.widthPixels * 0.54f).toInt()
        view.addView(info, FrameLayout.LayoutParams(infoWidth, -1))

        val orbWidth = (context.resources.displayMetrics.widthPixels * 0.32f).toInt().coerceAtMost(dp(330f))
        orbLarge = View(context).apply {
            background = orbBackground(palette.glow.alpha(64), palette.glow.alpha(16))
        }
        view.addView(
            orbLarge,
            FrameLayout.LayoutParams(orbWidth, orbWidth, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = -orbWidth / 3
            },
        )

        orbSmall = View(context).apply {
            background = orbBackground(palette.accentBright.alpha(34), 0x00000000)
        }
        view.addView(
            orbSmall,
            FrameLayout.LayoutParams(orbWidth / 2, orbWidth / 2, Gravity.END or Gravity.TOP).apply {
                marginEnd = dp(28f)
                topMargin = dp(6f)
            },
        )

        // Right-side action area: shows the resume app card + play pill when available.
        resumeAction.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), dp(8f), dp(10f), dp(8f))
            isFocusable = true
            isClickable = true
            clipChildren = false
            clipToPadding = false
        }
        resumeIconTile.apply {
            background = iconTileBackground()
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(12f).toFloat())
                }
            }
            visibility = View.GONE
        }
        resumeIconTile.addView(
            resumeIcon,
            FrameLayout.LayoutParams(-1, -1).apply {
                val pad = dp(6f)
                setMargins(pad, pad, pad, pad)
            },
        )
        resumeAction.addView(resumeIconTile, TvUi.params(dp(46f), dp(46f), right = dp(12f)))

        resumeLabels.apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        resumeLabels.addView(resumeKicker.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_continue_label).uppercase()
            textSize = 9.5f
            letterSpacing = 0.14f
            setTextColor(palette.accentBright)
        }, TvUi.params(-2, -2, bottom = dp(2f)))
        resumeLabels.addView(resumeAppName.apply {
            typeface = TvUi.interBold(context)
            textSize = 14f
            setTextColor(palette.text)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }, TvUi.params(dp(128f), -2))
        resumeAction.addView(resumeLabels, TvUi.params(-2, -2, right = dp(12f)))

        badge.apply {
            typeface = TvUi.icons(context)
            text = GLYPH_PLAY.toString()
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(palette.onAccent)
        }
        resumeAction.addView(badge, TvUi.params(dp(46f), dp(46f)))

        paintResumeAction(resumeAction.isFocused)
        resumeAction.onFocusChangeListener = View.OnFocusChangeListener { v, focused ->
            paintResumeAction(focused)
            v.animate().cancel()
            v.animate()
                .scaleX(if (focused) 1.06f else 1f)
                .scaleY(if (focused) 1.06f else 1f)
                .translationZ(if (focused) dp(8f).toFloat() else 0f)
                .setDuration(if (focused) 160L else 120L)
                .setInterpolator(if (focused) TvUi.focusInterpolator else TvUi.interpolator)
                .start()
        }
        resumeAction.setOnClickListener { onResumeClick(resumeApp) }
        view.addView(
            resumeAction,
            FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL).apply {
                marginEnd = dp(24f)
                bottomMargin = dp(8f)
            },
        )

        count.apply {
            typeface = TvUi.inter(context)
            textSize = 10.5f
            setTextColor(palette.muted)
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(3f), dp(10f), dp(3f))
            background = countBadgeBackground()
        }
        view.addView(
            count,
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
                marginEnd = dp(24f)
                bottomMargin = dp(10f)
            },
        )
    }

    fun setResumeApp(app: LauncherApp?) {
        val hadResume = resumeApp != null
        resumeApp = app
        resumeAction.contentDescription = app?.let { context.getString(R.string.hero_resume, it.label) }
        if (app != null) {
            resumeIcon.setImageDrawable(app.icon)
            resumeAppName.text = app.label
            resumeIconTile.visibility = View.VISIBLE
            resumeLabels.visibility = View.VISIBLE
            if (!hadResume && resumeAction.isAttachedToWindow) startPulse()
        } else {
            resumeIcon.setImageDrawable(null)
            resumeIconTile.visibility = View.GONE
            resumeLabels.visibility = View.GONE
            stopPulse()
        }
    }

    fun updatePalette(newPalette: Palette) {
        this.palette = newPalette
        view.background = heroBackground()
        kickerBar.background = GradientDrawable().apply {
            setColor(palette.accentBright)
            cornerRadius = dp(2f).toFloat()
        }
        kicker.setTextColor(palette.accentBright)
        title.setTextColor(palette.text)
        hint.setTextColor(palette.muted)
        count.setTextColor(palette.muted)
        count.background = countBadgeBackground()
        resumeIconTile.background = iconTileBackground()
        resumeKicker.setTextColor(palette.accentBright)
        resumeAppName.setTextColor(palette.text)
        badge.setTextColor(palette.onAccent)
        paintResumeAction(resumeAction.isFocused)
        orbLarge.background = orbBackground(palette.glow.alpha(64), palette.glow.alpha(16))
        orbSmall.background = orbBackground(palette.accentBright.alpha(34), 0x00000000)
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
        if (!resumeAction.isFocused) {
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
            .scaleX(if (out) 1.07f else 1f).scaleY(if (out) 1.07f else 1f)
            .setDuration(1100L)
            .setInterpolator(TvUi.interpolator)
            .withEndAction {
                if (!pulseActive) return@withEndAction
                if (resumeAction.isFocused || !resumeAction.isAttachedToWindow || resumeApp == null) {
                    pulseActive = false
                    badge.scaleX = 1f
                    badge.scaleY = 1f
                } else {
                    pulseStep(!out)
                }
            }
            .start()
    }

    private fun paintResumeAction(focused: Boolean) {
        resumeAction.background = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TL_BR
            colors = if (focused) {
                intArrayOf(
                    TvUi.blend(palette.surfaceRaised, palette.accent, 0.28f),
                    TvUi.blend(palette.surface, palette.accent, 0.10f),
                )
            } else {
                intArrayOf(palette.surfaceRaised.alpha(220), palette.surface.alpha(210))
            }
            cornerRadius = dp(20f).toFloat()
            setStroke(
                dp(if (focused) 2f else 1f),
                if (focused) palette.accentBright else palette.outline.alpha(180),
            )
        }
        badge.background = GradientDrawable().apply {
            orientation = GradientDrawable.Orientation.TL_BR
            colors = if (focused) {
                intArrayOf(palette.accentBright, palette.accent)
            } else {
                intArrayOf(palette.accent, TvUi.blend(palette.accent, palette.glow, 0.35f))
            }
            shape = GradientDrawable.OVAL
            setStroke(dp(if (focused) 2f else 1.5f), if (focused) palette.glow else palette.glow.alpha(120))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            resumeAction.outlineAmbientShadowColor = palette.backgroundDeep
            resumeAction.outlineSpotShadowColor = palette.glow
        }
    }

    private fun iconTileBackground() = GradientDrawable().apply {
        setColor(palette.surfaceRaised)
        cornerRadius = dp(12f).toFloat()
        setStroke(dp(1f), TvUi.blend(palette.outline, palette.surface, 0.3f))
    }

    private fun countBadgeBackground() = GradientDrawable().apply {
        setColor(palette.surface.alpha(200))
        cornerRadius = dp(12f).toFloat()
        setStroke(dp(1f), palette.outline.alpha(140))
    }

    private fun heroBackground(): LayerDrawable {
        val card = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(palette.surfaceRaised, palette.surface, palette.background),
        ).apply {
            cornerRadius = dp(26f).toFloat()
            setStroke(dp(1f), palette.outline)
        }
        val glow = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(palette.glow.alpha(56), palette.glow.alpha(14), 0x00000000),
        ).apply {
            cornerRadius = dp(26f).toFloat()
        }
        return LayerDrawable(arrayOf(card, glow))
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
