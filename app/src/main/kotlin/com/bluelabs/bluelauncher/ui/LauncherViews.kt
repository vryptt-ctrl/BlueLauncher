package com.bluelabs.bluelauncher.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
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

    val root = FrameLayout(context)
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val hero = FrameLayout(context)
    private val appsHeading = TextView(context)
    private val allAppsChip = heading()
    private val favoritesChip = heading()
    private val mediaChip = heading()
    private val gamesChip = heading()
    private val appsStrip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val appsScroll = horizontalScroll()
    private val favoritesTitle = heading()
    private val favoritesStrip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val favoritesScroll = horizontalScroll()
    private val recentTitle = heading()
    private val recentStrip = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val recentScroll = horizontalScroll()
    private val searchBox = EditText(context)
    private val searchPanel = FrameLayout(context)
    private val brand = TextView(context)
    private val searchChip = TextView(context)
    private val settingsChip = TextView(context)
    private val heroKicker = TextView(context)
    private val heroTitle = TextView(context)
    private val heroBadge = TextView(context)
    private val heroCount = TextView(context)
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
    private var diagnostics: (() -> String)? = null
    private var resumeApp: LauncherApp? = null
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val wallpaperExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "BlueLauncher-wallpaper").apply { priority = Thread.NORM_PRIORITY - 1 }
    }

    init {
        root.isFocusableInTouchMode = true
        root.background = scrimBackground()
        configureTree()
        updateClock()
        loadWallpaperAsync()
    }

    fun render(apps: List<LauncherApp>, query: String) {
        this.apps = apps
        if (this.query != query) {
            this.query = query
            if (searchBox.text.toString() != query) {
                isProgrammaticSearchUpdate = true
                searchBox.setText(query)
                searchBox.setSelection(searchBox.text.length)
                isProgrammaticSearchUpdate = false
            }
        }
        renderContent()
    }

    fun setDiagnostics(provider: () -> String) { diagnostics = provider }
    fun focusSearch() { searchBox.requestFocus() }
    fun focusFirstApp() { appsStrip.getChildAt(0)?.requestFocus() }

    /** Hero play button resumes the most recently opened app, Google-TV style. */
    fun setResumeApp(app: LauncherApp?) {
        resumeApp = app
        heroBadge.contentDescription = app?.let { context.getString(R.string.hero_resume, it.label) }
    }

    fun updateClock() {
        val now = Date()
        val format = if (preferences.clock24Hour) java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        else android.text.format.DateFormat.getTimeFormat(context)
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
        renderContent()
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
        toastLine.setTextColor(palette.text)
        toastLine.background = toastBackground()
        listOf(searchChip, settingsChip).forEach { chip ->
            chip.background = TvUi.pillBackground(palette, 22f)
            chip.setTextColor(palette.text)
        }
        listOf(appsHeading, favoritesTitle, recentTitle).forEach { it.setTextColor(palette.muted) }
    }

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
                root.background = BitmapDrawable(context.resources, bitmap)
                if (previous !== bitmap && previous?.isRecycled == false) previous.recycle()
            }
        }
    }

    fun dispose() {
        wallpaperGeneration++
        wallpaperExecutor.shutdownNow()
        val bitmap = (root.background as? BitmapDrawable)?.bitmap
        root.background = scrimBackground()
        if (bitmap?.isRecycled == false) bitmap.recycle()
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
        page.addView(settingsLabel(context.getString(R.string.background_section).uppercase(), palette.accentBright, 11f))
        page.addView(divider(), TvUi.params(-1, dp(1), top = dp(6), bottom = dp(4)))
        listOf(
            LauncherPreferences.WALLPAPER_NOCTURNE to context.getString(R.string.blue_nocturne),
            LauncherPreferences.WALLPAPER_INK to context.getString(R.string.midnight_ink),
            LauncherPreferences.WALLPAPER_OCEAN to context.getString(R.string.deep_ocean),
        ).forEach { (key, label) ->
            page.addView(settingsOption(label, key == preferences.wallpaper && preferences.wallpaperImage.isBlank()) { onWallpaper(key) },
                TvUi.params(-1, dp(40), bottom = dp(2)))
        }
        page.addView(settingsOption(context.getString(R.string.choose_wallpaper_image), preferences.wallpaperImage.isNotBlank()) { onChooseImage() },
            TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsLabel(context.getString(R.string.home_section).uppercase(), palette.accentBright, 11f),
            TvUi.params(-1, -2, top = dp(12)))
        page.addView(divider(), TvUi.params(-1, dp(1), top = dp(6), bottom = dp(4)))
        page.addView(settingsOption(context.getString(R.string.show_favorites), preferences.showFavorites) {
            onSections(!preferences.showFavorites, preferences.showRecent)
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsOption(context.getString(R.string.show_recent), preferences.showRecent) {
            onSections(preferences.showFavorites, !preferences.showRecent)
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsOption(context.getString(R.string.show_clock), preferences.showClock) {
            onClockSettings(!preferences.showClock, preferences.showDate, preferences.clock24Hour)
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsOption(context.getString(R.string.show_date), preferences.showDate) {
            onClockSettings(preferences.showClock, !preferences.showDate, preferences.clock24Hour)
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsOption(context.getString(R.string.clock_24_hour), preferences.clock24Hour) {
            onClockSettings(preferences.showClock, preferences.showDate, !preferences.clock24Hour)
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsOption(context.getString(R.string.app_labels), preferences.showAppLabels) {
            preferences.showAppLabels = !preferences.showAppLabels; renderContent()
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsLabel(context.getString(R.string.app_size_section).uppercase(), palette.accentBright, 11f),
            TvUi.params(-1, -2, top = dp(12)))
        page.addView(divider(), TvUi.params(-1, dp(1), top = dp(6), bottom = dp(4)))
        page.addView(settingsOption(iconSizeLabel(), false) {
            preferences.appIconSize = (preferences.appIconSize + 1) % 3; renderContent()
        }, TvUi.params(-1, dp(40), bottom = dp(2)))
        page.addView(settingsOption(context.getString(R.string.reset_layout), false, onReset), TvUi.params(-1, dp(40), top = dp(10)))
        page.addView(settingsOption(context.getString(R.string.about_diagnostics), false, onAbout), TvUi.params(-1, dp(40), top = dp(2)))
        val scroll = ScrollView(context).apply { isFillViewport = true; addView(page) }
        AlertDialog.Builder(context).setTitle(context.getString(R.string.settings_title))
            .setMessage(context.getString(R.string.settings_subtitle)).setView(scroll)
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun divider() = View(context).apply { setBackgroundColor(palette.outline) }

    private fun iconSizeLabel() = when (preferences.appIconSize) {
        LauncherPreferences.ICON_SIZE_COMPACT -> context.getString(R.string.app_cards_compact)
        LauncherPreferences.ICON_SIZE_LARGE -> context.getString(R.string.app_cards_large)
        else -> context.getString(R.string.app_cards_comfortable)
    }

    fun showAbout() {
        AlertDialog.Builder(context).setTitle(context.getString(R.string.about_title))
            .setMessage(context.getString(R.string.about_message, diagnostics?.invoke().orEmpty()))
            .setPositiveButton(android.R.string.ok, null).show()
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

        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        brand.apply {
            text = context.getString(R.string.brand); textSize = 14f; letterSpacing = 0.26f
            setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(palette.accentBright)
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
                if (!isProgrammaticSearchUpdate) { query = text; callbacks.queryChanged(text) }
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

        val filters = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        filters.addView(sectionHeading(appsHeading), LinearLayout.LayoutParams(0, dp(39), 1f))
        listOf(allAppsChip, favoritesChip, mediaChip, gamesChip).forEachIndexed { index, chip ->
            chip.apply {
                textSize = 11.5f; gravity = Gravity.CENTER; setPadding(dp(12), 0, dp(12), 0)
                TvUi.setSelectableBackground(this, { palette }, 21f, pill = true)
                setOnClickListener { categoryIndex = index; callbacks.selectCategory(AppCategory.entries[index]); renderContent() }
            }
            filters.addView(chip, TvUi.params(-2, dp(38), left = dp(6)))
        }
        content.addView(filters, TvUi.params(-1, dp(40), bottom = dp(6)))
        appsScroll.addView(appsStrip)
        content.addView(appsScroll, TvUi.params(-1, dp(134), bottom = dp(8)))
        addOptionalRow(favoritesTitle, favoritesScroll, favoritesStrip)
        addOptionalRow(recentTitle, recentScroll, recentStrip)
        listOf(appsScroll, favoritesScroll, recentScroll).forEach { scroll ->
            scroll.isHorizontalFadingEdgeEnabled = true
            scroll.setFadingEdgeLength(dp(38))
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
            text = context.getString(R.string.hero_kicker); textSize = 10.5f; letterSpacing = 0.18f
            setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(palette.accentBright)
        }, TvUi.params(-1, -2, bottom = dp(6)))
        info.addView(heroTitle.apply {
            text = context.getString(R.string.hero_title); textSize = 27f; letterSpacing = -0.02f
            setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(palette.text); setLineSpacing(0f, 1.05f)
        }, TvUi.params(-1, -2, bottom = dp(5)))
        heroHint.apply { textSize = 12.5f; setTextColor(palette.muted) }
        info.addView(heroHint, TvUi.params(-1, -2))
        hero.addView(info, FrameLayout.LayoutParams((context.resources.displayMetrics.widthPixels * 0.62f).toInt(), -1))
        val orbWidth = (context.resources.displayMetrics.widthPixels * 0.30f).toInt().coerceAtMost(dp(310))
        hero.addView(View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(palette.glow.alpha(58), palette.glow.alpha(16), 0x00000000)).apply { shape = GradientDrawable.OVAL }
        }, FrameLayout.LayoutParams(orbWidth, orbWidth, Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = -orbWidth / 3 })
        hero.addView(View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(palette.accentBright.alpha(30), 0x00000000)).apply { shape = GradientDrawable.OVAL }
        }, FrameLayout.LayoutParams(orbWidth / 2, orbWidth / 2, Gravity.END or Gravity.TOP).apply { marginEnd = dp(24); topMargin = dp(8) })
        heroBadge.apply { text = "▶"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(palette.onAccent) }
        heroBadge.isFocusable = true
        heroBadge.isClickable = true
        paintHeroBadge(heroBadge.isFocused)
        heroBadge.onFocusChangeListener = View.OnFocusChangeListener { v, focused ->
            paintHeroBadge(focused)
            v.animate().cancel()
            v.animate().scaleX(if (focused) 1.12f else 1f).scaleY(if (focused) 1.12f else 1f)
                .translationZ(if (focused) dp(8f).toFloat() else 0f)
                .setDuration(if (focused) 130L else 100L).setInterpolator(TvUi.interpolator).start()
        }
        heroBadge.setOnClickListener {
            val target = resumeApp
                ?: preferences.recent.firstNotNullOfOrNull { pkg -> apps.find { it.packageName == pkg } }
                ?: apps.firstOrNull()
            if (target != null) callbacks.launch(target) else focusSearch()
        }
        hero.addView(heroBadge, FrameLayout.LayoutParams(dp(62), dp(62), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = dp(30) })
        heroCount.apply { textSize = 11f; setTextColor(palette.muted); gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        hero.addView(heroCount, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END).apply {
            marginEnd = dp(20); bottomMargin = dp(12)
        })
    }

    private fun addOptionalRow(title: TextView, scroll: HorizontalScrollView, strip: LinearLayout) {
        content.addView(sectionHeading(title), TvUi.params(-1, dp(26), bottom = dp(3)))
        scroll.addView(strip)
        content.addView(scroll, TvUi.params(-1, dp(84), bottom = dp(6)))
    }

    private fun renderContent() {
        if (!initialized) return
        val normalized = query.trim()
        val displayed = filteredApps(normalized)
        appsHeading.text = (if (normalized.isNotEmpty()) {
            context.getString(R.string.results_heading, displayed.size)
        } else {
            context.getString(R.string.apps_heading, displayed.size)
        }).uppercase()
        heroCount.text = if (apps.isEmpty()) {
            context.getString(R.string.hero_empty)
        } else {
            context.resources.getQuantityString(R.plurals.hero_ready, apps.size, apps.size)
        }
        heroHint.text = when {
            normalized.isNotEmpty() -> context.getString(R.string.hero_hint_search)
            apps.isEmpty() -> context.getString(R.string.empty_apps_hint)
            else -> context.getString(R.string.hero_hint_idle)
        }
        rebuildAppStrip(appsStrip, displayed, compact = false, normalized = normalized)
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
        setResumeApp(recents.firstOrNull() ?: apps.firstOrNull())
        favoritesTitle.text = context.getString(R.string.favorites_section).uppercase()
        recentTitle.text = context.getString(R.string.recent_section).uppercase()
        allAppsChip.text = context.getString(R.string.filter_all)
        favoritesChip.text = context.getString(R.string.filter_favorites)
        mediaChip.text = context.getString(R.string.filter_media)
        gamesChip.text = context.getString(R.string.filter_games)
        listOf(allAppsChip, favoritesChip, mediaChip, gamesChip).forEachIndexed { index, chip ->
            val selected = categoryIndex == index
            chip.background = TvUi.pillBackground(palette, 21f, selected)
            chip.setTextColor(if (selected) palette.onAccent else palette.text)
        }
        favoritesTitle.visibility = if (showFavorites) View.VISIBLE else View.GONE
        favoritesScroll.visibility = favoritesTitle.visibility
        recentTitle.visibility = if (showRecent) View.VISIBLE else View.GONE
        recentScroll.visibility = recentTitle.visibility
    }

    private fun filteredApps(@Suppress("UNUSED_PARAMETER") normalized: String): List<LauncherApp> =
        AppCategory.entries.getOrElse(categoryIndex) { AppCategory.ALL }.filter(apps, preferences.favorites)

    private fun rebuildAppStrip(
        strip: LinearLayout,
        items: List<LauncherApp>,
        compact: Boolean,
        normalized: String = "",
        emptyHint: Boolean = false,
    ) {
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
        val favoriteOrder = preferences.favoritesInOrder(apps.map { it.packageName })
        items.forEachIndexed { index, app ->
            val (width, height) = cardDimensions(compact)
            strip.addView(appCard(app, compact, index, favoriteOrder), TvUi.params(width, height, right = dp(10)))
        }
    }

    private fun cardDimensions(compact: Boolean): Pair<Int, Int> {
        val scale = when (preferences.appIconSize) {
            LauncherPreferences.ICON_SIZE_COMPACT -> 0.88f
            LauncherPreferences.ICON_SIZE_LARGE -> 1.13f
            else -> 1f
        }
        return dp((if (compact) 176f else 148f) * scale) to dp((if (compact) 68f else 122f) * scale)
    }

    /** Poster-style card: icon tile on top with the label beneath; compact rows stay side-by-side. */
    private fun appCard(app: LauncherApp, compact: Boolean, index: Int, favoriteOrder: List<String>): View {
        val card = FrameLayout(context).apply {
            TvUi.setSelectableBackground(this, { palette }, 18f)
            isLongClickable = true
            contentDescription = "${app.label}, ${if (preferences.isFavorite(app.packageName)) context.getString(R.string.unfavorite_app) else context.getString(R.string.favorite_app)}"
            setOnClickListener { callbacks.launch(app) }
            setOnLongClickListener {
                if (!preferences.isFavorite(app.packageName)) {
                    callbacks.toggleFavorite(app)
                    return@setOnLongClickListener true
                }
                AlertDialog.Builder(context).setTitle(app.label)
                    .setItems(
                        arrayOf(
                            context.getString(R.string.remove_favorite),
                            context.getString(R.string.move_toward_start),
                            context.getString(R.string.move_toward_end),
                        ),
                    ) { _, which ->
                        when (which) {
                            0 -> callbacks.toggleFavorite(app)
                            1 -> callbacks.reorderApp(app, -1)
                            else -> callbacks.reorderApp(app, 1)
                        }
                    }.show()
                true
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                val currentIndex = favoriteOrder.indexOf(app.packageName)
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && preferences.isFavorite(app.packageName) && currentIndex > 0) {
                    callbacks.reorderApp(app, -1); return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && preferences.isFavorite(app.packageName) && currentIndex >= 0 && currentIndex < favoriteOrder.lastIndex) {
                    callbacks.reorderApp(app, 1); return@setOnKeyListener true
                }
                false
            }
        }
        val scale = when (preferences.appIconSize) {
            LauncherPreferences.ICON_SIZE_COMPACT -> 0.88f
            LauncherPreferences.ICON_SIZE_LARGE -> 1.13f
            else -> 1f
        }
        if (compact) {
            val iconTile = iconTile(app, dp(44f * scale))
            card.addView(iconTile, FrameLayout.LayoutParams(dp(44f * scale), dp(44f * scale),
                Gravity.START or Gravity.CENTER_VERTICAL).apply { marginStart = dp(12) })
            card.addView(TextView(context).apply {
                text = app.label; textSize = 12.5f; gravity = Gravity.CENTER_VERTICAL
                setTextColor(palette.text); maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            }, FrameLayout.LayoutParams(-1, -1).apply { marginStart = dp(12) + dp(44f * scale) + dp(11); marginEnd = dp(8) })
            if (preferences.isFavorite(app.packageName)) card.addView(favoriteDot(), FrameLayout.LayoutParams(dp(18), dp(18),
                Gravity.TOP or Gravity.END).apply { topMargin = dp(4); marginEnd = dp(5) })
        } else {
            val tile = dp(64f * scale)
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER or Gravity.TOP
                setPadding(dp(10), dp(13), dp(10), dp(10))
            }
            column.addView(iconTile(app, tile), FrameLayout.LayoutParams(tile, tile))
            column.addView(TextView(context).apply {
                text = app.label; textSize = 11.5f; gravity = Gravity.CENTER
                setTextColor(palette.text); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            }, TvUi.params(-1, -2, top = dp(9)))
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
        text = "✦"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(palette.onAccent)
        background = GradientDrawable().apply { setColor(palette.accent); shape = GradientDrawable.OVAL }
    }

    private fun sectionHeading(view: TextView) = view.apply {
        textSize = 10.5f; letterSpacing = 0.12f; setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(palette.muted); gravity = Gravity.CENTER_VERTICAL
    }

    private fun clockText(view: TextView, size: Float) = view.apply {
        textSize = size; setTextColor(palette.text); gravity = Gravity.CENTER_VERTICAL
        typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
    }

    private fun configureActionChip(view: TextView, label: String, action: () -> Unit) = view.apply {
        text = label; textSize = 11.5f; gravity = Gravity.CENTER; setPadding(dp(14), 0, dp(14), 0)
        setTextColor(palette.text); TvUi.setSelectableBackground(this, { palette }, 22f, pill = true)
        setOnClickListener { action() }
    }

    private fun settingsLabel(textValue: String, color: Int = palette.text, size: Float = 14f) = TextView(context).apply {
        text = textValue; textSize = size; letterSpacing = 0.08f; setTextColor(color)
        gravity = Gravity.CENTER_VERTICAL; setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun settingsOption(label: String, selected: Boolean, action: () -> Unit) = TextView(context).apply {
        text = if (selected) "✓   $label" else "     $label"
        textSize = 13.5f; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), 0, dp(14), 0)
        setTextColor(if (selected) palette.accentBright else palette.text)
        TvUi.setSelectableBackground(this, { palette }, 13f); setOnClickListener { action() }
    }

    private fun horizontalScroll() = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false; clipToPadding = false; overScrollMode = View.OVER_SCROLL_NEVER
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
