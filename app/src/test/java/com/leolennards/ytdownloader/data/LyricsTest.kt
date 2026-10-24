package com.leolennards.ytdownloader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import kotlin.io.path.createTempDirectory

class LyricsTest {
    @Test
    fun readsLrcTimes() {
        val lines = LyricsFinder.parseLrc(
            """
            [ar: The Weeknd]
            [00:13.42] Yeah
            [00:14.81]
            [00:26.95] I've been tryna call
            [01:02.5] One digit
            [00:40.00][01:40.00] Twice
            """.trimIndent(),
        )
        assertEquals(LyricLine(13_420, "Yeah"), lines.first())
        assertEquals(LyricLine(14_810, ""), lines[1])
        assertEquals(LyricLine(26_950, "I've been tryna call"), lines[2])
        assertTrue(lines.contains(LyricLine(62_500, "One digit")))
        // a line sung twice gets both times
        assertEquals(2, lines.count { it.text == "Twice" })
        // the tag line without a time is not a lyric
        assertTrue(lines.none { it.text.contains("Weeknd") })
    }

    @Test
    fun findsTheLineBeingSung() {
        val lines = listOf(LyricLine(1000, "a"), LyricLine(5000, "b"), LyricLine(9000, "c"))
        assertEquals(-1, LyricsFinder.currentLine(lines, 500))
        assertEquals(0, LyricsFinder.currentLine(lines, 1000))
        assertEquals(1, LyricsFinder.currentLine(lines, 8999))
        assertEquals(2, LyricsFinder.currentLine(lines, 200_000))
    }

    private fun c(title: String, artist: String, duration: Double, synced: Boolean = true) =
        LyricsFinder.Candidate(title, artist, duration, "words", if (synced) "[00:01.00] words" else null, false)

    @Test
    fun theLengthMustMatch() {
        // real lrclib results for blinding lights: one is a 248 second track from another album
        val results = listOf(c("Blinding Lights", "The Weeknd", 248.0), c("Blinding Lights", "The Weeknd", 202.0))
        assertEquals(202.0, LyricsFinder.pickBest(results, "Blinding Lights", listOf("The Weeknd"), 201.0)!!.durationSec, 0.0)
        // nothing within a few seconds: no lyrics rather than lines at the wrong times
        assertNull(LyricsFinder.pickBest(results, "Blinding Lights", listOf("The Weeknd"), 180.0))
    }

    @Test
    fun prefersSyncedAndSkipsWrongSongs() {
        val results = listOf(
            c("Blinding Lights", "The Weeknd", 200.0, synced = false),
            c("Blinding Lights", "The Weeknd", 201.0, synced = true),
            c("Blinding Lights (Remix)", "The Weeknd", 201.0),
            LyricsFinder.Candidate("Blinding Lights", "The Weeknd", 201.0, null, null, true),
        )
        val best = LyricsFinder.pickBest(results, "Blinding Lights", listOf("The Weeknd - Topic"), 201.0)
        assertNotNull(best!!.synced)
        assertNull(LyricsFinder.pickBest(results, "Blinding Lights", listOf("Someone Else"), 201.0))
    }

    @Test
    fun lyricsGoInTheFileAndComeBackOut() {
        val audio = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 1, 2, 3)
        val words = "I've been tryna call\nÉté, naïve — 日本語 too"
        val tagged = Id3Cover.embedLyrics(audio, words)!!
        assertEquals(words, Id3Cover.extractLyrics(tagged))
        // adding lyrics again replaces them instead of stacking a second copy
        val again = Id3Cover.embedLyrics(tagged, "new words")!!
        assertEquals("new words", Id3Cover.extractLyrics(again))
        // and a cover added afterwards keeps the lyrics
        val withCover = Id3Cover.embed(again, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 0xFF.toByte(), 0xD9.toByte()))!!
        assertEquals("new words", Id3Cover.extractLyrics(withCover))
        assertNull(Id3Cover.extractLyrics(audio))
    }

    // checks that a real tool reads the lyrics frame the way music apps would. needs ffmpeg
    @Test
    fun otherAppsCanReadTheLyrics() {
        val tools = listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin").map { java.io.File(it) }
            .firstOrNull { java.io.File(it, "ffmpeg").canExecute() && java.io.File(it, "ffprobe").canExecute() }
        Assume.assumeTrue("ffmpeg not installed", tools != null)
        val dir = createTempDirectory("lyrics").toFile()
        try {
            val mp3 = java.io.File(dir, "song.mp3")
            run("${tools!!.path}/ffmpeg", "-v", "error", "-f", "lavfi", "-i", "sine=duration=1", "-c:a", "libmp3lame",
                "-id3v2_version", "3", "-metadata", "title=Song", mp3.path)
            mp3.writeBytes(Id3Cover.embedLyrics(mp3.readBytes(), "First line\nSecond line")!!)
            val out = run("${tools.path}/ffprobe", "-v", "error", "-show_entries", "format_tags", "-of", "default=nw=1", mp3.path)
            assertTrue(out, out.contains("First line"))
            assertTrue(out, out.contains("title=Song"))
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun run(vararg command: String): String {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(output, 0, process.waitFor())
        return output
    }
}
