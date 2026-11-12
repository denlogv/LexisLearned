package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.domain.PlannerSnapshot
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import kotlin.random.Random
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerSnapshotTest {
    private val all = StudyMode.entries.toSet()
    private val ids = (1L..5L).toList()

    private fun planner(modes: Set<StudyMode> = all) = SessionPlanner(ids, modes, 2, Random(5), pairSize = 3)

    /** Answers the next step, correctly unless [wrong] says otherwise, and returns it. */
    private fun SessionPlanner.play(wrong: Set<Long> = emptySet()): SessionPlanner.Step {
        val step = next()!!
        if (step.mode == StudyMode.LEARN && modes.size > 1) {
            step.cardIds.forEach(::introduced)
        } else {
            answered(step, step.cardIds.associateWith { it !in wrong })
        }
        return step
    }

    /** Plays to the end and returns every outcome. */
    private fun SessionPlanner.finish(): List<SessionPlanner.Outcome> {
        val outcomes = ArrayList<SessionPlanner.Outcome>()
        while (next() != null) {
            play()
            outcomes += drainFinished()
        }
        return outcomes
    }

    private fun restore(snapshot: PlannerSnapshot, available: Set<Long> = ids.toSet()) =
        SessionPlanner.restore(snapshot, Random(9), available)

    @Test
    fun aRestoredPlannerHasTheStateOfTheOriginal() {
        val original = planner()
        repeat(7) { original.play(wrong = setOf(2L)) }
        original.next()
        val copy = restore(original.snapshot())
        assertEquals(original.snapshot(), copy.snapshot())
        assertEquals(original.stepsLeft, copy.stepsLeft)
        assertEquals(original.activeCards, copy.activeCards)
        assertEquals(original.modes, copy.modes)
    }

    @Test
    fun theSnapshotSurvivesBeingWrittenAsJson() {
        val original = planner()
        repeat(9) { original.play(wrong = setOf(3L)) }
        original.known(original.next()!!.cardIds.first())
        val json = Json.encodeToString(PlannerSnapshot.serializer(), original.snapshot())
        assertEquals(original.snapshot(), Json.decodeFromString(PlannerSnapshot.serializer(), json))
    }

    @Test
    fun theStepOnScreenComesBackFirstAndOnlyOnce() {
        val original = planner()
        repeat(3) { original.play() }
        val onScreen = original.next()
        val copy = restore(original.snapshot())
        assertEquals(onScreen, copy.next())
        assertEquals(onScreen, copy.next())
        copy.answered(onScreen!!, onScreen.cardIds.associateWith { true })
        assertNull(copy.snapshot().shown)
    }

    @Test
    fun noStepIsOnScreenAfterAnAnswer() {
        val original = planner()
        original.play()
        assertNull(original.snapshot().shown)
    }

    @Test
    fun theRestoredSessionRunsToTheEndWithEveryWordFinishingOnce() {
        val original = planner()
        val before = ArrayList<SessionPlanner.Outcome>()
        repeat(10) {
            original.play()
            before += original.drainFinished()
        }
        val outcomes = restore(original.snapshot()).finish()
        assertEquals(ids.toSet(), (before + outcomes).map { it.cardId }.toSet())
        assertEquals(ids.size, (before + outcomes).size)
    }

    @Test
    fun scoresAndPendingRepeatsSurvive() {
        val original = SessionPlanner(listOf(1L), setOf(StudyMode.SELECT), 1, Random(1))
        original.play(wrong = setOf(1L))
        assertEquals(1, original.stepsLeft)
        val copy = restore(original.snapshot())
        assertEquals(1, copy.stepsLeft)
        copy.play()
        assertEquals(0.0, copy.drainFinished().single().accuracy!!, 0.0)
    }

    @Test
    fun theRetryLimitIsNotResetByARestore() {
        val original = SessionPlanner(listOf(1L), setOf(StudyMode.SELECT), 1, Random(1))
        original.play(wrong = setOf(1L))
        original.play(wrong = setOf(1L))
        var copy = restore(original.snapshot())
        copy.play(wrong = setOf(1L))
        copy = restore(copy.snapshot())
        assertNull(copy.next())
        assertEquals(1, copy.drainFinished().size)
    }

    @Test
    fun knownWordsAndTheirUncollectedOutcomeAreKept() {
        val original = planner()
        val known = original.next()!!.cardIds.first()
        original.known(known)
        val copy = restore(original.snapshot())
        assertEquals(ids.size - 1, copy.activeCards)
        val outcome = copy.drainFinished().single()
        assertEquals(known, outcome.cardId)
        assertTrue(outcome.knew)
        assertTrue(copy.drainFinished().isEmpty())
    }

    @Test
    fun theLastCardIsStillNotRepeatedStraightAway() {
        val original = planner(setOf(StudyMode.SELECT))
        repeat(5) {
            val last = original.play().cardIds.first()
            val copy = restore(original.snapshot())
            assertNotEquals(last, copy.next()!!.cardIds.first())
        }
    }

    @Test
    fun cardsThatNoLongerExistAreDropped() {
        val original = planner()
        original.play()
        val copy = restore(original.snapshot(), available = setOf(1L, 2L))
        assertEquals(2, copy.activeCards)
        val seen = HashSet<Long>()
        while (true) {
            val step = copy.next() ?: break
            seen += step.cardIds
            copy.play()
        }
        assertTrue(seen.all { it in setOf(1L, 2L) })
    }

    @Test
    fun theStepOnScreenIsDroppedWhenItsCardIsGone() {
        val original = planner()
        val shown = original.next()!!
        val copy = restore(original.snapshot(), available = ids.toSet() - shown.cardIds.toSet())
        assertNull(copy.snapshot().shown)
        assertNotNull(copy.next())
    }

    @Test
    fun aSessionWithoutAnyExistingCardIsOver() {
        val original = planner()
        original.next()
        val copy = restore(original.snapshot(), available = emptySet())
        assertEquals(0, copy.activeCards)
        assertEquals(0, copy.stepsLeft)
        assertNull(copy.next())
    }

    @Test
    fun aStepThatDoesNotMatchThePlansIsDropped() {
        val original = planner()
        val shown = original.next()!!
        val other = StudyMode.entries.first { it != shown.mode }
        val bad = original.snapshot().copy(shown = SessionPlanner.Step(other, shown.cardIds))
        assertNull(restore(bad).snapshot().shown)
    }

    @Test
    fun takingASnapshotDoesNotChangeThePlanner() {
        val original = planner()
        repeat(4) { original.play() }
        val left = original.stepsLeft
        original.snapshot()
        original.snapshot()
        assertEquals(left, original.stepsLeft)
    }
}
