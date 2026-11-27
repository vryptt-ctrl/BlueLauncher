package com.bluelabs.bluelauncher.viewmodel

import android.content.Context
import android.net.Uri
import com.bluelabs.bluelauncher.data.AppCategory
import com.bluelabs.bluelauncher.data.AppRepository
import com.bluelabs.bluelauncher.data.LauncherApp
import com.bluelabs.bluelauncher.data.LauncherFilterEngine
import com.bluelabs.bluelauncher.data.LauncherPreferences
import com.bluelabs.bluelauncher.data.LauncherUiModel
import com.bluelabs.bluelauncher.data.WallpaperStore
import com.bluelabs.bluelauncher.nativeengine.NativeEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Controller/ViewModel managing state and business operations for Launcher.
 */
class LauncherViewModel(
    private val context: Context,
    val preferences: LauncherPreferences = LauncherPreferences(context.applicationContext),
    val repository: AppRepository = AppRepository(context.applicationContext),
    private val filterEngine: LauncherFilterEngine = LauncherFilterEngine(),
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "BlueLauncher-vm").apply { priority = Thread.NORM_PRIORITY - 1 }
    },
) {
    var installedApps: List<LauncherApp> = emptyList()
        private set

    var currentQuery: String = ""
        private set

    var currentCategory: AppCategory = AppCategory.ALL
        private set

    var dirtyApps: Boolean = true
        private set

    var hasLoadedOnce: Boolean = false
        private set

    var startupElapsedMs: Long = 0L

    var onModelUpdated: ((LauncherUiModel, Boolean) -> Unit)? = null
    var onWallpaperUpdated: ((String) -> Unit)? = null
    var onLaunchFailed: (() -> Unit)? = null

    fun markAppsDirty() {
        dirtyApps = true
    }

    fun loadApps(onFirstLoadAppsAvailable: (() -> Unit)? = null) {
        dirtyApps = false
        val isFirstLoad = !hasLoadedOnce
        backgroundExecutor.execute {
            val result = runCatching { repository.loadInstalledApps() }.getOrDefault(emptyList())
            installedApps = result
            val model = filterEngine.compute(
                installedApps = installedApps,
                query = currentQuery,
                category = currentCategory,
                preferences = preferences,
                animate = true,
            )
            onModelUpdated?.invoke(model, isFirstLoad)
            if (isFirstLoad && result.isNotEmpty() && currentQuery.isEmpty()) {
                onFirstLoadAppsAvailable?.invoke()
            }
            hasLoadedOnce = true
        }
    }

    fun setQuery(query: String) {
        currentQuery = query
        dispatchUpdate()
    }

    fun setCategory(category: AppCategory) {
        currentCategory = category
        dispatchUpdate()
    }

    fun cycleCategory() {
        val categories = AppCategory.entries
        currentCategory = categories[(categories.indexOf(currentCategory) + 1) % categories.size]
        dispatchUpdate()
    }

    fun launchApp(app: LauncherApp) {
        val success = runCatching { repository.launch(app.packageName) }.getOrDefault(false)
        if (success) {
            preferences.recordLaunch(app.packageName)
            dispatchUpdate(animate = false)
        } else {
            onLaunchFailed?.invoke()
        }
    }

    fun toggleFavorite(app: LauncherApp) {
        preferences.toggleFavorite(app.packageName)
        dispatchUpdate()
    }

    fun reorderApp(app: LauncherApp, direction: Int) {
        if (app.packageName in preferences.favorites) {
            preferences.moveFavorite(app.packageName, direction, installedApps.map { it.packageName })
        } else {
            preferences.moveApp(app.packageName, direction, installedApps.map { it.packageName })
        }
        dispatchUpdate()
    }

    fun updateWallpaper(wallpaper: String) {
        preferences.wallpaper = wallpaper
        WallpaperStore.clear(context.applicationContext)
        preferences.wallpaperImage = ""
        dispatchUpdate(animate = false)
        onWallpaperUpdated?.invoke(wallpaper)
    }

    fun importWallpaper(uri: Uri) {
        backgroundExecutor.execute {
            val path = WallpaperStore.import(context.applicationContext, uri) ?: return@execute
            preferences.wallpaperImage = path
            onWallpaperUpdated?.invoke(preferences.wallpaper)
        }
    }

    fun updateSections(favorites: Boolean, recent: Boolean) {
        preferences.showFavorites = favorites
        preferences.showRecent = recent
        dispatchUpdate(animate = false)
    }

    fun updateClockSettings(showClock: Boolean, showDate: Boolean, clock24: Boolean) {
        preferences.showClock = showClock
        preferences.showDate = showDate
        preferences.clock24Hour = clock24
    }

    fun resetLayout() {
        preferences.resetLayout()
        dispatchUpdate(animate = false)
    }

    fun dispatchUpdate(animate: Boolean = true) {
        val model = filterEngine.compute(
            installedApps = installedApps,
            query = currentQuery,
            category = currentCategory,
            preferences = preferences,
            animate = animate,
        )
        onModelUpdated?.invoke(model, false)
    }

    fun getDiagnostics(): String {
        return "UI ready in ${startupElapsedMs} ms\n" +
            "${installedApps.size} launchable apps\n" +
            "${preferences.favorites.size} pinned · ${preferences.recent.size} recent\n" +
            "Native search: ${if (NativeEngine.isAvailable) "Rust" else "Kotlin fallback"}"
    }

    fun dispose() {
        backgroundExecutor.shutdownNow()
    }
}
