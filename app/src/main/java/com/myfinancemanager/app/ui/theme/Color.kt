package com.myfinancemanager.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val AxioLime = Color(0xFFB4E300)
val AxioLimeDeep = Color(0xFF6EC800)
val AxioLimeDark = Color(0xFF5A7A00)   // lime for light-mode text/icons
val InkBlack = Color(0xFF000000)

// Dark neutrals
val Ink900 = Color(0xFF181818)  // background
val Ink850 = Color(0xFF1A1A1A)  // surface
val Ink800 = Color(0xFF212121)  // surfaceContainer
val Ink700 = Color(0xFF292929)  // surfaceContainerHigh
val Ink600 = Color(0xFF333333)  // surfaceContainerHighest
val InkOutline = Color(0xFF2F2F2F)
val InkOutlineStrong = Color(0xFF3D3D3D)
val CardLight = Color(0xFFEBEBEB)
val OnCardLight = Color(0xFF181818)
val TextPrimaryDark = Color(0xFFF5F5F5)
val TextSecondaryDark = Color(0xFF9E9E9E)
val TextMutedDark = Color(0xFF6B6B6B)

// Light neutrals
val Paper = Color(0xFFFFFFFF)
val PaperSurface = Color(0xFFF4F5F1)
val PaperContainerHigh = Color(0xFFEDEFE9)
val TextPrimaryLight = Color(0xFF181818)
val TextSecondaryLight = Color(0xFF6B6B6B)
val PaperOutline = Color(0xFFE2E4DE)

// Semantic
val MoneyPositiveDark = Color(0xFF7AE04A)
val MoneyNegativeDark = Color(0xFFF06060)
val MoneyInvestDark = Color(0xFF5B7CFA)
val MoneyWarning = Color(0xFFF5A623)
val MoneyInfoDark = Color(0xFF00B0D0)

val MoneyPositiveLight = Color(0xFF1F7A38)
val MoneyNegativeLight = Color(0xFFC4362E)
val MoneyInvestLight = Color(0xFF3A52C8)
val MoneyInfoLight = Color(0xFF00839B)

// Data-viz sequence (index 0 is the default series)
val ChartPalette = listOf(
    Color(0xFF4050E0), // indigo
    Color(0xFFB4E300), // lime
    Color(0xFF00B0D0), // cyan
    Color(0xFF9050D0), // purple
    Color(0xFFF06060), // coral
    Color(0xFF802040), // berry
    Color(0xFF4CAF50), // green
    Color(0xFF9E9E9E)  // gray
)

// Chart tracks and accents
val RingTrackDark = Color(0xFF2A2A2A)
val RingTrackLight = Color(0xFFE8E8E8)
val ChartIndigo = Color(0xFF4050E0)
val ChartIndigoSelected = Color(0xFF5B7CFA)
val EyebrowPurple = Color(0xFF9050D0)
val FeatureMint = Color(0xFFE9F7E4)
val FeatureLilac = Color(0xFFE7DDF5)

// Category badge palette (deterministic by category name hash)
val CategoryPalette = listOf(
    Color(0xFF00B0D0), Color(0xFF9050D0), Color(0xFF7AE04A), Color(0xFF4050E0),
    Color(0xFFF06060), Color(0xFFF5A623), Color(0xFF9E9E9E), Color(0xFFB4E300)
)
