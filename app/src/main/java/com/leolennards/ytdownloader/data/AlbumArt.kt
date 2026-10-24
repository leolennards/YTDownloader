package com.leolennards.ytdownloader.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer

// finds the official album cover for a song, so mp3s get the real artwork in high resolution
// instead of a youtube thumbnail (which is at most 720 px once it is made square, and often blurry).
// it asks apple's itunes search first (covers up to 1200 px) and deezer second (1000 px).
// both are free and need no account. only the song title and artist are sent.
// matching is strict on purpose: a wrong cover is worse than a soft one, so when the title or the
// main artist does not really match, nothing is returned and the youtube thumbnail is used
object AlbumArt {
    private const val TAG = "AlbumArt"
    private const val ITUNES_SIZE = 1200
    private const val TIMEOUT_MS = 7000

    data class Candidate(val title: String, val artist: String, val artUrl: String)

    // the cover as a bitmap, or null when there is no confident match or no connection
    fun find(title: String, artists: List<String>): Bitmap? {
        val cleanTitle = normalizeTitle(title)
        val names = artists.map { normalizeArtist(it) }.filter { it.isNotBlank() }.distinct()
        if (cleanTitle.isBlank() || names.isEmpty()) return null
        val query = "${names.first()} $cleanTitle"
        val sources = listOf(::itunes, ::deezer)
        for (source in sources) {
            val candidates = runCatching { source(query) }.onFailure { Log.w(TAG, "search failed", it) }.getOrNull() ?: continue
            val best = pickBest(candidates, title, artists) ?: continue
            val bitmap = runCatching { download(best.artUrl) }.getOrNull()
            if (bitmap != null) return bitmap
        }
        return null
    }

    // ---- matching, kept free of android so it can be unit tested ----

    // words that mean a different recording than the one being downloaded
    private val versionWords = listOf(
        "remix", "live", "acoustic", "instrumental", "karaoke", "cover", "tribute", "8 bit", "8bit",
        "sped up", "slowed", "reverb", "nightcore", "lullaby", "piano version", "edit", "mix", "demo",
    )

    fun pickBest(candidates: List<Candidate>, title: String, artists: List<String>): Candidate? {
        val wanted = normalizeTitle(title)
        val names = artists.map { normalizeArtist(it) }.filter { it.isNotBlank() }
        if (wanted.isBlank() || names.isEmpty()) return null
        return candidates
            .filter { artistMatches(it.artist, names) }
            .map { it to titleScore(normalizeTitle(it.title), wanted, rawWanted = title) }
            .filter { it.second >= 0.8 }
            .maxByOrNull { it.second }
            ?.first
    }

    // 1 for the same title, 0.85 when one fully contains the other, 0 when it is another version
    fun titleScore(candidate: String, wanted: String, rawWanted: String = wanted): Double {
        val wantedLower = normalizeBasic(rawWanted)
        val candidateWords = " $candidate "
        // a remix or live version only counts when that is what is being downloaded
        if (versionWords.any { " $it " in candidateWords && " $it " !in " $wantedLower " && " $it " !in " $wanted " }) return 0.0
        if (candidate == wanted) return 1.0
        val a = candidate.split(" ").toSet()
        val b = wanted.split(" ").toSet()
        if (a.containsAll(b) || b.containsAll(a)) {
            // "mine" vs "gold mine" are different songs, so one extra word is only allowed
            // when the title is long enough that one word cannot change what song it is
            val extra = (a - b).size + (b - a).size
            return if (extra <= 1 && minOf(a.size, b.size) >= 3) 0.85 else 0.0
        }
        return 0.0
    }

    // the candidate's main artist (before any "&", "feat" or ",") must be one of ours
    fun artistMatches(candidateArtist: String, wantedArtists: List<String>): Boolean {
        val main = normalizeArtist(
            candidateArtist.split(Regex("""\s*(,|&|\bx\b|\bfeat\.?|\bft\.?|\bfeaturing\b|\bwith\b)\s*""", RegexOption.IGNORE_CASE)).first(),
        )
        val full = normalizeArtist(candidateArtist)
        if (main.isBlank()) return false
        return wantedArtists.any { wanted ->
            wanted == main || wanted == full ||
                // "the weeknd" vs "weeknd", or a channel name with an extra word
                (wanted.length >= 4 && main.length >= 4 && (wanted.replace(" ", "") == main.replace(" ", "")))
        }
    }

    // lower case, no accents, only letters, digits and single spaces
    private fun normalizeBasic(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    // drops the bits that differ between youtube and the stores: (feat. x), [official video], - remastered
    fun normalizeTitle(s: String): String {
        var t = s
        t = t.replace(Regex("""\s*[(\[][^)\]]*(feat|ft\.|official|video|audio|lyric|visuali|remaster|explicit|clean|hd|4k|mv)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), " ")
        t = t.replace(Regex("""\s+(feat\.?|ft\.?|featuring)\s+.*$""", RegexOption.IGNORE_CASE), "")
        t = t.replace(Regex("""\s+-\s+.*remaster.*$""", RegexOption.IGNORE_CASE), "")
        return normalizeBasic(t)
    }

    // channel names like "DrakeVEVO" or "Drake - Topic" become just "drake"
    fun normalizeArtist(s: String): String {
        var a = s.trim()
        a = a.replace(Regex("""\s*-\s*topic$""", RegexOption.IGNORE_CASE), "")
        a = a.replace(Regex("""vevo$""", RegexOption.IGNORE_CASE), "")
        a = a.replace(Regex("""\s+(official|music|tv|channel)$""", RegexOption.IGNORE_CASE), "")
        a = normalizeBasic(a)
        return a.removePrefix("the ").trim()
    }

    // ---- the two searches ----

    private fun itunes(query: String): List<Candidate> {
        val json = get("https://itunes.apple.com/search?entity=song&limit=15&term=" + URLEncoder.encode(query, "UTF-8"))
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { i ->
            val r = results.getJSONObject(i)
            val art = r.optString("artworkUrl100").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Candidate(r.optString("trackName"), r.optString("artistName"), art.replace("100x100bb", "${ITUNES_SIZE}x${ITUNES_SIZE}bb"))
        }
    }

    private fun deezer(query: String): List<Candidate> {
        val json = get("https://api.deezer.com/search?limit=15&q=" + URLEncoder.encode(query, "UTF-8"))
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { i ->
            val r = data.getJSONObject(i)
            val art = r.optJSONObject("album")?.optString("cover_xl")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Candidate(r.optString("title"), r.optJSONObject("artist")?.optString("name").orEmpty(), art)
        }
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "AnyGrab")
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun download(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        return connection.inputStream.use { BitmapFactory.decodeStream(it) }
    }
}
