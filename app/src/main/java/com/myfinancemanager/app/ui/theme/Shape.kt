package com.myfinancemanager.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AxioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Every interactive control - button, chip, tab, search bar - is a full pill. */
val PillShape = RoundedCornerShape(percent = 50)

val ReceiptCardShape = RoundedCornerShape(24.dp)
