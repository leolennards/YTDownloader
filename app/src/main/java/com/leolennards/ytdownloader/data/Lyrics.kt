package com.leolennards.ytdownloader.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

// one line of synced lyrics: when it starts and what is sung
data class LyricLine(val timeMs: Long, val text: String)

// lyrics for a song. synced has a time for every line (karaoke style), plain is just the words
data class Lyrics(val plain: String, val synced: List<LyricLine>?) {
    val hasSynced: Boolean get() = !synced.isNullOrEmpty()
}

// finds lyrics on lrclib.net, a free and open lyrics database (no account needed).
// like the album art, matching is strict: the title and main artist must match, and the length
// of the song must be within a few seconds, otherwise lines would light up at the wrong time
object LyricsFinder {
    private const val TAG = "Lyrics"
    private const val TIMEOUT_MS = 8000
    private const val MAX_LENGTH_DIFF = 4.0

    data class Candidate(
        val title: String,
        val artist: String,
        val durationSec: Double,
        val plain: String?,
        val synced: String?,
        val instrumental: Boolean,
    )

    fun find(title: String, artists: List<String>, durationSec: Double?): Lyrics? {
        val names = artists.filter { it.isNotBlank() }
        if (title.isBlank() || names.isEmpty()) return null
        val cleanTitle = AlbumArt.normalizeTitle(title)
        val searches = listOf(
            "track_name=" + enc(cleanTitle) + "&artist_name=" + enc(AlbumArt.normalizeArtist(names.first())),
            "q=" + enc("${AlbumArt.normalizeArtist(names.first())} $cleanTitle"),
            // channel names like "RickAstleyVEVO" lose the space between names, so last of all search
            // by title only and let the artist check pick the right one
            "track_name=" + enc(cleanTitle),
        )
        for (query in searches) {
            val candidates = runCatching { search(query) }.onFailure { Log.w(TAG, "search failed", it) }.getOrNull() ?: continue
            val best = pickBest(candidates, title, names, durationSec) ?: continue
            return toLyrics(best)
        }
        return null
    }

    // ---- matching and parsing, free of android so they can be unit tested ----

    fun pickBest(candidates: List<Candidate>, title: String, artists: List<String>, durationSec: Double?): Candidate? {
        val wanted = AlbumArt.normalizeTitle(title)
        val names = artists.map { AlbumArt.normalizeArtist(it) }.filter { it.isNotBlank() }
        if (wanted.isBlank() || names.isEmpty()) return null
        return candidates
            .filter { !it.instrumental && (!it.plain.isNullOrBlank() || !it.synced.isNullOrBlank()) }
            .filter { AlbumArt.artistMatches(it.artist, names) }
            .filter { durationSec == null || durationSec <= 0 || kotlin.math.abs(it.durationSec - durationSec) <= MAX_LENGTH_DIFF }
            .map { it to AlbumArt.titleScore(AlbumArt.normalizeTitle(it.title), wanted, rawWanted = title) }
            .filter { it.second >= 0.8 }
            // the best title first, then synced lyrics over plain ones, then the closest length
            .sortedWith(
                compareByDescending<Pair<Candidate, Double>> { it.second }
                    .thenByDescending { !it.first.synced.isNullOrBlank() }
                    .thenBy { if (durationSec == null) 0.0 else kotlin.math.abs(it.first.durationSec - durationSec) },
            )
            .firstOrNull()
            ?.first
    }

    fun toLyrics(c: Candidate): Lyrics? {
        val synced = c.synced?.let { parseLrc(it) }?.takeIf { it.isNotEmpty() }
        val plain = c.plain?.takeIf { it.isNotBlank() } ?: synced?.joinToString("\n") { it.text }
        return plain?.let { Lyrics(it.trim(), synced) }
    }

