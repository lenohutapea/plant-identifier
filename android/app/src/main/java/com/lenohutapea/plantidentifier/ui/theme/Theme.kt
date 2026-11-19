package com.lenohutapea.plantidentifier.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette taken from the field: leaf ink, young paddy, rust lesions, ripening grain.
object Field {
    val LeafInk = Color(0xFF1F4D2B)
    val Paddy = Color(0xFF8DBF4A)
    val Sage = Color(0xFFF1F5EE)
    val Rust = Color(0xFFA63A2B)
    val Grain = Color(0xFFB7791F)
    val Stem = Color(0xFF6E8566)
    val Healthy = Color(0xFF3E8E41)
}

private val LightColors = lightColorScheme(
    primary = Field.LeafInk,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEBD3),
    onPrimaryContainer = Color(0xFF0E2A16),
    secondary = Field.Paddy,
    onSecondary = Color(0xFF13260A),
    tertiary = Field.Grain,
    onTertiary = Color.White,
    background = Field.Sage,
    onBackground = Color(0xFF17201A),
    surface = Color.White,
    onSurface = Color(0xFF17201A),
    surfaceVariant = Color(0xFFE3EBDD),
    onSurfaceVariant = Color(0xFF46564A),
    outline = Color(0xFF9AAE93),
    outlineVariant = Color(0xFFC9D6C3),
    error = Field.Rust,
    errorContainer = Color(0xFFF6DDD7),
    onErrorContainer = Color(0xFF4A120A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D5A2),
    onPrimary = Color(0xFF0E2A16),
    primaryContainer = Field.LeafInk,
    onPrimaryContainer = Color(0xFFDDEBD3),
    secondary = Field.Paddy,
    onSecondary = Color(0xFF13260A),
    tertiary = Color(0xFFE8B04B),
    onTertiary = Color(0xFF2C1B00),
    background = Color(0xFF0F1712),
    onBackground = Color(0xFFE2EADF),
    surface = Color(0xFF16211A),
    onSurface = Color(0xFFE2EADF),
    surfaceVariant = Color(0xFF223026),
    onSurfaceVariant = Color(0xFFB9C8B4),
    outline = Color(0xFF6E8566),
    outlineVariant = Color(0xFF2E3D32),
    error = Color(0xFFF2A28F),
    errorContainer = Color(0xFF5A1C12),
    onErrorContainer = Color(0xFFFFDAD3),
)

private val AppTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 21.sp),
    )
}

/** Herbarium-label convention: common name in a serif, scientific name in serif italic. */
val PlantNameStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp)
val PlantNameSmallStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp)
val LatinNameStyle = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 20.sp)

@Composable
fun PlantIdentifierTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
