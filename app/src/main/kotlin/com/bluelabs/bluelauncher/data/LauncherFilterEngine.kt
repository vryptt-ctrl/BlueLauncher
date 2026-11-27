package com.bluelabs.bluelauncher.data

import com.bluelabs.bluelauncher.nativeengine.NativeEngine

/**
 * Filtered app results ready for presentation on the launcher UI.
 */
data class LauncherUiModel(
    val displayApps: List<LauncherApp>,
    val favorites: List<LauncherApp>,
    val recents: List<LauncherApp>,
    val resumeApp: LauncherApp?,
    val query: String,
    val category: AppCategory,
    val animate: Boolean,
)

/**
 * Encapsulates the ranking, filtering, ordering, and resume-target computation.
 */
class LauncherFilterEngine {

    fun compute(
        installedApps: List<LauncherApp>,
        query: String,
        category: AppCategory,
        preferences: LauncherPreferences,
        animate: Boolean = true,
    ): LauncherUiModel {
        val normalized = query.trim()
        val order = preferences.orderedApps(installedApps.map { it.packageName })
        val byPackage = installedApps.associateBy { it.packageName }
        val inCategory = category.filter(installedApps, preferences.favorites)
        val candidates = inCategory.sortedBy {
            order.indexOf(it.packageName).let { index -> if (index < 0) Int.MAX_VALUE else index }
        }

        val ranking = NativeEngine.rank(
            apps = candidates.map { it.packageName to it.label },
            query = query,
            favorites = preferences.favoritesInOrder(order),
            recent = preferences.recent,
        )
        val filtered = ranking.mapNotNull(byPackage::get)
        val display = if (query.isBlank() && category == AppCategory.ALL) {
            order.mapNotNull(byPackage::get)
        } else {
            filtered
        }

        val installedPackages = installedApps.map { it.packageName }
        val favorites = preferences.favoritesInOrder(installedPackages).mapNotNull(byPackage::get)
        val recents = preferences.recent.mapNotNull(byPackage::get)

        val resume = preferences.recent.firstNotNullOfOrNull(byPackage::get)
            ?: order.firstNotNullOfOrNull(byPackage::get)

        return LauncherUiModel(
            displayApps = display,
            favorites = favorites,
            recents = recents,
            resumeApp = resume,
            query = query,
            category = category,
            animate = animate,
        )
    }
}
