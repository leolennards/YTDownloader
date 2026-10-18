package com.leolennards.ytdownloader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlatformsTest {
    @Test
    fun knowsTheBigSites() {
        assertEquals(Platform.YouTube, Platform.of("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(Platform.YouTube, Platform.of("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(Platform.YouTube, Platform.of("https://m.youtube.com/shorts/abc"))
        assertEquals(Platform.YouTubeMusic, Platform.of("https://music.youtube.com/watch?v=abc"))
        assertEquals(Platform.TikTok, Platform.of("https://vm.tiktok.com/ZMabc/"))
        assertEquals(Platform.Instagram, Platform.of("https://www.instagram.com/reel/abc/"))
        assertEquals(Platform.X, Platform.of("https://twitter.com/user/status/1"))
        assertEquals(Platform.X, Platform.of("https://x.com/user/status/1"))
        assertEquals(Platform.SoundCloud, Platform.of("https://soundcloud.com/artist/track"))
    }

    @Test
    fun otherLinksAreNull() {
        assertNull(Platform.of("https://example.com/video"))
        assertNull(Platform.of("not a link"))
        // a host that only ends with the same letters is not the site
        assertNull(Platform.of("https://notyoutube.com/watch"))
    }
}
