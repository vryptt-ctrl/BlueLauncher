package com.bluelabs.bluelauncher

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import com.bluelabs.bluelauncher.data.AppCategory
import com.bluelabs.bluelauncher.data.AppRepository
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.data.LauncherPreferences
import com.bluelabs.bluelauncher.data.WallpaperStore
import com.bluelabs.bluelauncher.nativeengine.NativeEngine
import com.bluelabs.bluelauncher.ui.LauncherViews
import com.bluelabs.bluelauncher.ui.Palette
import java.util.concurrent.Executors

/** One activity, one short-lived receiver and no foreground/background service. */
class MainActivity : ComponentActivity(), LauncherViews.Callbacks {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appLoader = Executors.newSingleThreadExecutor { task ->
        Thread(task, "BlueLauncher-apps").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private lateinit var preferences: LauncherPreferences
    private lateinit var repository: AppRepository
    private lateinit var launcherViews: LauncherViews
    private var installedApps: List<LauncherApp> = emptyList()
    private var currentQuery = ""
    private var category = AppCategory.ALL
    private var dirtyApps = true
    private var hasLoadedOnce = false
    private var startupElapsedMs = 0L
    private var receiverRegistered = false
    private val wallpaperPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importWallpaper(uri)
    }

    private val clockTick = object : Runnable {
        override fun run() {
            if (!isFinishing && ::launcherViews.isInitialized) launcherViews.updateClock()
            mainHandler.postDelayed(this, CLOCK_REFRESH_MS)
        }
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED -> scheduleAppRefresh()
            }
        }
    }

    private val refreshApps = Runnable {
        if (dirtyApps && !isFinishing && !isDestroyed) loadApps()
    }

    /** Coalesces the burst of package broadcasts an install or update produces. */
    private fun scheduleAppRefresh() {
        dirtyApps = true
        mainHandler.removeCallbacks(refreshApps)
        mainHandler.postDelayed(refreshApps, PACKAGE_REFRESH_DELAY_MS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startNanos = System.nanoTime()
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        preferences = LauncherPreferences(applicationContext)
        repository = AppRepository(applicationContext)
        launcherViews = LauncherViews(this, Palette.fromWallpaper(preferences.wallpaper), preferences, this)
        launcherViews.setDiagnostics {
            "UI ready in ${startupElapsedMs} ms\n" +
                "${installedApps.size} launchable apps\n" +
                "${preferences.favorites.size} pinned · ${preferences.recent.size} recent\n" +
                "Native search: ${if (NativeEngine.isAvailable) "Rust" else "Kotlin fallback"}"
        }
        setContentView(launcherViews.root)
        launcherViews.root.requestFocus()
        startupElapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        registerBackHandling()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        mainHandler.removeCallbacks(clockTick)
        mainHandler.post(clockTick)
        if (dirtyApps) loadApps()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(clockTick)
        mainHandler.removeCallbacks(refreshApps)
        // Stops the resume-badge pulse while another app is on screen.
        if (::launcherViews.isInitialized) launcherViews.stopHeroPulse()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Pressing Home again is the common case here. A full package rescan plus a
        // re-animated grid on every press looked like a flicker, so only re-read when
        // something actually changed.
        if (dirtyApps) loadApps()
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(packageReceiver)
            receiverRegistered = false
        }
        appLoader.shutdownNow()
        if (::launcherViews.isInitialized) launcherViews.dispose()
        mainHandler.removeCallbacks(refreshApps)
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /**
     * The activity never closes on back: a launcher should keep the TV usable even when the
     * remote's Back button is pressed. Back only clears an active search.
     */
    private fun registerBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                launcherViews.onBackPressed()
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> { openSettings(); return true }
            KeyEvent.KEYCODE_SEARCH -> { launcherViews.focusSearch(); return true }
            KeyEvent.KEYCODE_INFO -> { cycleCategory(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun launch(app: LauncherApp) {
        runCatching {
            if (repository.launch(app.packageName)) {
                preferences.recordLaunch(app.packageName)
                // The recents row must update, but the grid the user just left should not
                // replay its entrance animation every time they come back.
                renderApps(animate = false)
            } else launcherViews.showLaunchError()
        }.onFailure { launcherViews.showLaunchError() }
    }

    override fun toggleFavorite(app: LauncherApp) {
        preferences.toggleFavorite(app.packageName)
        renderApps()
    }

    override fun reorderApp(app: LauncherApp, direction: Int) {
        if (app.packageName in preferences.favorites) {
            preferences.moveFavorite(app.packageName, direction, installedApps.map { it.packageName })
        } else {
            preferences.moveApp(app.packageName, direction, installedApps.map { it.packageName })
        }
        renderApps()
    }

    override fun openSettings() {
        // Everything reachable from this panel changes a preference while the grid is
        // covered by the dialog; replaying the 30+ card entrance behind it only made the
        // panel feel slow, so renders triggered from here skip the animation.
        launcherViews.showSettings(
            onWallpaper = { wallpaper ->
                preferences.wallpaper = wallpaper
                // The picked JPEG is no longer referenced, so delete it instead of
                // leaving an orphan behind in filesDir.
                WallpaperStore.clear(applicationContext)
                preferences.wallpaperImage = ""
                launcherViews.updatePalette(Palette.fromWallpaper(wallpaper))
                renderApps(animate = false)
            },
            onSections = { favorites, recent ->
                preferences.showFavorites = favorites
                preferences.showRecent = recent
                renderApps(animate = false)
            },
            onClockSettings = { clock, date, clock24 ->
                preferences.showClock = clock
                preferences.showDate = date
                preferences.clock24Hour = clock24
                launcherViews.updateClock()
            },
            onReset = {
                preferences.resetLayout()
                renderApps(animate = false)
                launcherViews.updateClock()
            },
            onChooseImage = { wallpaperPicker.launch(arrayOf("image/*")) },
            onAbout = { launcherViews.showAbout() },
        )
    }

    override fun queryChanged(query: String) {
        currentQuery = query
        renderApps()
    }

    override fun selectCategory(category: AppCategory) {
        this.category = category
        renderApps()
    }

    override fun cycleCategory() {
        val categories = AppCategory.entries
        category = categories[(categories.indexOf(category) + 1) % categories.size]
        renderApps()
    }

    private fun importWallpaper(uri: Uri) {
        appLoader.execute {
            val path = WallpaperStore.import(applicationContext, uri) ?: return@execute
            if (isFinishing || isDestroyed) return@execute
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                preferences.wallpaperImage = path
                launcherViews.updatePalette(Palette.fromWallpaper(preferences.wallpaper))
            }
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            registerReceiver(packageReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun loadApps() {
        dirtyApps = false
        val isFirstLoad = !hasLoadedOnce
        appLoader.execute {
            val result = runCatching { repository.loadInstalledApps() }.getOrDefault(emptyList())
            if (isFinishing || isDestroyed) return@execute
            mainHandler.post {
                if (!isFinishing && !isDestroyed) {
                    installedApps = result
                    renderApps()
                    // Only the first load grabs the cursor. A package install/update used
                    // to yank focus out of the search box or a settings chip.
                    if (isFirstLoad && result.isNotEmpty() && currentQuery.isEmpty()) launcherViews.root.post {
                        if (!isFinishing) launcherViews.focusFirstApp()
                    }
                    hasLoadedOnce = true
                }
            }
        }
    }

    private fun renderApps(animate: Boolean = true) {
        val order = preferences.orderedApps(installedApps.map { it.packageName })
        val byPackage = installedApps.associateBy { it.packageName }
        val inCategory = category.filter(installedApps, preferences.favorites)
        val candidates = inCategory.sortedBy { order.indexOf(it.packageName).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        val ranking = NativeEngine.rank(
            apps = candidates.map { it.packageName to it.label },
            query = currentQuery,
            favorites = preferences.favoritesInOrder(order),
            recent = preferences.recent,
        )
        val filtered = ranking.mapNotNull(byPackage::get)
        val display = if (currentQuery.isBlank() && category == AppCategory.ALL) {
            order.mapNotNull(byPackage::get)
        } else {
            filtered
        }
        // The hero badge resumes the last opened app even while a search or category
        // filter hides it; falling back to the first *installed* app keeps it useful.
        val resume = preferences.recent.firstNotNullOfOrNull(byPackage::get) ?: order.firstNotNullOfOrNull(byPackage::get)
        launcherViews.render(display, currentQuery, category, animate, resume)
    }

    companion object {
        private const val CLOCK_REFRESH_MS = 60_000L
        private const val PACKAGE_REFRESH_DELAY_MS = 700L
    }
}
