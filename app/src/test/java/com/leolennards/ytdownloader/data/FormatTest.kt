package com.leolennards.ytdownloader.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    // ---- titles ----

    @Test
    fun cleanMusicTitle_removesArtistAndAudioTag() {
        assertEquals("Not You Too ft. Chris Brown", cleanMusicTitle("Drake - Not You Too (Audio) ft. Chris Brown"))
        assertEquals("Losses", cleanMusicTitle("Drake - Losses (Audio)"))
    }

    @Test
    fun cleanMusicTitle_removesOfficialVideoTags() {
        assertEquals("Toosie Slide", cleanMusicTitle("Drake - Toosie Slide (Official Music Video)"))
        assertEquals("Song Name", cleanMusicTitle("Song Name [Lyrics]"))
    }

    @Test
    fun cleanMusicTitle_keepsFeaturedArtists() {
        assertEquals("Song (feat. Future)", cleanMusicTitle("Drake - Song (feat. Future)"))
    }

    @Test
    fun cleanMusicTitle_leavesPlainTitlesAlone() {
        assertEquals("Just A Title", cleanMusicTitle("Just A Title"))
        assertEquals("On Bended Knee", cleanMusicTitle("Boyz II Men - On Bended Knee"))
    }

    @Test
    fun cleanMusicTitle_neverReturnsAnEmptyTitle() {
        assertEquals("(Audio)", cleanMusicTitle("(Audio)"))
    }

    // ---- numbers ----

    @Test
    fun formatDuration_showsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59))
        assertEquals("5:52", formatDuration(352))
    }

    @Test
    fun formatDuration_showsHoursWhenNeeded() {
        assertEquals("1:02:05", formatDuration(3725))
    }

    @Test
    fun formatSize_usesMbOrKb() {
        assertEquals("6.0 MB", formatSize(6_291_456))
        assertEquals("500 KB", formatSize(512_000))
        assertEquals("1 KB", formatSize(100))
    }

    @Test
    fun relativeDay_sameDayIsToday() {
        val now = 1_700_000_000_000L
        assertEquals("Today", relativeDay(now, now))
    }

    @Test
    fun relativeDay_fewDaysAgoIsThisWeek() {
        val now = 1_700_000_000_000L
        assertEquals("This week", relativeDay(now - 3 * 86_400_000L, now))
    }
}
