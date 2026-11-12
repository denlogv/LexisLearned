package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.domain.PaceTracker
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode

/**
 * How much time is left, as estimated from the learner's pace in this session.
 *
 * @property session milliseconds to finish the running session.
 * @property scope milliseconds to complete every word of the chapter (or part, or book) the session was started from,
 * including this session.
 */
data class TimeLeft(val session: Long, val scope: Long)

/**
 * Times the steps of one session and estimates how long the rest of it, and of the whole scope, will take.
 *
 * The scope estimate assumes the learner keeps going with the same settings: every session still to come runs every selected
 * mode for the configured number of rounds, and no answer is repeated. The waiting time between spaced sessions is not part of
 * it, as that is not time spent studying.
 *
 * @param clock a monotonic clock in milliseconds; tests replace it.
 * @param laterSteps the steps of the sessions after this one that the scope still needs, per mode and counted per card.
 * @param minSteps how many steps must be timed before anything is estimated.
 */
class SessionTimer(private val clock: () -> Long, private val laterSteps: Map<StudyMode, Int>, minSteps: Int) {
    private val pace = PaceTracker(minSteps)
    private var shownAt = clock()

    /** Starts timing a step: call when it appears on screen. */
    fun stepShown() {
        shownAt = clock()
    }

    /**
     * Stops timing the step that was shown last and adds it to the learner's pace.
     *
     * @param step the step that was answered.
     */
    fun stepDone(step: SessionPlanner.Step) = pace.record(step.mode, step.cardIds.size, clock() - shownAt)

    /**
     * Estimates the time left.
     *
     * @param stepsLeft the steps still to do in this session, per mode and counted per card.
     * @return the estimate, or null while fewer steps than the minimum have been timed.
     */
    fun remaining(stepsLeft: Map<StudyMode, Int>): TimeLeft? {
        val session = pace.estimate(stepsLeft) ?: return null
        val all = (stepsLeft.keys + laterSteps.keys).associateWith { (stepsLeft[it] ?: 0) + (laterSteps[it] ?: 0) }
        return TimeLeft(session, pace.estimate(all) ?: return null)
    }

    /** Where the real clock comes from. */
    companion object {
        /**
         * The current time on a clock that only runs forward, unaffected by changes of the date or time zone.
         *
         * @return milliseconds from an arbitrary starting point.
         */
        fun monotonicMillis(): Long = System.nanoTime() / NANOS_PER_MILLI

        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
