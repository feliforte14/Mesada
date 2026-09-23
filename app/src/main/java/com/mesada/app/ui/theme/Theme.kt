package com.mesada.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Basil = Color(0xFF2F6B4F)
    val Saffron = Color(0xFFD99A0B)
    val Plum = Color(0xFF8A4F7D)
    val Tomato = Color(0xFFC8412C)
    val Ink = Color(0xFF16211B)
}

private val Light = lightColorScheme(
    primary = Palette.Basil, onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E7DC), onPrimaryContainer = Palette.Basil,
    secondary = Palette.Saffron, onSecondary = Palette.Ink,
    tertiary = Palette.Plum, error = Palette.Tomato,
    background = Color(0xFFEAF0E8), onBackground = Palette.Ink,
    surface = Color.White, onSurface = Palette.Ink,
    surfaceVariant = Color(0xFFF4F7F2), onSurfaceVariant = Color(0xFF56655B),
    outline = Color(0xFFD3DCD1),
    inverseSurface = Palette.Ink, inverseOnSurface = Color(0xFFEAF0E8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF62B38C), onPrimary = Color(0xFF0F1713),
    primaryContainer = Color(0xFF23392D), onPrimaryContainer = Color(0xFF62B38C),
    secondary = Color(0xFFF0B93E), onSecondary = Palette.Ink,
    tertiary = Color(0xFFC58BB7), error = Color(0xFFEE6B55),
    background = Color(0xFF0F1713), onBackground = Color(0xFFE6EEE8),
    surface = Color(0xFF18231D), onSurface = Color(0xFFE6EEE8),
    surfaceVariant = Color(0xFF1F2C25), onSurfaceVariant = Color(0xFF9BAAA0),
    outline = Color(0xFF2B3A31),
    inverseSurface = Color(0xFF060A08), inverseOnSurface = Color(0xFFE6EEE8),
)

// Tamaños grandes: se usa a un brazo de distancia y con las manos ocupadas.
private val KitchenType = Typography(
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 56.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
)

@Composable
fun MesadaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = KitchenType,
        content = content,
    )
}
