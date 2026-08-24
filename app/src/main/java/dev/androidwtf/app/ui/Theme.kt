package dev.androidwtf.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The website's palette, in a Material 3 scheme.
 *
 * The site is a terminal-adjacent design because it sits next to a CLI. The app
 * is not: no monospace body text, no ANSI-green-on-black, no box drawing. It
 * keeps the identity — Android green on a green-shifted near-black, with the
 * tier colours — and otherwise behaves like an ordinary mobile app.
 */
val Accent = Color(0xFF3DDC84)
val Bg = Color(0xFF080C0A)
val Surface1 = Color(0xFF0F1512)
val Surface2 = Color(0xFF131A16)
val Line = Color(0xFF222C26)
val Ink = Color(0xFFEEF4EF)
val Muted = Color(0xFF8A978F)

val Tier0 = Color(0xFF3DDC84)
val Tier1 = Color(0xFF22D3EE)
val Tier2 = Color(0xFFFBBF24)
val Tier3 = Color(0xFFF87171)
val Danger = Color(0xFFF87171)

fun tierColour(t: Int) = when (t) {
    0 -> Tier0; 1 -> Tier1; 2 -> Tier2; else -> Tier3
}

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF06120B),
    secondary = Tier1,
    background = Bg,
    onBackground = Ink,
    surface = Surface1,
    onSurface = Ink,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger,
)

private val typography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
)

@Composable
fun AndroidWtfTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()   // the design is dark-only
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
