package com.leolennards.ytdownloader.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// shazam only tells you what a song is, it has no audio to download. so a shazam link is read for
// the song's title and artist, and that song is then found on youtube music and downloaded from there
object Shazam {
    private const val TIMEOUT_MS = 10000
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AnyGrab"

    data class Song(val title: String, val artist: String, val durationSeconds: Int?)

    // one search result: the link, its title, length and channel
    data class Hit(val url: String, val title: String, val durationSeconds: Int?, val channel: String?)

    // shazam.com song pages, and the shz.am short links the app shares
    fun isLink(url: String): Boolean {
        val host = runCatching { java.net.URI(url.trim()).host }.getOrNull()?.lowercase() ?: return false
        return host == "shazam.com" || host.endsWith(".shazam.com") || host == "shz.am"
    }

    // songs read from the text shazam shares with its links ("I used Shazam to discover <song> by
    // <artist>. https://..."), by link. the shazam page itself cannot be trusted: without a browser
    // it often shows some other trending song, so the shared text is the best source there is
    private val shared = java.util.concurrent.ConcurrentHashMap<String, Song>()
    // what was shared with each link, shown when the song could not be read so it can be fixed
    private val sharedText = java.util.concurrent.ConcurrentHashMap<String, String>()

    // call with any shared or pasted text before its links are downloaded
    fun remember(text: String) {
        extractLinks(text).filter { isLink(it) }.forEach { sharedText[it] = text.take(300) }
        fromShareText(text)?.let { (link, song) -> shared[link] = song }
    }

    // reads the song from a shazam link: the shared text first, then apple music (song links carry
    // the apple music id), then the page, but only when the page really is about that song
    fun song(url: String): Song {
        val link = url.trim()
        shared[link]?.let { return it }
        val finalUrl = runCatching { resolve(link) }.getOrDefault(link)
        val id = appleId(finalUrl)
        if (id != null) {
            runCatching { itunesLookup(id) }.getOrNull()?.let { return it }
            runCatching { parse(get(finalUrl), id) }.getOrNull()?.let { return it }
        }
        val received = sharedText[link]?.let { " Shazam sent: \"${it.replace(Regex("\\s+"), " ")}\"" }.orEmpty()
        error("Could not tell which song that Shazam link is.$received")
    }

    // ---- shared text and links, kept free of android so they can be unit tested ----

    // the link and the song from shazam's share text, or null when the text is not that
    fun fromShareText(text: String): Pair<String, Song>? {
        val link = extractLinks(text).firstOrNull { isLink(it) } ?: return null
        // shazam sends a subject ("Sweeterman - Drake") and a message, so each line is read on its own.
        // "X by Y" is the clearest, so every line is tried for that first
        val lines = text.lines().map { withoutLinks(it) }.filter { it.isNotBlank() }
        val song = lines.firstNotNullOfOrNull { byPhrase(it) }
            ?: lines.firstNotNullOfOrNull { dashPhrase(it) }
            ?: return null
        return link to song
    }

    private fun withoutLinks(line: String): String =
        line.split(Regex("\\s+")).filterNot { it.startsWith("http://") || it.startsWith("https://") }.joinToString(" ").trim()

    // "I used Shazam to discover X by Y.", "Found X by Y with Shazam, have a listen:", "X by Y"
    private fun byPhrase(line: String): Song? {
        var phrase = line.replace(Regex("""^.*?\b(?:discover(?:ed)?|found|check out|listen to|listening to)\s+""", RegexOption.IGNORE_CASE), "")
        phrase = phrase.replace(Regex("""\s+(?:with|on|via|using)\s+shazam\b.*$""", RegexOption.IGNORE_CASE), "")
        phrase = phrase.replace(Regex("""[\s.,:;!-]+$"""), "").trim()
        // the last lower case " by " splits them, so "Stand By Me by Ben E. King" works
        val split = phrase.lastIndexOf(" by ")
        if (split <= 0) return null
        return song(phrase.substring(0, split), phrase.substring(split + 4))
    }

