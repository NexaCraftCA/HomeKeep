package ca.nexacraft.homekeep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Terracotta = Color(0xFFB4552D)
private val TerracottaDark = Color(0xFF8C3D1D)
private val Cream = Color(0xFFFDF7F2)
private val Ink = Color(0xFF221A16)
private val Clay = Color(0xFF7A5C4A)
private val Sage = Color(0xFF5C6B4A)

private val LightScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCE),
    onPrimaryContainer = Color(0xFF3B0C00),
    secondary = Color(0xFF6D5C4B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E2CC),
    onSecondaryContainer = Color(0xFF261909),
    tertiary = Sage,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCE9C4),
    onTertiaryContainer = Color(0xFF1A2408),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFBF8),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3E5DC),
    onSurfaceVariant = Clay,
    outline = Color(0xFF85736A),
    outlineVariant = Color(0xFFD8C3B8),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB59B),
    onPrimary = Color(0xFF5B1A00),
    primaryContainer = Color(0xFF7E2E10),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFDDC4A8),
    onSecondary = Color(0xFF3C2E1F),
    secondaryContainer = Color(0xFF544434),
    onSecondaryContainer = Color(0xFFF6E2CC),
    tertiary = Color(0xFFC0D2A4),
    onTertiary = Color(0xFF2C3A12),
    tertiaryContainer = Color(0xFF425127),
    onTertiaryContainer = Color(0xFFDCE9C4),
    background = Color(0xFF1A1210),
    onBackground = Color(0xFFF0E0D8),
    surface = Color(0xFF1F1714),
    onSurface = Color(0xFFF0E0D8),
    surfaceVariant = Color(0xFF53433A),
    onSurfaceVariant = Color(0xFFD8C3B8),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF53433A),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun HomeKeepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content,
    )
}

val BrandPrimaryDark = TerracottaDark
