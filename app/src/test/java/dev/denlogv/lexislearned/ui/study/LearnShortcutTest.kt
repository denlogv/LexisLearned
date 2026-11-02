package dev.denlogv.lexislearned.ui.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearnShortcutTest {
    private fun shortcut(key: StepKey, revealed: Boolean = true, exposure: Boolean = false) = learnShortcutFor(key, revealed, exposure)

    @Test
    fun spaceTurnsTheCardOverFromEitherSide() {
        assertEquals(LearnShortcut.FLIP, shortcut(StepKey.Space, revealed = false))
        assertEquals(LearnShortcut.FLIP, shortcut(StepKey.Space, revealed = true))
    }

    @Test
    fun enterTurnsTheCardOverUntilItWasRevealed() {
        assertEquals(LearnShortcut.FLIP, shortcut(StepKey.Confirm, revealed = false))
        assertEquals(LearnShortcut.FLIP, shortcut(StepKey.Confirm, revealed = false, exposure = true))
    }

    @Test
    fun enterAfterRevealingGivesTheMiddleRating() {
        assertEquals(LearnShortcut.GOT_IT, shortcut(StepKey.Confirm))
    }

    @Test
    fun enterAfterRevealingAnIntroductionMovesOn() {
        assertEquals(LearnShortcut.NEXT, shortcut(StepKey.Confirm, exposure = true))
    }

    @Test
    fun numberKeysPickTheRatingButtonsInOrder() {
        assertEquals(LearnShortcut.FORGOT, shortcut(StepKey.Number(1)))
        assertEquals(LearnShortcut.GOT_IT, shortcut(StepKey.Number(2)))
        assertEquals(LearnShortcut.EASY, shortcut(StepKey.Number(3)))
        assertNull(shortcut(StepKey.Number(4)))
    }

    @Test
    fun numberKeysPickTheIntroductionButtonsInOrder() {
        assertEquals(LearnShortcut.KNOWN, shortcut(StepKey.Number(1), exposure = true))
        assertEquals(LearnShortcut.NEXT, shortcut(StepKey.Number(2), exposure = true))
        assertNull(shortcut(StepKey.Number(3), exposure = true))
    }

    @Test
    fun numberKeysDoNothingBeforeTheButtonsAppear() {
        assertNull(shortcut(StepKey.Number(1), revealed = false))
    }

    @Test
    fun otherKeysDoNothing() {
        assertNull(shortcut(StepKey.Letter(0)))
        assertNull(shortcut(StepKey.GiveUp))
    }

    @Test
    fun everyShortcutRunsItsButton() {
        val log = mutableListOf<String>()
        val actions = StudyActions(
            onGrade = { log += "grade $it" },
            onIntroduced = { log += "introduced" },
            onKnown = { log += "known" },
            onPair = { log += "pair" },
        )
        LearnShortcut.entries.forEach { it.perform({ log += "flip" }, actions) }
        assertEquals(listOf("flip", "grade AGAIN", "grade GOOD", "grade EASY", "known", "introduced"), log)
    }
}
