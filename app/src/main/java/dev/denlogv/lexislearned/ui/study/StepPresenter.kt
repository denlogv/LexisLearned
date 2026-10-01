package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.data.CardEntity
import dev.denlogv.lexislearned.data.DeckRepository
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.SessionPlanner
import dev.denlogv.lexislearned.domain.StudyMode
import kotlin.random.Random

/**
 * What a step shows once its cards are loaded.
 *
 * @property cards the cards on screen: the step's own cards plus fillers for a small Pair board.
 * @property decoyIds ids of the fillers, which are never graded.
 * @property options the answer choices for a Select step; empty for other modes.
 */
class PresentedStep(val cards: List<CardEntity>, val decoyIds: Set<Long>, val options: List<String>)

/**
 * Prepares a planned step for display: looks up its cards, pads a small Pair board with filler cards and builds the answer
 * choices for Select.
 *
 * @param repo where filler cards and wrong answers come from.
 * @param direction which side of a card is the question.
 * @param random shuffles the answer choices.
 */
class StepPresenter(private val repo: DeckRepository, private val direction: Direction, private val random: Random) {
    /**
     * Prepares a step.
     *
     * @param step the planned step.
     * @param cards the session's cards by id.
     * @return the content to show.
     */
    suspend fun present(step: SessionPlanner.Step, cards: Map<Long, CardEntity>): PresentedStep {
        val real = step.cardIds.mapNotNull { cards[it] }
        val fillers = if (step.mode == StudyMode.PAIR && real.size < MIN_PAIR_TILES) fillersFor(real) else emptyList()
        val options = if (step.mode == StudyMode.SELECT) selectOptions(real.first()) else emptyList()
        return PresentedStep(real + fillers, fillers.map { it.id }.toSet(), options)
    }

    /**
     * Picks filler cards so the Pair board has at least [MIN_PAIR_TILES] tiles with distinct answers.
     *
     * @param real the cards that are really being studied.
     * @return the fillers, from the same chapter where possible.
     */
    private suspend fun fillersFor(real: List<CardEntity>): List<CardEntity> {
        val answers = real.map { it.answer(direction) }.toSet()
        val missing = MIN_PAIR_TILES - real.size
        return repo.decoys(real.first(), real.map { it.id }, missing * CANDIDATE_FACTOR)
            .distinctBy { it.answer(direction) }
            .filter { it.answer(direction) !in answers }
            .take(missing)
    }

    /**
     * Builds the answer choices: the right answer among up to three wrong ones, in random order.
     *
     * @param card the word being asked.
     * @return the choices.
     */
    private suspend fun selectOptions(card: CardEntity): List<String> {
        val correct = card.answer(direction)
        val wrong = repo.distractors(card, WRONG_CANDIDATES).map {
            it.answer(direction)
        }.distinct().filter { it != correct }.take(WRONG_ANSWERS)
        return (wrong + correct).shuffled(random)
    }

    private companion object {
        /** Smallest Pair board; smaller groups are padded with filler cards. */
        const val MIN_PAIR_TILES = 4

        /** How many more filler candidates to ask for than needed, since some are dropped as duplicates. */
        const val CANDIDATE_FACTOR = 3

        /** Wrong answers shown with the right one in Select. */
        const val WRONG_ANSWERS = 3

        /** Candidates asked for when choosing wrong answers, since some are dropped as duplicates. */
        const val WRONG_CANDIDATES = 8
    }
}
