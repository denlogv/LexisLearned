package dev.denlogv.lexislearned.domain

import kotlin.random.Random

/**
 * One planned step of a card.
 *
 * @property mode the mode to practise.
 * @property retry true if this step repeats an earlier wrong answer; repeats do not count toward accuracy.
 */
internal class PlanEntry(val mode: StudyMode, val retry: Boolean = false)

/**
 * The remaining steps of one card in a session, and its score so far.
 *
 * A card's plan is every selected mode once per round. A wrong answer puts the same step back a little later, at most
 * [MAX_RETRIES] times per mode. The score is the share of graded steps answered correctly on the first try.
 *
 * @param id the card's id.
 * @param modes the modes to go through, in the order they should be practised.
 */
internal class CardPlan(val id: Long, modes: List<StudyMode>) {
    private val queue = ArrayDeque(modes.map { PlanEntry(it) })
    private val retries = HashMap<StudyMode, Int>()
    private var graded = 0
    private var firstTry = 0

    /** True if the learner marked this word as already known. */
    var knew = false
        private set

    /** Number of steps left, including pending repeats. */
    val stepsLeft: Int get() = queue.size

    /** True when no steps are left. */
    val isFinished: Boolean get() = queue.isEmpty()

    /** The mode of the next step. Must only be called while the plan is not finished. */
    val nextMode: StudyMode get() = queue.first().mode

    /** Share of graded steps answered correctly on the first try, or null if nothing was graded. */
    val accuracy: Double? get() = if (graded == 0) null else firstTry.toDouble() / graded

    /**
     * Completes the next step if it is the given mode.
     *
     * @param mode the mode that was just practised; ignored if it is not the card's next step.
     * @param correct whether the answer was right, or null for a step that is not a question (an introduction).
     * @param scored whether the answer counts toward accuracy and may trigger a repeat.
     * @return true if this was the card's last step.
     */
    fun complete(mode: StudyMode, correct: Boolean?, scored: Boolean): Boolean {
        val entry = queue.firstOrNull()
        if (entry == null || entry.mode != mode) return false
        queue.removeFirst()
        if (correct != null && scored) score(entry, correct)
        return queue.isEmpty()
    }

    /** Drops all remaining steps because the learner already knows this word. */
    fun markKnown() {
        queue.clear()
        knew = true
    }

    /**
     * Records an answer and schedules a repeat of the step if it was wrong.
     *
     * @param entry the step that was answered.
     * @param correct whether the answer was right.
     */
    private fun score(entry: PlanEntry, correct: Boolean) {
        if (!entry.retry) {
            graded++
            if (correct) firstTry++
        }
        if (!correct) scheduleRetry(entry.mode)
    }

    /**
     * Puts a repeat of a wrong step a couple of steps later, unless it was already repeated the maximum number of times.
     *
     * @param mode the mode to repeat.
     */
    private fun scheduleRetry(mode: StudyMode) {
        val used = retries[mode] ?: 0
        if (used >= MAX_RETRIES) return
        retries[mode] = used + 1
        queue.add(minOf(RETRY_GAP, queue.size), PlanEntry(mode, retry = true))
    }

    companion object {
        /** How many times a wrong step is repeated at most. */
        const val MAX_RETRIES = 2

        /** How many steps later a repeat is placed, so the same mode does not come back immediately. */
        const val RETRY_GAP = 2

        /**
         * Builds the order of modes for one card: Learn first in each round, the other modes in random order.
         *
         * @param modes the selected modes.
         * @param rounds how many times each mode is practised (at least one).
         * @param random source of randomness for the order.
         * @return the modes to practise, in order.
         */
        fun order(modes: Set<StudyMode>, rounds: Int, random: Random): List<StudyMode> {
            val others = (modes - StudyMode.LEARN).toList()
            return List(rounds.coerceAtLeast(1)) {
                listOfNotNull(StudyMode.LEARN.takeIf { it in modes }) + others.shuffled(random)
            }.flatten()
        }
    }
}
