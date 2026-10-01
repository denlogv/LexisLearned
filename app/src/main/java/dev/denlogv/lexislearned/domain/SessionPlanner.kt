package dev.denlogv.lexislearned.domain

import kotlin.random.Random

/**
 * Plans one study session.
 *
 * Every card gets its own plan (see [CardPlan]): each selected mode once per round, Learn first and the others in random
 * order. Steps of different cards are interleaved by a [StepPicker], and a card is finished only when its whole plan is
 * done. The planner knows only card ids and never touches the database, so it is easy to test.
 *
 * @param cardIds the cards in the session.
 * @property modes the selected study modes.
 * @param rounds how many times each word goes through all selected modes.
 * @param random source of randomness for the order of steps.
 */
class SessionPlanner(cardIds: List<Long>, val modes: Set<StudyMode>, rounds: Int, random: Random = Random.Default) {
    /**
     * One thing to show.
     *
     * @property mode the mode to practise.
     * @property cardIds the cards in this step: a single card, except for Pair.
     */
    data class Step(val mode: StudyMode, val cardIds: List<Long>)

    /**
     * What a finished card scored.
     *
     * @property cardId the card.
     * @property accuracy share answered correctly on the first try, or null if nothing was graded.
     * @property knew true if the learner marked the word as already known.
     */
    data class Outcome(val cardId: Long, val accuracy: Double?, val knew: Boolean)

    /** Learn is only a graded flashcard when it is the single selected mode. */
    private val gradedModes: Set<StudyMode> = if (modes == setOf(StudyMode.LEARN)) modes else modes - StudyMode.LEARN
    private val plans = cardIds.map { CardPlan(it, CardPlan.order(modes, rounds, random)) }
    private val byId = plans.associateBy { it.id }
    private val picker = StepPicker(random)
    private val finished = ArrayList<Outcome>()

    /** Steps still to do; a Pair step counts once per card in it. */
    val stepsLeft: Int get() = plans.sumOf { it.stepsLeft }

    /** Cards that still have steps to do. */
    val activeCards: Int get() = plans.count { !it.isFinished }

    /**
     * The next step.
     *
     * @return the step to show, or null when every card has finished its plan.
     */
    fun next(): Step? {
        val active = plans.filter { !it.isFinished }
        return if (active.isEmpty()) null else picker.pick(active)
    }

    /**
     * Records that a Learn step only introduced a card; it is not graded.
     *
     * @param cardId the card that was introduced.
     */
    fun introduced(cardId: Long) = complete(cardId, StudyMode.LEARN, null)

    /**
     * Marks a card as already known: its remaining steps are dropped and it counts as fully known.
     *
     * @param cardId the card the learner already knows.
     */
    fun known(cardId: Long) {
        val plan = byId.getValue(cardId)
        plan.markKnown()
        finished += Outcome(cardId, plan.accuracy, true)
    }

    /**
     * Records the results of a graded step.
     *
     * @param step the step that was shown.
     * @param results whether each card was answered correctly; cards without an entry count as correct.
     */
    fun answered(step: Step, results: Map<Long, Boolean>) {
        for (id in step.cardIds) complete(id, step.mode, results[id] ?: true)
    }

    /**
     * Takes the outcomes of cards that finished since the last call.
     *
     * @return the finished cards' outcomes; empty if none.
     */
    fun drainFinished(): List<Outcome> = finished.toList().also { finished.clear() }

    /**
     * Completes one card's step and records an outcome if it was the card's last.
     *
     * @param cardId the card.
     * @param mode the mode that was practised.
     * @param correct whether the answer was right, or null for an introduction.
     */
    private fun complete(cardId: Long, mode: StudyMode, correct: Boolean?) {
        val plan = byId.getValue(cardId)
        if (plan.complete(mode, correct, mode in gradedModes)) finished += Outcome(cardId, plan.accuracy, plan.knew)
    }

    /** Constants of the planner. */
    companion object {
        /** The most cards a Pair step contains. */
        const val PAIR_BATCH = 5
    }
}
