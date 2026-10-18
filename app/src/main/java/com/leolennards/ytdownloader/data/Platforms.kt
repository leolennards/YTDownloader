package com.leolennards.ytdownloader.data

// the sites people use most, so the paste screen can say which one a link is from.
// color is the site's own brand color, used for a small dot next to its name
enum class Platform(val label: String, val color: Long, private val hosts: List<String>) {
    YouTubeMusic("YouTube Music", 0xFFFF0033, listOf("music.youtube.com")),
    YouTube("YouTube", 0xFFFF0033, listOf("youtube.com", "youtu.be", "youtube-nocookie.com")),
    TikTok("TikTok", 0xFF25F4EE, listOf("tiktok.com")),
    Instagram("Instagram", 0xFFE1306C, listOf("instagram.com")),
    X("X", 0xFF9AA0A6, listOf("x.com", "twitter.com")),
    SoundCloud("SoundCloud", 0xFFFF5500, listOf("soundcloud.com")),
    Facebook("Facebook", 0xFF1877F2, listOf("facebook.com", "fb.watch")),
    Reddit("Reddit", 0xFFFF4500, listOf("reddit.com", "redd.it")),
    Vimeo("Vimeo", 0xFF1AB7EA, listOf("vimeo.com")),
    Twitch("Twitch", 0xFF9146FF, listOf("twitch.tv")),
    ;

    fun matches(host: String): Boolean = hosts.any { host == it || host.endsWith(".$it") }

    companion object {
        // shown under the title as examples, in this order
        val featured = listOf(YouTube, TikTok, Instagram, X, SoundCloud)

        // the site a link is from, or null for anything else (yt-dlp still knows many more)
        fun of(url: String): Platform? {
            val host = runCatching { java.net.URI(url.trim()).host }.getOrNull()?.lowercase() ?: return null
            return entries.firstOrNull { it.matches(host) }
        }
    }
}
