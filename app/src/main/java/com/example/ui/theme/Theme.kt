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

private val DarkColorScheme = darkColorScheme(
  primary = ElegantDarkPrimary,
  onPrimary = ElegantDarkOnPrimary,
  secondary = ElegantDarkSecondary,
  onSecondary = ElegantDarkOnSecondary,
  background = ElegantDarkBg,
  onBackground = ElegantDarkOnBg,
  surface = ElegantDarkBg,
  onSurface = ElegantDarkOnBg,
  surfaceVariant = ElegantDarkSecondary,
  onSurfaceVariant = ElegantDarkOnSecondary,
  outline = ElegantDarkBorder
)

private val LightColorScheme = DarkColorScheme // Force elegant dark even in light mode for consistency

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable system dynamics to preserve explicit designer theme
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
