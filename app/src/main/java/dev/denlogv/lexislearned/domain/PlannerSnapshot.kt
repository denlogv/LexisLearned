package dev.denlogv.lexislearned.domain

import kotlinx.serialization.Serializable

/**
 * The state of one card's plan: what is still to do and how it went so far.
 *
 * @property id the card.
 * @property queue the remaining steps in order, including pending repeats of wrong answers.
 * @property retries how many times a wrong step of each mode was already repeated.
 * @property graded number of graded steps answered on the first try so far.
 * @property firstTry how many of those were right.
 * @property knew true if the learner marked the word as known.
 */
@Serializable
data class CardPlanSnapshot(
    val id: Long,
    val queue: List<PlanEntry>,
    val retries: Map<StudyMode, Int>,
    val graded: Int,
    val firstTry: Int,
    val knew: Boolean,
)

/**
 * Everything a [SessionPlanner] needs to continue a session where it was left: the words with steps left, the order choices made so
 * far and the step on screen. It holds only ids and counts, so it can be written to a file and read back.
 *
 * Words that already finished are not part of it unless their outcome was not collected yet ([finished]).
 *
 * @property modes the selected study modes.
 * @property pairSize the most cards one Pair step contains.
 * @property plans the plans of the words that still have steps to do.
 * @property lastCard the card of the previous step, so the same card is not shown twice in a row.
 * @property lastMode the mode of the previous step, so a different mode is preferred next.
 * @property shown the step that was on screen and not answered yet, or null.
 * @property finished outcomes of words that finished since the last collection by [SessionPlanner.drainFinished].
 */
@Serializable
data class PlannerSnapshot(
    val modes: Set<StudyMode>,
    val pairSize: Int,
    val plans: List<CardPlanSnapshot>,
    val lastCard: Long?,
    val lastMode: StudyMode?,
    val shown: SessionPlanner.Step?,
    val finished: List<SessionPlanner.Outcome>,
)
