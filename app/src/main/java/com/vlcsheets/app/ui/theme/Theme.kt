package com.vlcsheets.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlcsheets.app.data.ThemeMode

// Brand palette: ink on warm paper, with a brass accent ---------------------

val Ink = Color(0xFF1F2A44)
val InkSoft = Color(0xFF3A4A72)
val Brass = Color(0xFFD4A24C)
val Sage = Color(0xFF5E8C7A)
val Clay = Color(0xFFC2674E)
val Paper = Color(0xFFF6F2E9)

val NightBg = Color(0xFF0E1117)
val NightSurface = Color(0xFF161A22)
val NightSurfaceHigh = Color(0xFF1F2430)
val NightOutline = Color(0xFF323949)

val DaySurface = Color(0xFFFFFDF8)
val DaySurfaceVariant = Color(0xFFECE6D8)
val DayOutline = Color(0xFFD6CEBC)

val BrandGradient = Brush.linearGradient(listOf(Ink, InkSoft))
val WarmGradient = Brush.linearGradient(listOf(Color(0xFF8A4B36), Clay))
val CoolGradient = Brush.linearGradient(listOf(Color(0xFF2F5A50), Sage))

private val LightScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE2F2),
    onPrimaryContainer = Color(0xFF111A30),
    secondary = Color(0xFF3F6B5C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8DE),
    onSecondaryContainer = Color(0xFF0D2A21),
    tertiary = Color(0xFF8A6116),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6E2BA),
    onTertiaryContainer = Color(0xFF2C1C00),
    background = Paper,
    onBackground = Color(0xFF1B1C20),
    surface = DaySurface,
    onSurface = Color(0xFF1B1C20),
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = Color(0xFF55524A),
    outline = DayOutline,
    outlineVariant = Color(0xFFE5DFD1),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8F1),
    surfaceContainer = Color(0xFFF2EDE2),
    surfaceContainerHigh = Color(0xFFEDE7DA),
    surfaceContainerHighest = Color(0xFFE7E0D1),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFB8C5E8),
    onPrimary = Color(0xFF1A2440),
    primaryContainer = Color(0xFF34436A),
    onPrimaryContainer = Color(0xFFDCE2F2),
    secondary = Color(0xFF9FCDBB),
    onSecondary = Color(0xFF0F3529),
    secondaryContainer = Color(0xFF284C40),
    onSecondaryContainer = Color(0xFFD3E8DE),
    tertiary = Brass,
    onTertiary = Color(0xFF3A2700),
    tertiaryContainer = Color(0xFF5A4110),
    onTertiaryContainer = Color(0xFFF6E2BA),
    background = NightBg,
    onBackground = Color(0xFFE9E6DF),
    surface = NightSurface,
    onSurface = Color(0xFFE9E6DF),
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = Color(0xFFB6B2A8),
    outline = NightOutline,
    outlineVariant = Color(0xFF262B37),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0A0C10),
    surfaceContainerLow = Color(0xFF12151C),
    surfaceContainer = Color(0xFF181C25),
    surfaceContainerHigh = Color(0xFF1F2430),
    surfaceContainerHighest = Color(0xFF282E3B),
)

// Serif for headings (like printed scores), sans for everything you read fast.
private val Display = FontFamily.Serif

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun VlcSheetsTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
