package com.leolennards.ytdownloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// buttons, toggles and chips, fully round
val PillShape = RoundedCornerShape(percent = 50)

val ArtShape = RoundedCornerShape(YtDimens.ArtRadius)

val YtShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),   // icon tiles, fields
    medium = RoundedCornerShape(24.dp),  // cards
    large = RoundedCornerShape(28.dp),   // hero artwork
    extraLarge = RoundedCornerShape(32.dp),
)
