package com.example.docscanner.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    secondary = DarkTextSecondary,
    tertiary = PureWhite,
    background = PureBlack,
    surface = DarkSurface,
    onPrimary = PureBlack,
    onSecondary = PureWhite,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    outline = DarkBorder,
    surfaceVariant = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    secondary = LightTextSecondary,
    tertiary = PureBlack,
    background = PureWhite,
    surface = LightSurface,
    onPrimary = PureWhite,
    onSecondary = PureBlack,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    outline = LightBorder,
    surfaceVariant = LightSurface
)

@Composable
fun DocScannerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
