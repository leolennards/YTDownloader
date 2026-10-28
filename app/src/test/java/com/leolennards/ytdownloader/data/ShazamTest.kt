package com.leolennards.ytdownloader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamTest {
    // cut down from the real page for https://www.shazam.com/track/522289236
    private val page = """
        <html><head>
        <meta property="og:title" content="DtMF - Bad Bunny: Song Lyrics, Music Videos &amp; Concerts"/>
        <script type="application/ld+json">{"@context":"https://schema.org","@type":"WebSite","name":"Shazam"}</script>
        <script type="application/ld+json">{
            "@context": "https://schema.org",
            "@type": "MusicRecording",
            "name": "DtMF",
            "duration": "PT3M57S",
            "byArtist": "Bad Bunny",
            "genre": "Latino"
        }</script>
        </head></html>
    """.trimIndent()

    @Test
    fun knowsShazamLinks() {
        assertTrue(Shazam.isLink("https://www.shazam.com/song/1787023936/dtmf"))
        assertTrue(Shazam.isLink("https://www.shazam.com/track/522289236"))
        assertTrue(Shazam.isLink("https://shz.am/t522289236"))
        assertFalse(Shazam.isLink("https://www.youtube.com/watch?v=abc"))
        assertFalse(Shazam.isLink("https://notshazam.com/song/1"))
    }

    @Test
    fun readsTheSongFromThePage() {
        assertEquals(Shazam.Song("DtMF", "Bad Bunny", 237), Shazam.parse(page))
        // a page that is not a song (an artist page) gives nothing
        assertNull(Shazam.parse("""<script type="application/ld+json">{"@type":"MusicGroup","name":"Bad Bunny"}</script>"""))
    }

    @Test
    fun ignoresAPageAboutAnotherSong() {
        // without a browser shazam often shows a trending song instead of the one in the link
        val html = """<script type="application/ld+json">{"@type":"MusicRecording","@id":"https://www.shazam.com/song/1787023936/dtmf",
            "name":"DtMF","byArtist":"Bad Bunny"}</script>"""
        assertNull(Shazam.parse(html, id = "1488408568"))
        assertEquals("DtMF", Shazam.parse(html, id = "1787023936")?.title)
    }

    @Test
    fun readsTheSongFromSharedText() {
        val (link, song) = Shazam.fromShareText(
            "I used Shazam to discover Blinding Lights by The Weeknd. https://www.shazam.com/track/524406447/blinding-lights",
        )!!
        assertEquals("https://www.shazam.com/track/524406447/blinding-lights", link)
        assertEquals(Shazam.Song("Blinding Lights", "The Weeknd", null), song)
        // "By" in the title and dots in the artist
        assertEquals(
            Shazam.Song("Stand By Me", "Ben E. King", null),
            Shazam.fromShareText("I used Shazam to discover Stand By Me by Ben E. King. https://shz.am/t123")?.second,
        )
        // the other ways shazam words it
        assertEquals(
            Shazam.Song("Circadian Rhythm", "Drake", null),
            Shazam.fromShareText("Found Circadian Rhythm by Drake with Shazam, have a listen: https://www.shazam.com/track/1/circadian-rhythm")?.second,
        )
        assertEquals(
            Shazam.Song("Lay It Down", "Lloyd", null),
            Shazam.fromShareText("Lay It Down by Lloyd\nhttps://www.shazam.com/track/2/lay-it-down")?.second,
        )
        assertEquals(
            Shazam.Song("Lay It Down", "Lloyd", null),
            Shazam.fromShareText("Check out Lay It Down by Lloyd on Shazam https://shz.am/t2")?.second,
        )
        // subject and message together, as the share arrives from the shazam app
        assertEquals(
            Shazam.Song("Sweeterman", "Drake", null),
            Shazam.fromShareText("Sweeterman - Drake\nI used Shazam to discover Sweeterman by Drake. https://www.shazam.com/track/3/sweeterman")?.second,
        )
        // only the subject has the name
        assertEquals(
            Shazam.Song("Sweeterman", "Drake", null),
            Shazam.fromShareText("Sweeterman - Drake\nhttps://www.shazam.com/track/3/sweeterman")?.second,
        )
        assertNull(Shazam.fromShareText("https://www.shazam.com/track/524406447"))
        assertNull(Shazam.fromShareText("I used Shazam to discover Song by Artist. https://youtube.com/watch?v=x"))
    }

    @Test
    fun findsTheAppleMusicId() {
        assertEquals("1488408568", Shazam.appleId("https://www.shazam.com/song/1488408568/blinding-lights?referrer=share"))
        assertEquals("1488408568", Shazam.appleId("https://www.shazam.com/en-us/song/1488408568/blinding-lights"))
        assertNull(Shazam.appleId("https://www.shazam.com/track/524406447/blinding-lights"))
    }

    @Test
    fun readsArtistObjectsAndEscapes() {
        val html = """<script type="application/ld+json">{"@type":"MusicRecording","name":"Movin&#x27; To The Sun",
            "byArtist":[{"@type":"MusicGroup","name":"HUGEL"},{"name":"Ultra Naté"}]}</script>"""
        assertEquals(Shazam.Song("Movin' To The Sun", "HUGEL, Ultra Naté", null), Shazam.parse(html))
    }

    @Test
    fun readsLengths() {
        assertEquals(237, Shazam.isoSeconds("PT3M57S"))
        assertEquals(3725, Shazam.isoSeconds("PT1H2M5S"))
        assertEquals(45, Shazam.isoSeconds("PT45S"))
        assertNull(Shazam.isoSeconds("PT"))
        assertNull(Shazam.isoSeconds("3:57"))
    }

    @Test
    fun readsSearchOutput() {
        val hits = Shazam.parseHits(
            "https://music.youtube.com/watch?v=aaa\tBlinding Lights\t200.0\tThe Weeknd\n" +
                "[youtube] some log line\n" +
                "https://www.youtube.com/watch?v=bbb\tThe Weeknd - Blinding Lights (Official Video)\tNA\tNA\n",
        )
        assertEquals(2, hits.size)
        assertEquals(Shazam.Hit("https://music.youtube.com/watch?v=aaa", "Blinding Lights", 200, "The Weeknd"), hits[0])
        assertEquals(Shazam.Hit("https://www.youtube.com/watch?v=bbb", "The Weeknd - Blinding Lights (Official Video)", null, null), hits[1])
    }

    private val blinding = Shazam.Song("Blinding Lights", "The Weeknd", 201)

    @Test
    fun picksTheStudioVersion() {
        val hits = listOf(
            Shazam.Hit("v1", "The Weeknd - Blinding Lights (Official Video)", 263, "The Weeknd"),
            Shazam.Hit("remix", "Blinding Lights (Remix)", 202, "The Weeknd"),
            Shazam.Hit("audio", "Blinding Lights", 200, "The Weeknd - Topic"),
        )
        // the music video has a long intro and the remix is another song, so the studio audio wins
        assertEquals("audio", Shazam.pickBest(hits, blinding)?.url)
    }

    @Test
    fun acceptsTheArtistInTheTitleOrTheRightLength() {
        // channel unknown, but the artist is in the title
        assertEquals("a", Shazam.pickBest(listOf(Shazam.Hit("a", "The Weeknd - Blinding Lights", null, null)), blinding)?.url)
        // another uploader, but the length is spot on
        assertEquals("b", Shazam.pickBest(listOf(Shazam.Hit("b", "Blinding Lights", 201, "Lyrics Hub")), blinding)?.url)
    }

    @Test
    fun refusesOtherSongs() {
        val hits = listOf(
            // a cover by someone else, length unknown
            Shazam.Hit("cover", "Blinding Lights", null, "Some Singer"),
            // the same artist but another song
            Shazam.Hit("other", "Save Your Tears", 215, "The Weeknd"),
            // karaoke
            Shazam.Hit("karaoke", "The Weeknd - Blinding Lights (Karaoke Version)", 201, "Sing King"),
        )
        assertNull(Shazam.pickBest(hits, blinding))
    }

    private val giveThanks = Shazam.Song("Give Thanks (feat. Allanzo)", "B3XX", null)

    @Test
    fun findsTheLengthInTheStore() {
        val tracks = listOf(
            Shazam.Track("Give Thanks (Remix)", "B3XX", 250),
            Shazam.Track("Give Thanks", "Other Band", 300),
            Shazam.Track("Give Thanks (feat. Allanzo)", "B3XX", 191),
        )
        assertEquals(191, Shazam.lengthOf(tracks, giveThanks))
        assertNull(Shazam.lengthOf(tracks.take(2), giveThanks))
    }
}
