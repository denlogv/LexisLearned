package dev.denlogv.lexislearned.domain

/**
 * Learns how fast the learner works during a session and turns that into a time estimate for the steps still to do.
 *
 * The unit is a *card-step*: one card going through one step of one mode. A Pair step with six cards is six card-steps, so
 * the pace of different modes can be compared with the step counts of [SessionPlanner]. Each mode has its own pace, because
 * typing a word takes longer than flipping a card; a mode the learner has not met yet is estimated with their average pace
 * over all modes.
 *
 * Nothing is estimated before [minSamples] card-steps have been timed. One step says little about the learner, so the first
 * estimates are rough and settle as more steps are timed; a larger [minSamples] waits for a steadier pace.
 *
 * @param minSamples how many card-steps must be timed before anything is estimated; values below 1 count as 1.
 */
class PaceTracker(private val minSamples: Int = 1) {
    private val millis = HashMap<StudyMode, Long>()
    private val counts = HashMap<StudyMode, Int>()

    /**
     * Records how long the learner took for a step.
     *
     * A step that took longer than [MAX_STEP_MILLIS] (the phone was put down, the app went to the background) is counted as
     * [MAX_STEP_MILLIS], so one long break does not make the whole estimate useless.
     *
     * @param mode the mode of the step.
     * @param cards how many cards the step contained; the time is shared between them.
     * @param duration how long the step was on screen, in milliseconds.
     */
    fun record(mode: StudyMode, cards: Int, duration: Long) {
        if (cards <= 0) return
        millis.merge(mode, duration.coerceIn(0, MAX_STEP_MILLIS), Long::plus)
        counts.merge(mode, cards, Int::plus)
    }

    /**
     * Estimates how long some card-steps will take at the learner's pace so far.
     *
     * @param steps how many card-steps are left, per mode; modes with no steps may be left out.
     * @return the estimate in milliseconds, or null while fewer than [minSamples] card-steps have been timed.
     */
    fun estimate(steps: Map<StudyMode, Int>): Long? {
        val average = average() ?: return null
        return steps.entries.sumOf { (mode, n) -> n * (paceOf(mode) ?: average) }.toLong()
    }

    /**
     * The learner's average time for a card-step over all modes.
     *
     * @return milliseconds, or null while fewer than [minSamples] card-steps have been timed.
     */
    private fun average(): Double? {
        val total = counts.values.sum()
        return if (total < minSamples.coerceAtLeast(1)) null else millis.values.sum().toDouble() / total
    }

    /**
     * The learner's average time for a card-step of one mode.
     *
     * @param mode the mode.
     * @return milliseconds, or null if the mode has not been timed yet.
     */
    private fun paceOf(mode: StudyMode): Double? = counts[mode]?.let { millis.getValue(mode).toDouble() / it }

    /** Constants of the estimate. */
    companion object {
        /** The longest time one step counts for: a minute, as a longer one is a break and not the learner's pace. */
        const val MAX_STEP_MILLIS = 60_000L
    }
}
