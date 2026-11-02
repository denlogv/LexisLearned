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
     * Applies a key press: a number key selects the word in that row, a letter key picks the translation in that row.
     *
     * @param key the key.
     * @param words the ids of the cards in the order of the rows of words.
     * @param translations the ids of the cards in the order of the rows of translations.
     * @return the new state; unchanged for any other key and for a row that does not exist, and as for a tap otherwise.
     */
    fun press(key: StepKey, words: List<Long>, translations: List<Long>): PairState = when (key) {
        is StepKey.Number -> words.getOrNull(key.value - 1)?.let(::pickLeft) ?: this
        is StepKey.Letter -> translations.getOrNull(key.index)?.let(::pickRight) ?: this
        else -> this
    }

    /**
     * Removes the red marking of a wrong pick.
     *
     * @return the new state.
     */
    fun clearWrong(): PairState = copy(wrong = null)
}

/** How many rows have a number key: 1 to 9, and 0 for the tenth. */
private const val NUMBERED_ROWS = 10

/**
 * The name of the key that selects the word in a row.
 *
 * @param row the row, counted from 0.
 * @return "1" to "9", then "0", or null for a row without a key.
 */
fun wordKeyName(row: Int): String? = if (row < NUMBERED_ROWS) ((row + 1) % NUMBERED_ROWS).toString() else null

/**
 * The name of the key that picks the translation in a row.
 *
 * @param row the row, counted from 0.
 * @return "A", "B" and so on, or null for a row without a key.
 */
fun translationKeyName(row: Int): String? = if (row < LETTERS) ('A' + row).toString() else null

/** How many letters there are on the keyboard. */
private const val LETTERS = 26
