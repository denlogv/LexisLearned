package dev.denlogv.lexislearned.ui.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairKeysTest {
    private val words = listOf(1L, 2L, 3L)
    private val translations = listOf(3L, 1L, 2L)
    private val board = PairState(words)

    @Test
    fun aNumberKeySelectsTheWordInThatRow() {
        assertEquals(2L, board.press(StepKey.Number(2), words, translations).left)
    }

    @Test
    fun aLetterKeyPicksTheTranslationInThatRow() {
        val matched = board.pickLeft(1).press(StepKey.Letter(1), words, translations)
        assertEquals(setOf(1L), matched.matched)
    }

    @Test
    fun aWrongLetterMarksTheMiss() {
        val missed = board.press(StepKey.Number(1), words, translations).press(StepKey.Letter(0), words, translations)
        assertEquals(3L, missed.wrong)
        assertEquals(setOf(1L, 3L), missed.missed)
    }

    @Test
    fun aLetterWithoutASelectedWordDoesNothing() {
        assertEquals(board, board.press(StepKey.Letter(0), words, translations))
    }

    @Test
    fun aRowThatDoesNotExistDoesNothing() {
        assertEquals(board, board.press(StepKey.Number(4), words, translations))
        assertEquals(board, board.press(StepKey.Letter(3), words, translations))
    }

    @Test
    fun aMatchedWordCannotBeSelectedAgain() {
        val matched = board.pickLeft(1).pickRight(1)
        assertEquals(matched, matched.press(StepKey.Number(1), words, translations))
    }

    @Test
    fun otherKeysDoNothing() {
        assertEquals(board, board.press(StepKey.Confirm, words, translations))
        assertEquals(board, board.press(StepKey.GiveUp, words, translations))
    }

    @Test
    fun theZeroKeySelectsTheTenthWord() {
        val ten = (1L..10L).toList()
        assertEquals(10L, PairState(ten).press(StepKey.Number(10), ten, ten).left)
    }

    @Test
    fun wordKeysAreNumberedFromOneAndEndWithZero() {
        assertEquals("1", wordKeyName(0))
        assertEquals("9", wordKeyName(8))
        assertEquals("0", wordKeyName(9))
        assertNull(wordKeyName(10))
    }

    @Test
    fun translationKeysAreLettered() {
        assertEquals("A", translationKeyName(0))
        assertEquals("L", translationKeyName(11))
        assertEquals("Z", translationKeyName(25))
        assertNull(translationKeyName(26))
    }
}
