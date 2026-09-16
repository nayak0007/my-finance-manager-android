package com.myfinancemanager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary = Color(0xFF0F6E56)
val GreenDark = Color(0xFF094936)
val IncomeGreen = Color(0xFF1B8A5A)
val ExpenseRed = Color(0xFFC44536)
val InvestBlue = Color(0xFF2B6CB0)
val SurfaceMint = Color(0xFFF3F8F5)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEADF),
    onPrimaryContainer = GreenDark,
    secondary = Color(0xFF3D6B5C),
    background = Color(0xFFF7FAF8),
    surface = Color.White,
    surfaceVariant = SurfaceMint,
    error = ExpenseRed,
    onBackground = Color(0xFF14231C),
    onSurface = Color(0xFF14231C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FCBB0),
    onPrimary = GreenDark,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9AD4C0),
    background = Color(0xFF0E1713),
    surface = Color(0xFF15201B),
    surfaceVariant = Color(0xFF1E2C26),
    error = Color(0xFFE07A70),
    onBackground = Color(0xFFE7F2EC),
    onSurface = Color(0xFFE7F2EC)
)

@Composable
fun MyFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
