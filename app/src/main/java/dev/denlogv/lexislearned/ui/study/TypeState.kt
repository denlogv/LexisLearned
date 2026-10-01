package dev.denlogv.lexislearned.ui.study

import dev.denlogv.lexislearned.domain.Answers
import dev.denlogv.lexislearned.domain.Grade

/**
 * The state of a Type step: the learner types the answer, checks it, then moves on.
 *
 * @property text what was typed so far.
 * @property result null until the answer is checked, then whether it was correct.
 */
data class TypeState(val text: String = "", val result: Boolean? = null) {
    /** True once the answer has been checked. */
    val checked: Boolean get() = result != null

    /** Whether the main button can be pressed: to check something typed, or to move on after checking. */
    val canSubmit: Boolean get() = checked || text.isNotBlank()

    /** The grade for this step: GOOD if the answer was correct, otherwise AGAIN. */
    val grade: Grade get() = if (result == true) Grade.GOOD else Grade.AGAIN

    /**
     * Changes the typed text. After checking the text is locked.
     *
     * @param new the new text.
     * @return the new state.
     */
    fun withText(new: String): TypeState = if (checked) this else copy(text = new)

    /**
     * Checks the typed text against the expected answer; does nothing if already checked or nothing was typed.
     *
     * @param expected the stored answer.
     * @return the new state with the result.
     */
    fun check(expected: String): TypeState = if (checked || text.isBlank()) this else copy(result = Answers.isCorrect(text, expected))
}
