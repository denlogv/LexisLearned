package dev.denlogv.lexislearned.ui.study

/**
 * The state of a Pair board: the learner taps a word on the left, then its translation on the right.
 *
 * @property ids the ids of all cards on the board, fillers included.
 * @property matched cards that were matched correctly.
 * @property missed cards involved in at least one wrong match; they are graded as wrong.
 * @property left the word currently selected on the left, if any.
 * @property wrong the translation that was just picked wrongly, shown briefly in red.
 */
data class PairState(
    val ids: List<Long>,
    val matched: Set<Long> = emptySet(),
    val missed: Set<Long> = emptySet(),
    val left: Long? = null,
    val wrong: Long? = null,
) {
    /** True when every card has been matched. */
    val isComplete: Boolean get() = matched.size == ids.size

    /**
     * Selects a word on the left.
     *
     * @param id the card.
     * @return the new state; unchanged if the card is already matched.
     */
    fun pickLeft(id: Long): PairState = if (id in matched) this else copy(left = id)

    /**
     * Picks a translation on the right for the selected word.
     *
     * @param id the card whose translation was tapped.
     * @return the new state: a correct pick matches the pair, a wrong one marks both cards as missed. Unchanged if no word is
     *   selected or the translation is already matched.
     */
    fun pickRight(id: Long): PairState {
        val selected = left
        return when {
            selected == null || id in matched -> this
            selected == id -> copy(matched = matched + id, left = null)
            else -> copy(wrong = id, missed = missed + selected + id)
        }
    }

    /**
     * Removes the red marking of a wrong pick.
     *
     * @return the new state.
     */
    fun clearWrong(): PairState = copy(wrong = null)
}
