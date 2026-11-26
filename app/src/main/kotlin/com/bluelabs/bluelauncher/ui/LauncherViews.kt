package com.bluelabs.bluelauncher.ui

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import com.bluelabs.bluelauncher.BuildConfig
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.data.AppCategory
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.data.LauncherPreferences
import com.bluelabs.bluelauncher.data.WallpaperStore
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

/** Programmatic Views keep cold start, dependencies and steady-state allocations small. */
class LauncherViews(
    private val context: Context,
    private var palette: Palette,
    private val preferences: LauncherPreferences,
    private val callbacks: Callbacks,
) {
    interface Callbacks {
        fun launch(app: LauncherApp)
        fun toggleFavorite(app: LauncherApp)
        fun reorderApp(app: LauncherApp, direction: Int)
        fun openSettings()
        fun queryChanged(query: String)
        fun selectCategory(category: AppCategory)
        fun cycleCategory()
    }

    val root = FrameLayout(context).apply {
        // Focus-scaled cards overshoot their bounds; every container in the card
        // chain must allow the overflow or the top border gets clipped.
        clipChildren = false
        clipToPadding = false
    }
    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
    }
    private val hero = FrameLayout(context).apply {
        clipChildren = false
        clipToPadding = false
    }
    private val appsHeading = TextView(context)
    private val allAppsChip = heading()
    private val favoritesChip = heading()
    private val mediaChip = heading()
    private val gamesChip = heading()
    private val appsStrip = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        // The scroll view clips this strip to the strip's own bounds, so the
        // strip needs padding on every side to hold the focus-scaled card
        // (1.07x) plus its stroke. The scroll view is pulled outwards by the
        // same amount so cards still line up with the content margin.
        clipToPadding = false
        setPadding(dp(HEADROOM_DP), dp(6), dp(HEADROOM_DP), dp(6))
    }
    private val appsScroll = horizontalScroll()
    private val appsRow = rowFrame().apply { addView(appsScroll, FrameLayout.LayoutParams(-1, -1)) }
    private val favoritesTitle = heading()
    private val favoritesStrip = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        // Same headroom scheme as appsStrip (compact cards grow the same 7%).
        clipToPadding = false
        setPadding(dp(HEADROOM_DP), dp(6), dp(HEADROOM_DP), dp(6))
    }
    private val favoritesScroll = horizontalScroll()
    private val favoritesRow = rowFrame().apply { addView(favoritesScroll, FrameLayout.LayoutParams(-1, -1)) }
    private val recentTitle = heading()
    private val recentStrip = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        clipToPadding = false
        setPadding(dp(HEADROOM_DP), dp(6), dp(HEADROOM_DP), dp(6))
    }
    private val recentScroll = horizontalScroll()
    private val recentRow = rowFrame().apply { addView(recentScroll, FrameLayout.LayoutParams(-1, -1)) }
    private val searchBox = EditText(context)
    private val searchPanel = FrameLayout(context).apply {
        clipChildren = false
        clipToPadding = false
    }
    private val brand = TextView(context)
    private val searchChip = TextView(context)
    private val settingsChip = TextView(context)
    private val heroKicker = TextView(context)
    private val heroTitle = TextView(context)
    private val heroBadge = TextView(context)
    private val heroCount = TextView(context)
    private lateinit var heroOrbLarge: View
    private lateinit var heroOrbSmall: View
    private val heroHint = TextView(context)
    private val clock = TextView(context)
    private val date = TextView(context)
    private val toastLine = TextView(context)
    private val scrollViewport = ScrollView(context)
    private var apps: List<LauncherApp> = emptyList()
    private var query = ""
    private var categoryIndex = 0
    private var initialized = false
    private var wallpaperGeneration = 0
    private var isProgrammaticSearchUpdate = false
    /** True when the pending [renderContent] came from a query edit (typing). */
    private var queryChanged = false
    /** Resume target chosen by the caller from the full app list, not the filtered one. */
    private var renderResumeApp: LauncherApp? = null
    private var diagnostics: (() -> String)? = null
    private var resumeApp: LauncherApp? = null
    private var pulseActive = false
    private var pulsesLeft = 0
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val wallpaperExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "BlueLauncher-wallpaper").apply { priority = Thread.NORM_PRIORITY - 1 }
    }

    companion object {
        /**
         * Horizontal breathing room inside every app strip, in dp. A focused card
         * grows by 7% (about 10 dp on a 148 dp card), and the scroll view clips the
         * strip to the strip's own bounds, so without this padding the accent stroke
         * of the first/last card is cut off at the row ends.
         */
        const val HEADROOM_DP = 10

        /** Number of breathing cycles for the resume badge before it settles. */
        private const val HERO_PULSES = 4

        /** Material Symbols Rounded codepoints (subset bundled as res/font/blue_icons.ttf). */
        const val GLYPH_SEARCH = ''
        const val GLYPH_SETTINGS = ''
        const val GLYPH_PLAY = ''
        const val GLYPH_STAR = ''
        const val GLYPH_FILTER_ALL = ''
        const val GLYPH_FILTER_FAVORITES = ''
        const val GLYPH_FILTER_MEDIA = ''
        const val GLYPH_FILTER_GAMES = ''
    }

    init {
        root.isFocusableInTouchMode = true
        root.background = scrimBackground()
        configureTree()
        updateClock()
        loadWallpaperAsync()
    }

    fun render(
        apps: List<LauncherApp>,
        query: String,
        category: AppCategory = AppCategory.ALL,
        animate: Boolean = true,
        resume: LauncherApp? = null,
    ) {
        this.apps = apps
        // The category travels with the render so the chips can never highlight a
        // filter that differs from the one MainActivity actually applied.
        categoryIndex = category.ordinal
        // Resolved against the full installed list by the caller: `apps` is already
        // filtered by query/category, so the badge used to point at whatever survived
        // the filter instead of the app the user actually opened last.
        renderResumeApp = resume
        if (this.query != query) {
            this.query = query
            queryChanged = true
            if (searchBox.text.toString() != query) {
                isProgrammaticSearchUpdate = true
                searchBox.setText(query)
                searchBox.setSelection(searchBox.text.length)
                isProgrammaticSearchUpdate = false
            }
        }
        renderContent(animate)
    }

    fun setDiagnostics(provider: () -> String) { diagnostics = provider }

    /**
     * Focuses the search field and asks the IME to appear. The window is created with
     * SOFT_INPUT_STATE_ALWAYS_HIDDEN, so a bare requestFocus() left the box untypeable on
     * devices with an on-screen keyboard (TV Gboard, emulators, phones).
     */
    fun focusSearch() {
        searchBox.requestFocus()
        searchBox.post {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    fun focusFirstApp() { appsStrip.getChildAt(0)?.requestFocus() }

    /** Hero play button resumes the most recently opened app, Google-TV style. */
    fun setResumeApp(app: LauncherApp?) {
        val hadResume = resumeApp != null
        resumeApp = app
        heroBadge.contentDescription = app?.let { context.getString(R.string.hero_resume, it.label) }
        if (app != null && !hadResume && heroBadge.isAttachedToWindow) startHeroPulse()
        if (app == null) stopHeroPulse()
    }

    fun updateClock() {
        val now = Date()
        // 24-hour mode keeps the system's own pattern so locales that use a separator
        // ("HH.mm") or a different hour cycle still look native.
        val format = if (preferences.clock24Hour) {
            android.text.format.DateFormat.getTimeFormat(context).let { system ->
                val pattern = (system as? java.text.SimpleDateFormat)?.toPattern()
                if (pattern != null && !pattern.contains('a')) system
                else java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            }
        } else android.text.format.DateFormat.getTimeFormat(context)
        clock.text = format.format(now)
        date.text = dateFormat.format(now)
        clock.visibility = if (preferences.showClock) View.VISIBLE else View.GONE
        date.visibility = if (preferences.showDate) View.VISIBLE else View.GONE
    }

    fun updatePalette(palette: Palette) {
        this.palette = palette
        searchBox.setHintTextColor(palette.muted)
        searchBox.setTextColor(palette.text)
        searchBox.background = TvUi.pillBackground(palette, 26f)
        hero.background = heroBackground()
        applyTextColors()
        loadWallpaperAsync()
        // A palette change repaints the existing cards; replaying the entrance would just
        // flash the grid, and this path runs while the settings panel is still open.
        renderContent(animate = false)
    }

    /** Palette changes must recolor the static header/hero text, not only the rebuilt app rows. */
    private fun applyTextColors() {
        brand.setTextColor(palette.accentBright)
        clock.setTextColor(palette.text)
        date.setTextColor(palette.muted)
        heroKicker.setTextColor(palette.accentBright)
        heroTitle.setTextColor(palette.text)
        heroHint.setTextColor(palette.muted)
        heroCount.setTextColor(palette.muted)
        heroBadge.setTextColor(palette.onAccent)
        paintHeroBadge(heroBadge.isFocused)
        // Rebuild the decorative orbs, otherwise the previous theme's tint stays on the
        // hero until the activity is recreated.
        heroOrbLarge.background = orbBackground(palette.glow.alpha(58), palette.glow.alpha(16))
        heroOrbSmall.background = orbBackground(palette.accentBright.alpha(30), 0x00000000)
        toastLine.setTextColor(palette.text)
        toastLine.background = toastBackground()
        listOf(searchChip, settingsChip).forEach { chip ->
            chip.background = TvUi.pillBackground(palette, 22f)
            chip.setTextColor(palette.text)
        }
        listOf(appsHeading, favoritesTitle, recentTitle).forEach { it.setTextColor(palette.muted) }
    }

    /** Soft radial decoration for the hero card, tinted by the active palette. */
    private fun orbBackground(inner: Int, outer: Int) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(inner, outer, 0x00000000),
    ).apply { shape = GradientDrawable.OVAL }

    /** Circular resume button: accent-filled disc with a glow ring; brighter when focused. */
    private fun paintHeroBadge(focused: Boolean) {
        heroBadge.background = GradientDrawable().apply {
            setColor(if (focused) palette.accentBright else palette.accent)
            shape = GradientDrawable.OVAL
            setStroke(if (focused) 3 else 2, if (focused) palette.glow else palette.glow.alpha(120))
        }
    }

    /** Deep gradient backdrop shown when no user wallpaper image is set. */
    private fun scrimBackground() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(palette.wallpaperStart, palette.background, palette.backgroundDeep),
    )

    /** Hero card with a soft accent glow on its leading edge. */
    private fun heroBackground(): LayerDrawable {
        val card = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(palette.surfaceRaised, palette.surface))
            .apply { cornerRadius = dp(26).toFloat(); setStroke(1, palette.outline) }
        val glow = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(palette.glow.alpha(52), palette.glow.alpha(10), 0x00000000))
            .apply { cornerRadius = dp(26).toFloat() }
        return LayerDrawable(arrayOf(glow, card))
    }

    /** Inset accent rail so the toast reads as a system notice. */
    private fun toastBackground(): LayerDrawable {
        val card = GradientDrawable().apply {
            setColor(palette.surfaceRaised); cornerRadius = dp(16).toFloat(); setStroke(1, palette.outline)
        }
        val rail = GradientDrawable().apply { setColor(palette.glow); cornerRadius = dp(2).toFloat() }
        return LayerDrawable(arrayOf(card, rail)).apply {
            setLayerInset(1, 0, dp(12), dp(266), dp(12))
        }
    }

    private fun loadWallpaperAsync() {
        val path = preferences.wallpaperImage
        val generation = ++wallpaperGeneration
        if (path.isBlank()) {
            val previous = (root.background as? BitmapDrawable)?.bitmap
            root.background = scrimBackground()
            if (previous?.isRecycled == false) previous.recycle()
            return
        }
        val width = root.width.coerceAtLeast(context.resources.displayMetrics.widthPixels)
        val height = root.height.coerceAtLeast(context.resources.displayMetrics.heightPixels)
        wallpaperExecutor.execute {
            val bitmap = WallpaperStore.load(context, path, width, height) ?: return@execute
            root.post {
                if (!root.isAttachedToWindow || generation != wallpaperGeneration) {
                    bitmap.recycle()
                    return@post
                }
                val previous = (root.background as? BitmapDrawable)?.bitmap
                // WallpaperStore returns a bitmap already cropped to this aspect ratio, so
                // FILL cannot distort it.
                root.background = BitmapDrawable(context.resources, bitmap)
                if (previous !== bitmap && previous?.isRecycled == false) previous.recycle()
            }
        }
    }

    fun dispose() {
        stopHeroPulse()
        wallpaperGeneration++
        wallpaperExecutor.shutdownNow()
        val bitmap = (root.background as? BitmapDrawable)?.bitmap
        root.background = scrimBackground()
        if (bitmap?.isRecycled == false) bitmap.recycle()
    }

    /**
     * Custom in-theme dialog panel: gradient card, accent stroke, spotlight focus.
     * Replaces the platform AlertDialog so every surface follows the active palette.
     */
    private fun showPanelDialog(
        title: String,
        subtitle: String?,
        content: View,
        dismissAction: ((Dialog) -> Unit)? = null,
    ): Dialog {
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(palette.surfaceRaised, palette.surface)).apply {
                cornerRadius = dp(24).toFloat(); setStroke(1, palette.outline)
            }
            setPadding(dp(24), dp(18), dp(24), dp(18))
        }
        panel.addView(TextView(context).apply {
            typeface = TvUi.interBold(context)
            text = title; textSize = 19f
            setTextColor(palette.text)
        })
        if (subtitle != null) {
            panel.addView(TextView(context).apply {
                text = subtitle; textSize = 12.5f; setTextColor(palette.muted)
            }, TvUi.params(-2, -2, top = dp(4)))
        }
        panel.addView(View(context).apply { setBackgroundColor(palette.outline) }, TvUi.params(-1, dp(1), top = dp(12), bottom = dp(8)))
        // Cap the scrollable body so long lists never push the Cancel button off-screen.
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val maxBodyHeight = (context.resources.displayMetrics.heightPixels * 0.62f).toInt()
        panel.addView(content, LinearLayout.LayoutParams(-1, minOf(content.measuredHeight, maxBodyHeight).coerceAtLeast(dp(80))).apply { bottomMargin = dp(6) })
        val cancelButton = TextView(context).apply {
            text = context.getString(android.R.string.cancel); textSize = 13f; gravity = Gravity.CENTER
            setPadding(dp(22), 0, dp(22), 0)
            TvUi.setSelectableBackground(this, { palette }, 22f, pill = true)
        }
        panel.addView(cancelButton, TvUi.params(-2, dp(40), top = dp(10)))
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(0x99000000.toInt()))
            setLayout((context.resources.displayMetrics.widthPixels * 0.56f).toInt().coerceAtMost(dp(560)), ViewGroup.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
        // Panel scales up from 92% while the scrim fades in; symmetric fade-out on dismiss.
        panel.alpha = 0f
        panel.scaleX = 0.92f
        panel.scaleY = 0.92f
        panel.pivotY = dp(24).toFloat()
        dialog.setOnShowListener {
            panel.animate().cancel()
            panel.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(190L)
                .setInterpolator(TvUi.focusInterpolator)
                .start()
        }
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss(); true
            } else false
        }
        dialog.show()
        cancelButton.setOnClickListener {
            // Always dismiss: a panel whose caller passed no dismissAction used to
            // fade its contents out but leave the Dialog attached, so an invisible
            // scrim kept swallowing every key press.
            val finish: (Dialog) -> Unit = dismissAction ?: Dialog::dismiss
            panel.animate().cancel()
            panel.animate()
                .alpha(0f).scaleX(0.94f).scaleY(0.94f)
                .setDuration(130L)
                .setInterpolator(TvUi.interpolator)
                .withEndAction { finish(dialog) }
                .start()
        }
        cancelButton.requestFocus()
        return dialog
    }

    /** Dialog-wide list option row: check mark + label with a selectable focus card. */
    private fun panelOption(label: String, selected: Boolean, action: () -> Unit) = TextView(context).apply {
        text = if (selected) "✓   $label" else "     $label"
        textSize = 13.5f; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), 0, dp(14), 0)
        setTextColor(if (selected) palette.accentBright else palette.text)
        TvUi.setSelectableBackground(this, { palette }, 13f); setOnClickListener { action() }
    }

    fun showSettings(
        onWallpaper: (String) -> Unit,
        onSections: (Boolean, Boolean) -> Unit,
        onClockSettings: (Boolean, Boolean, Boolean) -> Unit,
        onReset: () -> Unit,
        onChooseImage: () -> Unit,
        onAbout: () -> Unit,
    ) {
        val page = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(12), dp(24), dp(8)) }
        // Every row re-reads the preference when it paints, and each click repaints the
        // whole panel so a toggle is visible immediately while the dialog stays open.
        val rows = mutableListOf<() -> Unit>()
        page.addView(settingsLabel(context.getString(R.string.background_section).uppercase(), palette.accentBright, 11f))
        page.addView(divider(), TvUi.params(-1, dp(1), top = dp(6), bottom = dp(4)))
        listOf(
            LauncherPreferences.WALLPAPER_NOCTURNE to context.getString(R.string.blue_nocturne),
            LauncherPreferences.WALLPAPER_INK to context.getString(R.string.midnight_ink),
            LauncherPreferences.WALLPAPER_OCEAN to context.getString(R.string.deep_ocean),
        ).forEach { (key, label) ->
            page.addView(
                settingsRow(rows, { label }, { key == preferences.wallpaper && preferences.wallpaperImage.isBlank() }) { onWallpaper(key) },
                TvUi.params(-1, dp(40), bottom = dp(2)),
            )
        }
        page.addView(
            settingsRow(
                rows,
                { context.getString(R.string.choose_wallpaper_image) },
                { preferences.wallpaperImage.isNotBlank() },
            ) { onChooseImage() },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(settingsLabel(context.getString(R.string.home_section).uppercase(), palette.accentBright, 11f),
            TvUi.params(-1, -2, top = dp(12)))
        page.addView(divider(), TvUi.params(-1, dp(1), top = dp(6), bottom = dp(4)))
        page.addView(
            settingsRow(rows, { context.getString(R.string.show_favorites) }, { preferences.showFavorites }) {
                onSections(!preferences.showFavorites, preferences.showRecent)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(
            settingsRow(rows, { context.getString(R.string.show_recent) }, { preferences.showRecent }) {
                onSections(preferences.showFavorites, !preferences.showRecent)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(
            settingsRow(rows, { context.getString(R.string.show_clock) }, { preferences.showClock }) {
                onClockSettings(!preferences.showClock, preferences.showDate, preferences.clock24Hour)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(
            settingsRow(rows, { context.getString(R.string.show_date) }, { preferences.showDate }) {
                onClockSettings(preferences.showClock, !preferences.showDate, preferences.clock24Hour)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(
            settingsRow(rows, { context.getString(R.string.clock_24_hour) }, { preferences.clock24Hour }) {
                onClockSettings(preferences.showClock, preferences.showDate, !preferences.clock24Hour)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(
            settingsRow(rows, { context.getString(R.string.app_labels) }, { preferences.showAppLabels }) {
                preferences.showAppLabels = !preferences.showAppLabels
                // Behind an open panel: the grid is not being looked at, and animating
                // 30+ cards per toggle made the dialog feel sluggish.
                renderContent(animate = false)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(settingsLabel(context.getString(R.string.app_size_section).uppercase(), palette.accentBright, 11f),
            TvUi.params(-1, -2, top = dp(12)))
        page.addView(divider(), TvUi.params(-1, dp(1), top = dp(6), bottom = dp(4)))
        page.addView(
            // Card size cycles through three values, so the row shows the current name
            // instead of a check mark.
            settingsRow(rows, ::iconSizeLabel, { false }) {
                preferences.appIconSize = (preferences.appIconSize + 1) % 3
                renderContent(animate = false)
            },
            TvUi.params(-1, dp(40), bottom = dp(2)),
        )
        page.addView(settingsRow(rows, { context.getString(R.string.reset_layout) }, { false }, onReset), TvUi.params(-1, dp(40), top = dp(10)))
        page.addView(settingsRow(rows, { context.getString(R.string.about_diagnostics) }, { false }, onAbout), TvUi.params(-1, dp(40), top = dp(2)))
        val scroll = ScrollView(context).apply { isFillViewport = true; addView(page) }
        showPanelDialog(
            title = context.getString(R.string.settings_title),
            subtitle = context.getString(R.string.settings_subtitle),
            content = scroll,
            dismissAction = { dialog -> dialog.dismiss() },
        )
    }

    private fun divider() = View(context).apply { setBackgroundColor(palette.outline) }

    private fun iconSizeLabel() = when (preferences.appIconSize) {
        LauncherPreferences.ICON_SIZE_COMPACT -> context.getString(R.string.app_cards_compact)
        LauncherPreferences.ICON_SIZE_LARGE -> context.getString(R.string.app_cards_large)
        else -> context.getString(R.string.app_cards_comfortable)
    }

    fun showAbout() {
        val message = TextView(context).apply {
            text = context.getString(R.string.about_message, diagnostics?.invoke().orEmpty())
            textSize = 13f; setTextColor(palette.text); setLineSpacing(dp(3).toFloat(), 1f)
        }
        showPanelDialog(
            title = context.getString(R.string.about_title, BuildConfig.VERSION_NAME),
            subtitle = null,
            content = message,
            dismissAction = { dialog -> dialog.dismiss() },
        )
    }

    fun showLaunchError() {
        toastLine.text = context.getString(R.string.launch_failed)
        toastLine.visibility = View.VISIBLE
        toastLine.animate().cancel()
        toastLine.alpha = 0f
        toastLine.animate().alpha(1f).setDuration(160L).start()
        toastLine.postDelayed({ toastLine.visibility = View.GONE }, 2400L)
    }

    fun onBackPressed(): Boolean {
        if (searchBox.text.isNotEmpty()) { searchBox.setText(""); return true }
        return false
    }

    private fun configureTree() {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val margin = (screenWidth * 0.05f).toInt().coerceAtLeast(dp(30))
        content.setPadding(margin, dp(22), margin, dp(16))
        scrollViewport.apply {
            isFillViewport = false; isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false; clipToPadding = false; addView(content)
        }
        root.addView(scrollViewport, FrameLayout.LayoutParams(-1, -1))

        // From the root frame the platform focus finder has no directional anchor
        // (the root spans the whole screen) and used to pick the bottom row. Route
        // the first DOWN to the main apps strip instead.
        root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN &&
                event.action == KeyEvent.ACTION_DOWN &&
                root.hasFocus() && appsStrip.childCount > 0
            ) {
                appsStrip.getChildAt(0).requestFocus()
                true
            } else {
                false
            }
        }

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        brand.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.brand); textSize = 14f; letterSpacing = 0.26f
            setTextColor(palette.accentBright)
        }
        top.addView(brand, TvUi.params(-2, dp(40)))
        top.addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(clockText(clock, 27f))
        top.addView(clockText(date, 13f).apply { gravity = Gravity.END }, TvUi.params(-2, -2, left = dp(14)))
        top.addView(configureActionChip(searchChip, context.getString(R.string.search_action)) { focusSearch() }, TvUi.params(-2, dp(42), left = dp(20)))
        top.addView(configureActionChip(settingsChip, context.getString(R.string.settings_action)) { callbacks.openSettings() }, TvUi.params(-2, dp(42), left = dp(8)))
        content.addView(top, TvUi.params(-1, dp(46), bottom = dp(14)))

        configureHero()
        content.addView(hero, TvUi.params(-1, dp(164), bottom = dp(18)))
        searchBox.apply {
            hint = context.getString(R.string.search_hint); textSize = 15f; setSingleLine(true)
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(20), 0, dp(16), 0); setHintTextColor(palette.muted); setTextColor(palette.text)
            background = TvUi.pillBackground(palette, 26f)
            TvUi.setSelectableBackground(this, { palette }, 26f, pill = true)
            addTextChangedListener(SimpleTextWatcher { text ->
                if (!isProgrammaticSearchUpdate) {
                    // Mark the render as query-driven here: the watcher assigns `query`
                    // itself, so render() would otherwise see no change and replay the
                    // entrance animation for every keystroke.
                    query = text
                    queryChanged = true
                    callbacks.queryChanged(text)
                }
            })
            setOnEditorActionListener { _, _, _ -> focusFirstApp(); true }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    if (appsStrip.childCount > 0) appsStrip.getChildAt(0).requestFocus() else callbacks.cycleCategory()
                    true
                } else false
            }
        }
        searchPanel.addView(searchBox, FrameLayout.LayoutParams(dp(380), dp(48)))
        searchPanel.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN && appsStrip.childCount > 0) {
                appsStrip.getChildAt(0).requestFocus(); true
            } else false
        }
        content.addView(searchPanel, TvUi.params(-1, dp(48), bottom = dp(12)))

        val filters = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        filters.addView(sectionHeading(appsHeading), LinearLayout.LayoutParams(0, dp(39), 1f))
        listOf(allAppsChip, favoritesChip, mediaChip, gamesChip).forEachIndexed { index, chip ->
            chip.apply {
                // Mixed icon+text spans need the UI font as the base typeface; the glyph
                // itself is switched to the symbol font per character by TvUi.iconLabel.
                typeface = TvUi.inter(context)
                textSize = 11.5f; gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), 0)
                // The chip must know it is the active filter: the focus listener repaints
                // the background on every focus change, and without a `selected` provider
                // the chip lost its accent fill as soon as the cursor moved to a sibling.
                TvUi.setSelectableBackground(this, { palette }, 21f, pill = true, selected = { categoryIndex == index })
                // Only report the choice: the next render carries the applied category
                // back, so chip state and filtering cannot drift apart.
                setOnClickListener { callbacks.selectCategory(AppCategory.entries[index]) }
            }
            filters.addView(chip, TvUi.params(-2, dp(38), left = dp(6)))
        }
        content.addView(filters, TvUi.params(-1, dp(40), bottom = dp(6)))
        appsScroll.addView(appsStrip)
        // Content area (height minus 6dp top/bottom padding) must fully contain
        // the focus-scaled card: 122dp * 1.07 = ~131dp of card + 12dp padding.
        // The negative side margins cancel the strip's own horizontal headroom
        // so the first card still starts exactly at the content margin.
        content.addView(
            appsRow,
            TvUi.params(-1, dp(rowHeightDp(compact = false)), left = -dp(HEADROOM_DP), right = -dp(HEADROOM_DP), bottom = dp(8)),
        )
        addOptionalRow(favoritesTitle, favoritesRow, favoritesScroll, favoritesStrip)
        addOptionalRow(recentTitle, recentRow, recentScroll, recentStrip)
        listOf(appsScroll, favoritesScroll, recentScroll).forEach { scroll ->
            // The fade length is kept below the strip headroom so the accent
            // stroke of a focused card at either end of the row stays intact.
            scroll.isHorizontalFadingEdgeEnabled = true
            scroll.setFadingEdgeLength(dp(HEADROOM_DP))
        }
        toastLine.apply {
            textSize = 14f; gravity = Gravity.CENTER; setTextColor(palette.text); visibility = View.GONE
        }
        root.addView(toastLine, FrameLayout.LayoutParams(dp(300), dp(50), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(40)
        })
        initialized = true
    }

    private fun configureHero() {
        hero.background = heroBackground()
        hero.elevation = dp(2f).toFloat()
        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(28), dp(16), dp(12), dp(16))
        }
        info.addView(heroKicker.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_kicker); textSize = 10.5f; letterSpacing = 0.18f
            setTextColor(palette.accentBright)
        }, TvUi.params(-1, -2, bottom = dp(6)))
        info.addView(heroTitle.apply {
            typeface = TvUi.interBold(context)
            text = context.getString(R.string.hero_title); textSize = 27f; letterSpacing = -0.02f
            setTextColor(palette.text); setLineSpacing(0f, 1.05f)
        }, TvUi.params(-1, -2, bottom = dp(5)))
        heroHint.apply { textSize = 12.5f; setTextColor(palette.muted) }
        info.addView(heroHint, TvUi.params(-1, -2))
        hero.addView(info, FrameLayout.LayoutParams((context.resources.displayMetrics.widthPixels * 0.62f).toInt(), -1))
        val orbWidth = (context.resources.displayMetrics.widthPixels * 0.30f).toInt().coerceAtMost(dp(310))
        // The decorative orbs keep their geometry but must be repainted when the theme
        // changes, so they are kept as fields instead of throwaway views.
        heroOrbLarge = View(context).apply {
            background = orbBackground(palette.glow.alpha(58), palette.glow.alpha(16))
        }
        hero.addView(heroOrbLarge, FrameLayout.LayoutParams(orbWidth, orbWidth, Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = -orbWidth / 3 })
        heroOrbSmall = View(context).apply {
            background = orbBackground(palette.accentBright.alpha(30), 0x00000000)
        }
        hero.addView(heroOrbSmall, FrameLayout.LayoutParams(orbWidth / 2, orbWidth / 2, Gravity.END or Gravity.TOP).apply { marginEnd = dp(24); topMargin = dp(8) })
        heroBadge.apply {
            typeface = TvUi.icons(context)
            text = GLYPH_PLAY.toString(); textSize = 30f; gravity = Gravity.CENTER; setTextColor(palette.onAccent)
        }
        heroBadge.isFocusable = true
        heroBadge.isClickable = true
        paintHeroBadge(heroBadge.isFocused)
        heroBadge.onFocusChangeListener = View.OnFocusChangeListener { v, focused ->
            paintHeroBadge(focused)
            v.animate().cancel()
            v.animate().scaleX(if (focused) 1.12f else 1f).scaleY(if (focused) 1.12f else 1f)
                .translationZ(if (focused) dp(8f).toFloat() else 0f)
                .setDuration(if (focused) 160L else 120L)
                .setInterpolator(if (focused) TvUi.focusInterpolator else TvUi.interpolator)
                .start()
        }
        heroBadge.setOnClickListener {
            // render() already resolved the resume target against every installed app,
            // so no second lookup against the filtered list is needed here.
            val target = resumeApp ?: apps.firstOrNull()
            if (target != null) callbacks.launch(target) else focusSearch()
        }
        hero.addView(heroBadge, FrameLayout.LayoutParams(dp(62), dp(62), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = dp(30) })
        heroCount.apply { textSize = 11f; setTextColor(palette.muted); gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        hero.addView(heroCount, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
            marginEnd = dp(20); bottomMargin = dp(12)
        })
    }

    private fun addOptionalRow(
        title: TextView,
        row: FrameLayout,
        scroll: HorizontalScrollView,
        strip: LinearLayout,
    ) {
        content.addView(sectionHeading(title), TvUi.params(-1, dp(26), bottom = dp(3)))
        scroll.addView(strip)
        // Compact cards: 68dp * 1.07 = 73dp must fit inside 92-12 = 80dp of content area.
        // The negative side margins cancel the strip's horizontal headroom so the
        // first card still aligns with the content margin.
        content.addView(
            row,
            TvUi.params(-1, dp(rowHeightDp(compact = true)), left = -dp(HEADROOM_DP), right = -dp(HEADROOM_DP), bottom = dp(6)),
        )
    }

    /**
     * Short breathing pulse on the resume button while it is not focused. It runs a
     * fixed number of breaths: this is a home screen that stays on screen for hours,
     * so an endless animator would keep the view invalidating forever (the README
     * promises no continuously running animator).
     */
    private fun startHeroPulse() {
        pulseActive = true
        pulsesLeft = HERO_PULSES
        // Start by growing so the fixed number of steps ends back at rest: an even count
        // that began by shrinking would finish at 1.06 and snap down.
        pulseStep(out = true)
    }

    private fun pulseStep(out: Boolean) {
        // A live animation from the previous step is still attached to the badge, and
        // the badge starts at rest, so every step begins from whichever end it is at.
        if (pulsesLeft <= 0 || !pulseActive) {
            heroBadge.animate().cancel()
            heroBadge.scaleX = 1f
            heroBadge.scaleY = 1f
            return
        }
        pulsesLeft--
        heroBadge.animate()
            .scaleX(if (out) 1.06f else 1f).scaleY(if (out) 1.06f else 1f)
            .setDuration(1100L)
            .setInterpolator(TvUi.interpolator)
            .withEndAction {
                if (!pulseActive) return@withEndAction
                if (heroBadge.isFocused || !heroBadge.isAttachedToWindow || resumeApp == null) {
                    pulseActive = false
                    heroBadge.scaleX = 1f
                    heroBadge.scaleY = 1f
                } else {
                    pulseStep(!out)
                }
            }
            .start()
    }

    /** Stops the pulse without resetting a focus-scaled badge. */
    fun stopHeroPulse() {
        pulseActive = false
        pulsesLeft = 0
        heroBadge.animate().cancel()
        if (!heroBadge.isFocused) {
            heroBadge.scaleX = 1f
            heroBadge.scaleY = 1f
        }
    }

    private fun renderContent(animate: Boolean = true) {
        if (!initialized) return
        val normalized = query.trim()
        val displayed = filteredApps(normalized)
        // Query edits rebuild the row on every keystroke, so the entrance animation is
        // reserved for real layout changes (first load, favourites, theme).
        val animateRows = animate && !queryChanged
        queryChanged = false
        appsHeading.text = TvUi.iconLabel(context, (if (normalized.isNotEmpty()) {
            context.getString(R.string.results_heading, displayed.size)
        } else {
            context.getString(R.string.apps_heading, displayed.size)
        }).uppercase())
        heroCount.text = if (apps.isEmpty()) {
            context.getString(R.string.hero_empty)
        } else {
            context.resources.getQuantityString(R.plurals.hero_ready, apps.size, apps.size)
        }
        // Card-size changes need the rows resized before the strips are rebuilt.
        applyRowMetrics()
        heroHint.text = when {
            normalized.isNotEmpty() -> context.getString(R.string.hero_hint_search)
            apps.isEmpty() -> context.getString(R.string.empty_apps_hint)
            else -> context.getString(R.string.hero_hint_idle)
        }
        rebuildAppStrip(appsStrip, displayed, compact = false, normalized = normalized, animate = animateRows)
        val installedPackages = apps.map { it.packageName }
        val favorites = preferences.favoritesInOrder(installedPackages).mapNotNull { packageName -> apps.find { it.packageName == packageName } }
        val recents = preferences.recent.mapNotNull { packageName -> apps.find { it.packageName == packageName } }
        val showFavorites = preferences.showFavorites && normalized.isEmpty() && (favorites.isNotEmpty() || categoryIndex == 1)
        val showRecent = preferences.showRecent && normalized.isEmpty() && recents.isNotEmpty()
        // Hidden rows are cleared rather than rebuilt, keeping per-keystroke work small during search.
        if (showFavorites) {
            rebuildAppStrip(favoritesStrip, favorites, compact = true, emptyHint = favorites.isEmpty())
        } else {
            favoritesStrip.removeAllViews()
        }
        if (showRecent) {
            rebuildAppStrip(recentStrip, recents, compact = true)
        } else {
            recentStrip.removeAllViews()
        }
        setResumeApp(renderResumeApp ?: recents.firstOrNull() ?: apps.firstOrNull())
        favoritesTitle.text = TvUi.iconLabel(context, context.getString(R.string.favorites_section).uppercase())
        recentTitle.text = TvUi.iconLabel(context, context.getString(R.string.recent_section).uppercase())
        allAppsChip.text = TvUi.iconLabel(context, context.getString(R.string.filter_all))
        favoritesChip.text = TvUi.iconLabel(context, context.getString(R.string.filter_favorites))
        mediaChip.text = TvUi.iconLabel(context, context.getString(R.string.filter_media))
        gamesChip.text = TvUi.iconLabel(context, context.getString(R.string.filter_games))
        listOf(allAppsChip, favoritesChip, mediaChip, gamesChip).forEachIndexed { index, chip ->
            val selected = categoryIndex == index
            chip.background = TvUi.pillBackground(palette, 21f, selected)
            chip.setTextColor(if (selected) palette.onAccent else palette.text)
        }
        favoritesTitle.visibility = if (showFavorites) View.VISIBLE else View.GONE
        favoritesRow.visibility = favoritesTitle.visibility
        recentTitle.visibility = if (showRecent) View.VISIBLE else View.GONE
        recentRow.visibility = recentTitle.visibility
    }

    private fun filteredApps(@Suppress("UNUSED_PARAMETER") normalized: String): List<LauncherApp> =
        AppCategory.entries.getOrElse(categoryIndex) { AppCategory.ALL }.filter(apps, preferences.favorites)

    private fun rebuildAppStrip(
        strip: LinearLayout,
        items: List<LauncherApp>,
        compact: Boolean,
        normalized: String = "",
        emptyHint: Boolean = false,
        animate: Boolean = true,
    ) {
        // Remember which card held focus so a rebuild (favorite toggled, order
        // changed, theme applied) restores the cursor instead of dropping it to
        // the root view - which used to look like focus jumping between rows.
        // Only when this strip (or one of its cards) is focused, so a rebuild
        // triggered while the user is typing in search does not steal focus.
        val hadFocus = strip.hasFocus() ||
            (0 until strip.childCount).any { strip.getChildAt(it).hasFocus() }
        val focusedPackage = if (hadFocus) strip.tag as? String else null
        val focusedIndex = if (hadFocus) {
            (0 until strip.childCount).firstOrNull { strip.getChildAt(it).hasFocus() } ?: 0
        } else {
            0
        }
        strip.removeAllViews()
        if (items.isEmpty() && (!compact || emptyHint)) {
            strip.addView(TextView(context).apply {
                text = when {
                    normalized.isNotEmpty() -> context.getString(R.string.no_results, normalized)
                    categoryIndex == 1 && preferences.favorites.isEmpty() -> context.getString(R.string.no_favorites)
                    else -> context.getString(R.string.empty_apps)
                }
                textSize = 14f; setTextColor(palette.muted); gravity = Gravity.CENTER_VERTICAL
            }, TvUi.params(dp(400), dp(110)))
            return
        }
        var restored: View? = null
        items.forEachIndexed { index, app ->
            val (width, height) = cardDimensions(compact)
            val card = appCard(app, compact, index)
            card.tag = app.packageName
            if (app.packageName == focusedPackage) restored = card
            strip.addView(card, TvUi.params(width, height, right = dp(10)))
            // Items glide in from the leading edge with a per-index delay. Rows rebuilt
            // for every keystroke skip it: re-entering 30+ cards per character was the
            // cause of the "flicker while typing" on low-end boxes.
            if (animate) TvUi.staggerIn(card, index)
        }
        if (restored != null) {
            restored.post {
                if (!restored.hasFocus()) restored.requestFocus()
                TvUi.centerCardInStrip(restored)
            }
        } else if (hadFocus) {
            // The previously focused app left this list (unfavorited, filtered
            // out). Keep the cursor in the strip at the nearest position rather
            // than letting it fall through to the root view.
            strip.post {
                val fallback = strip.getChildAt(focusedIndex.coerceAtMost(strip.childCount - 1))
                if (fallback != null && fallback.isFocusable && !strip.hasFocus()) {
                    fallback.requestFocus()
                    TvUi.centerCardInStrip(fallback)
                }
            }
        }
    }

    /** Card size multiplier from the "App card size" setting. */
    private fun iconScale() = when (preferences.appIconSize) {
        LauncherPreferences.ICON_SIZE_COMPACT -> 0.88f
        LauncherPreferences.ICON_SIZE_LARGE -> 1.13f
        else -> 1f
    }

    /**
     * Row height that holds a focused card of the current size: the card grows
     * by the 1.07 focus scale, and the strip keeps 6 dp of vertical padding on
     * each side. Rows are clipped, so an undersized row would cut the bottom
     * accent stroke of the focused card. The comfortable/compact minimums are
     * the historical heights, so only the large card setting grows a row.
     */
    private fun rowHeightDp(compact: Boolean): Int {
        val base = if (compact) 68f else 122f
        val minimum = if (compact) 92 else 144
        val needed = (base * iconScale() * 1.07f + 12f).toInt() + 1
        return maxOf(minimum, needed)
    }

    /** Re-apply row heights after the card-size setting changes. */
    private fun applyRowMetrics() {
        listOf(
            appsRow to rowHeightDp(compact = false),
            favoritesRow to rowHeightDp(compact = true),
            recentRow to rowHeightDp(compact = true),
        ).forEach { (row, height) ->
            val params = row.layoutParams as ViewGroup.MarginLayoutParams
            val px = dp(height)
            if (params.height != px) {
                params.height = px
                row.layoutParams = params
            }
        }
    }

    private fun cardDimensions(compact: Boolean): Pair<Int, Int> {
        val scale = iconScale()
        return dp((if (compact) 176f else 148f) * scale) to dp((if (compact) 68f else 122f) * scale)
    }

    /** Poster-style card: icon tile on top with the label beneath; compact rows stay side-by-side. */
    private fun appCard(app: LauncherApp, compact: Boolean, index: Int): View {
        val card = FrameLayout(context).apply {
            TvUi.setSelectableBackground(this, { palette }, 18f, onFocus = { focused ->
                if (focused) {
                    // Remember the focused app on the strip so a later rebuild
                    // can restore the cursor to the same card.
                    (parent as? View)?.tag = app.packageName
                    TvUi.centerCardInStrip(this)
                }
            })
            isLongClickable = true
            contentDescription = "${app.label}, ${if (preferences.isFavorite(app.packageName)) context.getString(R.string.unfavorite_app) else context.getString(R.string.favorite_app)}"
            setOnClickListener { callbacks.launch(app) }
            setOnKeyListener { view, keyCode, event ->
                // Horizontal navigation belongs to this row. Without clamping, the
                // platform focus finder leaves the row at either end and lands on an
                // unrelated row, which reads as the cursor jumping around.
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
                    menu.addView(panelOption(label, false) { heldDialog?.dismiss(); action() },
                        TvUi.params(-1, dp(42), bottom = dp(2)))
                }
                heldDialog = showPanelDialog(
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
            card.addView(iconTile, FrameLayout.LayoutParams(dp(44f * scale), dp(44f * scale),
                Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = dp(12) })
            if (showLabels) {
                card.addView(TextView(context).apply {
                    text = app.label; textSize = 12.5f; gravity = Gravity.CENTER_VERTICAL
                    setTextColor(palette.text); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                }, FrameLayout.LayoutParams(-1, -1).apply { marginStart = dp(12) + dp(44f * scale) + dp(11); marginEnd = dp(8) })
            }
            if (preferences.isFavorite(app.packageName)) card.addView(favoriteDot(), FrameLayout.LayoutParams(dp(18), dp(18),
                Gravity.TOP or Gravity.END).apply { topMargin = dp(4); marginEnd = dp(5) })
        } else {
            val tile = dp(64f * scale)
            val column = LinearLayout(context).apply {
                // With labels hidden the icon tile has to sit in the middle of the card,
                // otherwise it hugs the top and leaves an empty band underneath.
                orientation = LinearLayout.VERTICAL
                gravity = if (showLabels) Gravity.CENTER or Gravity.TOP else Gravity.CENTER
                setPadding(dp(10), dp(13), dp(10), dp(10))
            }
            column.addView(iconTile(app, tile), FrameLayout.LayoutParams(tile, tile))
            if (showLabels) {
                column.addView(TextView(context).apply {
                    text = app.label; textSize = 11.5f; gravity = Gravity.CENTER
                    setTextColor(palette.text); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                }, TvUi.params(-1, -2, top = dp(9)))
            }
            card.addView(column, FrameLayout.LayoutParams(-1, -1))
            if (preferences.isFavorite(app.packageName)) card.addView(favoriteDot(), FrameLayout.LayoutParams(dp(20), dp(20),
                Gravity.TOP or Gravity.END).apply { topMargin = dp(5); marginEnd = dp(6) })
        }
        return card
    }

    /** Icon inside a rounded raised tile so any icon shape reads consistently. */
    private fun iconTile(app: LauncherApp, sizePx: Int): FrameLayout {
        val tile = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(palette.surfaceRaised); cornerRadius = dp(14f).toFloat()
                setStroke(1, TvUi.blend(palette.outline, palette.surface, 0.4f))
            }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(14f).toFloat())
                }
            }
        }
        tile.addView(ImageView(context).apply { setImageDrawable(app.icon) }, FrameLayout.LayoutParams(-1, -1).apply {
            val pad = (sizePx * 0.16f).toInt()
            setMargins(pad, pad, pad, pad)
        })
        return tile
    }

    private fun favoriteDot() = TextView(context).apply {
        typeface = TvUi.icons(context)
        text = GLYPH_STAR.toString(); textSize = 12f; gravity = Gravity.CENTER; setTextColor(palette.onAccent)
        background = GradientDrawable().apply { setColor(palette.accent); shape = GradientDrawable.OVAL }
    }

    /**
     * Section heading. The label is set through [TvUi.iconLabel], which types the icon
     * codepoints with the symbol font and the words with Inter; setting the whole string
     * in the icon font (as before) left Latin text to system fallback or tofu.
     */
    private fun sectionHeading(view: TextView) = view.apply {
        textSize = 11f; letterSpacing = 0.12f
        typeface = TvUi.interBold(context)
        setTextColor(palette.muted); gravity = Gravity.CENTER_VERTICAL
    }

    private fun clockText(view: TextView, size: Float) = view.apply {
        textSize = size; setTextColor(palette.text); gravity = Gravity.CENTER_VERTICAL
        typeface = TvUi.interLight(context)
    }

    private fun configureActionChip(view: TextView, label: String, action: () -> Unit) = view.apply {
        // Base font is Inter; TvUi.iconLabel swaps just the glyph onto the symbol font.
        typeface = TvUi.inter(context)
        text = TvUi.iconLabel(context, label)
        textSize = 11.5f; gravity = Gravity.CENTER; setPadding(dp(14), 0, dp(14), 0)
        setTextColor(palette.text); TvUi.setSelectableBackground(this, { palette }, 22f, pill = true)
        setOnClickListener { action() }
    }

    private fun settingsLabel(textValue: String, color: Int = palette.text, size: Float = 14f) = TextView(context).apply {
        typeface = TvUi.interBold(context)
        text = textValue; textSize = size; letterSpacing = 0.08f; setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL
    }

    /**
     * Settings row whose check mark is derived from the live preference and can be
     * repainted in place. A panel that only painted its marks once kept showing stale
     * state after a toggle, which made every option look unresponsive.
     */
    private fun settingsRow(
        refreshers: MutableList<() -> Unit>,
        label: () -> String,
        selected: () -> Boolean,
        action: () -> Unit,
    ): TextView {
        val row = TextView(context).apply {
            textSize = 13.5f; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), 0, dp(14), 0)
        }
        var repaint: () -> Unit = {}
        fun paint() {
            row.text = if (selected()) "✓   ${label()}" else "     ${label()}"
            repaint()
        }
        repaint = TvUi.setSelectableBackground(row, { palette }, 13f, selected = selected)
        // A click changes a preference; the next paint reads it back, so the panel can
        // never disagree with what was stored.
        row.setOnClickListener { action(); refreshers.forEach { refresh -> refresh() } }
        refreshers += { paint() }
        paint()
        return row
    }

    /**
     * Container for one horizontal strip. It clips its children to their own
     * bounds, which is what stops a scrolled card from painting across the
     * screen gutter, while the rest of the content column (hero, search pill,
     * category chips) keeps drawing outside its own bounds.
     */
    private fun rowFrame() = FrameLayout(context).apply {
        clipChildren = true
        clipToPadding = true
    }

    private fun horizontalScroll() = HorizontalScrollView(context).apply {
        // Both clips must stay enabled. clipChildren culls cards that are
        // scrolled out of the viewport; turning clipToPadding off as well made
        // the strip skip its own bounds clip entirely, so once the row was
        // scrolled the cards before the viewport were still painted at negative
        // offsets and smeared across the left gutter. The focus-scale headroom
        // lives in the strip's own padding, so nothing needs to escape here.
        isHorizontalScrollBarEnabled = false
        clipChildren = true
        clipToPadding = true
        overScrollMode = View.OVER_SCROLL_NEVER
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS; isFocusable = false
    }

    private fun heading() = TextView(context)
    private fun dp(value: Int) = TvUi.dp(root, value.toFloat())
    private fun dp(value: Float) = TvUi.dp(root, value)

    private fun Int.alpha(alpha: Int): Int = (this and 0x00ffffff) or (alpha.coerceIn(0, 255) shl 24)
}

private class SimpleTextWatcher(private val changed: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = changed(s?.toString().orEmpty())
    override fun afterTextChanged(s: android.text.Editable?) = Unit
}
