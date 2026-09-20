package com.leolennards.ytdownloader.data

// finds every link in some text so you can paste a few at once
fun extractLinks(text: String): List<String> =
    text.split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.startsWith("http://") || it.startsWith("https://") }
        .distinct()

// true if the link is a whole playlist or album and not one video.
// a video link with a normal list id (watch?v=...&list=...) counts as one video,
// but an album id (list=OLAK5uy...) is always the whole album
fun isPlaylistLink(url: String): Boolean {
    val uri = try {
        java.net.URI(url)
    } catch (e: Exception) {
        return false
    }
    val path = uri.path.orEmpty()
    val params = uri.rawQuery.orEmpty().split("&")
    // album playlists on youtube music start with OLAK5uy. the play all link on an album
    // has both v= and list=OLAK5uy so count it as the whole album
    if (params.any { it.startsWith("list=OLAK5uy") }) return true
    val hasList = params.any { it.startsWith("list=") }
    val hasVideo = params.any { it.startsWith("v=") }
    if (uri.host == "youtu.be") return path.startsWith("/playlist")
    return (hasList && !hasVideo) || path.startsWith("/playlist") || path.contains("/sets/") || path.contains("/album/")
}
