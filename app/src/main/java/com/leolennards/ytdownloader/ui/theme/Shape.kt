package com.leolennards.ytdownloader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// buttons, toggles and chips, fully round
val PillShape = RoundedCornerShape(percent = 50)

val YtShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),   // icon tiles
    medium = RoundedCornerShape(20.dp),  // cards
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
