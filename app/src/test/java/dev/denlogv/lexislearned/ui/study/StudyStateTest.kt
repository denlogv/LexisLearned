package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyStateTest {
    @Test
    fun correctPairingMatchesBothSides() {
        var s = PairState(listOf(1, 2))
        s = s.pickLeft(1).pickRight(1)
        assertEquals(setOf(1L), s.matched)
        assertNull(s.left)
        assertFalse(s.isComplete)
        s = s.pickLeft(2).pickRight(2)
        assertTrue(s.isComplete)
        assertTrue(s.missed.isEmpty())
    }

    @Test
    fun wrongPairingMarksBothCardsAsMissedAndCanBeCleared() {
        val s = PairState(listOf(1, 2)).pickLeft(1).pickRight(2)
        assertEquals(2L, s.wrong)
        assertEquals(setOf(1L, 2L), s.missed)
        assertNull(s.clearWrong().wrong)
    }

    @Test
    fun pairingIgnoresTapsThatDoNothing() {
        val start = PairState(listOf(1, 2))
        assertEquals(start, start.pickRight(1))
        val matched = start.pickLeft(1).pickRight(1)
        assertEquals(matched, matched.pickLeft(1))
        assertEquals(matched, matched.pickLeft(2).let { it.copy(left = null) }.pickRight(1))
    }

    @Test
    fun typingIsLockedAfterChecking() {
        var t = TypeState()
        assertFalse(t.canSubmit)
        t = t.withText("ipsum")
        assertTrue(t.canSubmit)
        t = t.check("ипсум")
        assertEquals(false, t.result)
        assertEquals(Grade.AGAIN, t.grade)
        assertEquals("ipsum", t.withText("x").text)
        assertTrue(t.canSubmit)
        val ok = TypeState("Dog").check("dog")
        assertEquals(Grade.GOOD, ok.grade)
        assertEquals(ok, ok.check("cat"))
    }

    @Test
    fun blankAnswerIsNotChecked() {
        assertEquals(TypeState("  "), TypeState("  ").check("x"))
    }

    @Test
    fun optionStateShowsRightAndWrongAfterPicking() {
        assertEquals(OptionState.OPEN, optionState("a", null, "a"))
        assertEquals(OptionState.CORRECT, optionState("a", "b", "a"))
        assertEquals(OptionState.WRONG, optionState("b", "b", "a"))
        assertEquals(OptionState.DIMMED, optionState("c", "b", "a"))
    }

    @Test
    fun studyUiDescribesProgress() {
        val ui = StudyUi(loading = false, mode = StudyMode.TYPE, stepsDone = 3, stepsLeft = 1, totalWords = 2, completedWords = 1)
        assertEquals(0.75f, ui.progress, 0f)
        assertEquals("Type", ui.title)
        assertEquals("1 step left · 1/2 words done", ui.statusLine)
        assertEquals("Study", ui.copy(finished = true).title)
        assertEquals("Study", StudyUi().title)
        assertEquals(0f, StudyUi().progress, 0f)
        assertEquals("3 steps left · 0/1 word done", StudyUi(stepsLeft = 3, totalWords = 1).statusLine)
    }
}
