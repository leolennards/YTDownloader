package com.leolennards.ytdownloader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ---- light: cream, near black, gold ----
val LightBackground = Color(0xFFF7F3EC)
val LightSurface = Color(0xFFFFFDF9)      // cards
val LightTrack = Color(0xFFECE4D3)        // toggle and progress track
val LightTonal = Color(0xFFEFE6D2)        // icon tiles, selected chips
val LightThumbnail = Color(0xFFE9E0CC)
val LightHairline = Color(0xFFE4DCCB)
val LightInk = Color(0xFF1C1A17)
val LightMuted = Color(0xFF6B645A)
val LightGold = Color(0xFFB8975A)         // buttons and progress
val LightGoldText = Color(0xFF7D6230)     // gold for text and icons (dark enough to read on cream)

// ---- dark: near black, warm white ----
val DarkBackground = Color(0xFF141210)
val DarkSurface = Color(0xFF1D1A16)
val DarkTrack = Color(0xFF26221C)
val DarkTonal = Color(0xFF2B241A)
val DarkThumbnail = Color(0xFF2A251E)
val DarkHairline = Color(0xFF2E2922)
val DarkInk = Color(0xFFF1EBDF)
val DarkMuted = Color(0xFFA69D8D)
val DarkGold = Color(0xFFD4B27A)
val DarkGoldText = Color(0xFFD4B27A)

val ErrorLight = Color(0xFFB3402A)
val ErrorDark = Color(0xFFE59280)

// extra colors that material3 has no slot for, use YtTheme.colors
@Immutable
data class YtExtraColors(
    val gold: Color,
    val goldText: Color,
    val tonal: Color,
    val track: Color,
    val hairline: Color,
    val thumbnail: Color,
)

val LightExtraColors = YtExtraColors(
    gold = LightGold,
    goldText = LightGoldText,
    tonal = LightTonal,
    track = LightTrack,
    hairline = LightHairline,
    thumbnail = LightThumbnail,
)

val DarkExtraColors = YtExtraColors(
    gold = DarkGold,
    goldText = DarkGoldText,
    tonal = DarkTonal,
    track = DarkTrack,
    hairline = DarkHairline,
    thumbnail = DarkThumbnail,
)
