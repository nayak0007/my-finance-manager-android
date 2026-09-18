package com.myfinancemanager.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic money colours, read through [LocalMoneyColors] so components never branch on theme. */
data class MoneyColors(
    val positive: Color,
    val negative: Color,
    val invest: Color,
    val warning: Color,
    val info: Color
)

val LocalMoneyColors = staticCompositionLocalOf {
    MoneyColors(MoneyPositiveDark, MoneyNegativeDark, MoneyInvestDark, MoneyWarning, MoneyInfoDark)
}

private val AxioDarkColors = darkColorScheme(
    primary = AxioLime,
    onPrimary = Ink900,
    primaryContainer = Color(0xFF2E3A00),
    onPrimaryContainer = AxioLime,
    secondary = MoneyInvestDark,
    onSecondary = Color.White,
    tertiary = MoneyInfoDark,
    background = Ink900,
    onBackground = TextPrimaryDark,
    surface = Ink850,
    onSurface = TextPrimaryDark,
    surfaceVariant = Ink800,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = Ink800,
    surfaceContainerHigh = Ink700,
    surfaceContainerHighest = Ink600,
    surfaceContainerLow = Ink850,
    outline = InkOutline,
    outlineVariant = InkOutlineStrong,
    error = MoneyNegativeDark,
    onError = Ink900
)

private val AxioLightColors = lightColorScheme(
    primary = AxioLimeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF6C8),
    onPrimaryContainer = Color(0xFF33420A),
    secondary = MoneyInvestLight,
    onSecondary = Color.White,
    tertiary = MoneyInfoLight,
    background = Paper,
    onBackground = TextPrimaryLight,
    surface = PaperSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PaperContainerHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = Paper,
    surfaceContainerHigh = PaperContainerHigh,
    surfaceContainerHighest = PaperContainerHigh,
    surfaceContainerLow = PaperSurface,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    error = MoneyNegativeLight,
    onError = Color.White
)

/**
 * axio is dark-first, so dark is the default regardless of the system setting. Light is a
 * derived secondary theme and is never selected automatically.
 */
@Composable
fun MyFinanceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val money = if (darkTheme) {
        MoneyColors(MoneyPositiveDark, MoneyNegativeDark, MoneyInvestDark, MoneyWarning, MoneyInfoDark)
    } else {
        MoneyColors(MoneyPositiveLight, MoneyNegativeLight, MoneyInvestLight, MoneyWarning, MoneyInfoLight)
    }
    CompositionLocalProvider(LocalMoneyColors provides money) {
        MaterialTheme(
            colorScheme = if (darkTheme) AxioDarkColors else AxioLightColors,
            typography = AxioTypography,
            shapes = AxioShapes,
            content = content
        )
    }
}
