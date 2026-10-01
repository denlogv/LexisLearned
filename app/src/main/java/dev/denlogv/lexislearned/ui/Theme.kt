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
import androidx.compose.ui.platform.LocalContext

/**
 * The app's Material 3 theme: dynamic colours on Android 12 and newer, fixed light or dark colours before that.
 *
 * @param content the UI to theme.
 */
@Composable
fun LexisLearnedTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme(isSystemInDarkTheme()), content = content)
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
