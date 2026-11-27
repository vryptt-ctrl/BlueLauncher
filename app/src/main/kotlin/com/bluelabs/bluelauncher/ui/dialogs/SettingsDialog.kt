package com.bluelabs.bluelauncher.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
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
        var activePalette = palette
        val rows = mutableListOf<() -> Unit>()
        fun refreshAll() = rows.forEach { it() }

        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
        }

        // Background section
        page.addView(sectionHeader(context, { activePalette }, context.getString(R.string.background_section), rows))
        val bgGroup = sectionCard(context, { activePalette }, rows)
        listOf(
            LauncherPreferences.WALLPAPER_NOCTURNE to context.getString(R.string.blue_nocturne),
            LauncherPreferences.WALLPAPER_INK to context.getString(R.string.midnight_ink),
            LauncherPreferences.WALLPAPER_OCEAN to context.getString(R.string.deep_ocean),
        ).forEach { (key, label) ->
            val themePalette = Palette.fromWallpaper(key)
            bgGroup.addView(
                themeRow(
                    context = context,
                    paletteProvider = { activePalette },
                    refreshers = rows,
                    label = label,
                    swatchStart = themePalette.wallpaperStart,
                    swatchAccent = themePalette.accent,
                    selected = { key == preferences.wallpaper && preferences.wallpaperImage.isBlank() },
                ) {
                    activePalette = themePalette
                    onWallpaper(key)
                    refreshAll()
                },
                TvUi.params(-1, dp(44f), bottom = dp(4f)),
            )
        }
        bgGroup.addView(
            themeRow(
                context = context,
                paletteProvider = { activePalette },
                refreshers = rows,
                label = context.getString(R.string.choose_wallpaper_image),
                swatchStart = activePalette.surfaceRaised,
                swatchAccent = activePalette.accentBright,
                isImagePicker = true,
                selected = { preferences.wallpaperImage.isNotBlank() },
            ) {
                onChooseImage()
                refreshAll()
            },
            TvUi.params(-1, dp(44f)),
        )
        page.addView(bgGroup, TvUi.params(-1, -2, bottom = dp(14f)))

        // Home screen toggles section
        page.addView(sectionHeader(context, { activePalette }, context.getString(R.string.home_section), rows))
        val homeGroup = sectionCard(context, { activePalette }, rows)
        val toggles = listOf(
            Triple(
                context.getString(R.string.show_favorites),
                { preferences.showFavorites },
                { onSections(!preferences.showFavorites, preferences.showRecent) },
            ),
            Triple(
                context.getString(R.string.show_recent),
                { preferences.showRecent },
                { onSections(preferences.showFavorites, !preferences.showRecent) },
            ),
            Triple(
                context.getString(R.string.show_clock),
                { preferences.showClock },
                { onClockSettings(!preferences.showClock, preferences.showDate, preferences.clock24Hour) },
            ),
            Triple(
                context.getString(R.string.show_date),
                { preferences.showDate },
                { onClockSettings(preferences.showClock, !preferences.showDate, preferences.clock24Hour) },
            ),
            Triple(
                context.getString(R.string.clock_24_hour),
                { preferences.clock24Hour },
                { onClockSettings(preferences.showClock, preferences.showDate, !preferences.clock24Hour) },
            ),
            Triple(
                context.getString(R.string.app_labels),
                { preferences.showAppLabels },
                {
                    preferences.showAppLabels = !preferences.showAppLabels
                    onAppLabelsToggle()
                },
            ),
        )
        toggles.forEachIndexed { index, (label, state, action) ->
            homeGroup.addView(
                toggleRow(context, { activePalette }, rows, label, state) {
                    action()
                    refreshAll()
                },
                TvUi.params(-1, dp(42f), bottom = if (index < toggles.lastIndex) dp(4f) else 0),
            )
        }
        page.addView(homeGroup, TvUi.params(-1, -2, bottom = dp(14f)))

        // App card size segmented selector
        page.addView(sectionHeader(context, { activePalette }, context.getString(R.string.app_size_section), rows))
        val sizeRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }
        val sizes = listOf(
            LauncherPreferences.ICON_SIZE_COMPACT to context.getString(R.string.size_compact),
            LauncherPreferences.ICON_SIZE_COMFORTABLE to context.getString(R.string.size_comfortable),
            LauncherPreferences.ICON_SIZE_LARGE to context.getString(R.string.size_large),
        )
        sizes.forEachIndexed { index, (sizeValue, label) ->
            val chip = TextView(context).apply {
                typeface = TvUi.inter(context)
                text = label
                textSize = 12.5f
                gravity = Gravity.CENTER
            }
            val repaint = TvUi.setSelectableBackground(
                chip,
                { activePalette },
                14f,
                pill = true,
                selected = { preferences.appIconSize == sizeValue },
            )
            chip.setOnClickListener {
                preferences.appIconSize = sizeValue
                onIconSizeChange()
                refreshAll()
            }
            rows += {
                chip.typeface = if (preferences.appIconSize == sizeValue) TvUi.interBold(context) else TvUi.inter(context)
                repaint()
            }
            sizeRow.addView(
                chip,
                LinearLayout.LayoutParams(0, dp(40f), 1f).apply {
                    if (index < sizes.lastIndex) marginEnd = dp(8f)
                },
            )
        }
        page.addView(sizeRow, TvUi.params(-1, -2, bottom = dp(14f)))

        // Footer actions
        val actionsGroup = sectionCard(context, { activePalette }, rows)
        actionsGroup.addView(
            actionRow(context, { activePalette }, rows, context.getString(R.string.reset_layout)) {
                onReset()
                refreshAll()
            },
            TvUi.params(-1, dp(42f), bottom = dp(4f)),
        )
        actionsGroup.addView(
            actionRow(context, { activePalette }, rows, context.getString(R.string.about_diagnostics), onAbout),
            TvUi.params(-1, dp(42f)),
        )
        page.addView(actionsGroup, TvUi.params(-1, -2))

        refreshAll()

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(page)
        }
        return PanelDialog.show(
            context = context,
            palette = palette,
            title = context.getString(R.string.settings_title),
            subtitle = context.getString(R.string.settings_subtitle),
            content = scroll,
            maxWidthDp = 620f,
            dismissAction = { dialog -> dialog.dismiss() },
        )
    }

    private fun sectionHeader(
        context: Context,
        paletteProvider: () -> Palette,
        title: String,
        refreshers: MutableList<() -> Unit>,
    ): TextView {
        val dp = { v: Float -> TvUi.dp(context, v) }
        return TextView(context).apply {
            typeface = TvUi.interBold(context)
            text = title.uppercase()
            textSize = 10.5f
            letterSpacing = 0.14f
            setPadding(dp(4f), 0, 0, dp(6f))
            setTextColor(paletteProvider().accentBright)
            refreshers += { setTextColor(paletteProvider().accentBright) }
        }
    }

    private fun sectionCard(
        context: Context,
        paletteProvider: () -> Palette,
        refreshers: MutableList<() -> Unit>,
    ): LinearLayout {
        val dp = { v: Float -> TvUi.dp(context, v) }
        fun paint(card: LinearLayout) {
            val palette = paletteProvider()
            card.background = GradientDrawable().apply {
                setColor((palette.backgroundDeep and 0x00FFFFFF) or (0x66 shl 24))
                cornerRadius = dp(16f).toFloat()
                setStroke(dp(1f), (palette.outline and 0x00FFFFFF) or (0x66 shl 24))
            }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
            paint(this)
            refreshers += { paint(this) }
        }
    }

    private fun themeRow(
        context: Context,
        paletteProvider: () -> Palette,
        refreshers: MutableList<() -> Unit>,
        label: String,
        swatchStart: Int,
        swatchAccent: Int,
        isImagePicker: Boolean = false,
        selected: () -> Boolean,
        action: () -> Unit,
    ): LinearLayout {
        val dp = { v: Float -> TvUi.dp(context, v) }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12f), 0, dp(12f), 0)
        }
        val swatch = View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(swatchStart, swatchAccent),
            ).apply {
                shape = if (isImagePicker) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
                if (isImagePicker) cornerRadius = dp(6f).toFloat()
                setStroke(dp(1.5f), paletteProvider().outline)
            }
        }
        val titleView = TextView(context).apply {
            text = label
            textSize = 13.5f
            gravity = Gravity.CENTER_VERTICAL
        }
        val checkBadge = TextView(context).apply {
            typeface = TvUi.interBold(context)
            text = "✓"
            textSize = 12f
            gravity = Gravity.CENTER
        }

        row.addView(swatch, TvUi.params(dp(20f), dp(20f), right = dp(12f)))
        row.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(checkBadge, TvUi.params(dp(24f), dp(24f)))

        val repaintBg = TvUi.setSelectableBackground(row, paletteProvider, 12f, selected = selected)
        fun paint() {
            val palette = paletteProvider()
            val isSelected = selected()
            titleView.typeface = if (isSelected) TvUi.interBold(context) else TvUi.inter(context)
            titleView.setTextColor(if (isSelected) palette.accentBright else palette.text)
            checkBadge.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            checkBadge.setTextColor(palette.onAccent)
            checkBadge.background = GradientDrawable().apply {
                setColor(palette.accent)
                shape = GradientDrawable.OVAL
            }
            repaintBg()
        }
        row.setOnClickListener { action() }
        refreshers += { paint() }
        paint()
        return row
    }

    private fun toggleRow(
        context: Context,
        paletteProvider: () -> Palette,
        refreshers: MutableList<() -> Unit>,
        label: String,
        checked: () -> Boolean,
        action: () -> Unit,
    ): LinearLayout {
        val dp = { v: Float -> TvUi.dp(context, v) }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), 0, dp(12f), 0)
        }
        val titleView = TextView(context).apply {
            text = label
            textSize = 13.5f
            gravity = Gravity.CENTER_VERTICAL
        }
        val track = FrameLayout(context).apply {
            setPadding(dp(3f), dp(3f), dp(3f), dp(3f))
        }
        val thumb = View(context)
        track.addView(thumb, FrameLayout.LayoutParams(dp(16f), dp(16f)))

        row.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(track, TvUi.params(dp(40f), dp(22f)))

        val repaintBg = TvUi.setSelectableBackground(row, paletteProvider, 12f, selected = checked)
        fun paint() {
            val palette = paletteProvider()
            val isChecked = checked()
            titleView.typeface = if (isChecked) TvUi.interBold(context) else TvUi.inter(context)
            titleView.setTextColor(if (isChecked) palette.accentBright else palette.text)

            track.background = GradientDrawable().apply {
                setColor(if (isChecked) palette.accent else palette.surface)
                cornerRadius = dp(11f).toFloat()
                setStroke(dp(1f), if (isChecked) palette.glow else palette.outline)
            }
            thumb.background = GradientDrawable().apply {
                setColor(if (isChecked) palette.onAccent else palette.muted)
                shape = GradientDrawable.OVAL
            }
            (thumb.layoutParams as FrameLayout.LayoutParams).gravity =
                if (isChecked) Gravity.END or Gravity.CENTER_VERTICAL else Gravity.START or Gravity.CENTER_VERTICAL
            thumb.requestLayout()
            repaintBg()
        }
        row.setOnClickListener { action() }
        refreshers += { paint() }
        paint()
        return row
    }

    private fun actionRow(
        context: Context,
        paletteProvider: () -> Palette,
        refreshers: MutableList<() -> Unit>,
        label: String,
        action: () -> Unit,
    ): LinearLayout {
        val dp = { v: Float -> TvUi.dp(context, v) }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14f), 0, dp(14f), 0)
        }
        val titleView = TextView(context).apply {
            typeface = TvUi.inter(context)
            text = label
            textSize = 13.5f
            gravity = Gravity.CENTER_VERTICAL
        }
        val chevron = TextView(context).apply {
            typeface = TvUi.interBold(context)
            text = "›"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        row.addView(titleView, TvUi.params(-2, -2))
        row.addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))
        row.addView(chevron, TvUi.params(-2, -2))

        val repaintBg = TvUi.setSelectableBackground(row, paletteProvider, 12f)
        fun paint() {
            val palette = paletteProvider()
            titleView.setTextColor(palette.text)
            chevron.setTextColor(palette.muted)
            repaintBg()
        }
        row.setOnClickListener { action() }
        refreshers += { paint() }
        paint()
        return row
    }
}
