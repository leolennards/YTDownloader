package com.leolennards.ytdownloader.data

import org.junit.Assume
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit

// runs the real song search against youtube with a real yt-dlp, so it only runs when LIVE_TESTS=1
// and YTDLP (the path to a yt-dlp file, run with python3) are set. it writes what it picked for
// every song to build/shazam-live.txt, to be checked by eye
class ShazamLiveTest {
    // title and artist as shazam names them
    private val songs = listOf(
        "Blinding Lights" to "The Weeknd",
        "Give Thanks (feat. Allanzo)" to "B3XX",
        "Sweeterman" to "Drake",
        "Stand By Me" to "Ben E. King",
        "Lay It Down" to "Lloyd",
        "Water" to "Tyla",
        "Calm Down" to "Rema & Selena Gomez",
        "Ojuelegba" to "Wizkid",
        "Jerusalema (feat. Nomcebo Zikode)" to "Master KG",
        "Despacito (feat. Daddy Yankee)" to "Luis Fonsi",
        "Bohemian Rhapsody" to "Queen",
        "HUMBLE." to "Kendrick Lamar",
        "Not Like Us" to "Kendrick Lamar",
        "Die With A Smile" to "Lady Gaga & Bruno Mars",
        "Espresso" to "Sabrina Carpenter",
        "DtMF" to "Bad Bunny",
        "Señorita" to "Shawn Mendes & Camila Cabello",
        "Smells Like Teen Spirit" to "Nirvana",
        "Hey Ya!" to "Outkast",
        "Love Story (Taylor's Version)" to "Taylor Swift",
        "Mr. Brightside" to "The Killers",
        "Umbrella (feat. JAY-Z)" to "Rihanna",
        "Rich Flex" to "Drake & 21 Savage",
        "Mnike (feat. DJ Maphorisa, Nandipha808, Ceeka RSA & Tyron Dee)" to "Tyler ICU & Tumela_za",
        "Lose Yourself" to "Eminem",
        "Peaches (feat. Daniel Caesar & Giveon)" to "Justin Bieber",
        "Unwritten" to "Natasha Bedingfield",
        "Gangnam Style (강남스타일)" to "PSY",
        "Circadian Rhythm" to "Drake",
        "Sicko Mode" to "Travis Scott",
    )

    private val engine = Shazam.Engine { url, options ->
        val process = ProcessBuilder(listOf("python3", "-I", System.getenv("YTDLP"), "--no-update") + options + url)
            .redirectError(File("/dev/null"))
            .start()
        val out = process.inputStream.bufferedReader().readText()
        process.waitFor(120, TimeUnit.SECONDS)
        out
    }

    @Test
    fun findsRealSongs() {
        Assume.assumeTrue("set LIVE_TESTS=1 and YTDLP to run", System.getenv("LIVE_TESTS") == "1" && System.getenv("YTDLP") != null)
        val report = StringBuilder()
        var found = 0
        for ((title, artist) in songs) {
            val song = Shazam.withLength(Shazam.Song(title, artist, null))
            val result = Shazam.find(song, engine)
            if (result.hit != null) found++
            report.append("## $title — $artist (${song.durationSeconds ?: "?"}s)\n")
            report.append("PICKED: ${result.hit?.let { "${it.title} · ${it.channel} · ${it.durationSeconds}s · ${it.url}" } ?: "NOTHING"}\n")
            report.append(result.report).append("\n\n")
        }
        report.insert(0, "found $found of ${songs.size}\n\n")
        File("build/shazam-live.txt").writeText(report.toString())
    }
}
