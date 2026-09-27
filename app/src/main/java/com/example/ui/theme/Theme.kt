package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LightForestGreen,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF06280F),
    primaryContainer = DeepGreen,
    onPrimaryContainer = SoftMint,
    secondary = MediumEcoGreen,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    tertiary = WarmGold,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1F2420),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF9FAFB),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF9CA3AF),
    outline = androidx.compose.ui.graphics.Color(0xFF2D2D2D)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestGreen,
    onPrimary = LightSurface,
    primaryContainer = SoftMint,
    onPrimaryContainer = DeepGreen,
    secondary = EcoGreen,
    onSecondary = LightSurface,
    tertiary = WarmGold,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    onBackground = CharcoalText,
    onSurface = CharcoalText,
    onSurfaceVariant = MutedText,
    outline = HairlineLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors to enforce our beautiful custom Forest health style
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
