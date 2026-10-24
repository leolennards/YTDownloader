package com.leolennards.ytdownloader.data

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

// asks the real lrclib.net, so it only runs when LIVE_TESTS=1 is set (it needs the internet)
class LyricsLiveTest {
    private fun live() = Assume.assumeTrue("set LIVE_TESTS=1 to run", System.getenv("LIVE_TESTS") == "1")

    @Test
    fun findsSyncedLyricsForARealSong() {
        live()
        val lyrics = LyricsFinder.find("Blinding Lights", listOf("The Weeknd - Topic"), 202.0)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.hasSynced)
        assertTrue(lyrics.plain.contains("I've been tryna call", ignoreCase = true))
    }

    @Test
    fun aYoutubeStyleTitleStillMatches() {
        live()
        val lyrics = LyricsFinder.find("Never Gonna Give You Up (Official Music Video)", listOf("RickAstleyVEVO"), 213.0)
        assertNotNull(lyrics)
    }

    @Test
    fun aMadeUpSongGetsNothing() {
        live()
        assertNull(LyricsFinder.find("Ember", listOf("The Glow"), 180.0))
    }
}