    // the subject on its own: "Sweeterman - Drake" (song first, then artist)
    private fun dashPhrase(line: String): Song? {
        val phrase = line.replace(Regex("""[\s.,:;!]+$"""), "").trim()
        val parts = phrase.split(" - ")
        if (parts.size != 2) return null
        return song(parts[0], parts[1])
    }

    private fun song(rawTitle: String, rawArtist: String): Song? {
        val title = rawTitle.trim().removeSurrounding("\"")
        val artist = rawArtist.trim().removeSurrounding("\"")
        if (title.isBlank() || artist.isBlank() || title.length > 120 || artist.length > 120) return null
        return Song(title, artist, null)
    }

    // shazam.com/song/1488408568/blinding-lights -> 1488408568 (an apple music song id)
    fun appleId(url: String): String? =
        Regex("""shazam\.com/(?:[a-z]{2}(?:-[a-z]{2})?/)?song/(\d+)""", RegexOption.IGNORE_CASE).find(url)?.groupValues?.get(1)

    // follows short links like shz.am to the real page
    private fun resolve(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.inputStream.close()
        return connection.url.toString()
    }

    private fun itunesLookup(id: String): Song? {
        // the song can be missing from one country's store, so try a few
        for (country in listOf("us", "za", "gb")) {
            val json = JSONObject(get("https://itunes.apple.com/lookup?country=$country&id=$id"))
            val result = json.optJSONArray("results")?.optJSONObject(0) ?: continue
            val title = result.optString("trackName")
            val artist = result.optString("artistName")
            if (title.isBlank() || artist.isBlank()) continue
            val millis = result.optLong("trackTimeMillis", 0)
            return Song(title, artist, if (millis > 0) (millis / 1000).toInt() else null)
        }
        return null
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        return connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    // ---- reading the page, kept free of android so it can be unit tested ----

    // only a block about the song with this apple music id counts, when an id is given
    fun parse(html: String, id: String? = null): Song? {
        val blocks = Regex("""<script[^>]*application/ld\+json[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(html).map { it.groupValues[1] }
        for (block in blocks) {
            val json = runCatching { JSONObject(block.trim()) }.getOrNull() ?: continue
            if (json.optString("@type") != "MusicRecording") continue
            if (id != null && appleId(json.optString("@id").ifBlank { json.optString("url") }) != id) continue
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
    // usually means a music video with an intro, so a result with the right length wins.
    // with maxGap, a result whose length is further off than that is not taken at all
    fun pickBest(hits: List<Hit>, song: Song, maxGap: Int? = null): Hit? {
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
            if (maxGap != null && gap != null && gap > maxGap) return@mapNotNull null
            val lengthOk = gap != null && gap <= 5
            if (!artistOk && !lengthOk) return@mapNotNull null
            var score = titleScore + (if (artistOk) 1.0 else 0.0) + (if (lengthOk) 1.0 else 0.0)
            if (gap != null && gap > 20) score -= 2.0
            hit to score
        }.maxByOrNull { it.second }?.first
    }

    // a short list of what a search found, for "Show details" when nothing matched
    fun describe(hits: List<Hit>): String = hits.take(5).joinToString("\n") { hit ->
        "- ${hit.title}" + (hit.channel?.let { " · $it" } ?: "") + (hit.durationSeconds?.let { " · ${it}s" } ?: "")
    }

    // ---- searching youtube, the same steps in the app and in the live test ----

    // runs yt-dlp on a link with these options and gives back what it printed
    fun interface Engine {
        fun run(url: String, options: List<String>): String
    }

    // url, title, length and artist (or channel) of each result, split by tabs
    const val PRINT = "%(webpage_url,url)s\t%(title)s\t%(duration)s\t%(artists.0,artist,channel,uploader)s"

    data class Found(val hit: Hit?, val report: String)

    // finds the song on youtube:
    // 1. youtube music's songs tab has the studio versions, but its results carry only titles, so the
    //    first three whose title matches are opened to read their real artist and length
    // 2. a normal youtube search, whose results have the channel and length already
    // a result within 10 seconds of the store's length is wanted first, so "Calm Down" with Selena
    // Gomez (239s) does not become Rema's solo version (220s). only when there is none anywhere is
    // a result further off taken, like a music video with an intro
    fun find(song: Song, engine: Engine): Found {
        val report = mutableListOf<String>()
        val query = "${song.artist} ${song.title}"
        val list = listOf("--flat-playlist", "--playlist-end", "8", "--print", PRINT)
        fun done(hit: Hit?) = Found(hit, report.joinToString("\n"))

        val music = runCatching {
            parseHits(engine.run("https://music.youtube.com/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "#songs", list))
        }.onFailure { report += "YouTube Music search failed: ${lastLine(it)}" }.getOrDefault(emptyList())
        if (music.isNotEmpty()) report += "YouTube Music found:\n${describe(music)}"
        val opened = mutableListOf<Hit>()
        for (candidate in music.filter { titleMatches(it.title, song) }.take(3)) {
            val details = runCatching {
                parseHits(engine.run(candidate.url, listOf("--no-playlist", "--skip-download", "--print", PRINT))).firstOrNull()
            }.onFailure { report += "Could not open ${candidate.url}: ${lastLine(it)}" }.getOrNull() ?: continue
            report += "Opened: ${describe(listOf(details)).removePrefix("- ")}"
            opened += details.copy(url = candidate.url)
            // a sure match, no need to open more
            if (pickBest(listOf(opened.last()), song, maxGap = 5) != null && song.durationSeconds != null) break
        }
        pickBest(opened, song, maxGap = CLOSE)?.let { return done(it) }

        val videos = runCatching { parseHits(engine.run("ytsearch10:$query", list)) }
            .onFailure { report += "YouTube search failed: ${lastLine(it)}" }.getOrDefault(emptyList())
        report += "YouTube found:\n${describe(videos)}"
        return done(pickBest(videos, song, maxGap = CLOSE) ?: pickBest(opened, song) ?: pickBest(videos, song))
    }

    private const val CLOSE = 10

    fun titleMatches(title: String, song: Song): Boolean =
        AlbumArt.titleScore(AlbumArt.normalizeTitle(cleanMusicTitle(title)), AlbumArt.normalizeTitle(song.title), song.title) >= 0.8

    private fun lastLine(e: Throwable): String = e.message?.lines()?.lastOrNull { it.isNotBlank() }.orEmpty()

    // the song's length from apple's itunes search, so the right youtube upload can be told
    // apart by its length. the shared text has no length in it
    fun withLength(song: Song): Song {
        if (song.durationSeconds != null) return song
        val term = java.net.URLEncoder.encode("${song.artist} ${song.title}", "UTF-8")
        val results = runCatching { JSONObject(get("https://itunes.apple.com/search?entity=song&limit=15&term=$term")).optJSONArray("results") }
            .getOrNull() ?: return song
        val tracks = (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            Track(r.optString("trackName"), r.optString("artistName"), (r.optLong("trackTimeMillis", 0) / 1000).toInt())
        }
        return song.copy(durationSeconds = lengthOf(tracks, song) ?: return song)
    }

    data class Track(val title: String, val artist: String, val seconds: Int)

    // the length of the store track that is this song, or null when none clearly is
    fun lengthOf(tracks: List<Track>, song: Song): Int? {
        val wanted = AlbumArt.normalizeTitle(song.title)
        val artists = song.artist.split(Regex("""\s*(,|&)\s*""")).map { AlbumArt.normalizeArtist(it) }.filter { it.isNotBlank() }
        if (wanted.isBlank() || artists.isEmpty()) return null
        return tracks.firstOrNull {
            it.seconds > 0 && AlbumArt.artistMatches(it.artist, artists) &&
                AlbumArt.titleScore(AlbumArt.normalizeTitle(it.title), wanted, song.title) >= 0.8
        }?.seconds
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
