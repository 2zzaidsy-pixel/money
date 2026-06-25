package com.paywise.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val PrimaryGreen = Color(0xFF00C853)
val PrimaryDark = Color(0xFF0D1117)
val SurfaceDark = Color(0xFF161B22)
val CardDark = Color(0xFF1C2333)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val AccentBlue = Color(0xFF58A6FF)
val AccentOrange = Color(0xFFFF9100)
val AccentRed = Color(0xFFFF5252)
val AccentPurple = Color(0xFFBB86FC)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF003300),
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = AccentPurple,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1D21),
    surface = Color.White,
    onSurface = Color(0xFF1A1D21),
    surfaceVariant = Color(0xFFF0F2F5),
    error = AccentRed,
    onError = Color.White,
    outline = Color(0xFFD0D5DD)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003300),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = AccentBlue,
    onSecondary = Color.Black,
    tertiary = AccentPurple,
    background = PrimaryDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    error = AccentRed,
    onError = Color.White,
    outline = Color(0xFF30363D)
)

@Composable
fun PayWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PayWiseTypography,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ),
        content = content
    )
}
