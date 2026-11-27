package com.bluelabs.bluelauncher.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.data.LauncherPreferences
import com.bluelabs.bluelauncher.ui.Palette
import com.bluelabs.bluelauncher.ui.TvUi

internal object SettingsDialog {

    fun show(
        context: Context,
        palette: Palette,
        preferences: LauncherPreferences,
        onWallpaper: (String) -> Unit,
        onSections: (Boolean, Boolean) -> Unit,
        onClockSettings: (Boolean, Boolean, Boolean) -> Unit,
        onAppLabelsToggle: () -> Unit,
        onIconSizeChange: () -> Unit,
        onReset: () -> Unit,
        onChooseImage: () -> Unit,
        onAbout: () -> Unit,
    ): Dialog {
        val dp = { v: Float -> TvUi.dp(context, v) }
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(12f), dp(24f), dp(8f))
        }

        // Every row re-reads the preference when it paints, and each click repaints the
        // whole panel so a toggle is visible immediately while the dialog stays open.
        val rows = mutableListOf<() -> Unit>()

        page.addView(settingsLabel(context, palette, context.getString(R.string.background_section).uppercase(), palette.accentBright, 11f))
        page.addView(divider(context, palette), TvUi.params(-1, dp(1f), top = dp(6f), bottom = dp(4f)))
        listOf(
            LauncherPreferences.WALLPAPER_NOCTURNE to context.getString(R.string.blue_nocturne),
            LauncherPreferences.WALLPAPER_INK to context.getString(R.string.midnight_ink),
            LauncherPreferences.WALLPAPER_OCEAN to context.getString(R.string.deep_ocean),
        ).forEach { (key, label) ->
            page.addView(
                settingsRow(
                    context,
                    palette,
                    rows,
                    { label },
                    { key == preferences.wallpaper && preferences.wallpaperImage.isBlank() },
                ) { onWallpaper(key) },
                TvUi.params(-1, dp(40f), bottom = dp(2f)),
            )
        }
        page.addView(
            settingsRow(
                context,
                palette,
                rows,
                { context.getString(R.string.choose_wallpaper_image) },
                { preferences.wallpaperImage.isNotBlank() },
            ) { onChooseImage() },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )

        page.addView(
            settingsLabel(context, palette, context.getString(R.string.home_section).uppercase(), palette.accentBright, 11f),
            TvUi.params(-1, -2, top = dp(12f)),
        )
        page.addView(divider(context, palette), TvUi.params(-1, dp(1f), top = dp(6f), bottom = dp(4f)))
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.show_favorites) }, { preferences.showFavorites }) {
                onSections(!preferences.showFavorites, preferences.showRecent)
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.show_recent) }, { preferences.showRecent }) {
                onSections(preferences.showFavorites, !preferences.showRecent)
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.show_clock) }, { preferences.showClock }) {
                onClockSettings(!preferences.showClock, preferences.showDate, preferences.clock24Hour)
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.show_date) }, { preferences.showDate }) {
                onClockSettings(preferences.showClock, !preferences.showDate, preferences.clock24Hour)
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.clock_24_hour) }, { preferences.clock24Hour }) {
                onClockSettings(preferences.showClock, preferences.showDate, !preferences.clock24Hour)
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.app_labels) }, { preferences.showAppLabels }) {
                preferences.showAppLabels = !preferences.showAppLabels
                onAppLabelsToggle()
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )

        page.addView(
            settingsLabel(context, palette, context.getString(R.string.app_size_section).uppercase(), palette.accentBright, 11f),
            TvUi.params(-1, -2, top = dp(12f)),
        )
        page.addView(divider(context, palette), TvUi.params(-1, dp(1f), top = dp(6f), bottom = dp(4f)))
        page.addView(
            settingsRow(context, palette, rows, { iconSizeLabel(context, preferences) }, { false }) {
                preferences.appIconSize = (preferences.appIconSize + 1) % 3
                onIconSizeChange()
            },
            TvUi.params(-1, dp(40f), bottom = dp(2f)),
        )

        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.reset_layout) }, { false }, onReset),
            TvUi.params(-1, dp(40f), top = dp(10f)),
        )
        page.addView(
            settingsRow(context, palette, rows, { context.getString(R.string.about_diagnostics) }, { false }, onAbout),
            TvUi.params(-1, dp(40f), top = dp(2f)),
        )

        val scroll = ScrollView(context).apply { isFillViewport = true; addView(page) }
        return PanelDialog.show(
            context = context,
            palette = palette,
            title = context.getString(R.string.settings_title),
            subtitle = context.getString(R.string.settings_subtitle),
            content = scroll,
            dismissAction = { dialog -> dialog.dismiss() },
        )
    }

    private fun divider(context: Context, palette: Palette) = View(context).apply {
        setBackgroundColor(palette.outline)
    }

    private fun iconSizeLabel(context: Context, preferences: LauncherPreferences) = when (preferences.appIconSize) {
        LauncherPreferences.ICON_SIZE_COMPACT -> context.getString(R.string.app_cards_compact)
        LauncherPreferences.ICON_SIZE_LARGE -> context.getString(R.string.app_cards_large)
        else -> context.getString(R.string.app_cards_comfortable)
    }

    private fun settingsLabel(
        context: Context,
        palette: Palette,
        textValue: String,
        color: Int = palette.text,
        size: Float = 14f,
    ) = TextView(context).apply {
        typeface = TvUi.interBold(context)
        text = textValue
        textSize = size
        letterSpacing = 0.08f
        setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun settingsRow(
        context: Context,
        palette: Palette,
        refreshers: MutableList<() -> Unit>,
        label: () -> String,
        selected: () -> Boolean,
        action: () -> Unit,
    ): TextView {
        val dp = { v: Float -> TvUi.dp(context, v) }
        val row = TextView(context).apply {
            textSize = 13.5f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), 0, dp(14f), 0)
        }
        var repaint: () -> Unit = {}
        fun paint() {
            row.text = if (selected()) "✓   ${label()}" else "     ${label()}"
            repaint()
        }
        repaint = TvUi.setSelectableBackground(row, { palette }, 13f, selected = selected)
        row.setOnClickListener {
            action()
            refreshers.forEach { refresh -> refresh() }
        }
        refreshers += { paint() }
        paint()
        return row
    }
}