    // "[01:02.35] words" lines into times and text. a line can have several times
    // ("[00:10.00][01:20.00] chorus"), and lines without a time (credits, tags) are skipped
    fun parseLrc(lrc: String): List<LyricLine> {
        val time = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val lines = mutableListOf<LyricLine>()
        lrc.lines().forEach { raw ->
            val stamps = time.findAll(raw).toList()
            if (stamps.isEmpty()) return@forEach
            val text = raw.substring(stamps.last().range.last + 1).trim()
            stamps.forEach { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                val ms = when (frac.length) {
                    0 -> 0L
                    1 -> frac.toLong() * 100
                    2 -> frac.toLong() * 10
                    else -> frac.take(3).toLong()
                }
                lines += LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }
        // empty lines are pauses in the song, keep at most one in a row
        val sorted = lines.sortedBy { it.timeMs }
        return sorted.filterIndexed { i, line -> line.text.isNotEmpty() || i == 0 || sorted[i - 1].text.isNotEmpty() }
    }

    // the line being sung at this moment, or -1 before the first line
    fun currentLine(lines: List<LyricLine>, positionMs: Long): Int {
        var index = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) index = i else break
        }
        return index
    }

    // ---- the search ----

    private fun search(query: String): List<Candidate> {
        val connection = URL("https://lrclib.net/api/search?$query").openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "AnyGrab (https://github.com/leolennards/YTDownloader)")
        val text = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val r = array.getJSONObject(i)
            Candidate(
                title = r.optString("trackName"),
                artist = r.optString("artistName"),
                durationSec = r.optDouble("duration", 0.0),
                plain = r.optString("plainLyrics").takeIf { !r.isNull("plainLyrics") && it.isNotBlank() },
                synced = r.optString("syncedLyrics").takeIf { !r.isNull("syncedLyrics") && it.isNotBlank() },
                instrumental = r.optBoolean("instrumental", false),
            )
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}

// gets the lyrics for a saved song: from what was kept before, then from inside the file,
// then (when allowed) from lrclib. whatever is found is kept for next time
object LyricsLoader {
    fun load(context: Context, uri: String, title: String, artist: String?, durationMs: Long, online: Boolean): Lyrics? {
        when (val cached = LyricsStore.read(context, uri)) {
            is LyricsStore.Cached.Found -> return cached.lyrics
            LyricsStore.Cached.None -> if (!online) return null
            null -> Unit
        }
        // plain words that are already in the file (added when it was downloaded, or by another app)
        val embedded = runCatching {
            context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
                val head = ByteArray(4 * 1024 * 1024)
                var read = 0
                while (read < head.size) {
                    val n = input.read(head, read, head.size - read)
                    if (n <= 0) break
                    read += n
                }
                Id3Cover.extractLyrics(head.copyOf(read))
            }
        }.getOrNull()
        if (!online) return embedded?.let { Lyrics(it, null) }
        val artists = listOfNotNull(title.substringBefore(" - ", "").takeIf { it.isNotBlank() }, artist)
        // the length makes sure the timing fits, read it from the file when the player does not know it yet
        val length = durationMs.takeIf { it > 0 } ?: runCatching {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, android.net.Uri.parse(uri))
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        }.getOrNull()
        val found = LyricsFinder.find(title, artists, length?.takeIf { it > 0 }?.div(1000.0))
            ?: embedded?.let { Lyrics(it, null) }
        LyricsStore.save(context, uri, found)
        return found
    }
}

// keeps the lyrics of saved songs in the app's own storage, so they work offline and are only
// looked up once. a song with no lyrics is remembered too, and asked about again after a week
object LyricsStore {
    private const val RETRY_AFTER_MS = 7L * 24 * 60 * 60 * 1000

    sealed interface Cached {
        data class Found(val lyrics: Lyrics) : Cached
        data object None : Cached
    }

    private fun file(context: Context, uri: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(uri.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(File(context.filesDir, "lyrics").apply { mkdirs() }, "$hash.json")
    }

    fun read(context: Context, uri: String): Cached? = runCatching {
        val f = file(context, uri)
        if (!f.exists()) return null
        val o = JSONObject(f.readText())
        if (o.optBoolean("none")) {
            return if (System.currentTimeMillis() - o.optLong("at") < RETRY_AFTER_MS) Cached.None else null
        }
        val synced = o.optJSONArray("synced")?.let { a ->
            (0 until a.length()).map { i -> a.getJSONObject(i).let { LyricLine(it.getLong("t"), it.getString("x")) } }
        }
        Cached.Found(Lyrics(o.getString("plain"), synced))
    }.getOrNull()

    fun save(context: Context, uri: String, lyrics: Lyrics?) {
        runCatching {
            val o = JSONObject()
            if (lyrics == null) {
                o.put("none", true).put("at", System.currentTimeMillis())
            } else {
                o.put("plain", lyrics.plain)
                lyrics.synced?.let { lines ->
                    o.put("synced", JSONArray().apply { lines.forEach { put(JSONObject().put("t", it.timeMs).put("x", it.text)) } })
                }
            }
            file(context, uri).writeText(o.toString())
        }
    }

    fun forget(context: Context, uri: String) {
        runCatching { file(context, uri).delete() }
    }
}
