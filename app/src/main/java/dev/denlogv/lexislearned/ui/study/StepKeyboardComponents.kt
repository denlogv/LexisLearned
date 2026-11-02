package dev.denlogv.lexislearned.ui.study

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration

/** Whether button labels show the keys that press them. */
private val LocalKeyHints = compositionLocalOf { false }

/**
 * Makes the button labels in [content] show their keys when a keyboard is connected.
 *
 * @param content the study step.
 */
@Composable
fun ProvideKeyHints(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val show = hasHardwareKeyboard(configuration.keyboard, configuration.hardKeyboardHidden)
    CompositionLocalProvider(LocalKeyHints provides show, content = content)
}

/**
 * A button label, with the key that presses the button if a keyboard is connected.
 *
 * @param label the label.
 * @param key the key's name, for example "1" or "Enter".
 * @return the text for the button.
 */
@Composable
fun hinted(label: String, key: String): String = keyHint(label, key, LocalKeyHints.current)

/**
 * A choice's label, with the key that picks it if a keyboard is connected.
 *
 * @param label the label.
 * @param key the key's name, for example "1" or "A".
 * @return the text for the choice.
 */
@Composable
fun prefixed(label: String, key: String): String = keyPrefix(label, key, LocalKeyHints.current)

/**
 * Makes a step react to the keyboard: the modifier holds the focus that key presses go to, and passes them on to [onKey].
 * Presses that no focused button or text field inside the step consumed reach it too.
 *
 * @param focus what moves the focus onto the step; see [rememberStepFocus].
 * @param onKey called with each key that goes down; returns true if the step used it.
 * @return this modifier with the key handling added.
 */
fun Modifier.stepKeys(focus: FocusRequester, onKey: (StepKey) -> Boolean): Modifier =
    focusRequester(focus).focusable().onKeyEvent { event -> event.passToStep(onKey) }

/**
 * Creates the focus of a step and takes the focus when the step appears, so the keyboard works at once. Without this the
 * focus is nowhere, and the first key press goes to the back button at the top.
 *
 * @param refocusOn values whose change takes the focus again; a button that disappears when it is pressed (a disabled one)
 *   would otherwise leave the focus nowhere.
 * @return the focus to pass to [stepKeys].
 */
@Composable
fun rememberStepFocus(vararg refocusOn: Any?): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit, *refocusOn) { focus.requestFocus() }
    return focus
}
