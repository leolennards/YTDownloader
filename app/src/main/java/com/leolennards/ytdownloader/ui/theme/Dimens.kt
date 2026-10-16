package com.leolennards.ytdownloader.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

object YtDimens {
    val ScreenPadding = 20.dp
    val CardRadius = 24.dp
    val CardPadding = 18.dp
    val Hairline = 1.dp
    val MinTouch = 48.dp
    val PrimaryButtonHeight = 56.dp
    val SectionGap = 24.dp
    val ItemGap = 12.dp
    // artwork in list rows and the big one on the player
    val RowArt = 56.dp
    val ArtRadius = 14.dp
    // room at the bottom of scrolling screens so the floating nav bar never covers content
    val NavBarClearance = 104.dp
}

// how much room the floating nav bar (and mini player) take, so lists can scroll clear of them
val LocalBottomClearance = compositionLocalOf { YtDimens.NavBarClearance }
