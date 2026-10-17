package com.vex.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vex.launcher.core.model.AccentTheme
import com.vex.launcher.core.model.LauncherSettings

private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B2050),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFF9E9EAE),
    onSecondary = Color(0xFF14141B),
    secondaryContainer = Color(0xFF26262F),
    onSecondaryContainer = Color(0xFFE2E2EA),
    background = Obsidian,
    onBackground = TextPrimaryDark,
    surface = Charcoal,
    onSurface = TextPrimaryDark,
    surfaceContainerLowest = Color(0xFF0B0B11),
    surfaceContainerLow = Color(0xFF101017),
    surfaceContainer = Charcoal,
    surfaceContainerHigh = CharcoalHigh,
    surfaceContainerHighest = CharcoalHighest,
    surfaceVariant = CharcoalHigh,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF21212B),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0A0A),
    scrim = Color(0xFF000000),
)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DEFF),
    onPrimaryContainer = Color(0xFF1B0B4D),
    secondary = Color(0xFF56566A),
    background = Color(0xFFF3F3F7),
    onBackground = TextPrimaryLight,
    surface = Color(0xFFFBFBFE),
    onSurface = TextPrimaryLight,
    surfaceContainerHigh = Color(0xFFECECF2),
    surfaceContainerHighest = Color(0xFFE3E3EA),
    surfaceVariant = Color(0xFFE9E9F0),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFC9C9D4),
    error = Color(0xFFB3261E),
)

private val VexTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 50.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

/**
 * Vex Launcher teması.
 * Koyu tema varsayılandır; vurgu rengi kullanıcı ayarından gelir.
 */
@Composable
fun VexTheme(
    settings: LauncherSettings,
    content: @Composable () -> Unit,
) {
    val accent = settings.accent.primary()
    val accentBright = settings.accent.primaryBright()

    val colors = if (settings.darkTheme) {
        DarkColors.copy(
            primary = accent,
            primaryContainer = accent.copy(alpha = 0.28f),
            onPrimaryContainer = accentBright,
        )
    } else {
        LightColors.copy(
            primary = accent,
            primaryContainer = accent.copy(alpha = 0.18f),
            onPrimaryContainer = accent,
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = VexTypography,
        content = content,
    )
}

/** Vurgu renginin yardımcı fonksiyonu (UI bileşenlerinde kısa erişim). */
@Composable
fun accentColor(settings: LauncherSettings): Color = settings.accent.primary()
