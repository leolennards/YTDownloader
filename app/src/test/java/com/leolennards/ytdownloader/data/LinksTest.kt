package com.leolennards.ytdownloader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinksTest {

    @Test
    fun extractLinks_findsEveryLinkInOrder() {
        val text = "check this https://a.com/x and http://b.org\nhttps://c.net/z"
        assertEquals(listOf("https://a.com/x", "http://b.org", "https://c.net/z"), extractLinks(text))
    }

    @Test
    fun extractLinks_removesDuplicates() {
        val text = "https://a.com/x https://a.com/x"
        assertEquals(listOf("https://a.com/x"), extractLinks(text))
    }

    @Test
    fun extractLinks_ignoresTextWithoutLinks() {
        assertTrue(extractLinks("no links here").isEmpty())
        assertTrue(extractLinks("").isEmpty())
    }

    @Test
    fun playlistLink_isDetected() {
        assertTrue(isPlaylistLink("https://www.youtube.com/playlist?list=PLabc123"))
    }

    @Test
    fun albumLink_isDetected() {
        assertTrue(isPlaylistLink("https://music.youtube.com/playlist?list=OLAK5uy_abc123"))
    }

    @Test
    fun albumPlayAllLink_countsAsAlbumEvenWithVideoId() {
        assertTrue(isPlaylistLink("https://www.youtube.com/watch?v=abc123&list=OLAK5uy_abc123"))
    }

    @Test
    fun singleVideo_isNotAPlaylist() {
        assertFalse(isPlaylistLink("https://www.youtube.com/watch?v=abc123"))
        assertFalse(isPlaylistLink("https://youtu.be/abc123"))
    }

    @Test
    fun videoInsideAPlaylist_countsAsOneVideo() {
        assertFalse(isPlaylistLink("https://www.youtube.com/watch?v=abc123&list=PLxyz789"))
    }

    @Test
    fun soundcloudSet_isDetected() {
        assertTrue(isPlaylistLink("https://soundcloud.com/artist/sets/my-set"))
    }

    @Test
    fun nonsense_isNotAPlaylist() {
        assertFalse(isPlaylistLink("not a url"))
    }
}
