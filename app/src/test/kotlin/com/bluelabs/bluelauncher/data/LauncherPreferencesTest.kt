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
