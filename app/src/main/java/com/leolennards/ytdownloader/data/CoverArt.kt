package com.leolennards.ytdownloader.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// makes the square cover art for mp3s.
// youtube thumbnails are 16:9 and some have black (or white) bars baked in, like old 4:3
// videos or vevo uploads. so: take the biggest picture we can get, cut off any flat bars,
// then cut a centered square out of what is left. no padding, no blur, it fills the whole square
object CoverArt {
    // big enough to look sharp on any phone, small enough to keep the mp3 light
    private const val MAX_SIDE = 1200
    private const val JPEG_QUALITY = 95

    // returns jpeg bytes, or null if there was no usable picture
    fun build(localThumbnail: File?, youtubeId: String?, fallbackUrl: String?, official: Bitmap? = null): ByteArray? {
        // the real album cover beats any thumbnail
        if (official != null) encode(official)?.let { return it }
        var best = localThumbnail?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
        // the thumbnail yt-dlp picked is usually the biggest, but try youtube's full size one when its small
        if (youtubeId != null && (best == null || min(best.width, best.height) < 720)) {
            val maxRes = download("https://i.ytimg.com/vi/$youtubeId/maxresdefault.jpg")
            if (maxRes != null && (best == null || maxRes.width * maxRes.height > best.width * best.height)) best = maxRes
        }
        if (best == null && fallbackUrl != null) best = download(fallbackUrl)
        return encode(best ?: return null)
    }

    // square, at most 1200 px, as a high quality jpeg
    fun encode(source: Bitmap): ByteArray? = try {
        val square = squareCover(source)
        ByteArrayOutputStream().use { out ->
            square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }

    fun squareCover(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val content = contentBounds(pixels, w, h)
        val side = min(content.width, content.height)
        val left = content.left + (content.width - side) / 2
        val top = content.top + (content.height - side) / 2
        val cropped = Bitmap.createBitmap(source, left, top, side, side)
        val target = min(side, MAX_SIDE)
        return if (target == side) cropped else Bitmap.createScaledBitmap(cropped, target, target, true)
    }

    private fun download(url: String): Bitmap? = try {
        URL(url).openStream().use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    data class Bounds(val left: Int, val top: Int, val width: Int, val height: Int)

    // finds the part of the picture inside any flat bars at the edges.
    // pixels are argb like Bitmap.getPixels gives them
    fun contentBounds(pixels: IntArray, w: Int, h: Int): Bounds {
        fun rowFlat(y: Int) = isFlat(w) { x -> pixels[y * w + x] }
        fun colFlat(x: Int, top: Int, bottom: Int) = isFlat(bottom - top) { i -> pixels[(top + i) * w + x] }

        var top = 0
        var bottom = h
        while (top < bottom - 1 && rowFlat(top)) top++
        while (bottom - 1 > top && rowFlat(bottom - 1)) bottom--
        // a picture that is almost all one color, keep it whole
        if (bottom - top < h * 0.3) {
            top = 0
            bottom = h
        }
        var left = 0
        var right = w
        while (left < right - 1 && colFlat(left, top, bottom)) left++
        while (right - 1 > left && colFlat(right - 1, top, bottom)) right--
        if (right - left < w * 0.3) {
            left = 0
            right = w
        }
        return Bounds(left, top, right - left, bottom - top)
    }

    // true when a line of pixels is basically one color (a bar), checks every few pixels
    private inline fun isFlat(length: Int, pixel: (Int) -> Int): Boolean {
        if (length <= 0) return true
        val step = max(1, length / 64)
        var count = 0
        var sum = 0.0
        var sumSq = 0.0
        var lo = 255.0
        var hi = 0.0
        var i = 0
        while (i < length) {
            val c = pixel(i)
            val luma = 0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF)
            sum += luma
            sumSq += luma * luma
            lo = min(lo, luma)
            hi = max(hi, luma)
            count++
            i += step
        }
        val mean = sum / count
        val spread = sqrt(max(0.0, sumSq / count - mean * mean))
        // jpeg noise makes a black bar a few shades off, so allow a little
        return spread < 4.0 && hi - lo < 24.0
    }
}

// puts a picture into an mp3's ID3v2 tag as the front cover, replacing any cover already there
object Id3Cover {
    private const val PADDING = 1024

