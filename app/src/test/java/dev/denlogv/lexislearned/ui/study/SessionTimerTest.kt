package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTimerTest {
    private var now = 1_000L
    private val check = SessionPlanner.Step(StudyMode.CHECK, listOf(1))

    /** Shows a step, lets [seconds] pass and answers it. */
    private fun SessionTimer.play(step: SessionPlanner.Step, seconds: Long) {
        stepShown()
        now += seconds * 1_000
        stepDone(step)
    }

    @Test
    fun noEstimateUntilTheFirstStepWasTimed() {
        val timer = SessionTimer({ now }, emptyMap(), minSteps = 1)
        assertNull(timer.remaining(mapOf(StudyMode.CHECK to 4)))
        timer.play(check, 5)
        assertEquals(TimeLeft(20_000, 20_000), timer.remaining(mapOf(StudyMode.CHECK to 4)))
    }

    @Test
    fun theEstimateWaitsForTheConfiguredNumberOfSteps() {
        val timer = SessionTimer({ now }, emptyMap(), minSteps = 3)
        repeat(2) { timer.play(check, 5) }
        assertNull(timer.remaining(mapOf(StudyMode.CHECK to 4)))
        timer.play(check, 5)
        assertEquals(TimeLeft(20_000, 20_000), timer.remaining(mapOf(StudyMode.CHECK to 4)))
    }

    @Test
    fun theScopeAlsoNeedsTheStepsOfTheSessionsStillToCome() {
        val later = mapOf(StudyMode.CHECK to 6, StudyMode.TYPE to 2)
        val timer = SessionTimer({ now }, later, minSteps = 1)
        repeat(3) { timer.play(check, 10) }
        timer.play(SessionPlanner.Step(StudyMode.TYPE, listOf(2)), 30)
        // 2 Check and 1 Type left now; 8 Check and 3 Type altogether
        val left = timer.remaining(mapOf(StudyMode.CHECK to 2, StudyMode.TYPE to 1))
        assertEquals(TimeLeft(2 * 10_000L + 30_000, 8 * 10_000L + 3 * 30_000), left)
    }

    @Test
    fun timeSpentBeforeAStepIsShownDoesNotCount() {
        val timer = SessionTimer({ now }, emptyMap(), minSteps = 1)
        repeat(3) {
            now += 600_000 // reading the previous answer, thinking, whatever happens between steps
            timer.play(check, 4)
        }
        assertEquals(4_000L, timer.remaining(mapOf(StudyMode.CHECK to 1))?.session)
    }

    @Test
    fun theRealClockOnlyRunsForward() {
        val first = SessionTimer.monotonicMillis()
        assertTrue(SessionTimer.monotonicMillis() >= first)
    }
}
