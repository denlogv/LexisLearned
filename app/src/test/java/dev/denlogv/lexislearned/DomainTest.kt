package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.domain.Answers
import dev.denlogv.lexislearned.domain.Card
import dev.denlogv.lexislearned.domain.CardProgress
import dev.denlogv.lexislearned.domain.CefrLevel
import dev.denlogv.lexislearned.domain.Chapter
import dev.denlogv.lexislearned.domain.Deck
import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.SessionGrading
import dev.denlogv.lexislearned.domain.Side
import dev.denlogv.lexislearned.domain.Sm2Scheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val day = 24 * 60 * 60_000L

    @Test
    fun schedulerGrowsIntervalsAndResetsOnAgain() {
        var p = CardProgress()
        p = Sm2Scheduler.review(p, Grade.GOOD, 0)
        assertEquals(1, p.intervalDays)
        assertEquals(day, p.dueAt)
        p = Sm2Scheduler.review(p, Grade.GOOD, day)
        assertEquals(6, p.intervalDays)
        p = Sm2Scheduler.review(p, Grade.GOOD, 7 * day)
        assertTrue(p.intervalDays > 6)
        val lapsed = Sm2Scheduler.review(p, Grade.AGAIN, 20 * day)
        assertEquals(0, lapsed.reps)
        assertEquals(1, lapsed.lapses)
        assertEquals(20 * day + 10 * 60_000L, lapsed.dueAt)
        assertTrue(lapsed.ease < p.ease)
    }

    @Test
    fun answersAreForgiving() {
        assertTrue(Answers.isCorrect("To Squeal", "to squeal"))
        assertTrue(Answers.isCorrect("визжать", "визжать / завизжать"))
        assertTrue(Answers.isCorrect("завизжать", "визжать / завизжать"))
        assertTrue(Answers.isCorrect("neispravnyj", "neispravnyi")) // one typo on a long word
        assertFalse(Answers.isCorrect("cat", "car"))
        assertFalse(Answers.isCorrect("", "x"))
        assertTrue(Answers.isCorrect("cafe", "café"))
    }

    @Test
    fun easyAnswersJumpAheadAndHardOnesGrowSlowly() {
        var easy = Sm2Scheduler.review(CardProgress(), Grade.EASY, 0)
        assertEquals(4, easy.intervalDays)
        assertEquals(2.65, easy.ease, 0.0001)
        easy = Sm2Scheduler.review(easy, Grade.EASY, 0)
        assertEquals(8, easy.intervalDays)
        easy = Sm2Scheduler.review(easy, Grade.EASY, 0)
        assertTrue(easy.intervalDays > 8 * 2)
        val learned = CardProgress(reps = 3, ease = 2.5, intervalDays = 10)
        val hard = Sm2Scheduler.review(learned, Grade.HARD, 0)
        assertEquals(12, hard.intervalDays)
        assertEquals(2.35, hard.ease, 0.0001)
    }

    @Test
    fun easeNeverDropsBelowTheFloor() {
        var p = CardProgress(reps = 5, ease = 1.4, intervalDays = 10)
        repeat(5) { p = Sm2Scheduler.review(p, Grade.HARD, 0) }
        assertEquals(1.3, p.ease, 0.0001)
        assertEquals(1.3, Sm2Scheduler.review(p, Grade.AGAIN, 0).ease, 0.0001)
        assertEquals(0, Sm2Scheduler.review(CardProgress(), Grade.AGAIN, 0).lapses)
    }

    @Test
    fun sessionAccuracyBecomesAGrade() {
        assertEquals(Grade.EASY, SessionGrading.gradeFor(1.0, false))
        assertEquals(Grade.GOOD, SessionGrading.gradeFor(0.85, false))
        assertEquals(Grade.HARD, SessionGrading.gradeFor(0.6, false))
        assertEquals(Grade.AGAIN, SessionGrading.gradeFor(0.5, false))
        assertEquals(Grade.EASY, SessionGrading.gradeFor(0.0, true))
        assertEquals(Grade.GOOD, SessionGrading.gradeFor(null, false))
        assertTrue(SessionGrading.counts(Grade.HARD))
        assertFalse(SessionGrading.counts(Grade.AGAIN))
    }

    @Test
    fun deckCountsItsCardsAndLevelsHaveNames() {
        val card = Card("c", Side("a"), Side("b"))
        val deck =
            Deck("d", "T", chapters = listOf(Chapter("1", "One", cards = listOf(card, card)), Chapter("2", "Two", cards = listOf(card))))
        assertEquals(3, deck.cardCount)
        assertEquals(CardProgress(), card.progress)
        assertEquals(listOf("A1", "A2", "B1", "B2", "C1", "C2"), CefrLevel.entries.map { it.label })
        assertEquals("Intermediate", CefrLevel.B1.title)
    }
}
