package com.bluelabs.bluelauncher.ui.components

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.data.LauncherPreferences
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.ui.TvUi
import com.bluelabs.bluelauncher.ui.dialogs.PanelDialog

internal class AppStripView(
    private val context: Context,
    private var palette: Palette,
    private val preferences: LauncherPreferences,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun launch(app: LauncherApp)
        fun toggleFavorite(app: LauncherApp)
        fun reorderApp(app: LauncherApp, direction: Int)
    }

    private fun dp(v: Float): Int = TvUi.dp(context, v)

    fun updatePalette(newPalette: Palette) {
        this.palette = newPalette
    }

    private fun iconScale() = when (preferences.appIconSize) {
        LauncherPreferences.ICON_SIZE_COMPACT -> 0.88f
        LauncherPreferences.ICON_SIZE_LARGE -> 1.13f
        else -> 1f
    }

    fun rowHeightDp(compact: Boolean): Int {
        val base = if (compact) 68f else 122f
        val minimum = if (compact) 92 else 144
        val needed = (base * iconScale() * 1.07f + 12f).toInt() + 1
        return maxOf(minimum, needed)
    }

    private fun cardDimensions(compact: Boolean): Pair<Int, Int> {
        val scale = iconScale()
        return dp((if (compact) 176f else 148f) * scale) to dp((if (compact) 68f else 122f) * scale)
    }

    fun rebuildStrip(
        strip: LinearLayout,
        items: List<LauncherApp>,
        compact: Boolean,
        normalized: String = "",
        emptyHint: Boolean = false,
        categoryIndex: Int = 0,
        animate: Boolean = true,
    ) {
        val hadFocus = strip.hasFocus() || (0 until strip.childCount).any { strip.getChildAt(it).hasFocus() }
        val focusedPackage = if (hadFocus) strip.tag as? String else null
        val focusedIndex = if (hadFocus) {
            (0 until strip.childCount).firstOrNull { strip.getChildAt(it).hasFocus() } ?: 0
        } else {
            0
        }
        strip.removeAllViews()
        if (items.isEmpty() && (!compact || emptyHint)) {
            strip.addView(
                TextView(context).apply {
                    text = when {
                        normalized.isNotEmpty() -> context.getString(R.string.no_results, normalized)
                        categoryIndex == 1 && preferences.favorites.isEmpty() -> context.getString(R.string.no_favorites)
                        else -> context.getString(R.string.empty_apps)
                    }
                    textSize = 14f
                    setTextColor(palette.muted)
                    gravity = Gravity.CENTER_VERTICAL
                },
                TvUi.params(dp(400f), dp(110f)),
            )
            return
        }
        var restored: View? = null
        items.forEachIndexed { index, app ->
            val (width, height) = cardDimensions(compact)
            val card = appCard(app, compact)
            card.tag = app.packageName
            if (app.packageName == focusedPackage) restored = card
            strip.addView(card, TvUi.params(width, height, right = dp(10f)))
            if (animate) TvUi.staggerIn(card, index)
        }
        if (restored != null) {
            restored.post {
                if (!restored.hasFocus()) restored.requestFocus()
                TvUi.centerCardInStrip(restored)
            }
        } else if (hadFocus) {
            strip.post {
                val fallback = strip.getChildAt(focusedIndex.coerceAtMost(strip.childCount - 1))
                if (fallback != null && fallback.isFocusable && !strip.hasFocus()) {
                    fallback.requestFocus()
                    TvUi.centerCardInStrip(fallback)
                }
            }
        }
    }

    private fun appCard(app: LauncherApp, compact: Boolean): View {
        val card = FrameLayout(context).apply {
            TvUi.setSelectableBackground(this, { palette }, 18f, onFocus = { focused ->
                if (focused) {
                    (parent as? View)?.tag = app.packageName
                    TvUi.centerCardInStrip(this)
                }
            })
            isLongClickable = true
            contentDescription = "${app.label}, ${if (preferences.isFavorite(app.packageName)) context.getString(R.string.unfavorite_app) else context.getString(R.string.favorite_app)}"
            setOnClickListener { callbacks.launch(app) }
            setOnKeyListener { view, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT && keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return@setOnKeyListener false
                }
                val row = view.parent as? ViewGroup ?: return@setOnKeyListener false
                val position = row.indexOfChild(view)
                val last = row.childCount - 1
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && position >= last) true
                else keyCode == KeyEvent.KEYCODE_DPAD_LEFT && position <= 0
            }
            setOnLongClickListener {
                if (!preferences.isFavorite(app.packageName)) {
                    callbacks.toggleFavorite(app)
                    return@setOnLongClickListener true
                }
                val options = listOf(
                    context.getString(R.string.remove_favorite) to { callbacks.toggleFavorite(app) },
                    context.getString(R.string.move_toward_start) to { callbacks.reorderApp(app, -1) },
                    context.getString(R.string.move_toward_end) to { callbacks.reorderApp(app, 1) },
                )
                var heldDialog: Dialog? = null
                val menu = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                options.forEach { (label, action) ->
                    menu.addView(
                        PanelDialog.panelOption(context, palette, label, false) {
                            heldDialog?.dismiss()
                            action()
                        },
                        TvUi.params(-1, dp(42f), bottom = dp(2f)),
                    )
                }
                heldDialog = PanelDialog.show(
                    context = context,
                    palette = palette,
                    title = app.label,
                    subtitle = null,
                    content = menu,
                    dismissAction = { it.dismiss() },
                )
                true
            }
        }

        val scale = iconScale()
        val showLabels = preferences.showAppLabels
        if (compact) {
            val iconTile = iconTile(app, dp(44f * scale))
            card.addView(
                iconTile,
                FrameLayout.LayoutParams(dp(44f * scale), dp(44f * scale), Gravity.START or Gravity.CENTER_VERTICAL).apply {
                    marginStart = dp(12f)
                },
            )
            if (showLabels) {
                card.addView(
                    TextView(context).apply {
                        text = app.label
                        textSize = 12.5f
                        gravity = Gravity.CENTER_VERTICAL
                        setTextColor(palette.text)
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    },
                    FrameLayout.LayoutParams(-1, -1).apply {
                        marginStart = dp(12f) + dp(44f * scale) + dp(11f)
                        marginEnd = dp(8f)
                    },
                )
            }
            if (preferences.isFavorite(app.packageName)) {
                card.addView(
                    favoriteDot(),
                    FrameLayout.LayoutParams(dp(18f), dp(18f), Gravity.TOP or Gravity.END).apply {
                        topMargin = dp(4f)
                        marginEnd = dp(5f)
                    },
                )
            }
        } else {
            val tile = dp(64f * scale)
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (showLabels) Gravity.CENTER or Gravity.TOP else Gravity.CENTER
                setPadding(dp(10f), dp(13f), dp(10f), dp(10f))
            }
            column.addView(iconTile(app, tile), FrameLayout.LayoutParams(tile, tile))
            if (showLabels) {
                column.addView(
                    TextView(context).apply {
                        text = app.label
                        textSize = 11.5f
                        gravity = Gravity.CENTER
                        setTextColor(palette.text)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    },
                    TvUi.params(-1, -2, top = dp(9f)),
                )
            }
            card.addView(column, FrameLayout.LayoutParams(-1, -1))
            if (preferences.isFavorite(app.packageName)) {
                card.addView(
                    favoriteDot(),
                    FrameLayout.LayoutParams(dp(20f), dp(20f), Gravity.TOP or Gravity.END).apply {
                        topMargin = dp(5f)
                        marginEnd = dp(6f)
                    },
                )
            }
        }
        return card
    }

    private fun iconTile(app: LauncherApp, sizePx: Int): FrameLayout {
        val tile = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(palette.surfaceRaised)
                cornerRadius = dp(14f).toFloat()
                setStroke(1, TvUi.blend(palette.outline, palette.surface, 0.4f))
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(14f).toFloat())
                }
            }
        }
        tile.addView(
            ImageView(context).apply { setImageDrawable(app.icon) },
            FrameLayout.LayoutParams(-1, -1).apply {
                val pad = (sizePx * 0.16f).toInt()
                setMargins(pad, pad, pad, pad)
            },
        )
        return tile
    }

    private fun favoriteDot() = TextView(context).apply {
        typeface = TvUi.icons(context)
        text = GLYPH_STAR.toString()
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(palette.onAccent)
        background = GradientDrawable().apply { setColor(palette.accent); shape = GradientDrawable.OVAL }
    }

    companion object {
        private const val GLYPH_STAR = ''
    }
}
