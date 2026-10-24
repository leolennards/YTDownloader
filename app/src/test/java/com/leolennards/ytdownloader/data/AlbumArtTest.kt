package com.leolennards.ytdownloader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// the candidates below are real search results from itunes and deezer (october 2026)
class AlbumArtTest {
    private fun c(title: String, artist: String) = AlbumArt.Candidate(title, artist, "$title|$artist")

    @Test
    fun picksTheRealSongOverRemixesAndCovers() {
        val results = listOf(
            c("Blinding Lights (Remix)", "The Weeknd & ROSALÍA"),
            c("Blinding Lights", "The Weeknd"),
        )
        assertEquals("Blinding Lights|The Weeknd", AlbumArt.pickBest(results, "Blinding Lights", listOf("The Weeknd - Topic"))?.artUrl)

        val drake = listOf(
            c("Nice for What (8-Bit Drake Emulation)", "8-Bit Arcade"),
            c("Nice For What", "Drake"),
        )
        assertEquals("Nice For What|Drake", AlbumArt.pickBest(drake, "Nice For What", listOf("DrakeVEVO"))?.artUrl)
    }

    @Test
    fun aFeatureIsNotTheMainArtist() {
        // searching "drake mine" finds beyonce's song with drake on it, which is not drake's "Mine"
        val results = listOf(c("Mine (feat. Drake)", "Beyoncé"))
        assertNull(AlbumArt.pickBest(results, "Mine", listOf("Drake")))
        // but it is right when it is the beyonce song being downloaded
        assertEquals("Mine (feat. Drake)|Beyoncé", AlbumArt.pickBest(results, "Mine (feat. Drake)", listOf("Beyonce"))?.artUrl)
    }

    @Test
    fun remixesCountWhenThatIsWhatIsDownloaded() {
        val results = listOf(c("Blinding Lights", "The Weeknd"), c("Blinding Lights (Remix)", "The Weeknd"))
        assertEquals(
            "Blinding Lights (Remix)|The Weeknd",
            AlbumArt.pickBest(results, "Blinding Lights (Remix)", listOf("The Weeknd"))?.artUrl,
        )
    }

    @Test
    fun differentSongsWithSimilarNamesDoNotMatch() {
        val results = listOf(c("Mine Pt. 2 Extended", "Drake"), c("Gold Mine", "Drake"))
        assertNull(AlbumArt.pickBest(results, "Mine", listOf("Drake")))
    }

    @Test
    fun youtubeTitleNoiseIsIgnored() {
        assertEquals("never gonna give you up", AlbumArt.normalizeTitle("Never Gonna Give You Up (Official Music Video) [4K Remaster]"))
        assertEquals("one dance", AlbumArt.normalizeTitle("One Dance (feat. Wizkid & Kyla)"))
        assertEquals("one dance", AlbumArt.normalizeTitle("One Dance ft. Wizkid"))
        assertEquals("beyonce", AlbumArt.normalizeArtist("Beyoncé"))
    }

    @Test
    fun channelNamesBecomeArtistNames() {
        assertEquals("drake", AlbumArt.normalizeArtist("DrakeVEVO"))
        assertEquals("weeknd", AlbumArt.normalizeArtist("The Weeknd - Topic"))
        assertEquals("rick astley", AlbumArt.normalizeArtist("Rick Astley"))
        assertTrue(AlbumArt.artistMatches("Drake & 21 Savage", listOf("drake")))
        assertTrue(AlbumArt.artistMatches("The Weeknd", listOf("weeknd")))
        assertFalse(AlbumArt.artistMatches("8-Bit Arcade", listOf("drake")))
    }

    @Test
    fun nothingToGoOnMeansNoMatch() {
        assertNull(AlbumArt.pickBest(listOf(c("Song", "Someone")), "Song", emptyList()))
        assertNull(AlbumArt.pickBest(emptyList(), "Song", listOf("Someone")))
    }
}
