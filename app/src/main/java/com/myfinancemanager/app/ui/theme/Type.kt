package com.myfinancemanager.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.myfinancemanager.app.R

/**
 * DM Sans is shipped as a single variable font so every weight instances from one file and the
 * rupee sign (U+20B9) is covered without falling back to the system face.
 */
@OptIn(ExperimentalTextApi::class)
private fun dmSans(weight: FontWeight): Font = Font(
    resId = R.font.dm_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val DmSans: FontFamily = FontFamily(
    dmSans(FontWeight.Normal),
    dmSans(FontWeight.Medium),
    dmSans(FontWeight.SemiBold),
    dmSans(FontWeight.Bold)
)

val AxioTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 35.sp, letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 17.sp, letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmSans, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.4.sp
    )
)

/** The donut-centre amount and portfolio totals. Material's scale tops out too small for this. */
val HeroNumber = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
    fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1).sp,
    fontFeatureSettings = "tnum"
)

/** Right-aligned list amounts. Tabular so digits do not jitter when a value updates. */
val TabularAmount = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp, lineHeight = 20.sp, fontFeatureSettings = "tnum"
)

/** 24sp hero input used by the amount field. */
val AmountInput = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp, lineHeight = 28.sp, fontFeatureSettings = "tnum"
)

/** 18sp stat-tile value. */
val StatValue = TextStyle(
    fontFamily = DmSans, fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum"
)
