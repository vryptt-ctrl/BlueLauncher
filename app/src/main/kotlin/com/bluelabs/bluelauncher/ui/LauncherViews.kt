package com.bluelabs.bluelauncher.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import com.bluelabs.bluelauncher.R
import com.bluelabs.bluelauncher.data.AppCategory
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.data.LauncherPreferences
import com.bluelabs.bluelauncher.data.LauncherUiModel
import com.bluelabs.bluelauncher.data.WallpaperStore
import com.bluelabs.bluelauncher.ui.components.AppStripView
import com.bluelabs.bluelauncher.ui.components.HeroView
import com.bluelabs.bluelauncher.ui.components.SearchBarView
import com.bluelabs.bluelauncher.ui.dialogs.AboutDialog
import com.bluelabs.bluelauncher.ui.dialogs.SettingsDialog
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
        clipChildren = false
        clipToPadding = false
    }

    private val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
    }

    private val heroView = HeroView(context, palette) { target ->
        val resolved = target ?: currentDisplayApps.firstOrNull()
        if (resolved != null) callbacks.launch(resolved) else focusSearch()
    }

    private val searchBar = SearchBarView(
        context,
        palette,
        object : SearchBarView.Callbacks {
            override fun onQueryChanged(query: String) {
                this@LauncherViews.query = query
                queryChanged = true
                callbacks.queryChanged(query)
            }

            override fun onSearchSubmit() {
                focusFirstApp()
            }

            override fun onDpadDown() {
                if (appsStrip.childCount > 0) appsStrip.getChildAt(0).requestFocus()
                else callbacks.cycleCategory()
            }
        },
    )

    private val appStripView = AppStripView(
        context,
        palette,
        preferences,
        object : AppStripView.Callbacks {
            override fun launch(app: LauncherApp) = callbacks.launch(app)
            override fun toggleFavorite(app: LauncherApp) = callbacks.toggleFavorite(app)
            override fun reorderApp(app: LauncherApp, direction: Int) = callbacks.reorderApp(app, direction)
        },
    )

    private val appsHeading = TextView(context)
    private val allAppsChip = heading()
    private val favoritesChip = heading()
    private val mediaChip = heading()
    private val gamesChip = heading()

    private val appsStrip = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        clipToPadding = false
        setPadding(dp(HEADROOM_DP), dp(6), dp(HEADROOM_DP), dp(6))
    }
    private val appsScroll = horizontalScroll()
    private val appsRow = rowFrame().apply { addView(appsScroll, FrameLayout.LayoutParams(-1, -1)) }

    private val favoritesTitle = heading()
    private val favoritesStrip = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
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

    private val brand = TextView(context)
    private val searchChip = TextView(context)
    private val settingsChip = TextView(context)
    private val clock = TextView(context)
    private val date = TextView(context)
    private val toastLine = TextView(context)
    private val scrollViewport = ScrollView(context)

    private var currentDisplayApps: List<LauncherApp> = emptyList()
    private var currentFavorites: List<LauncherApp> = emptyList()
    private var currentRecents: List<LauncherApp> = emptyList()
    private var query = ""
    private var categoryIndex = 0
    private var initialized = false
    private var wallpaperGeneration = 0
    private var queryChanged = false
    private var diagnostics: (() -> String)? = null
    private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    private val wallpaperExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "BlueLauncher-wallpaper").apply { priority = Thread.NORM_PRIORITY - 1 }
    }

    companion object {
        const val HEADROOM_DP = 10
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
        this.currentDisplayApps = apps
        this.categoryIndex = category.ordinal
        if (this.query != query) {
            this.query = query
            queryChanged = true
            searchBar.setQuerySilently(query)
        }
        val installedPackages = apps.map { it.packageName }
        this.currentFavorites = preferences.favoritesInOrder(installedPackages).mapNotNull { packageName ->
            apps.find { it.packageName == packageName }
        }
        this.currentRecents = preferences.recent.mapNotNull { packageName ->
            apps.find { it.packageName == packageName }
        }
        heroView.setResumeApp(resume ?: currentRecents.firstOrNull() ?: apps.firstOrNull())
        renderContent(animate)
    }

    fun renderModel(model: LauncherUiModel) {
        this.currentDisplayApps = model.displayApps
        this.currentFavorites = model.favorites
        this.currentRecents = model.recents
        this.categoryIndex = model.category.ordinal
        if (this.query != model.query) {
            this.query = model.query
            queryChanged = true
            searchBar.setQuerySilently(model.query)
        }
        heroView.setResumeApp(model.resumeApp ?: model.recents.firstOrNull() ?: model.displayApps.firstOrNull())
        renderContent(model.animate)
    }

    fun setDiagnostics(provider: () -> String) {
        diagnostics = provider
    }

    fun focusSearch() {
        searchBar.focus()
    }

    fun focusFirstApp() {
        appsStrip.getChildAt(0)?.requestFocus()
    }

    fun setResumeApp(app: LauncherApp?) {
        heroView.setResumeApp(app)
    }

    fun updateClock() {
        val now = Date()
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
        searchBar.updatePalette(palette)
        heroView.updatePalette(palette)
        appStripView.updatePalette(palette)
        applyTextColors()
        loadWallpaperAsync()
        renderContent(animate = false)
    }

    private fun applyTextColors() {
        brand.setTextColor(palette.accentBright)
        clock.setTextColor(palette.text)
        date.setTextColor(palette.muted)
        toastLine.setTextColor(palette.text)
        toastLine.background = toastBackground()
        listOf(searchChip, settingsChip).forEach { chip ->
            chip.background = TvUi.pillBackground(palette, 22f)
            chip.setTextColor(palette.text)
        }
        listOf(appsHeading, favoritesTitle, recentTitle).forEach { it.setTextColor(palette.muted) }
    }

    private fun scrimBackground() = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(palette.wallpaperStart, palette.background, palette.backgroundDeep),
    )

    private fun toastBackground(): LayerDrawable {
        val card = GradientDrawable().apply {
            setColor(palette.surfaceRaised)
            cornerRadius = dp(16).toFloat()
            setStroke(1, palette.outline)
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
        stopHeroPulse()
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
        SettingsDialog.show(
            context = context,
            palette = palette,
            preferences = preferences,
            onWallpaper = onWallpaper,
            onSections = onSections,
            onClockSettings = onClockSettings,
            onAppLabelsToggle = { renderContent(animate = false) },
            onIconSizeChange = { renderContent(animate = false) },
            onReset = onReset,
            onChooseImage = onChooseImage,
            onAbout = onAbout,
        )
    }

    fun showAbout() {
        AboutDialog.show(context, palette, diagnostics)
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
        return searchBar.clear()
    }

    private fun configureTree() {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val margin = (screenWidth * 0.05f).toInt().coerceAtLeast(dp(30))
        content.setPadding(margin, dp(22), margin, dp(16))
        scrollViewport.apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipChildren = false
            clipToPadding = false
            addView(content)
        }
        root.addView(scrollViewport, FrameLayout.LayoutParams(-1, -1))

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
            text = context.getString(R.string.brand)
            textSize = 14f
            letterSpacing = 0.26f
            setTextColor(palette.accentBright)
        }
        top.addView(brand, TvUi.params(-2, dp(40)))
        top.addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))
        top.addView(clockText(clock, 27f))
        top.addView(clockText(date, 13f).apply { gravity = Gravity.END }, TvUi.params(-2, -2, left = dp(14)))
        top.addView(configureActionChip(searchChip, context.getString(R.string.search_action)) { focusSearch() }, TvUi.params(-2, dp(42), left = dp(20)))
        top.addView(configureActionChip(settingsChip, context.getString(R.string.settings_action)) { callbacks.openSettings() }, TvUi.params(-2, dp(42), left = dp(8)))
        content.addView(top, TvUi.params(-1, dp(46), bottom = dp(14)))

        content.addView(heroView.view, TvUi.params(-1, dp(164), bottom = dp(18)))
        content.addView(searchBar.panel, TvUi.params(-1, dp(48), bottom = dp(12)))

        val filters = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        filters.addView(sectionHeading(appsHeading), LinearLayout.LayoutParams(0, dp(39), 1f))
        listOf(allAppsChip, favoritesChip, mediaChip, gamesChip).forEachIndexed { index, chip ->
            chip.apply {
                typeface = TvUi.inter(context)
                textSize = 11.5f
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(12), 0)
                TvUi.setSelectableBackground(this, { palette }, 21f, pill = true, selected = { categoryIndex == index })
                setOnClickListener { callbacks.selectCategory(AppCategory.entries[index]) }
            }
            filters.addView(chip, TvUi.params(-2, dp(38), left = dp(6)))
        }
        content.addView(filters, TvUi.params(-1, dp(40), bottom = dp(6)))
        appsScroll.addView(appsStrip)
        content.addView(
            appsRow,
            TvUi.params(-1, dp(appStripView.rowHeightDp(compact = false)), left = -dp(HEADROOM_DP), right = -dp(HEADROOM_DP), bottom = dp(8)),
        )
        addOptionalRow(favoritesTitle, favoritesRow, favoritesScroll, favoritesStrip)
        addOptionalRow(recentTitle, recentRow, recentScroll, recentStrip)
        listOf(appsScroll, favoritesScroll, recentScroll).forEach { scroll ->
            scroll.isHorizontalFadingEdgeEnabled = true
            scroll.setFadingEdgeLength(dp(HEADROOM_DP))
        }
        toastLine.apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(palette.text)
            visibility = View.GONE
        }
        root.addView(
            toastLine,
            FrameLayout.LayoutParams(dp(300), dp(50), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dp(40)
            },
        )
        initialized = true
    }

    private fun addOptionalRow(
        title: TextView,
        row: FrameLayout,
        scroll: HorizontalScrollView,
        strip: LinearLayout,
    ) {
        content.addView(sectionHeading(title), TvUi.params(-1, dp(26), bottom = dp(3)))
        scroll.addView(strip)
        content.addView(
            row,
            TvUi.params(-1, dp(appStripView.rowHeightDp(compact = true)), left = -dp(HEADROOM_DP), right = -dp(HEADROOM_DP), bottom = dp(6)),
        )
    }

    fun stopHeroPulse() {
        heroView.stopPulse()
    }

    private fun renderContent(animate: Boolean = true) {
        if (!initialized) return
        val normalized = query.trim()
        val displayed = currentDisplayApps
        val animateRows = animate && !queryChanged
        queryChanged = false

        appsHeading.text = TvUi.iconLabel(
            context,
            (if (normalized.isNotEmpty()) {
                context.getString(R.string.results_heading, displayed.size)
            } else {
                context.getString(R.string.apps_heading, displayed.size)
            }).uppercase(),
        )

        heroView.setContent(displayed.size, normalized)
        applyRowMetrics()

        appStripView.rebuildStrip(
            strip = appsStrip,
            items = displayed,
            compact = false,
            normalized = normalized,
            categoryIndex = categoryIndex,
            animate = animateRows,
        )

        val showFavorites = preferences.showFavorites && normalized.isEmpty() && (currentFavorites.isNotEmpty() || categoryIndex == 1)
        val showRecent = preferences.showRecent && normalized.isEmpty() && currentRecents.isNotEmpty()

        if (showFavorites) {
            appStripView.rebuildStrip(
                strip = favoritesStrip,
                items = currentFavorites,
                compact = true,
                emptyHint = currentFavorites.isEmpty(),
                categoryIndex = categoryIndex,
            )
        } else {
            favoritesStrip.removeAllViews()
        }

        if (showRecent) {
            appStripView.rebuildStrip(
                strip = recentStrip,
                items = currentRecents,
                compact = true,
                categoryIndex = categoryIndex,
            )
        } else {
            recentStrip.removeAllViews()
        }

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

    private fun applyRowMetrics() {
        listOf(
            appsRow to appStripView.rowHeightDp(compact = false),
            favoritesRow to appStripView.rowHeightDp(compact = true),
            recentRow to appStripView.rowHeightDp(compact = true),
        ).forEach { (row, height) ->
            val params = row.layoutParams as ViewGroup.MarginLayoutParams
            val px = dp(height)
            if (params.height != px) {
                params.height = px
                row.layoutParams = params
            }
        }
    }

    private fun sectionHeading(view: TextView) = view.apply {
        textSize = 11f
        letterSpacing = 0.12f
        typeface = TvUi.interBold(context)
        setTextColor(palette.muted)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun clockText(view: TextView, size: Float) = view.apply {
        textSize = size
        setTextColor(palette.text)
        gravity = Gravity.CENTER_VERTICAL
        typeface = TvUi.interLight(context)
    }

    private fun configureActionChip(view: TextView, label: String, action: () -> Unit) = view.apply {
        typeface = TvUi.inter(context)
        text = TvUi.iconLabel(context, label)
        textSize = 11.5f
        gravity = Gravity.CENTER
        setPadding(dp(14), 0, dp(14), 0)
        setTextColor(palette.text)
        TvUi.setSelectableBackground(this, { palette }, 22f, pill = true)
        setOnClickListener { action() }
    }

    private fun rowFrame() = FrameLayout(context).apply {
        clipChildren = true
        clipToPadding = true
    }

    private fun horizontalScroll() = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        clipChildren = true
        clipToPadding = true
        overScrollMode = View.OVER_SCROLL_NEVER
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        isFocusable = false
    }

    private fun heading() = TextView(context)
    private fun dp(value: Int) = TvUi.dp(root, value.toFloat())
}
