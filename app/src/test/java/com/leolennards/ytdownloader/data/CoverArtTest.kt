package com.leolennards.ytdownloader.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.io.path.createTempDirectory

class CoverArtTest {
    // a w x h picture with flat black bars and a noisy middle, like a letterboxed thumbnail
    private fun picture(w: Int, h: Int, top: Int, bottom: Int, left: Int, right: Int): IntArray {
        var seed = 7
        return IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            if (y < top || y >= h - bottom || x < left || x >= w - right) {
                0xFF000000.toInt() or (if (seed++ % 3 == 0) 0x020202 else 0)
            } else {
                seed = seed * 1103515245 + 12345
                0xFF000000.toInt() or (seed ushr 8 and 0xFFFFFF)
            }
        }
    }

    @Test
    fun trimsLetterboxBars() {
        // hqdefault style: 480x360 with 45 px bars top and bottom
        val bounds = CoverArt.contentBounds(picture(480, 360, 45, 45, 0, 0), 480, 360)
        assertEquals(CoverArt.Bounds(0, 45, 480, 270), bounds)
    }

    @Test
    fun trimsPillarboxBars() {
        val bounds = CoverArt.contentBounds(picture(1280, 720, 0, 0, 160, 160), 1280, 720)
        assertEquals(CoverArt.Bounds(160, 0, 960, 720), bounds)
    }

    @Test
    fun keepsPictureWithoutBars() {
        val bounds = CoverArt.contentBounds(picture(1280, 720, 0, 0, 0, 0), 1280, 720)
        assertEquals(CoverArt.Bounds(0, 0, 1280, 720), bounds)
    }

    @Test
    fun keepsPlainPictureWhole() {
        val plain = IntArray(100 * 100) { 0xFFFFFFFF.toInt() }
        assertEquals(CoverArt.Bounds(0, 0, 100, 100), CoverArt.contentBounds(plain, 100, 100))
    }

    private fun syncsafe(n: Int) = byteArrayOf(
        ((n shr 21) and 0x7F).toByte(), ((n shr 14) and 0x7F).toByte(),
        ((n shr 7) and 0x7F).toByte(), (n and 0x7F).toByte(),
    )

    private fun frame(id: String, body: ByteArray, version: Int) = ByteArrayOutputStream().apply {
        write(id.toByteArray())
        write(if (version == 4) syncsafe(body.size) else byteArrayOf(0, 0, (body.size shr 8).toByte(), body.size.toByte()))
        write(0)
        write(0)
        write(body)
    }.toByteArray()

    private fun mp3(version: Int, vararg frames: ByteArray): ByteArray {
        val all = frames.fold(ByteArray(0)) { acc, f -> acc + f } + ByteArray(32)
        return byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0) +
            syncsafe(all.size) + all + AUDIO
    }

    // reads the frames back out of a tag: id to body
    private fun readFrames(file: ByteArray): List<Pair<String, ByteArray>> {
        val version = file[3].toInt()
        fun ss(at: Int) = ((file[at].toInt() and 0x7F) shl 21) or ((file[at + 1].toInt() and 0x7F) shl 14) or
            ((file[at + 2].toInt() and 0x7F) shl 7) or (file[at + 3].toInt() and 0x7F)
        fun be(at: Int) = ((file[at].toInt() and 0xFF) shl 24) or ((file[at + 1].toInt() and 0xFF) shl 16) or
            ((file[at + 2].toInt() and 0xFF) shl 8) or (file[at + 3].toInt() and 0xFF)
        val end = 10 + ss(6)
        val out = mutableListOf<Pair<String, ByteArray>>()
        var pos = 10
        while (pos + 10 <= end && file[pos].toInt() != 0) {
            val size = if (version == 4) ss(pos + 4) else be(pos + 4)
            out += String(file, pos, 4) to file.copyOfRange(pos + 10, pos + 10 + size)
            pos += 10 + size
        }
        return out
    }

    @Test
    fun replacesOldCoverAndKeepsOtherTags() {
        for (version in 3..4) {
            val title = frame("TIT2", byteArrayOf(0) + "Song".toByteArray(), version)
            val oldCover = frame("APIC", byteArrayOf(0, 1, 2, 3), version)
            val result = Id3Cover.embed(mp3(version, title, oldCover), JPEG)
            assertNotNull(result)
            val frames = readFrames(result!!)
            assertEquals(listOf("TIT2", "APIC"), frames.map { it.first })
            val apic = frames[1].second
            assertArrayEquals(JPEG, apic.copyOfRange(apic.size - JPEG.size, apic.size))
            assertArrayEquals(AUDIO, result.copyOfRange(result.size - AUDIO.size, result.size))
        }
    }

    @Test
    fun addsTagWhenThereIsNone() {
        val result = Id3Cover.embed(AUDIO, JPEG)!!
        assertEquals(listOf("APIC"), readFrames(result).map { it.first })
        assertArrayEquals(AUDIO, result.copyOfRange(result.size - AUDIO.size, result.size))
    }

    @Test
    fun leavesUnusualTagsAlone() {
        val unsynced = mp3(3).also { it[5] = 0x80.toByte() }
        assertNull(Id3Cover.embed(unsynced, JPEG))
    }

    @Test
    fun readsBackTheCoverItWrote() {
        for (version in 3..4) {
            val title = frame("TIT2", byteArrayOf(0) + "Song".toByteArray(), version)
            val tagged = Id3Cover.embed(mp3(version, title), JPEG)!!
            assertArrayEquals(JPEG, Id3Cover.extract(tagged))
        }
    }

    @Test
    fun noCoverMeansNull() {
        assertNull(Id3Cover.extract(AUDIO))
        assertNull(Id3Cover.extract(mp3(3, frame("TIT2", byteArrayOf(0) + "Song".toByteArray(), 3))))
    }

    // files like the ones older versions of the app made: ffmpeg, a png cover, ID3 v2.3 or v2.4.
    // needs ffmpeg on the computer running the tests, skipped otherwise
    @Test
    fun readsCoversMadeByFfmpeg() {
        val ffmpeg = listOf("/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg").map { java.io.File(it) }
            .firstOrNull { it.canExecute() }
        Assume.assumeTrue("ffmpeg not installed", ffmpeg != null)
        val dir = createTempDirectory("covers").toFile()
        try {
            val png = java.io.File(dir, "cover.png")
            run(ffmpeg!!.path, "-v", "error", "-f", "lavfi", "-i", "testsrc=size=320x320", "-frames:v", "1", png.path)
            for (version in listOf("3", "4")) {
                val out = java.io.File(dir, "song$version.mp3")
                run(
                    ffmpeg.path, "-v", "error", "-f", "lavfi", "-i", "sine=duration=1", "-i", png.path,
                    "-map", "0", "-map", "1", "-c:a", "libmp3lame", "-c:v", "copy", "-id3v2_version", version,
                    "-metadata", "title=Song", "-metadata:s:v", "title=Album cover", "-metadata:s:v", "comment=Cover (front)",
                    out.path,
                )
                val cover = Id3Cover.extract(out.readBytes())
                assertNotNull("v2.$version cover", cover)
                assertArrayEquals("v2.$version cover", png.readBytes(), cover)
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun run(vararg command: String) {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(output, 0, process.waitFor())
    }

    @Test
    fun findsYoutubeIds() {
        assertEquals("dQw4w9WgXcQ", youtubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=x"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://music.youtube.com/watch?feature=share&v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertNull(youtubeId("https://soundcloud.com/artist/track"))
    }

    private companion object {
        val AUDIO = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64, 1, 2, 3, 4)
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 9, 9, 9, 0xFF.toByte(), 0xD9.toByte())
    }
}
