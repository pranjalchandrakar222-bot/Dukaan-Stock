package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.ThemeStyle

@Immutable
data class DukaanSemanticColors(
  val positive: Color,
  val positiveContainer: Color,
  val onPositiveContainer: Color,
  val negative: Color,
  val negativeContainer: Color,
  val onNegativeContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
  val sidebarBg: Color,
  val sidebarActive: Color,
  val sidebarText: Color,
)

val LocalDukaanColors =
  staticCompositionLocalOf {
    DukaanSemanticColors(
      positive = PositiveLight,
      positiveContainer = PositiveContainerLight,
      onPositiveContainer = OnPositiveContainerLight,
      negative = NegativeLight,
      negativeContainer = NegativeContainerLight,
      onNegativeContainer = OnNegativeContainerLight,
      warning = WarningLight,
      warningContainer = WarningContainerLight,
      onWarningContainer = OnWarningContainerLight,
      sidebarBg = StitchSidebarBg,
      sidebarActive = StitchSidebarSurface,
      sidebarText = StitchSidebarText,
    )
  }

private fun buildLightColorScheme(style: ThemeStyle): ColorScheme {
  val (primary, onPrimary, container, onContainer) =
    when (style) {
      ThemeStyle.EMERALD -> listOf(EmeraldPrimary, EmeraldOnPrimary, EmeraldContainer, EmeraldOnContainer)
      ThemeStyle.TERRACOTTA -> listOf(TerracottaPrimary, TerracottaOnPrimary, TerracottaContainer, TerracottaOnContainer)
      ThemeStyle.SLATE -> listOf(SlatePrimary, SlateOnPrimary, SlateContainer, SlateOnContainer)
      else -> listOf(CobaltPrimary, CobaltOnPrimary, CobaltContainer, CobaltOnContainer)
    }
  return lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = container,
    onPrimaryContainer = onContainer,
    secondary = EmeraldPrimary,
    onSecondary = EmeraldOnPrimary,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldOnContainer,
    background = LightCanvasBg,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceContainerLow = LightSurfaceLow,
    surfaceVariant = Color(0xFFEAF0F8),
    onSurfaceVariant = Color(0xFF475569),
    outlineVariant = LightOutlineVariant,
    error = NegativeLight,
    onError = Color.White,
    errorContainer = NegativeContainerLight,
    onErrorContainer = OnNegativeContainerLight,
  )
}

private fun buildDarkColorScheme(style: ThemeStyle): ColorScheme {
  val primary =
    when (style) {
      ThemeStyle.EMERALD -> Color(0xFF34D399)
      ThemeStyle.TERRACOTTA -> Color(0xFFFB923C)
      ThemeStyle.SLATE -> Color(0xFF94A3B8)
      else -> Color(0xFF60A5FA)
    }
  val container =
    when (style) {
      ThemeStyle.EMERALD -> Color(0xFF064E3B)
      ThemeStyle.TERRACOTTA -> Color(0xFF7C2D12)
      ThemeStyle.SLATE -> Color(0xFF334155)
      else -> Color(0xFF1E3A8A)
    }
  return darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF081226),
    primaryContainer = container,
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    background = DarkCanvasBg,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceContainerLow = DarkSurfaceLow,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outlineVariant = DarkOutlineVariant,
    error = NegativeDark,
    onError = Color(0xFF450A0A),
    errorContainer = NegativeContainerDark,
    onErrorContainer = OnNegativeContainerDark,
  )
}

@Composable
fun DukaanStockTheme(
  themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colorScheme =
    when {
      themeStyle == ThemeStyle.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> buildDarkColorScheme(themeStyle)
      else -> buildLightColorScheme(themeStyle)
    }

  val semanticColors =
    if (darkTheme) {
      DukaanSemanticColors(
        positive = PositiveDark,
        positiveContainer = PositiveContainerDark,
        onPositiveContainer = OnPositiveContainerDark,
        negative = NegativeDark,
        negativeContainer = NegativeContainerDark,
        onNegativeContainer = OnNegativeContainerDark,
        warning = WarningDark,
        warningContainer = WarningContainerDark,
        onWarningContainer = OnWarningContainerDark,
        sidebarBg = Color(0xFF0B1120),
        sidebarActive = Color(0xFF1D2948),
        sidebarText = StitchSidebarText,
      )
    } else {
      DukaanSemanticColors(
        positive = PositiveLight,
        positiveContainer = PositiveContainerLight,
        onPositiveContainer = OnPositiveContainerLight,
        negative = NegativeLight,
        negativeContainer = NegativeContainerLight,
        onNegativeContainer = OnNegativeContainerLight,
        warning = WarningLight,
        warningContainer = WarningContainerLight,
        onWarningContainer = OnWarningContainerLight,
        sidebarBg = StitchSidebarBg,
        sidebarActive = StitchSidebarSurface,
        sidebarText = StitchSidebarText,
      )
    }

  CompositionLocalProvider(LocalDukaanColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = DukaanTypography,
      content = content,
    )
  }
}
