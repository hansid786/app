package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
  lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDeep,
    secondary = IndigoDark,
    onSecondary = Color.White,
    secondaryContainer = IndigoSoft,
    onSecondaryContainer = IndigoDeep,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldLight,
    onTertiaryContainer = EmeraldSuccess,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SurfaceWhite,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = SlateTextMuted,
    outline = SlateBorder,
    outlineVariant = SlateBorderLight,
    error = RoseDestructive,
    onError = Color.White,
    errorContainer = RoseLight,
    onErrorContainer = RoseDestructive,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoDeep,
    onPrimaryContainer = IndigoLight,
    secondary = IndigoDark,
    onSecondary = Color.White,
    background = Color(0xFF0B0F19),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF131B2E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = SlateTextSubtle,
    outline = Color(0xFF334155),
    error = RoseDestructive,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

