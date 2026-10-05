package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.domain.Grade
import dev.denlogv.lexislearned.domain.SessionGrading
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {
    private val all = StudyMode.entries.toSet()

    /** Runs a session answering everything correctly; returns the modes seen per card and the outcomes. */
    private fun run(
        planner: SessionPlanner,
        correct: (Long, StudyMode) -> Boolean = { _, _ -> true },
    ): Pair<Map<Long, List<StudyMode>>, List<SessionPlanner.Outcome>> {
        val seen = HashMap<Long, MutableList<StudyMode>>()
        val outcomes = ArrayList<SessionPlanner.Outcome>()
        var guard = 0
        while (true) {
            val step = planner.next() ?: break
            check(guard++ < 10_000) { "planner does not terminate" }
            step.cardIds.forEach { seen.getOrPut(it) { ArrayList() } += step.mode }
            if (step.mode == StudyMode.LEARN && planner.modes.size > 1) {
                step.cardIds.forEach(planner::introduced)
            } else {
                planner.answered(step, step.cardIds.associateWith { correct(it, step.mode) })
            }
            outcomes += planner.drainFinished()
        }
        return seen to outcomes
    }

    /** Every round of a card's path is Learn first, then each other selected mode exactly once. */
    private fun assertRounds(seen: List<StudyMode>, modes: Set<StudyMode>, rounds: Int) {
        val per = modes.size
        assertEquals(per * rounds, seen.size)
        for (r in 0 until rounds) {
            val chunk = seen.subList(r * per, (r + 1) * per)
            assertEquals(modes, chunk.toSet())
            if (StudyMode.LEARN in modes) assertEquals(StudyMode.LEARN, chunk.first())
        }
    }

    @Test
    fun everyCardRunsThroughEveryMode() {
        val (seen, outcomes) = run(SessionPlanner((1L..6L).toList(), all, 1, Random(1)))
        assertEquals(6, outcomes.size)
        for (id in 1L..6L) assertRounds(seen.getValue(id), all, 1)
    }

    @Test
    fun roundsRepeatTheWholeSet() {
        val (seen, _) = run(SessionPlanner((1L..4L).toList(), all, 3, Random(2)))
        for (id in 1L..4L) assertRounds(seen.getValue(id), all, 3)
    }

    @Test
    fun onlySelectedModesAreUsed() {
        val modes = setOf(StudyMode.SELECT, StudyMode.TYPE)
        val (seen, _) = run(SessionPlanner((1L..5L).toList(), modes, 2, Random(3)))
        for (id in 1L..5L) assertRounds(seen.getValue(id), modes, 2)
    }

    @Test
    fun pairStepsHoldAtMostThePairSize() {
        val modes = setOf(StudyMode.PAIR)
        for (size in listOf(3, 8)) {
            val planner = SessionPlanner((1L..20L).toList(), modes, 1, Random(size), size)
            val sizes = generateSequence { planner.next()?.also { planner.answered(it, emptyMap()) } }.map { it.cardIds.size }.toList()
            assertEquals(20, sizes.sum())
            assertEquals(size, sizes.max())
        }
    }

    @Test
    fun modeOrderDiffersBetweenCards() {
        val (seen, _) = run(SessionPlanner((1L..12L).toList(), all, 1, Random(11)))
        assertTrue(seen.values.map { it.toList() }.toSet().size > 1)
    }

    @Test
    fun aLoneCardAnsweredWrongDoesNotRepeatTheSameModeBackToBack() {
        val modes = setOf(StudyMode.SELECT, StudyMode.CHECK, StudyMode.TYPE)
        for (seed in 1..20) {
            val (seen, _) = run(SessionPlanner(listOf(1L), modes, 1, Random(seed))) { _, _ -> false }
            val steps = seen.getValue(1L)
            assertEquals(9, steps.size) // each mode: original + 2 retries
            for (i in 1 until steps.size) assertTrue("seed $seed: $steps", steps[i] != steps[i - 1])
        }
    }

    @Test
    fun wrongAnswersRepeatTheStepAtMostTwice() {
        val planner = SessionPlanner(listOf(1L), setOf(StudyMode.TYPE), 1, Random(4))
        val (seen, outcomes) = run(planner) { _, _ -> false }
        assertEquals(3, seen.getValue(1L).size) // original + 2 retries
        assertEquals(0.0, outcomes.single().accuracy!!, 0.0)
    }

    @Test
    fun retryDoesNotCountTwiceTowardAccuracy() {
        var failedOnce = false
        val (_, outcomes) = run(SessionPlanner(listOf(1L), setOf(StudyMode.SELECT, StudyMode.TYPE), 1, Random(5))) { _, m ->
            if (m == StudyMode.SELECT && !failedOnce) {
                failedOnce = true
                false
            } else {
                true
            }
        }
        assertEquals(0.5, outcomes.single().accuracy!!, 0.0) // Select missed first try, Type right
    }

    @Test
    fun pairWaitsForAtLeastTwoCardsAndNeverStarvesTheSession() {
        val (seen, outcomes) = run(SessionPlanner(listOf(1L, 2L, 3L), setOf(StudyMode.PAIR, StudyMode.SELECT), 1, Random(6)))
        assertEquals(3, outcomes.size)
        for (id in 1L..3L) assertEquals(setOf(StudyMode.PAIR, StudyMode.SELECT), seen.getValue(id).toSet())
        // a lone card still completes (Pair is then padded with decoys by the caller)
        val (loneSeen, loneOutcomes) = run(SessionPlanner(listOf(9L), setOf(StudyMode.PAIR), 1, Random(7)))
        assertEquals(listOf(StudyMode.PAIR), loneSeen.getValue(9L))
        assertEquals(1, loneOutcomes.size)
    }

    @Test
    fun sameCardIsNotShownTwiceInARowWhileOthersRemain() {
        for (seed in 1..30) {
            val planner = SessionPlanner((1L..5L).toList(), all, 2, Random(seed))
            var last: Long? = null
            while (true) {
                val others = planner.activeCards > 1
                val step = planner.next() ?: break
                if (others && step.cardIds.size == 1 && step.cardIds.single() == last) error("seed $seed: repeat of $last")
                last = step.cardIds.first()
                if (step.mode == StudyMode.LEARN) {
                    step.cardIds.forEach(planner::introduced)
                } else {
                    planner.answered(step, step.cardIds.associateWith { true })
                }
            }
        }
    }

    @Test
    fun knownCardsLeaveTheSessionEarly() {
        val planner = SessionPlanner(listOf(1L, 2L), all, 1, Random(9))
        planner.known(1L)
        val outcome = planner.drainFinished().single()
        assertTrue(outcome.knew)
        val (seen, _) = run(planner)
        assertNull(seen[1L])
        assertRounds(seen.getValue(2L), all, 1)
    }

    @Test
    fun learnIsAGradedFlashcardWhenItIsTheOnlyMode() {
        val planner = SessionPlanner(listOf(1L), setOf(StudyMode.LEARN), 1, Random(10))
        val (_, outcomes) = run(planner) { _, _ -> false }
        assertEquals(0.0, outcomes.single().accuracy!!, 0.0)
    }

    @Test
    fun accuracyMapsToGrades() {
        assertEquals(Grade.EASY, SessionGrading.gradeFor(1.0, false))
        assertEquals(Grade.GOOD, SessionGrading.gradeFor(0.8, false))
        assertEquals(Grade.HARD, SessionGrading.gradeFor(0.6, false))
        assertEquals(Grade.AGAIN, SessionGrading.gradeFor(0.4, false))
        assertEquals(Grade.GOOD, SessionGrading.gradeFor(null, false)) // nothing graded (e.g. introduction only)
        assertEquals(Grade.EASY, SessionGrading.gradeFor(0.0, true))
        assertFalse(SessionGrading.counts(Grade.AGAIN))
    }
}
