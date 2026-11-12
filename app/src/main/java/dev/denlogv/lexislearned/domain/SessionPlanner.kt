package dev.denlogv.lexislearned.domain

import kotlin.random.Random
import kotlinx.serialization.Serializable

/**
 * Plans one study session.
 *
 * Every card gets its own plan (see [CardPlan]): each selected mode once per round, Learn first and the others in random
 * order. Steps of different cards are interleaved by a [StepPicker], and a card is finished only when its whole plan is
 * done. The planner knows only card ids and never touches the database, so it is easy to test.
 *
 * A session can be interrupted: [snapshot] captures its state and [restore] continues it.
 *
 * @property modes the selected study modes.
 * @param plans the plan of every card, finished or not.
 * @param picker chooses which card comes next.
 * @param shown the step that was returned by [next] and not answered yet, or null.
 */
class SessionPlanner private constructor(
    val modes: Set<StudyMode>,
    private val plans: List<CardPlan>,
    private val picker: StepPicker,
    private var shown: Step? = null,
) {
    /**
     * Plans a new session.
     *
     * @param cardIds the cards in the session.
     * @param modes the selected study modes.
     * @param rounds how many times each word goes through all selected modes.
     * @param random source of randomness for the order of steps.
     * @param pairSize the most cards one Pair step contains.
     */
    constructor(
        cardIds: List<Long>,
        modes: Set<StudyMode>,
        rounds: Int,
        random: Random = Random.Default,
        pairSize: Int = DEFAULT_PAIR_SIZE,
    ) : this(modes, cardIds.map { CardPlan(it, CardPlan.order(modes, rounds, random)) }, StepPicker(random, pairSize))

    /**
     * One thing to show.
     *
     * @property mode the mode to practise.
     * @property cardIds the cards in this step: a single card, except for Pair.
     */
    @Serializable
    data class Step(val mode: StudyMode, val cardIds: List<Long>)

    /**
     * What a finished card scored.
     *
     * @property cardId the card.
     * @property accuracy share answered correctly on the first try, or null if nothing was graded.
     * @property knew true if the learner marked the word as already known.
     */
    @Serializable
    data class Outcome(val cardId: Long, val accuracy: Double?, val knew: Boolean)

    /** Learn is only a graded flashcard when it is the single selected mode. */
    private val gradedModes: Set<StudyMode> = if (modes == setOf(StudyMode.LEARN)) modes else modes - StudyMode.LEARN
    private val byId = plans.associateBy { it.id }
    private val finished = ArrayList<Outcome>()

    /** Steps still to do; a Pair step counts once per card in it. */
    val stepsLeft: Int get() = plans.sumOf { it.stepsLeft }

    /** The same steps as [stepsLeft], counted per mode; a mode with no steps left is missing. */
    val stepsLeftByMode: Map<StudyMode, Int> get() = plans.flatMap { it.modesLeft }.groupingBy { it }.eachCount()

    /** Cards that still have steps to do. */
    val activeCards: Int get() = plans.count { !it.isFinished }

    /**
     * The next step.
     *
     * @return the step to show, or null when every card has finished its plan. A step that was restored from a snapshot comes back
     * first.
     */
    fun next(): Step? {
        shown?.let { return it }
        val active = plans.filter { !it.isFinished }
        return (if (active.isEmpty()) null else picker.pick(active)).also { shown = it }
    }

    /**
     * Captures the state of the session.
     *
     * @return everything [restore] needs to continue it; the planner itself is not changed.
     */
    fun snapshot(): PlannerSnapshot = PlannerSnapshot(
        modes = modes,
        pairSize = picker.pairSize,
        plans = plans.filter { !it.isFinished }.map { it.snapshot() },
        lastCard = picker.lastCard,
        lastMode = picker.lastMode,
        shown = shown,
        finished = finished.toList(),
    )

    /**
     * Records that a Learn step only introduced a card; it is not graded.
     *
     * @param cardId the card that was introduced.
     */
    fun introduced(cardId: Long) {
        shown = null
        complete(cardId, StudyMode.LEARN, null)
    }

    /**
     * Marks a card as already known: its remaining steps are dropped and it counts as fully known.
     *
     * @param cardId the card the learner already knows.
     */
    fun known(cardId: Long) {
        shown = null
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
        shown = null
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

    /** Constants of the planner and the way to continue a snapshot. */
    companion object {
        /**
         * Continues a session from a snapshot.
         *
         * @param snapshot what [snapshot] returned.
         * @param random source of randomness for the order of the steps still to come.
         * @param available the cards that still exist and may be studied; the plans of all others are dropped, and so is the step
         * that was on screen if it needs one of them.
         * @return the planner, which gives the same remaining steps per card and the same scores; it has no steps left if none of
         * the cards is available.
         */
        fun restore(snapshot: PlannerSnapshot, random: Random, available: Set<Long>): SessionPlanner {
            val plans = snapshot.plans.filter { it.id in available && it.queue.isNotEmpty() }.map { CardPlan.restore(it) }
            val picker = StepPicker(random, snapshot.pairSize, snapshot.lastCard, snapshot.lastMode)
            val planner = SessionPlanner(snapshot.modes, plans, picker, snapshot.shown?.takeIf { step -> plans.isReadyFor(step) })
            planner.finished += snapshot.finished
            return planner
        }

        /**
         * Tells whether a step can still be shown: each of its cards has a plan whose next step is the step's mode.
         *
         * @receiver the plans that are restored.
         * @param step the step that was on screen.
         * @return true if the step matches the plans.
         */
        private fun List<CardPlan>.isReadyFor(step: Step): Boolean =
            step.cardIds.all { id -> firstOrNull { it.id == id }?.nextMode == step.mode }

        /** The most cards a Pair step contains unless the learner chooses otherwise. */
        const val DEFAULT_PAIR_SIZE = 6

        /**
         * The steps that words still have to do in sessions not planned yet, assuming every such session is a plain one:
         * each selected mode [rounds] times and no repeats of wrong answers.
         *
         * @param modes the selected study modes.
         * @param rounds how many times each word goes through all selected modes in one session.
         * @param wordSessions how many sessions of one word are still to come, summed over all words.
         * @return the steps per mode, counted per card as [stepsLeftByMode] does.
         */
        fun stepsOfSessions(modes: Set<StudyMode>, rounds: Int, wordSessions: Int): Map<StudyMode, Int> =
            modes.associateWith { rounds.coerceAtLeast(1) * wordSessions }
    }
}
