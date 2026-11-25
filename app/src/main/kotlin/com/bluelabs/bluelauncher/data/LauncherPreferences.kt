package com.bluelabs.bluelauncher.data

import android.content.Context
import android.text.format.DateFormat

/** Small synchronous preference store for persistent home-screen personalization. */
class LauncherPreferences(context: Context) {
    private val store = context.getSharedPreferences("blue_launcher", Context.MODE_PRIVATE)
    private val systemUses24Hour = DateFormat.is24HourFormat(context)

    var favorites: List<String>
        get() {
            val stored = store.getString(KEY_FAVORITES, null)
            if (stored != null) return stored.split('\n').filter(String::isNotBlank)
            @Suppress("DEPRECATION")
            val legacy = runCatching { store.getStringSet(KEY_LEGACY_FAVORITES, null) }.getOrNull().orEmpty()
            if (legacy.isNotEmpty()) {
                favorites = legacy.toList()
                return favorites
            }
            return emptyList()
        }
        private set(value) { store.edit().putString(KEY_FAVORITES, value.distinct().joinToString("\n")).apply() }
    var recent: List<String>
        get() = store.getString(KEY_RECENT, "").orEmpty().split('\n').filter(String::isNotBlank)
        private set(value) { store.edit().putString(KEY_RECENT, value.joinToString("\n")).apply() }
    var appOrder: List<String>
        get() = store.getString(KEY_APP_ORDER, "").orEmpty().split('\n').filter(String::isNotBlank)
        private set(value) { store.edit().putString(KEY_APP_ORDER, value.joinToString("\n")).apply() }
    var showFavorites: Boolean
        get() = store.getBoolean(KEY_SHOW_FAVORITES, true)
        set(value) = store.edit().putBoolean(KEY_SHOW_FAVORITES, value).apply()
    var showRecent: Boolean
        get() = store.getBoolean(KEY_SHOW_RECENT, true)
        set(value) = store.edit().putBoolean(KEY_SHOW_RECENT, value).apply()
    var showClock: Boolean
        get() = store.getBoolean(KEY_SHOW_CLOCK, true)
        set(value) = store.edit().putBoolean(KEY_SHOW_CLOCK, value).apply()
    var showDate: Boolean
        get() = store.getBoolean(KEY_SHOW_DATE, true)
        set(value) = store.edit().putBoolean(KEY_SHOW_DATE, value).apply()
    var clock24Hour: Boolean
        get() = store.getBoolean(KEY_CLOCK_24, systemUses24Hour)
        set(value) = store.edit().putBoolean(KEY_CLOCK_24, value).apply()
    var wallpaper: String
        get() = store.getString(KEY_WALLPAPER, WALLPAPER_NOCTURNE).orEmpty()
        set(value) = store.edit().putString(KEY_WALLPAPER, value).apply()
    var wallpaperImage: String
        get() = store.getString(KEY_WALLPAPER_IMAGE, "").orEmpty()
        set(value) = store.edit().putString(KEY_WALLPAPER_IMAGE, value).apply()
    var appIconSize: Int
        get() = store.getInt(KEY_ICON_SIZE, ICON_SIZE_COMFORTABLE)
        set(value) = store.edit().putInt(KEY_ICON_SIZE, value.coerceIn(ICON_SIZE_COMPACT, ICON_SIZE_LARGE)).apply()
    var showAppLabels: Boolean
        get() = store.getBoolean(KEY_SHOW_LABELS, true)
        set(value) = store.edit().putBoolean(KEY_SHOW_LABELS, value).apply()
    var onboardingComplete: Boolean
        get() = store.getBoolean(KEY_ONBOARDING, false)
        set(value) = store.edit().putBoolean(KEY_ONBOARDING, value).apply()

    fun isFavorite(packageName: String) = packageName in favorites

    fun toggleFavorite(packageName: String) {
        favorites = if (packageName in favorites) favorites - packageName else favorites + packageName
    }

    fun moveApp(packageName: String, direction: Int, installed: List<String>) {
        val current = orderedApps(installed)
        val moved = moveWithin(current, packageName, direction)
        if (moved != current) appOrder = moved
    }

    fun moveFavorite(packageName: String, direction: Int, installed: List<String>) {
        val current = favoritesInOrder(installed)
        val moved = moveWithin(current, packageName, direction)
        if (moved != current) favorites = moved
    }

    fun favoritesInOrder(installed: List<String>): List<String> {
        val installedSet = installed.toHashSet()
        return favorites.filter(installedSet::contains)
    }

    fun recordLaunch(packageName: String) {
        recent = recentWith(packageName, recent)
    }

    fun saveAppOrder(packageNames: List<String>) { appOrder = packageNames.distinct() }

    fun orderedApps(packageNames: List<String>): List<String> = mergeOrder(appOrder, packageNames)

    fun resetLayout() {
        store.edit()
            .remove(KEY_SHOW_FAVORITES).remove(KEY_SHOW_RECENT).remove(KEY_SHOW_CLOCK)
            .remove(KEY_SHOW_DATE).remove(KEY_CLOCK_24).remove(KEY_APP_ORDER)
            .remove(KEY_ICON_SIZE).remove(KEY_SHOW_LABELS).apply()
    }

    companion object {
        const val WALLPAPER_NOCTURNE = "nocturne"
        const val WALLPAPER_INK = "ink"
        const val WALLPAPER_OCEAN = "ocean"
        const val ICON_SIZE_COMPACT = 0
        const val ICON_SIZE_COMFORTABLE = 1
        const val ICON_SIZE_LARGE = 2
        const val MAX_RECENT = 12

        /** Keeps the user's saved order, appends newly installed apps and drops uninstalled ones. */
        fun mergeOrder(saved: List<String>, installed: List<String>): List<String> {
            val installedSet = installed.toHashSet()
            val kept = saved.filter(installedSet::contains).distinct()
            val keptSet = kept.toHashSet()
            return kept + installed.filterNot(keptSet::contains)
        }

        /** Moves one item by one position; unknown packages and edge positions are left untouched. */
        fun moveWithin(items: List<String>, packageName: String, direction: Int): List<String> {
            val index = items.indexOf(packageName)
            if (index < 0 || direction == 0) return items
            val destination = (index + direction.coerceIn(-1, 1)).coerceIn(0, items.lastIndex)
            if (destination == index) return items
            return items.toMutableList().apply { add(destination, removeAt(index)) }
        }

        fun recentWith(packageName: String, current: List<String>): List<String> =
            (listOf(packageName) + current.filterNot { it == packageName }).take(MAX_RECENT)

        private const val KEY_FAVORITES = "favorites_v2"
        private const val KEY_LEGACY_FAVORITES = "favorites"
        private const val KEY_RECENT = "recent"
        private const val KEY_APP_ORDER = "app_order"
        private const val KEY_SHOW_FAVORITES = "show_favorites"
        private const val KEY_SHOW_RECENT = "show_recent"
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_CLOCK_24 = "clock_24"
        private const val KEY_WALLPAPER = "wallpaper"
        private const val KEY_WALLPAPER_IMAGE = "wallpaper_image"
        private const val KEY_ICON_SIZE = "icon_size"
        private const val KEY_SHOW_LABELS = "show_app_labels"
        private const val KEY_ONBOARDING = "onboarding_complete"
    }
}
