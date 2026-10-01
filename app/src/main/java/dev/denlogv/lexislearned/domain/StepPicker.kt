package dev.denlogv.lexislearned.domain

import kotlin.random.Random

/**
 * Chooses which step of a session comes next.
 *
 * The same card is not shown twice in a row while others remain, a mode different from the previous one is preferred,
 * and Pair (a multi-card step) waits until at least two cards are ready for it.
 *
 * @param random source of randomness for the choice.
 */
internal class StepPicker(private val random: Random) {
    private var lastCard: Long? = null
    private var lastMode: StudyMode? = null

    /**
     * Picks the next step.
     *
     * @param active the cards that still have steps to do; must not be empty.
     * @return the step to show.
     */
    fun pick(active: List<CardPlan>): SessionPlanner.Step {
        val pairReady = active.filter { it.nextMode == StudyMode.PAIR }
        val pick = choose(candidates(active, pairReady))
        val mode = pick.nextMode
        val ids = if (mode == StudyMode.PAIR) pairBatch(pick, pairReady) else listOf(pick.id)
        lastCard = pick.id
        lastMode = mode
        return SessionPlanner.Step(mode, ids)
    }

    /**
     * Narrows the cards that may be shown next: not the previous card if others exist, and no Pair unless it can be a group.
     *
     * @param active the cards that still have steps.
     * @param pairReady the active cards whose next step is Pair.
     * @return the eligible cards; never empty.
     */
    private fun candidates(active: List<CardPlan>, pairReady: List<CardPlan>): List<CardPlan> {
        val notLast = active.filter { it.id != lastCard }.ifEmpty { active }
        val pairOk = pairReady.size >= 2 || notLast.all { it.nextMode == StudyMode.PAIR }
        return notLast.filter { it.nextMode != StudyMode.PAIR || pairOk }.ifEmpty { notLast }
    }

    /**
     * Picks one of the eligible cards at random, preferring one whose mode differs from the previous step.
     *
     * @param candidates the eligible cards; must not be empty.
     * @return the chosen card.
     */
    private fun choose(candidates: List<CardPlan>): CardPlan =
        candidates.filter { it.nextMode != lastMode }.ifEmpty { candidates }.random(random)

    /**
     * Collects the cards for a Pair step: the chosen card plus other cards that are ready for Pair.
     *
     * @param pick the card that was chosen.
     * @param pairReady all cards whose next step is Pair.
     * @return the ids of up to [SessionPlanner.PAIR_BATCH] cards, starting with [pick].
     */
    private fun pairBatch(pick: CardPlan, pairReady: List<CardPlan>): List<Long> {
        val others = pairReady.filter { it !== pick }.shuffled(random).take(SessionPlanner.PAIR_BATCH - 1)
        return (listOf(pick) + others).map { it.id }
    }
}
