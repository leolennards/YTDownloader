package com.leolennards.ytdownloader.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// shazam only tells you what a song is, it has no audio to download. so a shazam link is read for
// the song's title, artist and length, and that song is then found on youtube music and downloaded
// from there. the shazam page has these in a schema.org MusicRecording block (no account needed)
object Shazam {
    private const val TIMEOUT_MS = 10000

    data class Song(val title: String, val artist: String, val durationSeconds: Int?)

    // one search result: the link, its title, length and channel
    data class Hit(val url: String, val title: String, val durationSeconds: Int?, val channel: String?)

    // shazam.com song pages, and the shz.am short links the app shares
    fun isLink(url: String): Boolean {
        val host = runCatching { java.net.URI(url.trim()).host }.getOrNull()?.lowercase() ?: return false
        return host == "shazam.com" || host.endsWith(".shazam.com") || host == "shz.am"
    }

    // reads the song from a shazam link, throws when the page is not a song
    fun song(url: String): Song {
        val connection = URL(url.trim()).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AnyGrab")
        val html = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        return parse(html) ?: error("That Shazam link is not a song")
    }

    // ---- reading the page, kept free of android so it can be unit tested ----

    fun parse(html: String): Song? {
        val blocks = Regex("""<script[^>]*application/ld\+json[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).map { it.groupValues[1] }
        for (block in blocks) {
            val json = runCatching { JSONObject(block.trim()) }.getOrNull() ?: continue
            if (json.optString("@type") != "MusicRecording") continue
            val title = decode(json.optString("name")).trim()
            val artist = decode(artistName(json.opt("byArtist"))).trim()
            if (title.isNotBlank() && artist.isNotBlank()) {
                return Song(title, artist, isoSeconds(json.optString("duration")))
            }
        }
        return null
    }

    // byArtist can be a name, a {"name": ...} object or a list of either
    private fun artistName(value: Any?): String = when (value) {
        is String -> value
        is JSONObject -> value.optString("name")
        is JSONArray -> (0 until value.length()).map { artistName(value.opt(it)) }.filter { it.isNotBlank() }.joinToString(", ")
        else -> ""
    }

    // "PT3M57S" -> 237
    fun isoSeconds(text: String): Int? {
        val match = Regex("""^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$""").find(text.trim()) ?: return null
        val (h, m, s) = match.destructured
        if (h.isEmpty() && m.isEmpty() && s.isEmpty()) return null
        return (h.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
    }

    private fun decode(s: String): String =
        s.replace("&amp;", "&").replace("&#x27;", "'").replace("&#39;", "'").replace("&quot;", "\"")

    // ---- picking the right youtube result ----

    // the result that is really this song: the title must match (no remix or live version unless
    // the song is one) and either the artist or the length must match too. a length far off
    // usually means a music video with an intro, so a result with the right length wins
    fun pickBest(hits: List<Hit>, song: Song): Hit? {
        val wanted = AlbumArt.normalizeTitle(song.title)
        val artists = song.artist.split(Regex("""\s*(,|&|\bx\b|\bfeat\.?|\bft\.?)\s*""", RegexOption.IGNORE_CASE))
            .map { AlbumArt.normalizeArtist(it) }.filter { it.isNotBlank() }
        if (wanted.isBlank() || artists.isEmpty()) return null
        return hits.mapNotNull { hit ->
            val rawTitle = hit.title
            val titleScore = AlbumArt.titleScore(AlbumArt.normalizeTitle(cleanMusicTitle(rawTitle)), wanted, song.title)
            if (titleScore < 0.8) return@mapNotNull null
            val titleWords = " ${AlbumArt.normalizeArtist(rawTitle)} "
            val artistOk = (hit.channel != null && AlbumArt.artistMatches(hit.channel, artists)) ||
                artists.any { " $it " in titleWords }
            val gap = if (hit.durationSeconds != null && song.durationSeconds != null) {
                kotlin.math.abs(hit.durationSeconds - song.durationSeconds)
            } else {
                null
            }
            val lengthOk = gap != null && gap <= 5
            if (!artistOk && !lengthOk) return@mapNotNull null
            var score = titleScore + (if (artistOk) 1.0 else 0.0) + (if (lengthOk) 1.0 else 0.0)
            if (gap != null && gap > 20) score -= 2.0
            hit to score
        }.maxByOrNull { it.second }?.first
    }

    // the lines printed by yt-dlp for a search (url, title, length, channel split by tabs)
    fun parseHits(output: String): List<Hit> = output.lines().mapNotNull { line ->
        val parts = line.split("\t")
        val url = parts.getOrNull(0)?.trim().orEmpty()
        if (!url.startsWith("http")) return@mapNotNull null
        fun field(i: Int) = parts.getOrNull(i)?.trim()?.takeIf { it.isNotBlank() && it != "NA" }
        Hit(url, field(1) ?: return@mapNotNull null, field(2)?.toDoubleOrNull()?.toInt(), field(3))
    }.distinctBy { it.url }
}
