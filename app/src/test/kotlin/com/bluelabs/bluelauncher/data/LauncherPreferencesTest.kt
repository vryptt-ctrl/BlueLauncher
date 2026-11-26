package com.bluelabs.bluelauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LauncherPreferencesTest {
    @Test
    fun savedOrderIsKeptAndNewPackagesAreAppended() {
        assertEquals(
            listOf("b", "a", "c"),
            LauncherPreferences.mergeOrder(listOf("b", "a"), listOf("a", "b", "c")),
        )
    }

    @Test
    fun savedOrderDropsUninstalledAndDuplicatePackages() {
        assertEquals(
            listOf("b", "c", "a"),
            LauncherPreferences.mergeOrder(listOf("gone", "b", "b", "c"), listOf("a", "b", "c")),
        )
    }

    @Test
    fun favoriteMovesOnePositionTowardTheStart() {
        assertEquals(
            listOf("a", "c", "b"),
            LauncherPreferences.moveWithin(listOf("a", "b", "c"), "c", -1),
        )
    }

    @Test
    fun favoriteMovesOnePositionTowardTheEnd() {
        assertEquals(
            listOf("b", "a", "c"),
            LauncherPreferences.moveWithin(listOf("a", "b", "c"), "a", 1),
        )
    }

    @Test
    fun movingPastAnEdgeOrUnknownPackageIsANoOp() {
        val items = listOf("a", "b", "c")
        assertSame(items, LauncherPreferences.moveWithin(items, "a", -1))
        assertSame(items, LauncherPreferences.moveWithin(items, "c", 1))
        assertSame(items, LauncherPreferences.moveWithin(items, "missing", 1))
    }

    @Test
    fun movingAFavoriteKeepsPinsForUninstalledApps() {
        // "gone" is pinned but not installed (uninstalled/disabled): moving "c" must not
        // drop it from the stored list.
        val favorites = listOf("a", "gone", "b", "c")
        assertEquals(
            listOf("a", "gone", "c", "b"),
            LauncherPreferences.moveFavoriteIn(favorites, "c", -1, listOf("a", "b", "c")),
        )
    }

    @Test
    fun movingAFavoriteAcrossAHiddenPinSkipsIt() {
        // "b" is only one visible slot away from "c" even though a hidden pin sits between.
        // Swapping keeps "gone" at index 2, so it returns to the same place on reinstall.
        val favorites = listOf("a", "b", "gone", "c")
        val moved = LauncherPreferences.moveFavoriteIn(favorites, "c", -1, listOf("a", "b", "c"))
        assertEquals(listOf("a", "c", "gone", "b"), moved)
        assertEquals(2, moved.indexOf("gone"))
    }

    @Test
    fun movingAFavoriteAtTheVisibleEdgeDoesNothing() {
        val favorites = listOf("gone", "a", "b")
        assertSame(favorites, LauncherPreferences.moveFavoriteIn(favorites, "a", -1, listOf("a", "b")))
        assertSame(favorites, LauncherPreferences.moveFavoriteIn(favorites, "b", 1, listOf("a", "b")))
        assertSame(favorites, LauncherPreferences.moveFavoriteIn(favorites, "missing", 1, listOf("a", "b")))
    }

    @Test
    fun recentHistoryIsBoundedMostRecentFirstAndDeduplicated() {
        var history = (0 until 20).fold(emptyList<String>()) { current, index ->
            LauncherPreferences.recentWith("app$index", current)
        }
        assertEquals(LauncherPreferences.MAX_RECENT, history.size)
        assertEquals("app19", history.first())
        assertEquals("app8", history.last())

        history = LauncherPreferences.recentWith("app15", history)
        assertEquals("app15", history.first())
        assertEquals(LauncherPreferences.MAX_RECENT, history.size)
    }
}
