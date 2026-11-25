package `in`.versionx.calendarwidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = CoralDark,
    onPrimaryContainer = Color.White,
    secondary = Amber,
    onSecondary = Ink,
    tertiary = Berry,
    background = Color(0xFF141317),
    surface = Color(0xFF1E1C22),
    surfaceVariant = Color(0xFF2A2730),
)

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Berry,
    onSecondary = Color.White,
    tertiary = Sky,
    background = Paper,
    surface = Color.White,
    surfaceVariant = Color(0xFFFCEDE6),
)

/**
 * App theme. Uses the app's own colorful palette rather than Android 12+ dynamic
 * (wallpaper-derived) colors, so the app looks the same and stays colorful on every
 * device.
 */
@Composable
fun SimpleCalendarWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
