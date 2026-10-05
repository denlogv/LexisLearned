package dev.denlogv.lexislearned.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * The app's Material 3 theme: dynamic colours on Android 12 and newer, fixed light or dark colours before that.
 *
 * @param content the UI to theme.
 */
@Composable
fun LexisLearnedTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = colorScheme(dark).withRedErrors(dark), content = content)
}

/**
 * Picks the colour scheme.
 *
 * @param dark whether the system is in dark mode.
 * @return the dynamic scheme if supported, else the default light or dark scheme.
 */
@Composable
private fun colorScheme(dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
}

/** Red of error text and outlines on a light background. */
private val ErrorOnLight = Color(0xFFC62828)

/** Red of error text and outlines on a dark background; lighter so it stays readable. */
private val ErrorOnDark = Color(0xFFEF5350)

/** Solid red behind wrong answers, with white text on it, in both modes. */
private val ErrorFill = Color(0xFFC62828)

/**
 * Gives the scheme a real red for errors. The dynamic and default dark schemes use a pale pink, which does not read as a
 * mistake, and the red should be the same on every screen, so it is set once here and everything that shows an error uses
 * the theme's error colours.
 *
 * @receiver the scheme to adjust.
 * @param dark whether the scheme is a dark one.
 * @return the scheme with red error colours.
 */
private fun ColorScheme.withRedErrors(dark: Boolean): ColorScheme = copy(
    error = if (dark) ErrorOnDark else ErrorOnLight,
    onError = Color.White,
    errorContainer = ErrorFill,
    onErrorContainer = Color.White,
)
