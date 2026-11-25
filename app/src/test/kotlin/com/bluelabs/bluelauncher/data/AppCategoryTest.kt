package com.bluelabs.bluelauncher.data

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryTest {
    private fun app(name: String, label: String, category: Int = ApplicationInfo.CATEGORY_UNDEFINED) =
        LauncherApp(name, label, icon = null, category = category)

    @Test
    fun favoritesCategoryUsesPinnedPackages() {
        val apps = listOf(app("one", "One"), app("two", "Two"))
        assertEquals(listOf("two"), AppCategory.FAVORITES.filter(apps, setOf("two")).map { it.packageName })
    }

    @Test
    fun mediaAndGamesUsePlatformCategoriesAndFriendlyFallbacks() {
        val apps = listOf(
            app("movie", "Theatre", ApplicationInfo.CATEGORY_VIDEO),
            app("com.example.game", "Arcade", ApplicationInfo.CATEGORY_UNDEFINED),
            app("ordinary", "Calculator"),
        )
        assertEquals(listOf("movie"), AppCategory.MEDIA.filter(apps, emptySet()).map { it.packageName })
        assertEquals(listOf("com.example.game"), AppCategory.GAMES.filter(apps, emptySet()).map { it.packageName })
    }
}
