package com.bluelabs.bluelauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvUiTest {
    private val glyph = '\uE9B0'
    private val otherGlyph = '\uE87E'

    @Test
    fun textWithoutGlyphsYieldsNoRuns() {
        assertTrue(TvUi.glyphRuns("Your apps").isEmpty())
        assertTrue(TvUi.glyphRuns("").isEmpty())
    }

    @Test
    fun aLeadingGlyphRunEndsWhereTheWordsStart() {
        // "  All apps": one glyph, then spaces and Latin text.
        assertEquals(listOf(0 to 1), TvUi.glyphRuns("$glyph   All apps"))
    }

    @Test
    fun contiguousGlyphsAreOneRun() {
        assertEquals(listOf(0 to 2), TvUi.glyphRuns("$glyph$otherGlyph Pinned"))
    }

    @Test
    fun aTrailingGlyphRunIsClosedAtTheEnd() {
        assertEquals(listOf(6 to 8), TvUi.glyphRuns("Apps  $glyph$otherGlyph"))
    }

    @Test
    fun separateRunsStaySeparate() {
        // glyph + "abc" + glyph = 5 characters, so the trailing run is 4..5.
        assertEquals(listOf(0 to 1, 4 to 5), TvUi.glyphRuns("${glyph}abc$otherGlyph"))
    }
}
