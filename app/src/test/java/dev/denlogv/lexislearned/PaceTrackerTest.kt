package dev.denlogv.lexislearned

import dev.denlogv.lexislearned.domain.PaceTracker
import dev.denlogv.lexislearned.domain.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaceTrackerTest {
    private fun tracker(vararg timed: Triple<StudyMode, Int, Long>, minSamples: Int = 1) = PaceTracker(minSamples).also { t ->
        timed.forEach { (mode, cards, millis) -> t.record(mode, cards, millis) }
    }

    @Test
    fun nothingIsEstimatedBeforeTheFirstStepWasTimed() {
        val pace = tracker()
        assertNull(pace.estimate(mapOf(StudyMode.TYPE to 5)))
        assertNull(pace.estimate(emptyMap()))
        pace.record(StudyMode.TYPE, 1, 10_000)
        assertEquals(50_000L, pace.estimate(mapOf(StudyMode.TYPE to 5)))
    }

    @Test
    fun aLargerMinimumWaitsForMoreSteps() {
        val pace = tracker(Triple(StudyMode.TYPE, 1, 10_000), Triple(StudyMode.TYPE, 1, 10_000), minSamples = 3)
        assertNull(pace.estimate(mapOf(StudyMode.TYPE to 5)))
        pace.record(StudyMode.TYPE, 1, 10_000)
        assertEquals(50_000L, pace.estimate(mapOf(StudyMode.TYPE to 5)))
        assertEquals(10_000L, tracker(Triple(StudyMode.TYPE, 1, 10_000), minSamples = 0).estimate(mapOf(StudyMode.TYPE to 1)))
    }

    @Test
    fun everyModeKeepsItsOwnPace() {
        val pace = tracker(
            Triple(StudyMode.TYPE, 1, 20_000),
            Triple(StudyMode.TYPE, 1, 10_000),
            Triple(StudyMode.CHECK, 1, 3_000),
        )
        assertEquals(2 * 15_000L + 3 * 3_000L, pace.estimate(mapOf(StudyMode.TYPE to 2, StudyMode.CHECK to 3)))
    }

    @Test
    fun aModeNotMetYetUsesTheAveragePaceOfAllModes() {
        val pace = tracker(Triple(StudyMode.TYPE, 1, 20_000), Triple(StudyMode.CHECK, 1, 10_000), Triple(StudyMode.CHECK, 1, 0))
        assertEquals(2 * 10_000L, pace.estimate(mapOf(StudyMode.SELECT to 2)))
    }

    @Test
    fun aPairStepSharesItsTimeBetweenItsCards() {
        val pace = tracker(Triple(StudyMode.PAIR, 4, 40_000), Triple(StudyMode.PAIR, 2, 20_000))
        assertEquals(10_000L, pace.estimate(mapOf(StudyMode.PAIR to 1)))
    }

    @Test
    fun aLongBreakCountsAsAtMostAMinute() {
        val pace = tracker(Triple(StudyMode.TYPE, 1, 3_600_000), Triple(StudyMode.TYPE, 1, -5), Triple(StudyMode.TYPE, 1, 0))
        assertEquals(PaceTracker.MAX_STEP_MILLIS, pace.estimate(mapOf(StudyMode.TYPE to 3)))
    }

    @Test
    fun anEmptyStepIsIgnoredAndNoStepsLeftTakeNoTime() {
        val pace = tracker(Triple(StudyMode.TYPE, 0, 99_000))
        assertNull(pace.estimate(mapOf(StudyMode.TYPE to 1)))
        pace.record(StudyMode.TYPE, 3, 30_000)
        assertEquals(0L, pace.estimate(emptyMap()))
        assertEquals(10_000L, pace.estimate(mapOf(StudyMode.TYPE to 1)))
    }
}
