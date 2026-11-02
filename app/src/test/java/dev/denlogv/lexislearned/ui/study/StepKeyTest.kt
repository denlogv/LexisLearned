package dev.denlogv.lexislearned.ui.study

import android.content.res.Configuration
import android.view.KeyEvent as AndroidKeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_0
import android.view.KeyEvent.KEYCODE_1
import android.view.KeyEvent.KEYCODE_9
import android.view.KeyEvent.KEYCODE_A
import android.view.KeyEvent.KEYCODE_D
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_NUMPAD_0
import android.view.KeyEvent.KEYCODE_NUMPAD_5
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import android.view.KeyEvent.KEYCODE_SPACE
import android.view.KeyEvent.KEYCODE_TAB
import android.view.KeyEvent.KEYCODE_Z
import android.view.KeyEvent.META_ALT_ON
import android.view.KeyEvent.META_CTRL_ON
import android.view.KeyEvent.META_META_ON
import android.view.KeyEvent.META_SHIFT_ON
import androidx.compose.ui.input.key.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StepKeyTest {
    private fun event(code: Int, action: Int = ACTION_DOWN, repeat: Int = 0, meta: Int = 0) =
        KeyEvent(AndroidKeyEvent(0L, 0L, action, code, repeat, meta))

    private fun key(code: Int, action: Int = ACTION_DOWN, repeat: Int = 0, meta: Int = 0): StepKey? {
        var received: StepKey? = null
        event(code, action, repeat, meta).passToStep {
            received = it
            true
        }
        return received
    }

    @Test
    fun enterOnEitherBlockConfirms() {
        assertEquals(StepKey.Confirm, key(KEYCODE_ENTER))
        assertEquals(StepKey.Confirm, key(KEYCODE_NUMPAD_ENTER))
    }

    @Test
    fun spaceBarIsSpace() {
        assertEquals(StepKey.Space, key(KEYCODE_SPACE))
    }

    @Test
    fun numberKeysOnBothBlocksGiveTheirNumber() {
        assertEquals(StepKey.Number(1), key(KEYCODE_1))
        assertEquals(StepKey.Number(9), key(KEYCODE_9))
        assertEquals(StepKey.Number(5), key(KEYCODE_NUMPAD_5))
    }

    @Test
    fun zeroIsTheTenthNumber() {
        assertEquals(StepKey.Number(10), key(KEYCODE_0))
        assertEquals(StepKey.Number(10), key(KEYCODE_NUMPAD_0))
    }

    @Test
    fun lettersCountFromA() {
        assertEquals(StepKey.Letter(0), key(KEYCODE_A))
        assertEquals(StepKey.Letter(25), key(KEYCODE_Z))
    }

    @Test
    fun shiftDoesNotChangeALetter() {
        assertEquals(StepKey.Letter(3), key(KEYCODE_D, meta = META_SHIFT_ON))
    }

    @Test
    fun controlDMeansIDontKnow() {
        assertEquals(StepKey.GiveUp, key(KEYCODE_D, meta = META_CTRL_ON))
    }

    @Test
    fun otherControlCombinationsAreLeftToTheSystem() {
        assertNull(key(KEYCODE_A, meta = META_CTRL_ON))
        assertNull(key(KEYCODE_ENTER, meta = META_CTRL_ON))
    }

    @Test
    fun altAndMetaCombinationsAreLeftToTheSystem() {
        assertNull(key(KEYCODE_D, meta = META_ALT_ON))
        assertNull(key(KEYCODE_1, meta = META_META_ON))
        assertNull(key(KEYCODE_D, meta = META_CTRL_ON or META_ALT_ON))
    }

    @Test
    fun keysWithoutAMeaningAreIgnored() {
        assertNull(key(KEYCODE_TAB))
        assertNull(key(KEYCODE_DPAD_DOWN))
    }

    @Test
    fun releasingAKeyDoesNothing() {
        assertNull(key(KEYCODE_ENTER, action = ACTION_UP))
    }

    @Test
    fun holdingAKeyDownDoesNotRepeatIt() {
        assertNull(key(KEYCODE_ENTER, repeat = 1))
    }

    @Test
    fun aRepeatOfAStepKeyIsUsedUpSoNothingElseGetsIt() {
        assertTrue(event(KEYCODE_ENTER, repeat = 1).passToStep { true })
        assertTrue(event(KEYCODE_SPACE, repeat = 3).passToStep { false })
    }

    @Test
    fun aRepeatOfAnotherKeyIsLeftToTheSystem() {
        assertFalse(event(KEYCODE_DPAD_DOWN, repeat = 1).passToStep { true })
    }

    @Test
    fun aReleaseIsLeftToTheSystem() {
        assertFalse(event(KEYCODE_ENTER, action = ACTION_UP).passToStep { true })
    }

    @Test
    fun aPressIsUsedUpOnlyIfTheStepUsesIt() {
        assertTrue(event(KEYCODE_ENTER).passToStep { true })
        assertFalse(event(KEYCODE_ENTER).passToStep { false })
    }

    @Test
    fun aKeyboardIsDetectedFromTheConfiguration() {
        assertFalse(hasHardwareKeyboard(Configuration.KEYBOARD_NOKEYS, Configuration.HARDKEYBOARDHIDDEN_UNDEFINED))
        assertTrue(hasHardwareKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_NO))
        assertTrue(hasHardwareKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_UNDEFINED))
    }

    @Test
    fun aFoldedAwayKeyboardDoesNotCount() {
        assertFalse(hasHardwareKeyboard(Configuration.KEYBOARD_QWERTY, Configuration.HARDKEYBOARDHIDDEN_YES))
    }

    @Test
    fun aKeyHintIsAddedOnlyWhenShown() {
        assertEquals("Next (Enter)", keyHint("Next", "Enter", show = true))
        assertEquals("Next", keyHint("Next", "Enter", show = false))
    }
}
