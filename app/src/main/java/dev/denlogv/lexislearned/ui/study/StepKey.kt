package dev.denlogv.lexislearned.ui.study

import android.content.res.Configuration
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.type

/** A key a learner can press during a study step, decoded from the raw key event so that the steps do not deal with key codes. */
sealed interface StepKey {
    /** Enter, on the main block or the number pad. */
    data object Confirm : StepKey

    /** The space bar. */
    data object Space : StepKey

    /**
     * A number key, on the top row or the number pad.
     *
     * @property value the number the key carries; 0 counts as 10, so ten choices fit on the number keys.
     */
    data class Number(val value: Int) : StepKey

    /**
     * A letter key.
     *
     * @property index the position in the alphabet, 0 for A.
     */
    data class Letter(val index: Int) : StepKey

    /** Ctrl+D: "I don't know". */
    data object GiveUp : StepKey
}

/**
 * Decodes a key code.
 *
 * @param keyCode the Android key code.
 * @param ctrl whether Ctrl is held.
 * @param alt whether Alt or Meta is held; such a combination belongs to the system or the window, so it is never a step key.
 * @return the step key, or null if the key means nothing to a study step.
 */
fun stepKeyFor(keyCode: Int, ctrl: Boolean, alt: Boolean): StepKey? = when {
    alt -> null
    ctrl -> if (keyCode == AndroidKeyEvent.KEYCODE_D) StepKey.GiveUp else null
    else -> plainStepKey(keyCode)
}

/**
 * Decodes a key pressed without Ctrl, Alt or Meta.
 *
 * @param keyCode the Android key code.
 * @return the step key, or null if the key means nothing to a study step.
 */
private fun plainStepKey(keyCode: Int): StepKey? = when (keyCode) {
    AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> StepKey.Confirm
    AndroidKeyEvent.KEYCODE_SPACE -> StepKey.Space
    in AndroidKeyEvent.KEYCODE_1..AndroidKeyEvent.KEYCODE_9 -> StepKey.Number(keyCode - AndroidKeyEvent.KEYCODE_0)
    in AndroidKeyEvent.KEYCODE_NUMPAD_1..AndroidKeyEvent.KEYCODE_NUMPAD_9 -> StepKey.Number(keyCode - AndroidKeyEvent.KEYCODE_NUMPAD_0)
    AndroidKeyEvent.KEYCODE_0, AndroidKeyEvent.KEYCODE_NUMPAD_0 -> StepKey.Number(TEN)
    in AndroidKeyEvent.KEYCODE_A..AndroidKeyEvent.KEYCODE_Z -> StepKey.Letter(keyCode - AndroidKeyEvent.KEYCODE_A)
    else -> null
}

/** The value of the 0 key: the tenth choice. */
private const val TEN = 10

/**
 * Passes a key event on to a study step. Only the moment a key goes down counts, so one press never answers two steps in a
 * row. While a step key is held down, its repeats are used up without doing anything: let through, the first repeat reaches
 * the step that the press has just opened before that step has taken the focus, and the focus then lands on the step's first
 * card or button, which takes every later Enter for itself instead of the step.
 *
 * @receiver the key event.
 * @param onKey the step's handling of a key that went down; returns true if the step used it.
 * @return true if the event is used up: a press the step used, or a repeat of a key a study step uses.
 */
fun KeyEvent.passToStep(onKey: (StepKey) -> Boolean): Boolean {
    val key = stepKeyFor(nativeKeyEvent.keyCode, isCtrlPressed, isAltPressed || isMetaPressed)
    return when {
        key == null || type != KeyEventType.KeyDown -> false
        nativeKeyEvent.repeatCount > 0 -> true
        else -> onKey(key)
    }
}

/**
 * Whether a keyboard is connected and ready to type on, which is when the buttons show the keys that press them. A keyboard
 * that is folded away, such as a closed slide-out keyboard or a folio cover turned behind the device, does not count.
 *
 * @param keyboardType the `keyboard` field of the device configuration.
 * @param hardKeyboardHidden the `hardKeyboardHidden` field of the device configuration.
 * @return false on a device without a keyboard or with its keyboard folded away.
 */
fun hasHardwareKeyboard(keyboardType: Int, hardKeyboardHidden: Int): Boolean =
    keyboardType != Configuration.KEYBOARD_NOKEYS && hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES

/**
 * A button label with the key that presses it, when a keyboard is connected.
 *
 * @param label the label.
 * @param key the key's name, for example "1" or "Enter".
 * @param show whether to add the key.
 * @return "Next (Enter)", or just "Next" when [show] is false.
 */
fun keyHint(label: String, key: String, show: Boolean): String = if (show) "$label ($key)" else label

/**
 * A choice's label with the key that picks it, when a keyboard is connected.
 *
 * @param label the label.
 * @param key the key's name, for example "1" or "A"; null if no key picks it.
 * @param show whether to add the key.
 * @return "1  label", or just the label when [show] is false or there is no key.
 */
fun keyPrefix(label: String, key: String?, show: Boolean): String = if (show && key != null) "$key  $label" else label
