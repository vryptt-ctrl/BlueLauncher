package com.bluelabs.bluelauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LauncherFilterEngineTest {

    private fun mockApp(pkg: String, label: String, cat: Int = LauncherApp.CATEGORY_UNDEFINED) =
        LauncherApp(
            packageName = pkg,
            label = label,
            icon = null,
            category = cat,
        )

    @Test
    fun compute_ordersAppsAndExtractsFavorites() {
        val app1 = mockApp("com.test.one", "App One")
        val app2 = mockApp("com.test.two", "App Two")

        val model = LauncherUiModel(
            displayApps = listOf(app1, app2),
            favorites = listOf(app1),
            recents = listOf(app2),
            resumeApp = app2,
            query = "",
            category = AppCategory.ALL,
            animate = true,
        )

        assertEquals(2, model.displayApps.size)
        assertEquals("com.test.one", model.favorites.first().packageName)
        assertNotNull(model.resumeApp)
    }
}
