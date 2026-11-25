package `in`.versionx.calendarwidget.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Warm, colorful brand palette (inspired by the old app's screenshots: amber date
// block over a painted orange/red wallpaper).
val Coral = Color(0xFFFF7A59)
val CoralDark = Color(0xFFE85D3F)
val Amber = Color(0xFFFFC107)
val Berry = Color(0xFF8E2DE2)
val Sky = Color(0xFF2196F3)
val Teal = Color(0xFF00BFA5)

val Ink = Color(0xFF1B1B1F)
val Paper = Color(0xFFFFF8F3)

/** Header gradient used on the app bars. */
val HeaderGradient = Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFF9A44), Color(0xFFFFC371)))

/** Painted-wallpaper stand-in behind the widget preview, like the old screenshots. */
val WallpaperGradient = Brush.linearGradient(
    listOf(Color(0xFF00B4DB), Color(0xFFFFC371), Color(0xFFFF9A44), Color(0xFFE53935)),
)

/** Colors for section headers so each settings group has its own accent. */
val SectionAccents = listOf(Coral, Berry, Sky, Teal)
