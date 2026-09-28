package com.mesada.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Basil = Color(0xFF059669)
    val Saffron = Color(0xFFF97316)
    val Plum = Color(0xFF9333EA)
    val Tomato = Color(0xFFDC2626)
    val Ink = Color(0xFF0F172A)
    // Acentos por comida, para dar identidad visual a cada tarjeta en "Tu día".
    val Breakfast = Color(0xFFF59E0B)
    val Lunch = Color(0xFF059669)
    val Snack = Color(0xFF9333EA)
    val Dinner = Color(0xFF2563EB)
}

private val Light = lightColorScheme(
    primary = Palette.Basil, onPrimary = Color.White,
    primaryContainer = Color(0xFFCDF2E2), onPrimaryContainer = Color(0xFF033D2A),
    secondary = Palette.Saffron, onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4CC), onSecondaryContainer = Color(0xFF4A2600),
    tertiary = Palette.Plum, onTertiary = Color.White,
    error = Palette.Tomato, onError = Color.White,
    background = Color(0xFFF3FAF6), onBackground = Palette.Ink,
    surface = Color.White, onSurface = Palette.Ink,
    surfaceVariant = Color(0xFFE6F4EC), onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFFCFEFD), surfaceContainerHigh = Color(0xFFFFFFFF),
    outline = Color(0xFFBFDBCE), outlineVariant = Color(0xFFDCEFE4),
    inverseSurface = Color(0xFF13201A), inverseOnSurface = Color(0xFFEFF4EC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF34D399), onPrimary = Color(0xFF04140D),
    primaryContainer = Color(0xFF065F46), onPrimaryContainer = Color(0xFFB8ECD1),
    secondary = Color(0xFFFB923C), onSecondary = Color(0xFF2A1500),
    secondaryContainer = Color(0xFF7C2D12), onSecondaryContainer = Color(0xFFFFE4CC),
    tertiary = Color(0xFFD8B4FE), onTertiary = Color(0xFF3B0764),
    error = Color(0xFFF08A76), onError = Color(0xFF2A0A04),
    background = Color(0xFF0E1512), onBackground = Color(0xFFE6EEE8),
    surface = Color(0xFF141D18), onSurface = Color(0xFFE6EEE8),
    surfaceVariant = Color(0xFF1E2A24), onSurfaceVariant = Color(0xFF9FB0A6),
    surfaceContainer = Color(0xFF18231D), surfaceContainerHigh = Color(0xFF202C26),
    outline = Color(0xFF35463C), outlineVariant = Color(0xFF283730),
    inverseSurface = Color(0xFFE6EEE8), inverseOnSurface = Color(0xFF15231C),
)

private val MesadaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

// Tamaños grandes: se usa a un brazo de distancia y con las manos ocupadas.
private val KitchenType = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 60.sp, lineHeight = 64.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 50.sp, lineHeight = 54.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
)

@Composable
fun MesadaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = KitchenType,
        shapes = MesadaShapes,
        content = content,
    )
}