    // returns the new file bytes, or null if the tag is in a shape this does not handle
    fun embed(mp3: ByteArray, jpeg: ByteArray): ByteArray? {
        val hasTag = mp3.size >= 10 && mp3[0] == 'I'.code.toByte() && mp3[1] == 'D'.code.toByte() && mp3[2] == '3'.code.toByte()
        val version: Int
        val keptFrames = ByteArrayOutputStream()
        val audioStart: Int
        if (hasTag) {
            version = mp3[3].toInt()
            val flags = mp3[5].toInt() and 0xFF
            // unsynchronised or extended headers are rare (ffmpeg never writes them), dont risk breaking the file
            if (version !in 3..4 || flags and 0xC0 != 0) return null
            val tagSize = syncsafe(mp3, 6)
            val footer = if (version == 4 && flags and 0x10 != 0) 10 else 0
            val end = 10 + tagSize
            if (end > mp3.size) return null
            audioStart = end + footer
            var pos = 10
            while (pos + 10 <= end) {
                if (mp3[pos].toInt() == 0) break // padding
                val id = String(mp3, pos, 4, Charsets.ISO_8859_1)
                val size = if (version == 4) syncsafe(mp3, pos + 4) else int32(mp3, pos + 4)
                val frameEnd = pos + 10 + size
                if (size < 0 || frameEnd > end) return null
                if (id != "APIC") keptFrames.write(mp3, pos, 10 + size)
                pos = frameEnd
            }
        } else {
            version = 3
            audioStart = 0
        }

        val body = ByteArrayOutputStream().apply {
            write("image/jpeg".toByteArray(Charsets.ISO_8859_1))
            write(0)
            write(3) // front cover
            write(0) // empty description
            write(jpeg)
        }.toByteArray()
        val apic = ByteArrayOutputStream().apply {
            write(0) // latin-1 text encoding
            write(body)
        }.toByteArray()

        val frames = ByteArrayOutputStream().apply {
            write(keptFrames.toByteArray())
            write("APIC".toByteArray(Charsets.ISO_8859_1))
            write(if (version == 4) syncsafeBytes(apic.size) else int32Bytes(apic.size))
            write(0)
            write(0)
            write(apic)
        }.toByteArray()

        return ByteArrayOutputStream(frames.size + PADDING + mp3.size - audioStart + 10).apply {
            write(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), version.toByte(), 0, 0))
            write(syncsafeBytes(frames.size + PADDING))
            write(frames)
            write(ByteArray(PADDING))
            write(mp3, audioStart, mp3.size - audioStart)
        }.toByteArray()
    }

    // reads the cover picture back out of an mp3's ID3v2 tag, for phones whose own reader cannot.
    // only needs the start of the file, the tag is always at the front
    fun extract(mp3: ByteArray): ByteArray? {
        if (mp3.size < 10 || mp3[0] != 'I'.code.toByte() || mp3[1] != 'D'.code.toByte() || mp3[2] != '3'.code.toByte()) return null
        val version = mp3[3].toInt()
        if (version !in 2..4) return null
        val flags = mp3[5].toInt() and 0xFF
        val end = (10 + syncsafe(mp3, 6)).coerceAtMost(mp3.size)
        var pos = 10
        if (flags and 0x40 != 0 && version >= 3) {
            // skip the extended header
            val extSize = if (version == 4) syncsafe(mp3, pos) else int32(mp3, pos) + 4
            pos += extSize
        }
        val idLength = if (version == 2) 3 else 4
        val headerLength = if (version == 2) 6 else 10
        while (pos + headerLength <= end) {
            if (mp3[pos].toInt() == 0) break
            val id = String(mp3, pos, idLength, Charsets.ISO_8859_1)
            val size = when (version) {
                2 -> ((mp3[pos + 3].toInt() and 0xFF) shl 16) or ((mp3[pos + 4].toInt() and 0xFF) shl 8) or (mp3[pos + 5].toInt() and 0xFF)
                4 -> syncsafe(mp3, pos + 4)
                else -> int32(mp3, pos + 4)
            }
            val bodyStart = pos + headerLength
            val bodyEnd = bodyStart + size
            if (size <= 0 || bodyEnd > end) return null
            if (id == "APIC" || id == "PIC") return pictureData(mp3, bodyStart, bodyEnd, version == 2)
            pos = bodyEnd
        }
        return null
    }

    // skips the text encoding, mime type (or 3 letter format), picture type and description
    private fun pictureData(b: ByteArray, start: Int, end: Int, v2: Boolean): ByteArray? {
        val encoding = b[start].toInt()
        var pos = start + 1
        if (v2) {
            pos += 3
        } else {
            while (pos < end && b[pos].toInt() != 0) pos++
            pos++
        }
        pos++ // picture type
        // the description ends with one zero byte, or two for utf-16
        if (encoding == 1 || encoding == 2) {
            while (pos + 1 < end && !(b[pos].toInt() == 0 && b[pos + 1].toInt() == 0)) pos += 2
            pos += 2
        } else {
            while (pos < end && b[pos].toInt() != 0) pos++
            pos++
        }
        return if (pos < end) b.copyOfRange(pos, end) else null
    }

    private fun syncsafe(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0x7F) shl 21) or ((b[at + 1].toInt() and 0x7F) shl 14) or
            ((b[at + 2].toInt() and 0x7F) shl 7) or (b[at + 3].toInt() and 0x7F)

    private fun int32(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)

    private fun syncsafeBytes(n: Int) = byteArrayOf(
        ((n shr 21) and 0x7F).toByte(),
        ((n shr 14) and 0x7F).toByte(),
        ((n shr 7) and 0x7F).toByte(),
        (n and 0x7F).toByte(),
    )

    private fun int32Bytes(n: Int) = byteArrayOf(
        (n shr 24).toByte(),
        (n shr 16).toByte(),
        (n shr 8).toByte(),
        n.toByte(),
    )
}
